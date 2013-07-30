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

package com.example.android.cardflip;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * This application creates 2 stacks of playing cards. Using fling events,
 * these cards can be flipped from one stack to another where each flip comes with
 * an associated animation. The cards can be flipped horizontally from left to right
 * or right to left depending on which stack the animating card currently belongs to.
 *
 * This application demonstrates an animation where a stack of cards can either be
 * be rotated out or back in about their bottom left corner in a counter-clockwise direction.
 * Rotate out: Down fling on stack of cards
 * Rotate in: Up fling on stack of cards
 * Full rotation: Tap on stack of cards
 *
 * Note that in this demo touch events are disabled in the middle of any animation so
 * only one card can be flipped at a time. When the cards are in a rotated-out
 * state, no new cards can be rotated to or from that stack. These changes were made to
 * simplify the code for this demo.
 */

public class CardFlip extends Activity implements CardFlipListener {

    final static int CARD_PILE_OFFSET = 3;
    final static int STARTING_NUMBER_CARDS = 15;
    final static int RIGHT_STACK = 0;
    final static int LEFT_STACK = 1;

    int mCardWidth = 0;
    int mCardHeight = 0;

    int mVerticalPadding;
    int mHorizontalPadding;

    boolean mTouchEventsEnabled = true;
    boolean[] mIsStackEnabled;

    RelativeLayout mLayout;

    List<ArrayList<CardView>> mStackCards;

    GestureDetector gDetector;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mStackCards = new ArrayList<ArrayList<CardView>>();
        mStackCards.add(new ArrayList<CardView>());
        mStackCards.add(new ArrayList<CardView>());

        mIsStackEnabled = new boolean[2];
        mIsStackEnabled[0] = true;
        mIsStackEnabled[1] = true;

        mVerticalPadding = getResources().getInteger(R.integer.vertical_card_magin);
        mHorizontalPadding = getResources().getInteger(R.integer.horizontal_card_magin);

        gDetector = new GestureDetector(this, mGestureListener);

