/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.apps.tag;

import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;

/**
 * @author nnk@google.com (Nick Kralevich)
 */
public class TagBroadcastReceiverTest extends ActivityInstrumentationTestCase2<Tags> {
    /**
     * Creates an {@link ActivityInstrumentationTestCase2} for the {@link Tags} activity.
     */
    public TagBroadcastReceiverTest() {
        super(Tags.class);
    }

    public void testWrongMessage() {
        TagBroadcastReceiver receiver = new TagBroadcastReceiver();
        Intent i = new Intent().setAction("BOGUS");
        receiver.onReceive(getActivity().getBaseContext(), i);
        assertDatabaseNoChange(receiver);
    }

    private void assertDatabaseNoChange(TagBroadcastReceiver receiver) {
        // TODO: implement
    }

}
