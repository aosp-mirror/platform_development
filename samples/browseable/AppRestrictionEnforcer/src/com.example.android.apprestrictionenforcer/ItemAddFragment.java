/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.example.android.apprestrictionenforcer;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Provides a dialog to create a new restriction item for the sample bundle array.
 */
public class ItemAddFragment extends DialogFragment implements View.OnClickListener {

    public interface OnItemAddedListener {
        void onItemAdded(String key, String value);
    }

    private OnItemAddedListener mListener;
    private EditText mEditKey;
    private EditText mEditValue;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Fragment parentFragment = getParentFragment();
        mListener = (OnItemAddedListener) (parentFragment == null ? activity : parentFragment);
    }

    @Override
    public void onDetach() {
        mListener = null;
        super.onDetach();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getDialog().setTitle(R.string.add_item);
        return inflater.inflate(R.layout.fragment_item_add, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mEditKey = (EditText) view.findViewById(R.id.key);
        mEditValue = (EditText) view.findViewById(R.id.value);
        view.findViewById(R.id.ok).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ok:
                if (addItem()) {
                    dismiss();
                }
                break;
        }
    }

    private boolean addItem() {
        String key = mEditKey.getText().toString();
        if (TextUtils.isEmpty(key)) {
            Toast.makeText(getActivity(), "Input the key.", Toast.LENGTH_SHORT).show();
            return false;
        }
        String value = mEditValue.getText().toString();
        if (TextUtils.isEmpty(value)) {
            Toast.makeText(getActivity(), "Input the value.", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (mListener != null) {
            mListener.onItemAdded(key, value);
        }
        return true;
    }

}
