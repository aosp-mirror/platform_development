/*
 * Copyright 2014 The Android Open Source Project
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

package com.example.android.fragmenttransition;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

class MeatAdapter extends BaseAdapter {

    private final LayoutInflater mLayoutInflater;
    private final int mResourceId;

    public MeatAdapter(LayoutInflater inflater, int resourceId) {
        mLayoutInflater = inflater;
        mResourceId = resourceId;
    }

    @Override
    public int getCount() {
        return Meat.MEATS.length;
    }

    @Override
    public Meat getItem(int position) {
        return Meat.MEATS[position];
    }

    @Override
    public long getItemId(int position) {
        return Meat.MEATS[position].resourceId;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final View view;
        final ViewHolder holder;
        if (null == convertView) {
            view = mLayoutInflater.inflate(mResourceId, parent, false);
            holder = new ViewHolder();
            assert view != null;
            holder.image = (ImageView) view.findViewById(R.id.image);
            holder.title = (TextView) view.findViewById(R.id.title);
            view.setTag(holder);
        } else {
            view = convertView;
            holder = (ViewHolder) view.getTag();
        }
        bindView(holder, position);
        return view;
    }

    public void bindView(ViewHolder holder, int position) {
        Meat meat = getItem(position);
        holder.image.setImageResource(meat.resourceId);
        holder.title.setText(meat.title);
    }

    public static class ViewHolder {
        public ImageView image;
        public TextView title;
    }

}
