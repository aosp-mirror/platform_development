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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.android.batchstepsensor.R;

import java.util.ArrayList;

/**
 * A Card contains a description and has a visual state. Optionally a card also contains a title,
 * progress indicator and zero or more actions. It is constructed through the {@link Builder}.
 */
public class Card {

    public static final int ACTION_POSITIVE = 1;
    public static final int ACTION_NEGATIVE = 2;
    public static final int ACTION_NEUTRAL = 3;

    public static final int PROGRESS_TYPE_NO_PROGRESS = 0;
    public static final int PROGRESS_TYPE_NORMAL = 1;
    public static final int PROGRESS_TYPE_INDETERMINATE = 2;
    public static final int PROGRESS_TYPE_LABEL = 3;

    private OnCardClickListener mClickListener;


    // The card model contains a reference to its desired layout (for extensibility), title,
    // description, zero to many action buttons, and zero or 1 progress indicators.
    private int mLayoutId = R.layout.card;

    /**
     * Tag that uniquely identifies this card.
     */
    private String mTag = null;

    private String mTitle = null;
    private String mDescription = null;

    private View mCardView = null;
    private View mOverlayView = null;
    private TextView mTitleView = null;
    private TextView mDescView = null;
    private View mActionAreaView = null;

    private Animator mOngoingAnimator = null;

    /**
     * Visual state, either {@link #CARD_STATE_NORMAL}, {@link #CARD_STATE_FOCUSED} or
     * {@link #CARD_STATE_INACTIVE}.
     */
    private int mCardState = CARD_STATE_NORMAL;
    public static final int CARD_STATE_NORMAL = 1;
    public static final int CARD_STATE_FOCUSED = 2;
    public static final int CARD_STATE_INACTIVE = 3;

    /**
     * Represent actions that can be taken from the card.  Stylistically the developer can
     * designate the action as positive, negative (ok/cancel, for instance), or neutral.
     * This "type" can be used as a UI hint.
     * @see com.example.android.sensors.batchstepsensor.Card.CardAction
     */
    private ArrayList<CardAction> mCardActions = new ArrayList<CardAction>();

    /**
     * Some cards will have a sense of "progress" which should be associated with, but separated
     * from its "parent" card.  To push for simplicity in samples, Cards are designed to have
     * a maximum of one progress indicator per Card.
     */
    private CardProgress mCardProgress = null;

    public Card() {
    }

    public String getTag() {
        return mTag;
    }

    public View getView() {
        return mCardView;
    }


    public Card setDescription(String desc) {
        if (mDescView != null) {
            mDescription = desc;
            mDescView.setText(desc);
        }
        return this;
    }

    public Card setTitle(String title) {
        if (mTitleView != null) {
            mTitle = title;
            mTitleView.setText(title);
        }
        return this;
    }


    /**
     * Return the UI state, either {@link #CARD_STATE_NORMAL}, {@link #CARD_STATE_FOCUSED}
     * or {@link #CARD_STATE_INACTIVE}.
     */
    public int getState() {
        return mCardState;
    }

    /**
     * Set the UI state. The parameter describes the state and must be either
     * {@link #CARD_STATE_NORMAL}, {@link #CARD_STATE_FOCUSED} or {@link #CARD_STATE_INACTIVE}.
     * Note: This method must be called from the UI Thread.
     * @param state
     * @return The card itself, allows for chaining of calls
     */
    public Card setState(int state) {
        mCardState = state;
        if (null != mOverlayView) {
            if (null != mOngoingAnimator) {
                mOngoingAnimator.end();
                mOngoingAnimator = null;
            }
            switch (state) {
                case CARD_STATE_NORMAL: {
                    mOverlayView.setVisibility(View.GONE);
                    mOverlayView.setAlpha(1.f);
                    break;
                }
                case CARD_STATE_FOCUSED: {
                    mOverlayView.setVisibility(View.VISIBLE);
                    mOverlayView.setBackgroundResource(R.drawable.card_overlay_focused);
                    ObjectAnimator animator = ObjectAnimator.ofFloat(mOverlayView, "alpha", 0.f);
                    animator.setRepeatMode(ObjectAnimator.REVERSE);
                    animator.setRepeatCount(ObjectAnimator.INFINITE);
                    animator.setDuration(1000);
                    animator.start();
                    mOngoingAnimator = animator;
                    break;
                }
                case CARD_STATE_INACTIVE: {
                    mOverlayView.setVisibility(View.VISIBLE);
                    mOverlayView.setAlpha(1.f);
                    mOverlayView.setBackgroundColor(Color.argb(0xaa, 0xcc, 0xcc, 0xcc));
                    break;
                }
            }
        }
        return this;
    }

