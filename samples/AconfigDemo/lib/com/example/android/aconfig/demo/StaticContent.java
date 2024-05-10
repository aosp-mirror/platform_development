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

package com.example.android.aconfig.demo;

import static com.example.android.aconfig.demo.flags.Flags.appendStaticContent;
import static com.example.android.aconfig.demo.flags.Flags.thirdFlag;
import static com.example.android.aconfig.demo.flags.Flags.readOnlyFlag;

public class StaticContent {

    public StaticContent() {};

    public String getContent() {

        StringBuilder sBuffer = new StringBuilder();

        if (appendStaticContent()) {
            sBuffer.append("The flag: appendStaticContent is ON!!\n\n");
        } else {
            sBuffer.append("The flag: appendStaticContent is OFF!!\n\n");
        }

        if (thirdFlag()) {
            sBuffer.append("The flag: thirdFlag is ON!!\n\n");
        } else {
            sBuffer.append("The flag: thirdFlag is OFF!!\n\n");
        }

        if (readOnlyFlag()) {
            sBuffer.append("The flag: read only flag static is ON!!\n\n");
        } else {
            sBuffer.append("The flag: read only flag static is OFF!!\n\n");
        }

        return sBuffer.toString();
    }
}

