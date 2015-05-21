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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;

import com.example.android.batchstepsensor.R;

/**
 * A Fragment that handles a stream of cards.
 * Cards can be shown or hidden. When a card is shown it can also be marked as not-dismissible, see
 * {@link CardStreamLinearLayout#addCard(android.view.View, boolean)}.
 */
public class CardStreamFragment extends Fragment {

    private static final int INITIAL_SIZE = 15;
    private CardStreamLinearLayout mLayout = null;
    private LinkedHashMap<String, Card> mVisibleCards = new LinkedHashMap<String, Card>(INITIAL_SIZE);
    private HashMap<String, Card> mHiddenCards = new HashMap<String, Card>(INITIAL_SIZE);
    private HashSet<String> mDismissibleCards = new HashSet<String>(INITIAL_SIZE);

    // Set the listener to handle dismissed cards by moving them to the hidden cards map.
    private CardStreamLinearLayout.OnDissmissListener mCardDismissListener =
            new CardStreamLinearLayout.OnDissmissListener() {
                @Override
                public void onDismiss(String tag) {
                    dismissCard(tag);
                }
            };


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.cardstream, container, false);
        mLayout = (CardStreamLinearLayout) view.findViewById(R.id.card_stream);
        mLayout.setOnDismissListener(mCardDismissListener);

        return view;
    }

    /**
     * Add a visible, dismissible card to the card stream.
     *
     * @param card
     */
    public void addCard(Card card) {
        final String tag = card.getTag();

        if (!mVisibleCards.containsKey(tag) && !mHiddenCards.containsKey(tag)) {
            final View view = card.getView();
            view.setTag(tag);
            mHiddenCards.put(tag, card);
        }
    }

    /**
     * Add and show a card.
     *
     * @param card
     * @param show
     */
    public void addCard(Card card, boolean show) {
        addCard(card);
        if (show) {
            showCard(card.getTag());
        }
    }

    /**
     * Remove a card and return true if it has been successfully removed.
     *
     * @param tag
     * @return
     */
    public boolean removeCard(String tag) {
        // Attempt to remove a visible card first
        Card card = mVisibleCards.get(tag);
        if (card != null) {
            // Card is visible, also remove from layout
            mVisibleCards.remove(tag);
            mLayout.removeView(card.getView());
            return true;
        } else {
            // Card is hidden, no need to remove from layout
            card = mHiddenCards.remove(tag);
            return card != null;
        }
    }

    /**
     * Show a dismissible card, returns false if the card could not be shown.
     *
     * @param tag
     * @return
     */
    public boolean showCard(String tag) {
        return showCard(tag, true);
    }

    /**
     * Show a card, returns false if the card could not be shown.
     *
     * @param tag
     * @param dismissible
     * @return
     */
    public boolean showCard(String tag, boolean dismissible) {
        final Card card = mHiddenCards.get(tag);
        // ensure the card is hidden and not already visible
        if (card != null && !mVisibleCards.containsValue(tag)) {
            mHiddenCards.remove(tag);
            mVisibleCards.put(tag, card);
            mLayout.addCard(card.getView(), dismissible);
            if (dismissible) {
                mDismissibleCards.add(tag);
            }
            return true;
        }
        return false;
    }

    /**
     * Hides the card, returns false if the card could not be hidden.
     *
     * @param tag
     * @return
     */
    public boolean hideCard(String tag) {
        final Card card = mVisibleCards.get(tag);
        if (card != null) {
            mVisibleCards.remove(tag);
            mDismissibleCards.remove(tag);
            mHiddenCards.put(tag, card);

            mLayout.removeView(card.getView());
            return true;
        }
        return mHiddenCards.containsValue(tag);
    }


    private void dismissCard(String tag) {
        final Card card = mVisibleCards.get(tag);
        if (card != null) {
            mDismissibleCards.remove(tag);
            mVisibleCards.remove(tag);
            mHiddenCards.put(tag, card);
        }
    }


    public boolean isCardVisible(String tag) {
        return mVisibleCards.containsValue(tag);
    }

    /**
     * Returns true if the card is shown and is dismissible.
     *
     * @param tag
     * @return
     */
    public boolean isCardDismissible(String tag) {
        return mDismissibleCards.contains(tag);
    }

    /**
     * Returns the Card for this tag.
     *
     * @param tag
     * @return
     */
    public Card getCard(String tag) {
        final Card card = mVisibleCards.get(tag);
        if (card != null) {
            return card;
        } else {
            return mHiddenCards.get(tag);
        }
    }

    /**
     * Moves the view port to show the card with this tag.
     *
     * @param tag
     * @see CardStreamLinearLayout#setFirstVisibleCard(String)
     */
    public void setFirstVisibleCard(String tag) {
        final Card card = mVisibleCards.get(tag);
        if (card != null) {
            mLayout.setFirstVisibleCard(tag);
        }
    }

    public int getVisibleCardCount() {
        return mVisibleCards.size();
    }

    public Collection<Card> getVisibleCards() {
        return mVisibleCards.values();
    }

    public void restoreState(CardStreamState state, OnCardClickListener callback) {
        // restore hidden cards
        for (Card c : state.hiddenCards) {
            Card card = new Card.Builder(callback,c).build(getActivity());
            mHiddenCards.put(card.getTag(), card);
        }

        // temporarily set up list of dismissible
        final HashSet<String> dismissibleCards = state.dismissibleCards;

        //restore shown cards
        for (Card c : state.visibleCards) {
            Card card = new Card.Builder(callback,c).build(getActivity());
            addCard(card);
            final String tag = card.getTag();
            showCard(tag, dismissibleCards.contains(tag));
        }

        // move to first visible card
        final String firstShown = state.shownTag;
        if (firstShown != null) {
            mLayout.setFirstVisibleCard(firstShown);
        }

        mLayout.triggerShowInitialAnimation();
    }

    public CardStreamState dumpState() {
        final Card[] visible = cloneCards(mVisibleCards.values());
        final Card[] hidden = cloneCards(mHiddenCards.values());
        final HashSet<String> dismissible = new HashSet<String>(mDismissibleCards);
        final String firstVisible = mLayout.getFirstVisibleCardTag();

        return new CardStreamState(visible, hidden, dismissible, firstVisible);
    }

    private Card[] cloneCards(Collection<Card> cards) {
        Card[] cardArray = new Card[cards.size()];
        int i = 0;
        for (Card c : cards) {
            cardArray[i++] = c.createShallowClone();
        }

        return cardArray;
    }

}
