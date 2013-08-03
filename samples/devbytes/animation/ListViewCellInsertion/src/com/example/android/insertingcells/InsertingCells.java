/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.example.android.insertingcells;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * This application creates a ListView to which new elements can be added from the
 * top. When a new element is added, it is animated from above the bounds
 * of the list to the top. When the list is scrolled all the way to the top and a new
 * element is added, the row animation is accompanied by an image animation that pops
 * out of the round view and pops into the correct position in the top cell.
 */
public class InsertingCells extends Activity implements OnRowAdditionAnimationListener {

    private ListItemObject mValues[];

    private InsertionListView mListView;

    private Button mButton;

    private Integer mItemNum = 0;

    private RoundView mRoundView;

    private int mCellHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mValues = new ListItemObject[] {
                new ListItemObject("Chameleon", R.drawable.chameleon, 0),
                new ListItemObject("Rock", R.drawable.rock, 0),
                new ListItemObject("Flower", R.drawable.flower, 0),
        };

        mCellHeight = (int)(getResources().getDimension(R.dimen.cell_height));

        List<ListItemObject> mData = new ArrayList<ListItemObject>();
        CustomArrayAdapter mAdapter = new CustomArrayAdapter(this, R.layout.list_view_item, mData);
        RelativeLayout mLayout = (RelativeLayout)findViewById(R.id.relative_layout);

        mRoundView = (RoundView)findViewById(R.id.round_view);
        mButton = (Button)findViewById(R.id.add_row_button);
        mListView = (InsertionListView)findViewById(R.id.listview);

        mListView.setAdapter(mAdapter);
        mListView.setData(mData);
        mListView.setLayout(mLayout);
        mListView.setRowAdditionAnimationListener(this);
    }

    public void addRow(View view) {
        mButton.setEnabled(false);

        mItemNum++;
        ListItemObject obj = mValues[mItemNum % mValues.length];
        final ListItemObject newObj = new ListItemObject(obj.getTitle(), obj.getImgResource(),
                mCellHeight);

        boolean shouldAnimateInNewImage = mListView.shouldAnimateInNewImage();
        if (!shouldAnimateInNewImage) {
            mListView.addRow(newObj);
            return;
        }

        mListView.setEnabled(false);
        ObjectAnimator animator = mRoundView.getScalingAnimator();
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationRepeat(Animator animation) {
                mListView.addRow(newObj);
            }
        });
        animator.start();
    }

    @Override
    public void onRowAdditionAnimationStart() {
        mButton.setEnabled(false);
    }

    @Override
    public void onRowAdditionAnimationEnd() {
        mButton.setEnabled(true);
    }
}
