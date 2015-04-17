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

package com.example.android.receiveshare;

import android.app.Activity;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.FileNotFoundException;
import java.io.IOException;

public class ReceiveShare extends Activity {
    static Uri getShareUri(Intent intent) {
        Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (uri == null) {
            ClipData clip = intent.getClipData();
            if (clip != null && clip.getItemCount() > 0) {
                uri = clip.getItemAt(0).getUri();
            }
        }
        return uri;
    }

    static CharSequence buildShareInfo(ContentResolver resolver, Intent intent) {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        if (intent.getType() != null) {
            sb.append("Type: "); sb.append(intent.getType()); sb.append("\n");
        }
        CharSequence text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
        if (text != null) {
            sb.append("Text: "); sb.append(text);
            String html = intent.getStringExtra(Intent.EXTRA_HTML_TEXT);
            if (html != null) {
                sb.append("\n\n"); sb.append("HTML: "); sb.append(html);
            }
        } else {
            Uri uri = getShareUri(intent);
            if (uri != null) {
                sb.append("Uri: "); sb.append(uri.toString()); sb.append("\n");
                try {
                    AssetFileDescriptor afd = resolver.openAssetFileDescriptor(
                            uri, "r");
                    sb.append("Start offset: ");
                    sb.append(Long.toString(afd.getStartOffset()));
                    sb.append("\n");
                    sb.append("Length: ");
                    sb.append(Long.toString(afd.getLength()));
                    sb.append("\n");
                    afd.close();
                } catch (FileNotFoundException e) {
                    sb.append(e.toString());
                } catch (SecurityException e) {
                    sb.append(e.toString());
                } catch (IOException e) {
                    sb.append(e.toString());
                }
            }
        }
        return sb;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.receive_share);

        Button sendButton = (Button)findViewById(R.id.send_to_service);
        final Uri uri = getShareUri(getIntent());
        if (uri != null) {
            sendButton.setEnabled(true);
        } else {
            sendButton.setEnabled(false);
        }

        TextView content = (TextView)findViewById(R.id.receive_share_data);
        content.append(buildShareInfo(getContentResolver(), getIntent()));

        // Watch for button clicks.
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ReceiveShare.this, ReceiveShareService.class);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                ClipData clip = ClipData.newUri(getContentResolver(), "Something", uri);
                intent.setClipData(clip);
                startService(intent);
                finish();
            }
        });
    }
}
