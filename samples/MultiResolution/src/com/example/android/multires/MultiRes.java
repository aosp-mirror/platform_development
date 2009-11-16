/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.example.android.multires;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public final class MultiRes extends Activity {

    private int mCurrentPhotoIndex = 0;
    private int[] mPhotoIds = new int[] { R.drawable.sample_0,
            R.drawable.sample_1, R.drawable.sample_2, R.drawable.sample_3,
            R.drawable.sample_4, R.drawable.sample_5, R.drawable.sample_6,
            R.drawable.sample_7 };

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        showPhoto(mCurrentPhotoIndex);

        // Handle clicks on the 'Next' button.
        Button nextButton = (Button) findViewById(R.id.next_button);
        nextButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mCurrentPhotoIndex = (mCurrentPhotoIndex + 1)
                        % mPhotoIds.length;
                showPhoto(mCurrentPhotoIndex);
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt("photo_index", mCurrentPhotoIndex);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        mCurrentPhotoIndex = savedInstanceState.getInt("photo_index");
        showPhoto(mCurrentPhotoIndex);
        super.onRestoreInstanceState(savedInstanceState);
    }

    private void showPhoto(int photoIndex) {
        ImageView imageView = (ImageView) findViewById(R.id.image_view);
        imageView.setImageResource(mPhotoIds[photoIndex]);

        TextView statusText = (TextView) findViewById(R.id.status_text);
        statusText.setText(String.format("%d/%d", photoIndex + 1,
                mPhotoIds.length));
    }
}