    /**
     * Set the type of progress indicator.
     * The progress type can only be changed if the Card was initially build with a progress
     * indicator.
     * See {@link Builder#setProgressType(int)}.
     * Must be a value of either {@link #PROGRESS_TYPE_NORMAL},
     * {@link #PROGRESS_TYPE_INDETERMINATE}, {@link #PROGRESS_TYPE_LABEL} or
     * {@link #PROGRESS_TYPE_NO_PROGRESS}.
     * @param progressType
     * @return The card itself, allows for chaining of calls
     */
    public Card setProgressType(int progressType) {
        if (mCardProgress == null) {
            mCardProgress = new CardProgress();
        }
        mCardProgress.setProgressType(progressType);
        return this;
    }

    /**
     * Return the progress indicator type. A value of either {@link #PROGRESS_TYPE_NORMAL},
     * {@link #PROGRESS_TYPE_INDETERMINATE}, {@link #PROGRESS_TYPE_LABEL}. Otherwise if no progress
     * indicator is enabled, {@link #PROGRESS_TYPE_NO_PROGRESS} is returned.
     * @return
     */
    public int getProgressType() {
        if (mCardProgress == null) {
            return PROGRESS_TYPE_NO_PROGRESS;
        }
        return mCardProgress.progressType;
    }

    /**
     * Set the progress to the specified value. Only applicable if the card has a
     * {@link #PROGRESS_TYPE_NORMAL} progress type.
     * @param progress
     * @return
     * @see #setMaxProgress(int)
     */
    public Card setProgress(int progress) {
        if (mCardProgress != null) {
            mCardProgress.setProgress(progress);
        }
        return this;
    }

    /**
     * Set the range of the progress to 0...max. Only applicable if the card has a
     * {@link #PROGRESS_TYPE_NORMAL} progress type.
     * @return
     */
    public Card setMaxProgress(int max){
        if (mCardProgress != null) {
            mCardProgress.setMax(max);
        }
        return this;
    }

    /**
     * Set the label text for the progress if the card has a progress type of
     * {@link #PROGRESS_TYPE_NORMAL}, {@link #PROGRESS_TYPE_INDETERMINATE} or
     * {@link #PROGRESS_TYPE_LABEL}
     * @param text
     * @return
     */
    public Card setProgressLabel(String text) {
        if (mCardProgress != null) {
            mCardProgress.setProgressLabel(text);
        }
        return this;
    }

    /**
     * Toggle the visibility of the progress section of the card. Only applicable if
     * the card has a progress type of
     * {@link #PROGRESS_TYPE_NORMAL}, {@link #PROGRESS_TYPE_INDETERMINATE} or
     * {@link #PROGRESS_TYPE_LABEL}.
     * @param isVisible
     * @return
     */
    public Card setProgressVisibility(boolean isVisible) {
        if (mCardProgress.progressView == null) {
            return this; // Card does not have progress
        }
        mCardProgress.progressView.setVisibility(isVisible ? View.VISIBLE : View.GONE);

        return this;
    }

    /**
     * Adds an action to this card during build time.
     *
     * @param label
     * @param id
     * @param type
     */
    private void addAction(String label, int id, int type) {
        CardAction cardAction = new CardAction();
        cardAction.label = label;
        cardAction.id = id;
        cardAction.type = type;
        mCardActions.add(cardAction);
    }

    /**
     * Toggles the visibility of a card action.
     * @param actionId
     * @param isVisible
     * @return
     */
    public Card setActionVisibility(int actionId, boolean isVisible) {
        int visibilityFlag = isVisible ? View.VISIBLE : View.GONE;
        for (CardAction action : mCardActions) {
            if (action.id == actionId && action.actionView != null) {
                action.actionView.setVisibility(visibilityFlag);
            }
        }
        return this;
    }

