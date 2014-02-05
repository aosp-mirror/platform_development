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
package com.example.android.apis.animation;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.transition.Transition;
import android.transition.TransitionValues;
import android.transition.Visibility;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

/**
 *
 */
public class Fall extends Visibility {
    private static final TimeInterpolator sDecelerate = new DecelerateInterpolator();
    private static final TimeInterpolator sAccelerate = new AccelerateInterpolator();
    private static final String TAG = "Fall";
    private static final String PROPNAME_SCREEN_LOCATION = "android:fade:screen_location";

    private View mHero;

    public void setHero(View hero) {
        mHero = hero;
    }

    private Animator createAnimation(final View view, long startDelay, final float startY,
            float endY, AnimatorListenerAdapter listener, TimeInterpolator interpolator) {
        if (startY == endY) {
            // run listener if we're noop'ing the animation, to get the end-state results now
            if (listener != null) {
                listener.onAnimationEnd(null);
            }
            return null;
        }
        final ObjectAnimator anim = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, startY, endY);

        if (listener != null) {
            anim.addListener(listener);
            anim.addPauseListener(listener);
        }
        anim.setInterpolator(interpolator);
        anim.setStartDelay(startDelay);
        AnimatorSet wrapper = new AnimatorSet();
        wrapper.play(anim);
        wrapper.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                view.setTranslationY(startY);
            }
        });
        return wrapper;
    }

    private void captureValues(TransitionValues transitionValues) {
        int[] loc = new int[2];
        transitionValues.view.getLocationOnScreen(loc);
        transitionValues.values.put(PROPNAME_SCREEN_LOCATION, loc);
    }

    @Override
    public void captureStartValues(TransitionValues transitionValues) {
        super.captureStartValues(transitionValues);
        captureValues(transitionValues);
    }

    @Override
    public void captureEndValues(TransitionValues transitionValues) {
        super.captureEndValues(transitionValues);
        captureValues(transitionValues);
    }

    @Override
    public Animator onAppear(ViewGroup sceneRoot,
            TransitionValues startValues, int startVisibility,
            TransitionValues endValues, int endVisibility) {
        if (endValues == null) {
            return null;
        }
        final View endView = endValues.view;
        Log.v(TAG, "onAppear: " + endView.getId());
        final float endY = endView.getTranslationY();
        final float startY = endY + sceneRoot.getHeight();

        TransitionListener transitionListener = new TransitionListener() {
            boolean mCanceled = false;
            float mPausedY;

            @Override
            public void onTransitionCancel(Transition transition) {
                endView.setTranslationY(endY);
                mCanceled = true;
            }

            @Override
            public void onTransitionStart(Transition transition) {
            }

            @Override
            public void onTransitionEnd(Transition transition) {
                if (!mCanceled) {
                    endView.setTranslationY(endY);
                }
            }

            @Override
            public void onTransitionPause(Transition transition) {
                mPausedY = endView.getTranslationY();
                endView.setTranslationY(endY);
            }

            @Override
            public void onTransitionResume(Transition transition) {
                endView.setTranslationY(mPausedY);
            }
        };
        addListener(transitionListener);
        int[] loc = (int[]) endValues.values.get(PROPNAME_SCREEN_LOCATION);
        long startDelay = calculateRiseStartDelay(sceneRoot, endView, loc);
        return createAnimation(endView, startDelay, startY, endY, null, sDecelerate);
    }

    @Override
    public Animator onDisappear(ViewGroup sceneRoot,
            TransitionValues startValues, int startVisibility,
            TransitionValues endValues, int endVisibility) {
        View view = null;
        View startView = (startValues != null) ? startValues.view : null;
        View endView = (endValues != null) ? endValues.view : null;
        View overlayView = null;
        View viewToKeep = null;
        if (endView == null || endView.getParent() == null) {
            if (endView != null) {
                // endView was removed from its parent - add it to the overlay
                view = overlayView = endView;
            } else if (startView != null) {
                // endView does not exist. Use startView only under certain
                // conditions, because placing a view in an overlay necessitates
                // it being removed from its current parent
                if (startView.getParent() == null) {
                    // no parent - safe to use
                    view = overlayView = startView;
                } else if (startView.getParent() instanceof View &&
                        startView.getParent().getParent() == null) {
                    View startParent = (View) startView.getParent();
                    int id = startParent.getId();
                    if (id != View.NO_ID && sceneRoot.findViewById(id) != null && canRemoveViews()) {
                        // no parent, but its parent is unparented  but the parent
                        // hierarchy has been replaced by a new hierarchy with the same id
                        // and it is safe to un-parent startView
                        view = overlayView = startView;
                    }
                }
            }
        } else {
            // visibility change
            if (endVisibility == View.INVISIBLE) {
                view = endView;
                viewToKeep = view;
            } else {
                // Becoming GONE
                if (startView == endView) {
                    view = endView;
                    viewToKeep = view;
                } else {
                    view = startView;
                    overlayView = view;
                }
            }
        }
        final int finalVisibility = endVisibility;

        int[] loc = (int[]) startValues.values.get(PROPNAME_SCREEN_LOCATION);
        // TODO: add automatic facility to Visibility superclass for keeping views around
        if (overlayView != null) {
            // TODO: Need to do this for general case of adding to overlay
            long startDelay = calculateFallStartDelay(sceneRoot, overlayView, loc);
            int screenX = loc[0];
            int screenY = loc[1];
            loc = new int[2];
            sceneRoot.getLocationOnScreen(loc);
            overlayView.offsetLeftAndRight((screenX - loc[0]) - overlayView.getLeft());
            overlayView.offsetTopAndBottom((screenY - loc[1]) - overlayView.getTop());
            sceneRoot.getOverlay().add(overlayView);
            // TODO: add automatic facility to Visibility superclass for keeping views around
            final float startY = overlayView.getTranslationY();
            float endY = startY + sceneRoot.getHeight();
            final View finalView = view;
            final View finalOverlayView = overlayView;
            final View finalViewToKeep = viewToKeep;
            final ViewGroup finalSceneRoot = sceneRoot;
            final AnimatorListenerAdapter endListener = new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    finalView.setTranslationY(startY);
                    // TODO: restore view offset from overlay repositioning
                    if (finalViewToKeep != null) {
                        finalViewToKeep.setVisibility(finalVisibility);
                    }
                    if (finalOverlayView != null) {
                        finalSceneRoot.getOverlay().remove(finalOverlayView);
                    }
                }

                @Override
                public void onAnimationPause(Animator animation) {
                    if (finalOverlayView != null) {
                        finalSceneRoot.getOverlay().remove(finalOverlayView);
                    }
                }

                @Override
                public void onAnimationResume(Animator animation) {
                    if (finalOverlayView != null) {
                        finalSceneRoot.getOverlay().add(finalOverlayView);
                    }
                }
            };
            return createAnimation(view, startDelay, startY, endY, endListener, sAccelerate);
        }
        if (viewToKeep != null) {
            long startDelay = calculateFallStartDelay(sceneRoot, viewToKeep, loc);
            // TODO: find a different way to do this, like just changing the view to be
            // VISIBLE for the duration of the transition
            viewToKeep.setVisibility((View.VISIBLE));
            // TODO: add automatic facility to Visibility superclass for keeping views around
            final float startY = viewToKeep.getTranslationY();
            float endY = startY + sceneRoot.getHeight();
            final View finalView = view;
            final View finalOverlayView = overlayView;
            final View finalViewToKeep = viewToKeep;
            final ViewGroup finalSceneRoot = sceneRoot;
            final AnimatorListenerAdapter endListener = new AnimatorListenerAdapter() {
                boolean mCanceled = false;
                float mPausedY = -1;

                @Override
                public void onAnimationPause(Animator animation) {
                    if (finalViewToKeep != null && !mCanceled) {
                        finalViewToKeep.setVisibility(finalVisibility);
                    }
                    mPausedY = finalView.getTranslationY();
                    finalView.setTranslationY(startY);
                }

                @Override
                public void onAnimationResume(Animator animation) {
                    if (finalViewToKeep != null && !mCanceled) {
                        finalViewToKeep.setVisibility(View.VISIBLE);
                    }
                    finalView.setTranslationY(mPausedY);
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    mCanceled = true;
                    if (mPausedY >= 0) {
                        finalView.setTranslationY(mPausedY);
                    }
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (!mCanceled) {
                        finalView.setTranslationY(startY);
                    }
                    // TODO: restore view offset from overlay repositioning
                    if (finalViewToKeep != null && !mCanceled) {
                        finalViewToKeep.setVisibility(finalVisibility);
                    }
                    if (finalOverlayView != null) {
                        finalSceneRoot.getOverlay().remove(finalOverlayView);
                    }
                }
            };
            return createAnimation(view, startDelay, startY, endY, endListener, sAccelerate);
        }
        return null;
    }

    private long calculateFallStartDelay(View sceneRoot, View view, int[] viewLoc) {
        int[] loc = new int[2];
        sceneRoot.getLocationOnScreen(loc);
        int bottom = loc[1] + sceneRoot.getHeight();
        float distance = bottom - viewLoc[1] + view.getTranslationY();
        if (mHero != null) {
            mHero.getLocationOnScreen(loc);
            float heroX = loc[0] + mHero.getTranslationX() + (mHero.getWidth() / 2.0f);
            float viewX = viewLoc[0] + view.getTranslationX() + (view.getWidth() / 2.0f);
            float distanceX = Math.abs(heroX - viewX);
            float distanceXRatio = distanceX / sceneRoot.getWidth();
            distance += (1 - distanceXRatio) * mHero.getHeight();
        }
        float distanceRatio = distance/sceneRoot.getHeight() / 3;
        return Math.max(0, Math.round(distanceRatio * getDuration()));
    }

    private long calculateRiseStartDelay(View sceneRoot, View view, int[] viewLoc) {
        int[] loc = new int[2];
        sceneRoot.getLocationOnScreen(loc);
        int top = loc[1];
        float distance = viewLoc[1] + view.getTranslationY() - top;
        if (mHero != null) {
            mHero.getLocationOnScreen(loc);
            float heroX = loc[0] + mHero.getTranslationX() + (mHero.getWidth() / 2.0f);
            float viewX = viewLoc[0] + view.getTranslationX() + (view.getWidth() / 2.0f);
            float distanceX = Math.abs(heroX - viewX);
            float distanceXRatio = distanceX / sceneRoot.getWidth();
            distance += distanceXRatio * mHero.getHeight();
        }
        float distanceRatio = distance/sceneRoot.getHeight() / 3;
        return Math.max(0, Math.round(distanceRatio * getDuration()));
    }
}
