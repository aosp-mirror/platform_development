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

import java.util.HashSet;

/**
 * A struct object that holds the state of a {@link CardStreamFragment}.
 */
public class CardStreamState {
    protected Card[] visibleCards;
    protected Card[] hiddenCards;
    protected HashSet<String> dismissibleCards;
    protected String shownTag;

    protected CardStreamState(Card[] visible, Card[] hidden, HashSet<String> dismissible, String shownTag) {
        visibleCards = visible;
        hiddenCards = hidden;
        dismissibleCards = dismissible;
        this.shownTag = shownTag;
    }

}
