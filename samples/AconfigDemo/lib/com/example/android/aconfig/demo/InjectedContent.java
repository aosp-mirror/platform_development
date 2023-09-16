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

import com.example.android.aconfig.demo.flags.FeatureFlags;
import javax.inject.Inject;


public class InjectedContent {

    private FeatureFlags featureFlags;

    @Inject
    public InjectedContent(FeatureFlags featureFlags) {
        this.featureFlags = featureFlags;
    };

    public String getContent() {

        StringBuilder sBuffer = new StringBuilder();

        if (featureFlags.appendInjectedContent()) {
            sBuffer.append("The flag: appendInjectedContent is ON!!\n\n");
        } else {
            sBuffer.append("The flag: appendInjectedContent is OFF!!\n\n");
        }

        if (featureFlags.readOnlyFlag()) {
            sBuffer.append("The flag: read only flag injected is ON!!\n\n");
        } else {
            sBuffer.append("The flag: read only flag injected is OFF!!\n\n");
        }

        return sBuffer.toString();
    }
}

