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

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ListFragment;
import android.content.ClipData;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextPaint;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemLongClickListener;

public class TitlesFragment extends ListFragment {
    private int mCategory = 0;
    private int mCurPosition = 0;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //Current position should survive screen rotations.
        if (savedInstanceState != null) {
            mCategory = savedInstanceState.getInt("category");
            mCurPosition = savedInstanceState.getInt("listPosition");
        }

        populateTitles(mCategory);
        ListView lv = getListView();
        lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        selectPosition(mCurPosition);
        lv.setCacheColorHint(Color.WHITE);
        lv.setOnItemLongClickListener(new OnItemLongClickListener() {

            public boolean onItemLongClick(AdapterView<?> av, View v, int pos,
                    long id) {
                final String title = (String) ((TextView) v).getText();

                // Set up clip data with the category||entry_id format.
                final String textData = String.format("%d||%d", mCategory, pos);
                ClipData data = ClipData.newPlainText(title, textData);

                v.startDrag(data, new MyDragShadowBuilder(v, title), null, 0);
                return true;
            }
        });
    }

    private static class MyDragShadowBuilder extends View.DragShadowBuilder {
        private static Drawable mShadow;
        private static String mLabel;
        private static int mViewHeight;
        
        public MyDragShadowBuilder(View v, String label) {
            super(v);
            mShadow = new ColorDrawable(Color.BLUE);
            mShadow.setBounds(0, 0, v.getWidth(), v.getHeight());
            mLabel = label;
            mViewHeight = v.getHeight();
        }

        @Override
        public void onDrawShadow(Canvas canvas) {
            super.onDrawShadow(canvas);
            mShadow.draw(canvas);
            Paint paint = new TextPaint();
            paint.setTextSize(20);
            paint.setColor(Color.LTGRAY);
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            paint.setAntiAlias(true);
            canvas.drawText(mLabel, 20, (float) (mViewHeight * .6), paint);
        }
    }

    public void populateTitles(int category) {
        DirectoryCategory cat = Directory.getCategory(category);
        String[] items = new String[cat.getEntryCount()];
        for (int i = 0; i < cat.getEntryCount(); i++)
            items[i] = cat.getEntry(i).getName();
        setListAdapter(new ArrayAdapter<String>(getActivity(),
                R.layout.title_list_item, items));
        mCategory = category;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        updateImage(position);
    }

    private void updateImage(int position) {
        ContentFragment frag = (ContentFragment) getFragmentManager()
                .findFragmentById(R.id.frag_content);
        frag.updateContentAndRecycleBitmap(mCategory, position);
    }

    public void selectPosition(int position) {
        ListView lv = getListView();
        lv.setItemChecked(position, true);
        updateImage(position);

    }

    @Override
    public void onSaveInstanceState (Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("listPosition", mCurPosition);
        outState.putInt("category", mCategory);
    }
}
