/*
 * Copyright (C) 2008 Google Inc.
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

package com.android.commands.monkey;

import java.io.FileOutputStream;
import java.io.IOException;

import android.app.IActivityManager;
import android.view.IWindowManager;
/**
 * monkey keyboard flip event
 */
public class MonkeyFlipEvent extends MonkeyEvent {

    // Raw keyboard flip event data
    // Works on emulator and dream

    private static final byte[] FLIP_0 = {
        0x7f, 0x06,
        0x00, 0x00,
        (byte) 0xe0, 0x39,
        0x01, 0x00,
        0x05, 0x00,
        0x00, 0x00,
        0x01, 0x00,
        0x00, 0x00 };

    private static final byte[] FLIP_1 = {
        (byte) 0x85, 0x06,
        0x00, 0x00,
        (byte) 0x9f, (byte) 0xa5,
        0x0c, 0x00,
        0x05, 0x00,
        0x00, 0x00,
        0x00, 0x00,
        0x00, 0x00 };

    private final boolean mKeyboardOpen;

    public MonkeyFlipEvent(boolean keyboardOpen) {
        super(EVENT_TYPE_FLIP);
        mKeyboardOpen = keyboardOpen;
    }

    @Override
    public int injectEvent(IWindowManager iwm, IActivityManager iam, int verbose) {
        if (verbose > 0) {
            System.out.println(":Sending Flip keyboardOpen=" + mKeyboardOpen);
        }

        // inject flip event
        try {
            FileOutputStream f = new FileOutputStream("/dev/input/event0");
            f.write(mKeyboardOpen ? FLIP_0 : FLIP_1);
            f.close();
            return MonkeyEvent.INJECT_SUCCESS;
        } catch (IOException e) {
            System.out.println("Got IOException performing flip" + e);
            return MonkeyEvent.INJECT_FAIL;
        }
    }
}
