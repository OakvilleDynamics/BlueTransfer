package org.ratchetrockers1706.bluetransfer;

import android.app.IntentService;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.util.UUID;

public class BluetoothServerService extends IntentService {

    private class ToastRunner implements Runnable {
        private String message;
        public ToastRunner(String message) {
            this.message = message;
        }

        @Override
        public void run() {
            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
        }
    }

    private class ProcessThread extends Thread {
        private BluetoothSocket socket = null;

        public ProcessThread(BluetoothSocket sock) {
            this.socket = sock;
        }

        @Override
        public void run() {
            String input = null;
            int emptyDirCount = 0;
            PrintWriter out = null;
            BufferedReader in = null;
            try {
                // Read command from client
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
                while (socket.isConnected() && ((input = in.readLine()) != null)) {
                    Log.e(BluetoothServerService.class.getName(), "Received: " + input);
                    // process command
                    if ("list".equalsIgnoreCase(input)) {
                        File myDir = getDataDirectory();
                        File files[] = myDir.listFiles();
                        for (File file : files) {
                            out.print(file.length());
                            out.print(" ");
                            out.print(md5(readFile(file)));
                            out.print(" ");
                            out.println(file.getName());
                        }
                        out.println("---");
                        out.flush();
                        if (files == null || files.length == 0) {
                            emptyDirCount++;
                            if (emptyDirCount > 5) {
                                emptyDirCount = 0;
                                return;
                            }
                        }
                    } else if (input.toLowerCase().startsWith("get ")) {
                        File myDir = getDataDirectory();
                        String filename = input.substring(4).trim();
                        File inFile = new File(myDir, filename);
                        if (inFile.exists()) {
                            out.print(readFile(inFile));
                            out.flush();
                        } else {
                            out.println("0");
                        }
                    } else if (input.toLowerCase().startsWith("delete ")) {
                        File myDir = getDataDirectory();
                        String filename = input.substring(6).trim();
                        File inFile = new File(myDir, filename);
                        if (inFile.exists()) {
                            inFile.delete();
                        }
                    } else if ("clear".equalsIgnoreCase(input)) {
                        File myDir = getDataDirectory();
                        File[] files = myDir.listFiles();
                        for (File f : files) {
                            f.delete();
                        }
                    } else if (input.toLowerCase().startsWith("toast ")) {
                        Log.i(BluetoothServerService.class.getName(), "Toast: " + input);
                        myHandler.post(new ToastRunner(input.substring(6).trim()));
                    } else {
                        out.println("What?");
                        out.flush();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                //Clean up
                if (out != null) {
                    out.flush();
                    out.close();
                }
                try {
                    if (in != null) {
                        in.close();
                    }
                } catch (IOException ioe) {

                }
                try {
                    if (socket != null) {
                        socket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private final BluetoothServerSocket mmServerSocket;
    private Handler myHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        myHandler = new Handler();
    }

    public void run() {
        BluetoothSocket socket = null;
        // Keep listening until exception occurs or a socket is returned.
        Log.i(BluetoothServerService.class.getName(),"Waiting for connections.");
        while (true) {
            try {
                if (mmServerSocket != null) {
                    socket = mmServerSocket.accept();
                    Log.i(BluetoothServerService.class.getName(), "Socket accepted");
                }
            } catch (IOException e) {
                Log.e(BluetoothServerService.class.getName(), "Socket's accept() method failed", e);
                break;
            }

            if (socket != null) {
                // A connection was accepted. Perform work associated with
                // the connection in a separate thread.
                manageMyConnectedSocket(socket);
                socket = null;
            }
        }
    }

    private void manageMyConnectedSocket(BluetoothSocket socket) {
        ProcessThread p = new ProcessThread(socket);
        p.start();
    }

    // Closes the connect socket and causes the thread to finish.
    public void cancel() {
        try {
            mmServerSocket.close();
        } catch (IOException e) {
            Log.e(BluetoothServerService.class.getName(), "Could not close the connect socket", e);
        }
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {
        run();
    }

    public BluetoothServerService() {
        super("BluetoothServerService");
        // Use a temporary object that is later assigned to mmServerSocket
        // because mmServerSocket is final.
        BluetoothServerSocket tmp = null;
        Log.i(BluetoothServerService.class.getName(), "Starting BluetoothServerService service.");
        try {
            // MY_UUID is the app's UUID string, also used by the client code.
            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            //String UUID = "00001101-0000-1000-8000-00805F9B34FB";
            String knownUUID = "00001073-0000-1000-8000-00805F9B34F7";
            //String knownServiceName = "FRC1706";
            String knownServiceName = "TTTService";
            tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(knownServiceName, UUID.fromString(knownUUID));
            Log.i(BluetoothServerService.class.getName(), "Listening...");
        } catch (IOException e) {
            Log.e(BluetoothServerService.class.getName(), "Socket's listen() method failed", e);
        }
        mmServerSocket = tmp;
    }
    private static String hex(byte[] bytes) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < bytes.length; i++) {
            String s = Integer.toHexString(0x0ff & bytes[i]);
            if (s.length() == 0) {
                sb.append("00");
            } else if (s.length() == 1) {
                sb.append("0").append(s);
            } else {
                sb.append(s);
            }
        }
        return sb.toString();
    }

    private static String md5(String contents) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(contents.getBytes());
            return hex(md5.digest());
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private static String readFile(File f) {
        try  {
            FileReader reader = new FileReader(f);
            char[] buffer = new char[(int)f.length()];
            reader.read(buffer, 0, (int)f.length());
            reader.close();
            return new String(buffer);
        } catch (IOException e) {
            Log.e(BluetoothServerService.class.getName(), "Unable to read file " + f.getName(), e);
        }
        return "";
    }

    public static File getDataDirectory() {
        File directory = Environment.getExternalStorageDirectory();
        File myDir = new File(directory + "/ScoutingData");
        myDir.mkdirs();
        return myDir;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }
}
