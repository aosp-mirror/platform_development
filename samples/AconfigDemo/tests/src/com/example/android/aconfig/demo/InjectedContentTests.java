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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import com.example.android.aconfig.demo.flags.FeatureFlags;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class InjectedContentTests {

    @Test
    public void testInjectedContentFlagOn() throws Exception {
        FeatureFlags fakeFeatureFlag = mock(FeatureFlags.class);
        when(fakeFeatureFlag.appendInjectedContent()).thenReturn(true);
        InjectedContent injectedContent = new InjectedContent(fakeFeatureFlag);
        StringBuilder expected = new StringBuilder();
        expected.append("The flag: appendInjectedContent is ON!!\n\n");
        assertEquals("Get appendInjectedContent", expected.toString(), injectedContent.getContent());
    }
}

