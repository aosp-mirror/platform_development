/**
 * Copyright (c) 2015, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.apis.content;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import com.example.android.apis.R;

/**
 * Example of sharing content from a private content provider.
 */
public class ShareContent extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.share_content);

        // Watch for button clicks.
        ((Button)findViewById(R.id.share_image)).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                Uri.Builder b = new Uri.Builder();
                b.scheme("content");
                b.authority("com.example.android.apis.content.FileProvider");
                TypedValue tv = new TypedValue();
                getResources().getValue(R.drawable.jellies, tv, true);
                b.appendEncodedPath(Integer.toString(tv.assetCookie));
                b.appendEncodedPath(tv.string.toString());
                Uri uri = b.build();
                intent.setType("image/jpeg");
                intent.putExtra(Intent.EXTRA_STREAM, uri);
                intent.setClipData(ClipData.newUri(getContentResolver(), "image", uri));
                startActivity(Intent.createChooser(intent, "Select share target"));
            }
        });
    }
}
