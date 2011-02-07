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
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.view.Window;
import android.view.WindowManager;

public class ContentFragment extends Fragment {
    // The bitmap currently used by ImageView
    private Bitmap mBitmap = null;

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
                case DragEvent.ACTION_DRAG_ENTERED:
                    view.setBackgroundColor(Color.LTGRAY);
                    break;
                case DragEvent.ACTION_DRAG_EXITED:
                    view.setBackgroundColor(Color.TRANSPARENT);
                    break;
                case DragEvent.ACTION_DRAG_STARTED:
                    return processDragStarted(event);
                case DragEvent.ACTION_DROP:
                    view.setBackgroundColor(Color.TRANSPARENT);
                    return processDrop(event, imageView);
                }
                return false;
            }
        });

        view.setOnClickListener(new OnClickListener() {


            public void onClick(View v) {
                ActionBar bar = getActivity().getActionBar();
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

    private boolean processDrop(DragEvent event, ImageView imageView) {
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
                    updateContentAndRecycleBitmap(category, entryId);
                    // Update list fragment with selected entry.
                    TitlesFragment titlesFrag = (TitlesFragment)
                            getFragmentManager().findFragmentById(R.id.frag_title);
                    titlesFrag.selectPosition(entryId);
                    return true;
                }
            }
        }
        return false;
    }

    public void updateContentAndRecycleBitmap(int category, int position) {
        // Get the bitmap that needs to be drawn and update the ImageView
        Bitmap next = Directory.getCategory(category).getEntry(position)
                .getBitmap(getResources());
        ((ImageView) getView().findViewById(R.id.image)).setImageBitmap(next);
        if (mBitmap != null) {
            // This is an advanced call and should be used if you
            // are working with a lot of bitmaps. The bitmap is dead
            // after this call.
            mBitmap.recycle();
        }
    }
}
