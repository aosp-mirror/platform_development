package com.example.android.wearable.elizachat;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class MainActivity extends Activity {

    @SuppressWarnings("unused")
    private static final String TAG = "MainActivity";

    public static final String EXTRA_MESSAGE = "message";

    public static final String ACTION_NOTIFY = "com.example.android.wearable.elizachat.NOTIFY";

    public static final String ACTION_GET_CONVERSATION
            = "com.example.android.wearable.elizachat.CONVERSATION";

    private BroadcastReceiver mReceiver;

    private TextView mHistoryView;

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        setContentView(R.layout.activity_main);
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                processMessage(intent);
            }
        };
        mHistoryView = (TextView) findViewById(R.id.history);
        startResponderService();
    }

    private void startResponderService() {
        Intent serviceIntent = new Intent(ResponderService.ACTION_INCOMING);
        startService(serviceIntent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver,
                new IntentFilter(ACTION_NOTIFY));
        mHistoryView.setText("");
        Intent serviceIntent = new Intent(ACTION_GET_CONVERSATION);
        startService(serviceIntent);

    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
        super.onPause();
    }

    private void processMessage(Intent intent) {
        String text = intent.getStringExtra(EXTRA_MESSAGE);
        if (!TextUtils.isEmpty(text)) {
            mHistoryView.append("\n" + text);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_stop_service:
                stopService(new Intent(this, ResponderService.class));
                finish();
                break;
        }
        return true;
    }
}
