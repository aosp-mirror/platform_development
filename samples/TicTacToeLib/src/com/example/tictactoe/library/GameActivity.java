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

package com.example.tictactoe.library;

import android.app.Activity;
import android.os.Bundle;

public class GameActivity extends Activity {

    public final static String EXTRA_START_WITH_HUMAN =
        "com.example.tictactoe.library.GameActivity.EXTRA_START_WITH_HUMAN";

    private boolean mTurnIsHuman;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        /*
         * IMPORTANT TIP: all resource IDs from this library must be
         * different from the projects that will include it. E.g.
         * if my layout were named "main.xml", I would have to use the ID
         * R.layout.main; however there is already a *different*
         * ID with the same name in the main project and when the library
         * gets merged in the project the wrong ID would end up being used.
         *
         * To avoid such potential conflicts, it's probably a good idea
         * to add a prefix to the library resource names.
         */
        setContentView(R.layout.lib_game);

        mTurnIsHuman = getIntent().getBooleanExtra(
                EXTRA_START_WITH_HUMAN, true);


    }

}
