/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.hcgallery;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RemoteViews;

/** This is the main "launcher" activity.
 * When running on a "large" or larger screen, this activity displays both the
 * TitlesFragments and the Content Fragment. When on a smaller screen size, this
 * activity displays only the TitlesFragment. In which case, selecting a list
 * item opens the ContentActivity, holds only the ContentFragment. */
public class MainActivity extends Activity implements TitlesFragment.OnItemSelectedListener {

    private Animator mCurrentTitlesAnimator;
    private String[] mToggleLabels = {"Show Titles", "Hide Titles"};
    private static final int NOTIFICATION_DEFAULT = 1;
    private static final String ACTION_DIALOG = "com.example.android.hcgallery.action.DIALOG";
    private int mThemeId = -1;
    private boolean mDualFragments = false;
    private boolean mTitlesHidden = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(savedInstanceState != null) {
            if (savedInstanceState.getInt("theme", -1) != -1) {
              mThemeId = savedInstanceState.getInt("theme");
              this.setTheme(mThemeId);
            }
            mTitlesHidden = savedInstanceState.getBoolean("titlesHidden");
        }

        setContentView(R.layout.main);

        ActionBar bar = getActionBar();
        bar.setDisplayShowTitleEnabled(false);

        ContentFragment frag = (ContentFragment) getFragmentManager()
                .findFragmentById(R.id.content_frag);
        if (frag != null) mDualFragments = true;
        
        if (mTitlesHidden) {
            getFragmentManager().beginTransaction()
                    .hide(getFragmentManager().findFragmentById(R.id.titles_frag)).commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        // If the device doesn't support camera, remove the camera menu item
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            menu.removeItem(R.id.menu_camera);
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If not showing both fragments, remove the "toggle titles" menu item
        if (!mDualFragments) {
            menu.removeItem(R.id.menu_toggleTitles);
        } else {
            menu.findItem(R.id.menu_toggleTitles).setTitle(mToggleLabels[mTitlesHidden ? 0 : 1]);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_camera:
            Intent intent = new Intent(this, CameraActivity.class);
            intent.putExtra("theme", mThemeId);
            startActivity(intent);
            return true;

        case R.id.menu_toggleTitles:
            toggleVisibleTitles();
            return true;

        case R.id.menu_toggleTheme:
            if (mThemeId == R.style.AppTheme_Dark) {
                mThemeId = R.style.AppTheme_Light;
            } else {
                mThemeId = R.style.AppTheme_Dark;
            }
            this.recreate();
            return true;

        case R.id.menu_showDialog:
            showDialog("This is indeed an awesome dialog.");
            return true;

        case R.id.menu_showStandardNotification:
            showNotification(false);
            return true;

        case R.id.menu_showCustomNotification:
            showNotification(true);
            return true;

        default:
            return super.onOptionsItemSelected(item);
        }
    }

