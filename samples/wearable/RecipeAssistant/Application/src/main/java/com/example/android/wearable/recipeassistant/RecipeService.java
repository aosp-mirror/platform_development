package com.example.android.wearable.recipeassistant;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.NotificationCompat;

import java.util.ArrayList;

public class RecipeService extends Service {
    private NotificationManagerCompat mNotificationManager;
    private Binder mBinder = new LocalBinder();
    private Recipe mRecipe;

    public class LocalBinder extends Binder {
        RecipeService getService() {
            return RecipeService.this;
        }
    }

    @Override
    public void onCreate() {
        mNotificationManager = NotificationManagerCompat.from(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction().equals(Constants.ACTION_START_COOKING)) {
            createNotification(intent);
            return START_STICKY;
        }
        return START_NOT_STICKY;
    }

    private void createNotification(Intent intent) {
        mRecipe = Recipe.fromBundle(intent.getBundleExtra(Constants.EXTRA_RECIPE));
        ArrayList<Notification> notificationPages = new ArrayList<Notification>();

        int stepCount = mRecipe.recipeSteps.size();

        for (int i = 0; i < stepCount; ++i) {
            Recipe.RecipeStep recipeStep = mRecipe.recipeSteps.get(i);
            NotificationCompat.BigTextStyle style = new NotificationCompat.BigTextStyle();
            style.bigText(recipeStep.stepText);
            style.setBigContentTitle(String.format(
                    getResources().getString(R.string.step_count), i + 1, stepCount));
            style.setSummaryText("");
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
            builder.setStyle(style);
            notificationPages.add(builder.build());
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        if (mRecipe.recipeImage != null) {
            Bitmap recipeImage = Bitmap.createScaledBitmap(
                    AssetUtils.loadBitmapAsset(this, mRecipe.recipeImage),
                    Constants.NOTIFICATION_IMAGE_WIDTH, Constants.NOTIFICATION_IMAGE_HEIGHT, false);
            builder.setLargeIcon(recipeImage);
        }
        builder.setContentTitle(mRecipe.titleText);
        builder.setContentText(mRecipe.summaryText);
        builder.setSmallIcon(R.mipmap.ic_notification_recipe);

        Notification notification = builder
                .extend(new NotificationCompat.WearableExtender()
                        .addPages(notificationPages))
                .build();
        mNotificationManager.notify(Constants.NOTIFICATION_ID, notification);
    }
}
