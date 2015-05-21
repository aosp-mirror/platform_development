/*
* Copyright 2013 The Android Open Source Project
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


package com.example.android.batchstepsensor.cardstream;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.RelativeLayout;

/**
 * Custom Button with a special 'pressed' effect for touch events.
 */
public class CardLayout extends RelativeLayout {

    private boolean mSwiping = false;
    private float mDownX = 0.f;
    private float mDownY = 0.f;
    private float mTouchSlop = 0.f;

    public CardLayout(Context context) {
        super(context);
        init();
    }

    public CardLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CardLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init(){
        setFocusable(true);
        setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
        setWillNotDraw(false);
        setClickable(true);

        mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop() * 2.f;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch(event.getAction()){
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                mSwiping = false;
                break;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {

        switch(event.getAction()){
            case MotionEvent.ACTION_MOVE:
                if( !mSwiping ){
                    mSwiping = Math.abs(mDownX - event.getX()) > mTouchSlop;
                }
                break;
            case MotionEvent.ACTION_DOWN:
                mDownX = event.getX();
                mDownY = event.getY();
                mSwiping = false;
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                mSwiping = false;
                break;
        }
        return mSwiping;
    }
}