    /**
     * Toggles visibility of the action area of this Card through an animation.
     * @param isVisible
     * @return
     */
    public Card setActionAreaVisibility(boolean isVisible) {
        if (mActionAreaView == null) {
            return this; // Card does not have an action area
        }

        if (isVisible) {
            // Show the action area
            mActionAreaView.setVisibility(View.VISIBLE);
            mActionAreaView.setPivotY(0.f);
            mActionAreaView.setPivotX(mCardView.getWidth() / 2.f);
            mActionAreaView.setAlpha(0.5f);
            mActionAreaView.setRotationX(-90.f);
            mActionAreaView.animate().rotationX(0.f).alpha(1.f).setDuration(400);
        } else {
            // Hide the action area
            mActionAreaView.setPivotY(0.f);
            mActionAreaView.setPivotX(mCardView.getWidth() / 2.f);
            mActionAreaView.animate().rotationX(-90.f).alpha(0.f).setDuration(400).setListener(
                    new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mActionAreaView.setVisibility(View.GONE);
                        }
                    });
        }
        return this;
    }


    /**
     * Creates a shallow clone of the card.  Shallow means all values are present, but no views.
     * This is useful for saving/restoring in the case of configuration changes, like screen
     * rotation.
     *
     * @return A shallow clone of the card instance
     */
    public Card createShallowClone() {
        Card cloneCard = new Card();

        // Outer card values
        cloneCard.mTitle = mTitle;
        cloneCard.mDescription = mDescription;
        cloneCard.mTag = mTag;
        cloneCard.mLayoutId = mLayoutId;
        cloneCard.mCardState = mCardState;

        // Progress
        if (mCardProgress != null) {
            cloneCard.mCardProgress = mCardProgress.createShallowClone();
        }

        // Actions
        for (CardAction action : mCardActions) {
            cloneCard.mCardActions.add(action.createShallowClone());
        }

        return cloneCard;
    }


    /**
     * Prepare the card to be stored for configuration change.
     */
    public void prepareForConfigurationChange() {
        // Null out views.
        mCardView = null;
        for (CardAction action : mCardActions) {
            action.actionView = null;
        }
        mCardProgress.progressView = null;
    }

    /**
     * Creates a new {@link #Card}.
     */
    public static class Builder {
        private Card mCard;

        /**
         * Instantiate the builder with data from a shallow clone.
         * @param listener
         * @param card
         * @see Card#createShallowClone()
         */
        protected Builder(OnCardClickListener listener, Card card) {
            mCard = card;
            mCard.mClickListener = listener;
        }

        /**
         * Instantiate the builder with the tag of the card.
         * @param listener
         * @param tag
         */
        public Builder(OnCardClickListener listener, String tag) {
            mCard = new Card();
            mCard.mTag = tag;
            mCard.mClickListener = listener;
        }

        public Builder setTitle(String title) {
            mCard.mTitle = title;
            return this;
        }

        public Builder setDescription(String desc) {
            mCard.mDescription = desc;
            return this;
        }

        /**
         * Add an action.
         * The type describes how this action will be displayed. Accepted values are
         * {@link #ACTION_NEUTRAL}, {@link #ACTION_POSITIVE} or {@link #ACTION_NEGATIVE}.
         *
         * @param label The text to display for this action
         * @param id Identifier for this action, supplied in the click listener
         * @param type UI style of action
         * @return
         */
        public Builder addAction(String label, int id, int type) {
            mCard.addAction(label, id, type);
            return this;
        }

        /**
         * Override the default layout.
         * The referenced layout file has to contain the same identifiers as defined in the default
         * layout configuration.
         * @param layout
         * @return
         * @see R.layout.card
         */
        public Builder setLayout(int layout) {
            mCard.mLayoutId = layout;
            return this;
        }

        /**
         * Set the type of progress bar to display.
         * Accepted values are:
         * <ul>
         *     <li>{@link #PROGRESS_TYPE_NO_PROGRESS} disables the progress indicator</li>
         *     <li>{@link #PROGRESS_TYPE_NORMAL} 
         *     displays a standard, linear progress indicator.</li>
         *     <li>{@link #PROGRESS_TYPE_INDETERMINATE} displays an indeterminate (infite) progress
         *     indicator.</li>
         *     <li>{@link #PROGRESS_TYPE_LABEL} only displays a label text in the progress area
         *     of the card.</li>
         * </ul>
         *
         * @param progressType
         * @return
         */
        public Builder setProgressType(int progressType) {
            mCard.setProgressType(progressType);
            return this;
        }

        public Builder setProgressLabel(String label) {
            // ensure the progress layout has been initialized, use 'no progress' by default
            if (mCard.mCardProgress == null) {
                mCard.setProgressType(PROGRESS_TYPE_NO_PROGRESS);
            }
            mCard.mCardProgress.label = label;
            return this;
        }

        public Builder setProgressMaxValue(int maxValue) {
            // ensure the progress layout has been initialized, use 'no progress' by default
            if (mCard.mCardProgress == null) {
                mCard.setProgressType(PROGRESS_TYPE_NO_PROGRESS);
            }
            mCard.mCardProgress.maxValue = maxValue;
            return this;
        }

        public Builder setStatus(int status) {
            mCard.setState(status);
            return this;
        }

        public Card build(Activity activity) {
            LayoutInflater inflater = activity.getLayoutInflater();
            // Inflating the card.
            ViewGroup cardView = (ViewGroup) inflater.inflate(mCard.mLayoutId,
                    (ViewGroup) activity.findViewById(R.id.card_stream), false);

            // Check that the layout contains a TextView with the card_title id
            View viewTitle = cardView.findViewById(R.id.card_title);
            if (mCard.mTitle != null && viewTitle != null) {
                mCard.mTitleView = (TextView) viewTitle;
                mCard.mTitleView.setText(mCard.mTitle);
            } else if (viewTitle != null) {
                viewTitle.setVisibility(View.GONE);
            }

            // Check that the layout contains a TextView with the card_content id
            View viewDesc = cardView.findViewById(R.id.card_content);
            if (mCard.mDescription != null && viewDesc != null) {
                mCard.mDescView = (TextView) viewDesc;
                mCard.mDescView.setText(mCard.mDescription);
            } else if (viewDesc != null) {
                cardView.findViewById(R.id.card_content).setVisibility(View.GONE);
            }


            ViewGroup actionArea = (ViewGroup) cardView.findViewById(R.id.card_actionarea);

            // Inflate Progress
            initializeProgressView(inflater, actionArea);

            // Inflate all action views.
            initializeActionViews(inflater, cardView, actionArea);

            mCard.mCardView = cardView;
            mCard.mOverlayView = cardView.findViewById(R.id.card_overlay);

            return mCard;
        }

        /**
         * Initialize data from the given card.
         * @param card
         * @return
         * @see Card#createShallowClone()
         */
        public Builder cloneFromCard(Card card) {
            mCard = card.createShallowClone();
            return this;
        }

        /**
         * Build the action views by inflating the appropriate layouts and setting the text and 
         * values.
         * @param inflater
         * @param cardView
         * @param actionArea
         */
        private void initializeActionViews(LayoutInflater inflater, ViewGroup cardView,
                                           ViewGroup actionArea) {
            if (!mCard.mCardActions.isEmpty()) {
                // Set action area to visible only when actions are visible
                actionArea.setVisibility(View.VISIBLE);
                mCard.mActionAreaView = actionArea;
            }

            // Inflate all card actions
            for (final CardAction action : mCard.mCardActions) {

                int useActionLayout = 0;
                switch (action.type) {
                    case Card.ACTION_POSITIVE:
                        useActionLayout = R.layout.card_button_positive;
                        break;
                    case Card.ACTION_NEGATIVE:
                        useActionLayout = R.layout.card_button_negative;
                        break;
                    case Card.ACTION_NEUTRAL:
                    default:
                        useActionLayout = R.layout.card_button_neutral;
                        break;
                }

                action.actionView = inflater.inflate(useActionLayout, actionArea, false);
                Button actionButton = (Button) action.actionView.findViewById(R.id.card_button);

                actionButton.setText(action.label);
                actionButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mCard.mClickListener.onCardClick(action.id, mCard.mTag);
                    }
                });
                actionArea.addView(action.actionView);
            }
        }

        /**
         * Build the progress view into the given ViewGroup.
         *
         * @param inflater
         * @param actionArea
         */
        private void initializeProgressView(LayoutInflater inflater, ViewGroup actionArea) {

            // Only inflate progress layout if a progress type other than NO_PROGRESS was set.
            if (mCard.mCardProgress != null) {
                //Setup progress card.
                View progressView = inflater.inflate(R.layout.card_progress, actionArea, false);
                ProgressBar progressBar = 
                        (ProgressBar) progressView.findViewById(R.id.card_progress);
                ((TextView) progressView.findViewById(R.id.card_progress_text))
                        .setText(mCard.mCardProgress.label);
                progressBar.setMax(mCard.mCardProgress.maxValue);
                progressBar.setProgress(0);
                mCard.mCardProgress.progressView = progressView;
                mCard.mCardProgress.setProgressType(mCard.getProgressType());
                actionArea.addView(progressView);
            }
        }
    }

    /**
     * Represents a clickable action, accessible from the bottom of the card.
     * Fields include the label, an ID to specify the action that was performed in the callback,
     * an action type (positive, negative, neutral), and the callback.
     */
    public class CardAction {

        public String label;
        public int id;
        public int type;
        public View actionView;

        public CardAction createShallowClone() {
            CardAction actionClone = new CardAction();
            actionClone.label = label;
            actionClone.id = id;
            actionClone.type = type;
            return actionClone;
            // Not the view.  Never the view (don't want to hold view references for
            // onConfigurationChange.
        }

    }

    /**
     * Describes the progress of a {@link Card}.
     * Three types of progress are supported:
     * <ul><li>{@link Card#PROGRESS_TYPE_NORMAL: Standard progress bar with label text</li>
     * <li>{@link Card#PROGRESS_TYPE_INDETERMINATE}: Indeterminate progress bar with label txt</li>
     * <li>{@link Card#PROGRESS_TYPE_LABEL}: Label only, no progresss bar</li>
     * </ul>
     */
    public class CardProgress {
        private int progressType = Card.PROGRESS_TYPE_NO_PROGRESS;
        private String label = "";
        private int currProgress = 0;
        private int maxValue = 100;

        public View progressView = null;
        private ProgressBar progressBar = null;
        private TextView progressLabel = null;

        public CardProgress createShallowClone() {
            CardProgress progressClone = new CardProgress();
            progressClone.label = label;
            progressClone.currProgress = currProgress;
            progressClone.maxValue = maxValue;
            progressClone.progressType = progressType;
            return progressClone;
        }

        /**
         * Set the progress. Only useful for the type {@link #PROGRESS_TYPE_NORMAL}.
         * @param progress
         * @see android.widget.ProgressBar#setProgress(int)
         */
        public void setProgress(int progress) {
            currProgress = progress;
            final ProgressBar bar = getProgressBar();
            if (bar != null) {
                bar.setProgress(currProgress);
                bar.invalidate();
            }
        }

        /**
         * Set the range of the progress to 0...max.
         * Only useful for the type {@link #PROGRESS_TYPE_NORMAL}.
         * @param max
         * @see android.widget.ProgressBar#setMax(int)
         */
        public void setMax(int max) {
            maxValue = max;
            final ProgressBar bar = getProgressBar();
            if (bar != null) {
                bar.setMax(maxValue);
            }
        }

        /**
         * Set the label text that appears near the progress indicator.
         * @param text
         */
        public void setProgressLabel(String text) {
            label = text;
            final TextView labelView = getProgressLabel();
            if (labelView != null) {
                labelView.setText(text);
            }
        }

        /**
         * Set how progress is displayed. The parameter must be one of three supported types:
         * <ul><li>{@link Card#PROGRESS_TYPE_NORMAL: Standard progress bar with label text</li>
         * <li>{@link Card#PROGRESS_TYPE_INDETERMINATE}: 
         * Indeterminate progress bar with label txt</li>
         * <li>{@link Card#PROGRESS_TYPE_LABEL}: Label only, no progresss bar</li>
         * @param type
         */
        public void setProgressType(int type) {
            progressType = type;
            if (progressView != null) {
                switch (type) {
                    case PROGRESS_TYPE_NO_PROGRESS: {
                        progressView.setVisibility(View.GONE);
                        break;
                    }
                    case PROGRESS_TYPE_NORMAL: {
                        progressView.setVisibility(View.VISIBLE);
                        getProgressBar().setIndeterminate(false);
                        break;
                    }
                    case PROGRESS_TYPE_INDETERMINATE: {
                        progressView.setVisibility(View.VISIBLE);
                        getProgressBar().setIndeterminate(true);
                        break;
                    }
                }
            }
        }

        private TextView getProgressLabel() {
            if (progressLabel != null) {
                return progressLabel;
            } else if (progressView != null) {
                progressLabel = (TextView) progressView.findViewById(R.id.card_progress_text);
                return progressLabel;
            } else {
                return null;
            }
        }

        private ProgressBar getProgressBar() {
            if (progressBar != null) {
                return progressBar;
            } else if (progressView != null) {
                progressBar = (ProgressBar) progressView.findViewById(R.id.card_progress);
                return progressBar;
            } else {
                return null;
            }
        }

    }
}

