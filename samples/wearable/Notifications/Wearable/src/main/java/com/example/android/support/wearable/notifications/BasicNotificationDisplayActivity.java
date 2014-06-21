package com.example.android.support.wearable.notifications;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

/**
 * Custom display activity for a sample notification.
 */
public class BasicNotificationDisplayActivity extends Activity {
    public static final String EXTRA_TITLE = "title";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_display);

        String title = getIntent().getStringExtra(EXTRA_TITLE);

        ((TextView) findViewById(R.id.title)).setText(title);
    }
}
