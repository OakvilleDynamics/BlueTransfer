package org.ratchetrockers1706.bluetransfer;

import android.Manifest;
import android.content.Intent;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends AppCompatActivity {

    Intent mServiceIntent = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                0);
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                1);

        // Start background bluetooth service
        mServiceIntent = new Intent(this, BluetoothServerService.class);
        Log.i(this.getLocalClassName(), "Got an intent " + mServiceIntent.getClass().getName());
        startService(mServiceIntent);

    }
}