        mLayout = (RelativeLayout)findViewById(R.id.main_relative_layout);
        ViewTreeObserver observer = mLayout.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    mLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    mLayout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }

                mCardHeight = mLayout.getHeight();
                mCardWidth = mLayout.getWidth() / 2;

                for (int x = 0; x < STARTING_NUMBER_CARDS; x++) {
                    addNewCard(RIGHT_STACK);
                }
            }
        });
    }

    /**
     * Adds a new card to the specified stack. Also performs all the necessary layout setup
     * to place the card in the correct position.
     */
    public void addNewCard(int stack) {
        CardView view = new CardView(this);
        view.updateTranslation(mStackCards.get(stack).size());
        view.setCardFlipListener(this);
        view.setPadding(mHorizontalPadding, mVerticalPadding, mHorizontalPadding, mVerticalPadding);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(mCardWidth,
                mCardHeight);
        params.topMargin = 0;
        params.leftMargin = (stack == RIGHT_STACK ? mCardWidth : 0);

        mStackCards.get(stack).add(view);
        mLayout.addView(view, params);
    }

    /**
     * Gesture Detector listens for fling events in order to potentially initiate
     * a card flip event when a fling event occurs. Also listens for tap events in
     * order to potentially initiate a full rotation animation.
     */
    private GestureDetector.SimpleOnGestureListener mGestureListener = new GestureDetector
            .SimpleOnGestureListener() {
        @Override
        public boolean onSingleTapUp(MotionEvent motionEvent) {
            int stack = getStack(motionEvent);
            rotateCardsFullRotation(stack, CardView.Corner.BOTTOM_LEFT);
            return true;
        }

        @Override
        public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent2, float v,
                               float v2) {
            int stack = getStack(motionEvent);
            ArrayList<CardView> cardStack = mStackCards.get(stack);
            int size = cardStack.size();
            if (size > 0) {
                rotateCardView(cardStack.get(size - 1), stack, v, v2);
            }
            return true;
        }
    };

    /** Returns the appropriate stack corresponding to the MotionEvent. */
    public int getStack(MotionEvent ev) {
        boolean isLeft = ev.getX() <= mCardWidth;
        return isLeft ? LEFT_STACK : RIGHT_STACK;
    }

    /**
     * Uses the stack parameter, along with the velocity values of the fling event
     * to determine in what direction the card must be flipped. By the same logic, the
     * new stack that the card belongs to after the animation is also determined
     * and updated.
     */
    public void rotateCardView(final CardView cardView, int stack, float velocityX,
                               float velocityY) {

        boolean xGreaterThanY = Math.abs(velocityX) > Math.abs(velocityY);

        boolean bothStacksEnabled = mIsStackEnabled[RIGHT_STACK] && mIsStackEnabled[LEFT_STACK];

        ArrayList<CardView>leftStack = mStackCards.get(LEFT_STACK);
        ArrayList<CardView>rightStack = mStackCards.get(RIGHT_STACK);

        switch (stack) {
            case RIGHT_STACK:
                if (velocityX < 0 &&  xGreaterThanY) {
                    if (!bothStacksEnabled) {
                        break;
                    }
                    mLayout.bringChildToFront(cardView);
                    mLayout.requestLayout();
                    rightStack.remove(rightStack.size() - 1);
                    leftStack.add(cardView);
                    cardView.flipRightToLeft(leftStack.size() - 1, (int)velocityX);
                    break;
                } else if (!xGreaterThanY) {
                    boolean rotateCardsOut = velocityY > 0;
                    rotateCards(RIGHT_STACK, CardView.Corner.BOTTOM_LEFT, rotateCardsOut);
                }
                break;
            case LEFT_STACK:
                if (velocityX > 0 && xGreaterThanY) {
                    if (!bothStacksEnabled) {
                        break;
                    }
                    mLayout.bringChildToFront(cardView);
                    mLayout.requestLayout();
                    leftStack.remove(leftStack.size() - 1);
                    rightStack.add(cardView);
                    cardView.flipLeftToRight(rightStack.size() - 1, (int)velocityX);
                    break;
                } else if (!xGreaterThanY) {
                    boolean rotateCardsOut = velocityY > 0;
                    rotateCards(LEFT_STACK, CardView.Corner.BOTTOM_LEFT, rotateCardsOut);
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onCardFlipEnd() {
        mTouchEventsEnabled = true;
    }

    @Override
    public void onCardFlipStart() {
        mTouchEventsEnabled = false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent me) {
        if (mTouchEventsEnabled) {
            return gDetector.onTouchEvent(me);
        } else {
            return super.onTouchEvent(me);
        }
    }

    /**
     * Retrieves an animator object for each card in the specified stack that either
     * rotates it in or out depending on its current state. All of these animations
     * are then played together.
     */
    public void rotateCards (final int stack, CardView.Corner corner,
                             final boolean isRotatingOut) {
        List<Animator> animations = new ArrayList<Animator>();

        ArrayList <CardView> cards = mStackCards.get(stack);

        for (int i = 0; i < cards.size(); i++) {
            CardView cardView = cards.get(i);
            animations.add(cardView.getRotationAnimator(i, corner, isRotatingOut, false));
            mLayout.bringChildToFront(cardView);
        }
        /** All the cards are being brought to the front in order to guarantee that
         * the cards being rotated in the current stack will overlay the cards in the
         * other stack. After the z-ordering of all the cards is updated, a layout must
         * be requested in order to apply the changes made.*/
        mLayout.requestLayout();

        AnimatorSet set = new AnimatorSet();
        set.playTogether(animations);
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mIsStackEnabled[stack] = !isRotatingOut;
            }
        });
        set.start();
    }

    /**
     * Retrieves an animator object for each card in the specified stack to complete a
     * full revolution around one of its corners, and plays all of them together.
     */
    public void rotateCardsFullRotation (int stack, CardView.Corner corner) {
        List<Animator> animations = new ArrayList<Animator>();

        ArrayList <CardView> cards = mStackCards.get(stack);
        for (int i = 0; i < cards.size(); i++) {
            CardView cardView = cards.get(i);
            animations.add(cardView.getFullRotationAnimator(i, corner, false));
            mLayout.bringChildToFront(cardView);
        }
        /** Same reasoning for bringing cards to front as in rotateCards().*/
        mLayout.requestLayout();

        mTouchEventsEnabled = false;
        AnimatorSet set = new AnimatorSet();
        set.playTogether(animations);
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mTouchEventsEnabled = true;
            }
        });
        set.start();
    }
}