    /** Respond to the "toogle titles" item in the action bar */
    public void toggleVisibleTitles() {
        // Use these for custom animations.
        final FragmentManager fm = getFragmentManager();
        final TitlesFragment f = (TitlesFragment) fm
                .findFragmentById(R.id.titles_frag);
        final View titlesView = f.getView();

        // Determine if we're in portrait, and whether we're showing or hiding the titles
        // with this toggle.
        final boolean isPortrait = getResources().getConfiguration().orientation ==
                Configuration.ORIENTATION_PORTRAIT;

        final boolean shouldShow = f.isHidden() || mCurrentTitlesAnimator != null;

        // Cancel the current titles animation if there is one.
        if (mCurrentTitlesAnimator != null)
            mCurrentTitlesAnimator.cancel();

        // Begin setting up the object animator. We'll animate the bottom or right edge of the
        // titles view, as well as its alpha for a fade effect.
        ObjectAnimator objectAnimator = ObjectAnimator.ofPropertyValuesHolder(
                titlesView,
                PropertyValuesHolder.ofInt(
                        isPortrait ? "bottom" : "right",
                        shouldShow ? getResources().getDimensionPixelSize(R.dimen.titles_size)
                                   : 0),
                PropertyValuesHolder.ofFloat("alpha", shouldShow ? 1 : 0)
        );

        // At each step of the animation, we'll perform layout by calling setLayoutParams.
        final ViewGroup.LayoutParams lp = titlesView.getLayoutParams();
        objectAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                // *** WARNING ***: triggering layout at each animation frame highly impacts
                // performance so you should only do this for simple layouts. More complicated
                // layouts can be better served with individual animations on child views to
                // avoid the performance penalty of layout.
                if (isPortrait) {
                    lp.height = (Integer) valueAnimator.getAnimatedValue();
                } else {
                    lp.width = (Integer) valueAnimator.getAnimatedValue();
                }
                titlesView.setLayoutParams(lp);
            }
        });

        if (shouldShow) {
            fm.beginTransaction().show(f).commit();
            objectAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animator) {
                    mCurrentTitlesAnimator = null;
                    mTitlesHidden = false;
                    invalidateOptionsMenu();
                }
            });

        } else {
            objectAnimator.addListener(new AnimatorListenerAdapter() {
                boolean canceled;

                @Override
                public void onAnimationCancel(Animator animation) {
                    canceled = true;
                    super.onAnimationCancel(animation);
                }

                @Override
                public void onAnimationEnd(Animator animator) {
                    if (canceled)
                        return;
                    mCurrentTitlesAnimator = null;
                    fm.beginTransaction().hide(f).commit();
                    mTitlesHidden = true;
                    invalidateOptionsMenu();
                }
            });
        }

        // Start the animation.
        objectAnimator.start();
        mCurrentTitlesAnimator = objectAnimator;

        // Manually trigger onNewIntent to check for ACTION_DIALOG.
        onNewIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (ACTION_DIALOG.equals(intent.getAction())) {
            showDialog(intent.getStringExtra(Intent.EXTRA_TEXT));
        }
    }

    void showDialog(String text) {
        // DialogFragment.show() will take care of adding the fragment
        // in a transaction.  We also want to remove any currently showing
        // dialog, so make our own transaction and take care of that here.
        FragmentTransaction ft = getFragmentManager().beginTransaction();

        DialogFragment newFragment = MyDialogFragment.newInstance(text);

        // Show the dialog.
        newFragment.show(ft, "dialog");
    }

    void showNotification(boolean custom) {
        final Resources res = getResources();
        final NotificationManager notificationManager = (NotificationManager) getSystemService(
                NOTIFICATION_SERVICE);

        Notification.Builder builder = new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_notify_example)
                .setAutoCancel(true)
                .setTicker(getString(R.string.notification_text))
                .setContentIntent(getDialogPendingIntent("Tapped the notification entry."));

        if (custom) {
            // Sets a custom content view for the notification, including an image button.
            RemoteViews layout = new RemoteViews(getPackageName(), R.layout.notification);
            layout.setTextViewText(R.id.notification_title, getString(R.string.app_name));
            layout.setOnClickPendingIntent(R.id.notification_button,
                    getDialogPendingIntent("Tapped the 'dialog' button in the notification."));
            builder.setContent(layout);

            // Notifications in Android 3.0 now have a standard mechanism for displaying large
            // bitmaps such as contact avatars. Here, we load an example image and resize it to the
            // appropriate size for large bitmaps in notifications.
            Bitmap largeIconTemp = BitmapFactory.decodeResource(res,
                    R.drawable.notification_default_largeicon);
            Bitmap largeIcon = Bitmap.createScaledBitmap(
                    largeIconTemp,
                    res.getDimensionPixelSize(android.R.dimen.notification_large_icon_width),
                    res.getDimensionPixelSize(android.R.dimen.notification_large_icon_height),
                    false);
            largeIconTemp.recycle();

            builder.setLargeIcon(largeIcon);

        } else {
            builder
                    .setNumber(7) // An example number.
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(getString(R.string.notification_text));
        }

        notificationManager.notify(NOTIFICATION_DEFAULT, builder.getNotification());
    }

    PendingIntent getDialogPendingIntent(String dialogText) {
        return PendingIntent.getActivity(
                this,
                dialogText.hashCode(), // Otherwise previous PendingIntents with the same
                                       // requestCode may be overwritten.
                new Intent(ACTION_DIALOG)
                        .putExtra(Intent.EXTRA_TEXT, dialogText)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                0);
    }

    @Override
    public void onSaveInstanceState (Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("theme", mThemeId);
        outState.putBoolean("titlesHidden", mTitlesHidden);
    }

    /** Implementation for TitlesFragment.OnItemSelectedListener.
     * When the TitlesFragment receives an onclick event for a list item,
     * it's passed back to this activity through this method so that we can
     * deliver it to the ContentFragment in the manner appropriate */
    public void onItemSelected(int category, int position) {
      if (!mDualFragments) {
          // If showing only the TitlesFragment, start the ContentActivity and
          // pass it the info about the selected item
          Intent intent = new Intent(this, ContentActivity.class);
          intent.putExtra("category", category);
          intent.putExtra("position", position);
          intent.putExtra("theme", mThemeId);
          startActivity(intent);
      } else {
          // If showing both fragments, directly update the ContentFragment
          ContentFragment frag = (ContentFragment) getFragmentManager()
                  .findFragmentById(R.id.content_frag);
          frag.updateContentAndRecycleBitmap(category, position);
      }
    }


    /** Dialog implementation that shows a simple dialog as a fragment */
    public static class MyDialogFragment extends DialogFragment {

        public static MyDialogFragment newInstance(String title) {
            MyDialogFragment frag = new MyDialogFragment();
            Bundle args = new Bundle();
            args.putString("text", title);
            frag.setArguments(args);
            return frag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            String text = getArguments().getString("text");

            return new AlertDialog.Builder(getActivity())
                    .setTitle("A Dialog of Awesome")
                    .setMessage(text)
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                }
                            }
                    )
                    .create();
        }
    }
}
