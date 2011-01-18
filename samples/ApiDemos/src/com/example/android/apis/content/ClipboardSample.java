/**
 * Copyright (c) 2010, The Android Open Source Project
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

import com.example.android.apis.R;

import android.app.Activity;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;

public class ClipboardSample extends Activity {
    ClipboardManager mClipboard;

    Spinner mSpinner;
    TextView mMimeTypes;
    EditText mEditText;

    CharSequence mStyledText;
    String mPlainText;

    ClipboardManager.OnPrimaryClipChangedListener mPrimaryChangeListener
            = new ClipboardManager.OnPrimaryClipChangedListener() {
        public void onPrimaryClipChanged() {
            updateClipData();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mClipboard = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);

        // See res/any/layout/resources.xml for this view layout definition.
        setContentView(R.layout.clipboard);

        TextView tv;

        mStyledText = getText(R.string.styled_text);
        tv = (TextView)findViewById(R.id.styled_text);
        tv.setText(mStyledText);

        mPlainText = mStyledText.toString();
        tv = (TextView)findViewById(R.id.plain_text);
        tv.setText(mPlainText);

        mSpinner = (Spinner) findViewById(R.id.clip_type);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.clip_data_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner.setAdapter(adapter);
        mSpinner.setOnItemSelectedListener(
                new OnItemSelectedListener() {
                    public void onItemSelected(
                            AdapterView<?> parent, View view, int position, long id) {
                    }
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                });

        mMimeTypes = (TextView)findViewById(R.id.clip_mime_types);
        mEditText = (EditText)findViewById(R.id.clip_text);

        mClipboard.addPrimaryClipChangedListener(mPrimaryChangeListener);
        updateClipData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mClipboard.removePrimaryClipChangedListener(mPrimaryChangeListener);
    }

    public void pasteStyledText(View button) {
        mClipboard.setPrimaryClip(ClipData.newPlainText("Styled Text", mStyledText));
    }

    public void pastePlainText(View button) {
        mClipboard.setPrimaryClip(ClipData.newPlainText("Styled Text", mPlainText));
    }

    public void pasteIntent(View button) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.android.com/"));
        mClipboard.setPrimaryClip(ClipData.newIntent("VIEW intent", intent));
    }

    public void pasteUri(View button) {
        mClipboard.setPrimaryClip(ClipData.newRawUri("URI", Uri.parse("http://www.android.com/")));
    }

    void updateClipData() {
        ClipData clip = mClipboard.getPrimaryClip();
        String[] mimeTypes = clip != null ? clip.getDescription().filterMimeTypes("*/*") : null;
        mMimeTypes.setText("");
        if (mimeTypes != null) {
            for (int i=0; i<mimeTypes.length; i++) {
                mMimeTypes.append(mimeTypes[i]);
                mMimeTypes.append("\n");
            }
        }
        if (clip == null) {
            mSpinner.setSelection(0);
            mEditText.setText("");
        } else if (clip.getItemAt(0).getText() != null) {
            mSpinner.setSelection(1);
            mEditText.setText(clip.getItemAt(0).getText());
        } else if (clip.getItemAt(0).getIntent() != null) {
            mSpinner.setSelection(2);
            mEditText.setText(clip.getItemAt(0).getIntent().toUri(0));
        } else if (clip.getItemAt(0).getUri() != null) {
            mSpinner.setSelection(3);
            mEditText.setText(clip.getItemAt(0).getUri().toString());
        } else {
            mSpinner.setSelection(0);
            mEditText.setText("Clip containing no data");
        }
    }
}
