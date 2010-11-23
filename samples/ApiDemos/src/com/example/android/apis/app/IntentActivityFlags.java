package com.example.android.apis.app;

import com.example.android.apis.R;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

/**
 * Example of various Intent flags to modify the activity stack.
 */
public class IntentActivityFlags extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.intent_activity_flags);

        // Watch for button clicks.
        Button button = (Button)findViewById(R.id.flag_activity_clear_task);
        button.setOnClickListener(mFlagActivityClearTaskListener);
        button = (Button)findViewById(R.id.flag_activity_clear_task_pi);
        button.setOnClickListener(mFlagActivityClearTaskPIListener);
    }

    /**
     * This creates an array of Intent objects representing the back stack
     * for a user going into the Views/Lists API demos.
     */
//BEGIN_INCLUDE(intent_array)
    private Intent[] buildIntentsToViewsLists() {
        // We will use FLAG_ACTIVITY_CLEAR_TASK to complete replace our
        // current task with a new Intent.
        Intent[] intents = new Intent[3];

        // The main activity started from launcher is action MAIN and
        // category LAUNCHER; we want to match that.
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        // We will use FLAG_ACTIVITY_CLEAR_TASK to completely replace our
        // current task with a new Intent.
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        // Set the actual activity to launch.
        intent.setClass(IntentActivityFlags.this, com.example.android.apis.ApiDemos.class);
        intents[0] = intent;

        intent = new Intent(Intent.ACTION_MAIN);
        intent.setClass(IntentActivityFlags.this, com.example.android.apis.ApiDemos.class);
        intent.putExtra("com.example.android.apis.Path", "Views");
        intents[1] = intent;

        intent = new Intent(Intent.ACTION_MAIN);
        intent.setClass(IntentActivityFlags.this, com.example.android.apis.ApiDemos.class);
        intent.putExtra("com.example.android.apis.Path", "Views/Lists");

        intents[2] = intent;
        return intents;
    }
//END_INCLUDE(intent_array)

    private OnClickListener mFlagActivityClearTaskListener = new OnClickListener() {
        public void onClick(View v) {
            startActivities(buildIntentsToViewsLists());
        }
    };

    private OnClickListener mFlagActivityClearTaskPIListener = new OnClickListener() {
        public void onClick(View v) {
            Context context = IntentActivityFlags.this;
//BEGIN_INCLUDE(pending_intent)
            PendingIntent pi = PendingIntent.getActivities(context, 0,
                    buildIntentsToViewsLists(), PendingIntent.FLAG_UPDATE_CURRENT);
//END_INCLUDE(pending_intent)
            try {
                pi.send();
            } catch (CanceledException e) {
                Log.w("IntentActivityFlags", "Failed sending PendingIntent", e);
            }
        }
    };
}
