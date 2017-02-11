package com.example.android.wearable.wear.wearnotifications;

import android.app.Notification;

/**
 * Controller used to instruct main activity to update {@link Notification} based on changes in
 * the {@link CustomRecyclerAdapter} (item selected) which is tied to the
 * {@link android.support.wearable.view.WearableRecyclerView}.
 */

public class Controller {

    private StandaloneMainActivity mView;

    Controller(StandaloneMainActivity standaloneMainActivity) {
        mView = standaloneMainActivity;
    }

    public void itemSelected(String notificationStyleSelected) {
        mView.itemSelected(notificationStyleSelected);
    }
}