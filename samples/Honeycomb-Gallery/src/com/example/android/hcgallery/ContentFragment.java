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

import java.util.StringTokenizer;

import android.app.ActionBar;
import android.app.Fragment;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipData.Item;
import android.os.Bundle;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ImageView;

public class ContentFragment extends Fragment {

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.content_welcome, null);
        final ImageView imageView = (ImageView) view.findViewById(R.id.image);
        view.setDrawingCacheEnabled(false);

        view.setOnDragListener(new View.OnDragListener() {
            public boolean onDrag(View v, DragEvent event) {
                switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    return processDragStarted(event);
                case DragEvent.ACTION_DROP:
                    return processDrop(event, imageView);
                }
                return false;
            }
        });

        view.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                ActionBar bar = ContentFragment.this.getActivity()
                        .getActionBar();
                if (bar != null) {
                    if (bar.isShowing()) {
                        bar.hide();
                    } else {
                        bar.show();
                    }
                }
            }
        });
        return view;
    }

   boolean processDragStarted(DragEvent event) {
        // Determine whether to continue processing drag and drop based on the
        // plain text mime type.
        ClipDescription clipDesc = event.getClipDescription();
        if (clipDesc != null) {
            return clipDesc.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN);
        }
        return false;
    }

   boolean processDrop(DragEvent event, ImageView imageView) {
        // Attempt to parse clip data with expected format: category||entry_id.
        // Ignore event if data does not conform to this format.
        ClipData data = event.getClipData();
        if (data != null) {
            if (data.getItemCount() > 0) {
                Item item = data.getItemAt(0);
                String textData = (String) item.getText();
                if (textData != null) {
                    StringTokenizer tokenizer = new StringTokenizer(textData, "||");
                    if (tokenizer.countTokens() != 2) {
                        return false;
                    }
                    int category = -1;
                    int entryId = -1;
                    try {
                        category = Integer.parseInt(tokenizer.nextToken());
                        entryId = Integer.parseInt(tokenizer.nextToken());
                    } catch (NumberFormatException exception) {
                        return false;
                    }
                    imageView.setImageBitmap(
                        Directory.getCategory(category)
                                 .getEntry(entryId)
                                 .getBitmap(getResources()));
                    return true;
                }
            }
        }
        return false;
    }
}
