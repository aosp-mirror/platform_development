/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.example.android.vdmdemo.client;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.StyleRes;
import androidx.recyclerview.widget.RecyclerView;
import java.util.function.Consumer;

/** Recycler view that can resize a child dynamically. */
public final class ClientView extends RecyclerView {
  private boolean isResizing = false;
  private Consumer<Rect> resizeDoneCallback = null;
  private Drawable resizingRect = null;
  private Rect resizingBounds = new Rect();
  private float resizeOffsetX = 0;
  private float resizeOffsetY = 0;

  public ClientView(Context context) {
    super(context);
    init();
  }

  public ClientView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  public ClientView(Context context, AttributeSet attrs, @StyleRes int defStyle) {
    super(context, attrs, defStyle);
    init();
  }

  private void init() {
    resizingRect = getContext().getResources().getDrawable(R.drawable.resize_rect);
  }

  void startResizing(View viewToResize, MotionEvent origin, Consumer<Rect> callback) {
    isResizing = true;
    resizeDoneCallback = callback;
    viewToResize.getGlobalVisibleRect(resizingBounds);
    resizingRect.setBounds(resizingBounds);
    getRootView().getOverlay().add(resizingRect);
    resizeOffsetX = origin.getRawX() - resizingBounds.right;
    resizeOffsetY = origin.getRawY() - resizingBounds.top;
  }

  private void stopResizing() {
    if (!isResizing) {
      return;
    }
    isResizing = false;
    resizeOffsetX = resizeOffsetY = 0;
    getRootView().getOverlay().clear();
    if (resizeDoneCallback != null) {
      resizeDoneCallback.accept(resizingBounds);
      resizeDoneCallback = null;
    }
  }

  @Override
  public boolean dispatchTouchEvent(MotionEvent ev) {
    if (!isResizing) {
      return super.dispatchTouchEvent(ev);
    }
    switch (ev.getAction()) {
      case MotionEvent.ACTION_UP:
        stopResizing();
        break;
      case MotionEvent.ACTION_MOVE:
        resizingBounds.right = (int) (ev.getRawX() - resizeOffsetX);
        resizingBounds.top = (int) (ev.getRawY() - resizeOffsetY);
        resizingRect.setBounds(resizingBounds);
        break;
      default:
        break;
    }
    return true;
  }
}
