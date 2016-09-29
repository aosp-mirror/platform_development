package com.example.android.wearable.watchface.provider;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Intent;
import android.support.wearable.complications.ProviderUpdateRequester;
import android.util.Log;

/**
 * Simple {@link IntentService} subclass for asynchronously requesting an update for the random
 * number complication (triggered via TapAction on complication).
 */
public class UpdateComplicationDataService extends IntentService {

    private static final String TAG = "UpdateCompService";

    public static final String ACTION_UPDATE_COMPLICATION =
            "com.example.android.wearable.watchface.provider.action.UPDATE_COMPLICATION";

    public static final String EXTRA_COMPLICATION_ID =
            "com.example.android.wearable.watchface.provider.action.COMPLICATION_ID";

    public UpdateComplicationDataService() {
        super("UpdateComplicationDataService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        if (intent != null) {

            final String action = intent.getAction();

            if (ACTION_UPDATE_COMPLICATION.equals(action)) {

                int complicationId = intent.getIntExtra(EXTRA_COMPLICATION_ID, -1);
                handleActionUpdateComplicationData(complicationId);
            }
        }
    }

    /**
     * Handle action UpdateComplicationData in the provided background thread with the provided
     * parameters.
     */
    private void handleActionUpdateComplicationData(int complicationId) {

        Log.d(TAG, "Complication id to update via service: " + complicationId);

        ComponentName componentName =
                new ComponentName(getApplicationContext(), RandomNumberProviderService.class);

        ProviderUpdateRequester providerUpdateRequester =
                new ProviderUpdateRequester(getApplicationContext(), componentName);

        if (complicationId > 0) {
            // This method only updates the specific complication tapped on the watch, if you
            // wanted to update all active complications associated with your data, you would
            // call providerUpdateRequester.requestUpdateAll().
            providerUpdateRequester.requestUpdate(complicationId);
        }
    }
}