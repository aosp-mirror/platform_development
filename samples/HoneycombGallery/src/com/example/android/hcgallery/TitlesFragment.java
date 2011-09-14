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

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.content.ClipData;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Fragment that shows the list of images
 * As an extension of ListFragment, this fragment uses a default layout
 * that includes a single ListView, which you can acquire with getListView()
 * When running on a screen size smaller than "large", this fragment appears alone
 * in MainActivity. In this case, selecting a list item opens the ContentActivity,
 * which likewise holds only the ContentFragment.
 */
public class TitlesFragment extends ListFragment implements ActionBar.TabListener {
    OnItemSelectedListener mListener;
    private int mCategory = 0;
    private int mCurPosition = 0;
    private boolean mDualFragments = false;

    /** Container Activity must implement this interface and we ensure
     * that it does during the onAttach() callback
     */
    public interface OnItemSelectedListener {
        public void onItemSelected(int category, int position);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Check that the container activity has implemented the callback interface
        try {
            mListener = (OnItemSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() 
                    + " must implement OnItemSelectedListener");
        }
    }

    /** This is where we perform setup for the fragment that's either
     * not related to the fragment's layout or must be done after the layout is drawn.
     * Notice that this fragment does not implement onCreateView(), because it extends
     * ListFragment, which includes a ListView as the root view by default, so there's
     * no need to set up the layout.
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ContentFragment frag = (ContentFragment) getFragmentManager()
                .findFragmentById(R.id.content_frag);
        if (frag != null) mDualFragments = true;

        ActionBar bar = getActivity().getActionBar();
        bar.setDisplayHomeAsUpEnabled(false);
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Must call in order to get callback to onCreateOptionsMenu()
        setHasOptionsMenu(true);

        Directory.initializeDirectory();
        for (int i = 0; i < Directory.getCategoryCount(); i++) {
            bar.addTab(bar.newTab().setText(Directory.getCategory(i).getName())
                    .setTabListener(this));
        }

        //Current position should survive screen rotations.
        if (savedInstanceState != null) {
            mCategory = savedInstanceState.getInt("category");
            mCurPosition = savedInstanceState.getInt("listPosition");
            bar.selectTab(bar.getTabAt(mCategory));
        }

        populateTitles(mCategory);
        ListView lv = getListView();
        lv.setCacheColorHint(Color.TRANSPARENT); // Improves scrolling performance

        if (mDualFragments) {
            // Highlight the currently selected item
            lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            // Enable drag and dropping
            lv.setOnItemLongClickListener(new OnItemLongClickListener() {
                public boolean onItemLongClick(AdapterView<?> av, View v, int pos, long id) {
                    final String title = (String) ((TextView) v).getText();

                    // Set up clip data with the category||entry_id format.
                    final String textData = String.format("%d||%d", mCategory, pos);
                    ClipData data = ClipData.newPlainText(title, textData);
                    v.startDrag(data, new MyDragShadowBuilder(v), null, 0);
                    return true;
                }
            });
        }

        // If showing both fragments, select the appropriate list item by default
        if (mDualFragments) selectPosition(mCurPosition);

        // Attach a GlobalLayoutListener so that we get a callback when the layout
        // has finished drawing. This is necessary so that we can apply top-margin
        // to the ListView in order to dodge the ActionBar. Ordinarily, that's not
        // necessary, but we've set the ActionBar to "overlay" mode using our theme,
        // so the layout does not account for the action bar position on its own.
        ViewTreeObserver observer = getListView().getViewTreeObserver();
        observer.addOnGlobalLayoutListener(layoutListener);
    }

    @Override
    public void onDestroyView() {
      super.onDestroyView();
      // Always detach ViewTreeObserver listeners when the view tears down
      getListView().getViewTreeObserver().removeGlobalOnLayoutListener(layoutListener);
    }

    /** Attaches an adapter to the fragment's ListView to populate it with items */
    public void populateTitles(int category) {
        DirectoryCategory cat = Directory.getCategory(category);
        String[] items = new String[cat.getEntryCount()];
        for (int i = 0; i < cat.getEntryCount(); i++)
            items[i] = cat.getEntry(i).getName();
        // Convenience method to attach an adapter to ListFragment's ListView
        setListAdapter(new ArrayAdapter<String>(getActivity(),
                R.layout.title_list_item, items));
        mCategory = category;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        // Send the event to the host activity via OnItemSelectedListener callback
        mListener.onItemSelected(mCategory, position);
        mCurPosition = position;
    }

    /** Called to select an item from the listview */
    public void selectPosition(int position) {
        // Only if we're showing both fragments should the item be "highlighted"
        if (mDualFragments) {
            ListView lv = getListView();
            lv.setItemChecked(position, true);
        }
        // Calls the parent activity's implementation of the OnItemSelectedListener
        // so the activity can pass the event to the sibling fragment as appropriate
        mListener.onItemSelected(mCategory, position);
    }

    @Override
    public void onSaveInstanceState (Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("listPosition", mCurPosition);
        outState.putInt("category", mCategory);
    }

    /** This defines how the draggable list items appear during a drag event */
    private class MyDragShadowBuilder extends View.DragShadowBuilder {
        private Drawable mShadow;

        public MyDragShadowBuilder(View v) {
            super(v);

            final TypedArray a = v.getContext().obtainStyledAttributes(R.styleable.AppTheme);
            mShadow = a.getDrawable(R.styleable.AppTheme_listDragShadowBackground);
            mShadow.setCallback(v);
            mShadow.setBounds(0, 0, v.getWidth(), v.getHeight());
            a.recycle();
        }

        @Override
        public void onDrawShadow(Canvas canvas) {
            super.onDrawShadow(canvas);
            mShadow.draw(canvas);
            getView().draw(canvas);
        }
    }

    // Because the fragment doesn't have a reliable callback to notify us when
    // the activity's layout is completely drawn, this OnGlobalLayoutListener provides
    // the necessary callback so we can add top-margin to the ListView in order to dodge
    // the ActionBar. Which is necessary because the ActionBar is in overlay mode, meaning
    // that it will ordinarily sit on top of the activity layout as a top layer and
    // the ActionBar height can vary. Specifically, when on a small/normal size screen,
    // the action bar tabs appear in a second row, making the action bar twice as tall.
    ViewTreeObserver.OnGlobalLayoutListener layoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            int barHeight = getActivity().getActionBar().getHeight();
            ListView listView = getListView();
            FrameLayout.LayoutParams params = (LayoutParams) listView.getLayoutParams();
            // The list view top-margin should always match the action bar height
            if (params.topMargin != barHeight) {
                params.topMargin = barHeight;
                listView.setLayoutParams(params);
            }
            // The action bar doesn't update its height when hidden, so make top-margin zero
            if (!getActivity().getActionBar().isShowing()) {
              params.topMargin = 0;
              listView.setLayoutParams(params);
            }
        }
    };


    /* The following are callbacks implemented for the ActionBar.TabListener,
     * which this fragment implements to handle events when tabs are selected.
     */

    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
        TitlesFragment titleFrag = (TitlesFragment) getFragmentManager()
                .findFragmentById(R.id.titles_frag);
        titleFrag.populateTitles(tab.getPosition());
        
        if (mDualFragments) {
            titleFrag.selectPosition(0);
        }
    }

    /* These must be implemented, but we don't use them */
    
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
    }

    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
    }

}
