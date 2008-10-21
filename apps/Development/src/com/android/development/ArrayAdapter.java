/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.development;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.BaseAdapter;

import java.util.List;

public abstract class ArrayAdapter<E> extends BaseAdapter
{
    public ArrayAdapter(Context context, int layoutRes) {
        mContext = context;
        mInflater = (LayoutInflater)context.getSystemService(
            Context.LAYOUT_INFLATER_SERVICE);
        mLayoutRes = layoutRes;
    }

    public void setSource(List<E> list) {
        mList = list;
    }

    public abstract void bindView(View view, E item);

    public E itemForPosition(int position) {
        if (mList == null) {
            return null;
        }

        return mList.get(position);
    }

    public int getCount() {
        return mList != null ? mList.size() : 0;
    }

    public Object getItem(int position) {
        return position;
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        if (convertView == null) {
            view = mInflater.inflate(mLayoutRes, parent, false);
        } else {
            view = convertView;
        }
        bindView(view, mList.get(position));
        return view;
    }

    private final Context mContext;
    private final LayoutInflater mInflater;
    private final int mLayoutRes;
    private List<E> mList;
}

