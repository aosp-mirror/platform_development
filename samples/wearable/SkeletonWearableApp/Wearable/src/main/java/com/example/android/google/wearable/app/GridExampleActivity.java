/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.example.android.google.wearable.app;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.wearable.view.CardFragment;
import android.support.wearable.view.FragmentGridPagerAdapter;
import android.support.wearable.view.GridViewPager;
import android.support.wearable.view.ImageReference;
import java.util.HashMap;
import java.util.Map;

public class GridExampleActivity extends Activity {
    private static final int NUM_ROWS = 10;
    private static final int NUM_COLS = 3;

    MainAdapter mAdapter;
    GridViewPager mPager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.grid_activity);
        mPager = (GridViewPager) findViewById(R.id.fragment_container);
        mAdapter = new MainAdapter(getFragmentManager());
        mPager.setAdapter(mAdapter);

    }

    private static class MainAdapter extends FragmentGridPagerAdapter{
        Map<Point, ImageReference> mBackgrounds = new HashMap<Point, ImageReference>();

        public MainAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getRowCount() {
            return NUM_ROWS;
        }

        @Override
        public int getColumnCount(int rowNum) {
            return NUM_COLS;
        }

        @Override
        public Fragment getFragment(int rowNum, int colNum) {
            return MainFragment.newInstance(rowNum, colNum);
        }

        @Override
        public ImageReference getBackground(int row, int column) {
            Point pt = new Point(column, row);
            ImageReference ref = mBackgrounds.get(pt);
            if (ref == null) {
                Bitmap bm = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888);
                Canvas c = new Canvas(bm);
                Paint p = new Paint();
                // Clear previous image.
                c.drawRect(0, 0, 200, 200, p);
                p.setAntiAlias(true);
                p.setTypeface(Typeface.DEFAULT);
                p.setTextSize(64);
                p.setColor(Color.LTGRAY);
                c.drawText(column+ "-" + row, 20, 100, p);
                ref = ImageReference.forBitmap(bm);
                mBackgrounds.put(pt, ref);
            }
            return ref;
        }
    }

    public static class MainFragment extends CardFragment {
        private static MainFragment newInstance(int rowNum, int colNum) {
            Bundle args = new Bundle();
            args.putString(CardFragment.KEY_TITLE, "Row:" + rowNum);
            args.putString(CardFragment.KEY_TEXT, "Col:" + colNum);
            MainFragment f = new MainFragment();
            f.setArguments(args);
            return f;
        }
    }
}
