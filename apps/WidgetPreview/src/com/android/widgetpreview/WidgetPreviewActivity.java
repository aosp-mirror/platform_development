/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.widgetpreview;

import android.app.Activity;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class WidgetPreviewActivity extends Activity implements OnClickListener {

    private static final String LOG_TAG = "WidgetPreviewActivity";
    private static final boolean DEBUG = true;
    private static final int APPWIDGET_HOST_ID = 2048;
    private static final int REQUEST_WIDGET = 0;
    private static final int REQUEST_CONFIGURE = 1;

    private AppWidgetHost mAppWidgetHost = null;
    private FrameLayout mAppWidgetFrame = null;
    private AppWidgetHostView mAppWidgetView = null;
    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private String mAppWidgetName;
    private int mPreviewWidth;
    private int mPreviewHeight;

    private Button mSnapshotButton = null;
    private Button mEmailButton = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAppWidgetFrame = (FrameLayout)findViewById(R.id.main_frame);
        mSnapshotButton = (Button)findViewById(R.id.snapshot_button);
        mSnapshotButton.setOnClickListener(this);
        mEmailButton = (Button)findViewById(R.id.email_button);
        mEmailButton.setOnClickListener(this);

        mAppWidgetHost = new AppWidgetHost(getApplicationContext(), APPWIDGET_HOST_ID);

        final Object retainedObj = getLastNonConfigurationInstance();
        if (retainedObj instanceof AppWidgetProviderInfo) {
            AppWidgetProviderInfo info = (AppWidgetProviderInfo) retainedObj;
            int id = mAppWidgetHost.allocateAppWidgetId();
            AppWidgetManager.getInstance(getBaseContext()).bindAppWidgetIdIfAllowed(id, info.provider);
            setAppWidget(id);
        } else {
            startChooseActivity();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mAppWidgetHost.startListening();
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        AppWidgetProviderInfo info = AppWidgetManager.getInstance(
                getBaseContext()).getAppWidgetInfo(mAppWidgetId);
        return info;
    }

    private void startChooseActivity() {
        int id = mAppWidgetHost.allocateAppWidgetId();
        Intent selectIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_PICK);
        selectIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
        startActivityForResult(selectIntent, REQUEST_WIDGET);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_WIDGET) {
            if (data != null) {
                int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
                if (data.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_ID)) {
                    appWidgetId = data.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID);
                }

                if (resultCode == RESULT_OK) {
                    setAppWidget(appWidgetId);
                } else {
                    mAppWidgetHost.deleteAppWidgetId(appWidgetId);
                    finish();
                }
            } else {
                finish();
            }
        } else if (requestCode == REQUEST_CONFIGURE) {
            if (data != null) {
                int appWidgetId = data.getExtras().getInt(
                        AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
                if (resultCode == RESULT_OK) {
                    finishSetAppWidget(appWidgetId);
                } else {
                    mAppWidgetHost.deleteAppWidgetId(appWidgetId);
                }
            }
        }
    }

    private void setAppWidget(int appWidgetId) {
        if (mAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            mAppWidgetHost.deleteAppWidgetId(mAppWidgetId);
        }

        /* Check for configuration */
        AppWidgetProviderInfo providerInfo =
            AppWidgetManager.getInstance(getBaseContext()).getAppWidgetInfo(appWidgetId);

        if (providerInfo.configure != null) {
            Intent configureIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
            configureIntent.setComponent(providerInfo.configure);
            configureIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);

            if (configureIntent != null) {
                try {
                    startActivityForResult(configureIntent, REQUEST_CONFIGURE);
                } catch (ActivityNotFoundException e) {
                    Log.d(LOG_TAG, "Configuration activity not found: " + e);
                    Toast errorToast = Toast.makeText(
                            getBaseContext(), R.string.configure_error, Toast.LENGTH_SHORT);
                    errorToast.show();
                }
            }
        } else {
            finishSetAppWidget(appWidgetId);
        }
    }

    private void finishSetAppWidget(int appWidgetId) {
        AppWidgetProviderInfo providerInfo =
            AppWidgetManager.getInstance(getBaseContext()).getAppWidgetInfo(appWidgetId);
        if (providerInfo != null) {
            mAppWidgetView =
                    mAppWidgetHost.createView(getBaseContext(), appWidgetId, providerInfo);

            int [] dimensions =
                    getLauncherCellDimensions(providerInfo.minWidth, providerInfo.minHeight);

            mPreviewWidth = dimensions[0];
            mPreviewHeight = dimensions[1];

            mAppWidgetName =
                AppWidgetManager.getInstance(getBaseContext()).getAppWidgetInfo(appWidgetId).label;
            mAppWidgetName = mAppWidgetName.replaceAll("[^a-zA-Z0-9]", "_");

            ViewGroup.LayoutParams p = new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            mAppWidgetView.setLayoutParams(p);
            mAppWidgetFrame.removeAllViews();
            mAppWidgetHost.deleteAppWidgetId(mAppWidgetId);
            mAppWidgetFrame.addView(mAppWidgetView, mPreviewWidth, mPreviewHeight);
            mAppWidgetId = appWidgetId;
        }
    }

    // Taken from CellLayout.java
    public int[] getLauncherCellDimensions(int width, int height) {
        // Always assume we're working with the smallest span to make sure we
        // reserve enough space in both orientations.
        Resources resources = getResources();
        int cellWidth = resources.getDimensionPixelSize(R.dimen.workspace_cell_width);
        int cellHeight = resources.getDimensionPixelSize(R.dimen.workspace_cell_height);
        int widthGap = resources.getDimensionPixelSize(R.dimen.workspace_width_gap);
        int heightGap = resources.getDimensionPixelSize(R.dimen.workspace_height_gap);
        int previewCellSize = resources.getDimensionPixelSize(R.dimen.preview_cell_size);

        // This logic imitates Launcher's CellLayout.rectToCell.
        // Always round up to next largest cell
        int smallerSize = Math.min(cellWidth, cellHeight);
        int spanX = (width + smallerSize) / smallerSize;
        int spanY = (height + smallerSize) / smallerSize;

        // We use a fixed preview cell size so that you get the same preview image for
        // the same cell-sized widgets across all devices
        width = spanX * previewCellSize + ((spanX - 1) * widthGap);
        height = spanY * previewCellSize + ((spanY - 1) * heightGap);
        return new int[] { width, height };
    }

    private File buildFile(String name) {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return null;
        }

        File path = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS);
        int orientationCode = getResources().getConfiguration().orientation;
        String orientation;
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            orientation = "landscape";
        } else if (orientationCode == Configuration.ORIENTATION_PORTRAIT) {
            orientation = "portrait";
        } else if (orientationCode == Configuration.ORIENTATION_SQUARE) {
            orientation = "square";
        } else {
            orientation = "undefined";
        }
        return new File(path, name + "_ori_" + orientation + ".png");
    }

    public Bitmap getPreviewBitmap() {
        mAppWidgetView.invalidate();
        Bitmap bmp = Bitmap.createBitmap(
                mAppWidgetView.getWidth(), mAppWidgetView.getHeight(), Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        mAppWidgetView.draw(c);
        return bmp;
    }

    private boolean saveImage(Bitmap bmp, String name) {
        File pic = buildFile(mAppWidgetName);
        if (pic == null) {
            Log.d(LOG_TAG, "External storage not present");
            return false;
        }

        pic.getParentFile().mkdirs();
        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(pic);
            if (!bmp.compress(CompressFormat.PNG, 100, fout)) {
                Log.d(LOG_TAG, "Failed to compress image");
                return false;
            }
            return true;
        } catch (IOException e) {
            Log.d(LOG_TAG, "Error writing to disk: " + e);
        } finally {
            try {
                if (fout != null) {
                    fout.close();
                }
            } catch (IOException e) {
                Log.d(LOG_TAG, "Could not close file: " + e);
            }
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        if (!getFragmentManager().popBackStackImmediate()) {
            startChooseActivity();
        }
    }

    @Override
    public void onClick(View v) {
        if (v == mSnapshotButton) {
            int textId = R.string.saving_preview;

            Toast preToast = Toast.makeText(getBaseContext(), textId, Toast.LENGTH_SHORT);
            preToast.show();

            Bitmap bmp = getPreviewBitmap();
            if (saveImage(bmp, mAppWidgetName)) {
                textId = R.string.preview_saved;
            } else {
                textId = R.string.preview_save_error;
            }

            Toast postToast = Toast.makeText(getBaseContext(), textId, Toast.LENGTH_SHORT);
            postToast.show();
        } else if (v == mEmailButton) {
            File file = buildFile(mAppWidgetName);
            if (file.exists()) {
                Intent emailIntent = new Intent(Intent.ACTION_SEND);
                emailIntent.setType("image/png");
                emailIntent.putExtra(Intent.EXTRA_SUBJECT,
                        getResources().getString(R.string.email_subject));
                emailIntent.putExtra(Intent.EXTRA_TEXT,
                        getResources().getString(R.string.email_body));
                emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
                startActivity(emailIntent);
            } else {
                Toast postToast = Toast.makeText(
                        getBaseContext(), R.string.no_preview, Toast.LENGTH_SHORT);
                postToast.show();
            }
        }
    }
}
