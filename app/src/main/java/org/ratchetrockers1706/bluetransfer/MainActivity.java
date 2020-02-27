package org.ratchetrockers1706.bluetransfer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    Intent mServiceIntent = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Start background bluetooth service
        mServiceIntent = new Intent(this, BluetoothServerService.class);
        Log.i(this.getLocalClassName(), "Got an intent " + mServiceIntent.getClass().getName());
        startService(mServiceIntent);

    }

    // Handling the received Intents for the "my-integer" event
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Extract data included in the Intent
            String theMessage = intent.getStringExtra("message");
            Log.i(this.getClass().getSimpleName(), "WOOHOO!!  Got a message: " + theMessage);
            ((TextView)findViewById(R.id.messageText)).setText(theMessage);
        }
    };

    @Override
    protected void onPause() {
        // Unregister since the activity is not visible
        LocalBroadcastManager.getInstance(this)
                .unregisterReceiver(mMessageReceiver);
        super.onPause();
    }
    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(mMessageReceiver,
                        new IntentFilter("scouting-message"));
    }
}
