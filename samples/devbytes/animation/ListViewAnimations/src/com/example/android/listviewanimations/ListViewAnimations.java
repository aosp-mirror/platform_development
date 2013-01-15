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

package com.example.android.listviewanimations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;

/**
 * This example shows how animating ListView items can lead to problems as views are recycled,
 * and how to perform these types of animations correctly with new API added in Jellybean.
 *
 * Watch the associated video for this demo on the DevBytes channel of developer.android.com
 * or on YouTube at https://www.youtube.com/watch?v=8MIfSxgsHIs.
 */
public class ListViewAnimations extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_view_animations);

        final CheckBox vpaCB = (CheckBox) findViewById(R.id.vpaCB);
        final CheckBox setTransientStateCB = (CheckBox) findViewById(R.id.setTransientStateCB);
        final ListView listview = (ListView) findViewById(R.id.listview);
        final ArrayList<String> cheeseList = new ArrayList<String>();
        for (int i = 0; i < Cheeses.sCheeseStrings.length; ++i) {
            cheeseList.add(Cheeses.sCheeseStrings[i]);
        }
        final StableArrayAdapter adapter = new StableArrayAdapter(this,
                android.R.layout.simple_list_item_1, cheeseList);
        listview.setAdapter(adapter);

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                final String item = (String) parent.getItemAtPosition(position);
                if (vpaCB.isChecked()) {
                    view.animate().setDuration(1000).alpha(0).
                    withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            cheeseList.remove(item);
                            adapter.notifyDataSetChanged();
                            view.setAlpha(1);
                        }
                    });
                } else {
                    // Here's where the problem starts - this animation will animate a View object.
                    // But that View may get recycled if it is animated out of the container,
                    // and the animation will continue to fade a view that now contains unrelated
                    // content.
                    ObjectAnimator anim = ObjectAnimator.ofFloat(view, View.ALPHA, 0);
                    anim.setDuration(1000);
                    if (setTransientStateCB.isChecked()) {
                        // Here's the correct way to do this: if you tell a view that it has
                        // transientState, then ListView ill avoid recycling it until the
                        // transientState flag is reset.
                        // A different approach is to use ViewPropertyAnimator, which sets the
                        // transientState flag internally.
                        view.setHasTransientState(true);
                    }
                    anim.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            cheeseList.remove(item);
                            adapter.notifyDataSetChanged();
                            view.setAlpha(1);
                            if (setTransientStateCB.isChecked()) {
                                view.setHasTransientState(false);
                            }
                        }
                    });
                    anim.start();

                }
            }

        });
    }

    private class StableArrayAdapter extends ArrayAdapter<String> {

        HashMap<String, Integer> mIdMap = new HashMap<String, Integer>();

        public StableArrayAdapter(Context context, int textViewResourceId,
                List<String> objects) {
            super(context, textViewResourceId, objects);
            for (int i = 0; i < objects.size(); ++i) {
                mIdMap.put(objects.get(i), i);
            }
        }

        @Override
        public long getItemId(int position) {
            String item = getItem(position);
            return mIdMap.get(item);
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

    }

}
