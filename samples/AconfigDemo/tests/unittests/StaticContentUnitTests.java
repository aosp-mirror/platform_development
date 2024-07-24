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

import android.platform.test.flag.junit.SetFlagsRule;
import com.example.android.aconfig.demo.flags.Flags;

import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public final class StaticContentUnitTests {
    @Rule public final SetFlagsRule mSetFlagsRule;

    @Parameterized.Parameters(name = "isInitWithDefault={0}")
    public static Object[] data() {
        return new Boolean[] {false, true};
    }

    public StaticContentUnitTests(boolean isInitWithDefault) {
        mIsInitWithDefault = isInitWithDefault;
        mSetFlagsRule = new SetFlagsRule(isInitWithDefault
                ? SetFlagsRule.DefaultInitValueType.DEVICE_DEFAULT
                : SetFlagsRule.DefaultInitValueType.NULL_DEFAULT);
    }

    private boolean mIsInitWithDefault;

    @Test
    public void staticContent_enable_staticFlag_disable_thirdFlag() {
        Assume.assumeTrue(!mIsInitWithDefault);
        mSetFlagsRule.enableFlags(Flags.FLAG_APPEND_STATIC_CONTENT);
        mSetFlagsRule.disableFlags(Flags.FLAG_THIRD_FLAG);
        mSetFlagsRule.disableFlags(Flags.FLAG_READ_ONLY_FLAG);
        StaticContent statiContent = new StaticContent();
        String ret = statiContent.getContent();
        StringBuilder expected = new StringBuilder();
        expected.append("The flag: appendStaticContent is ON!!\n\n");
        expected.append("The flag: thirdFlag is OFF!!\n\n");
        expected.append("The flag: read only flag static is OFF!!\n\n");
        assertEquals("Expected message", expected.toString(), ret);
    }

    @Test
    public void staticContent_enable_thirdFlag_with_default() {
        Assume.assumeTrue(mIsInitWithDefault);
        mSetFlagsRule.enableFlags(Flags.FLAG_THIRD_FLAG);
        StaticContent statiContent = new StaticContent();
        String ret = statiContent.getContent();
        StringBuilder expected = new StringBuilder();
        expected.append("The flag: appendStaticContent is OFF!!\n\n");
        expected.append("The flag: thirdFlag is ON!!\n\n");
        expected.append("The flag: read only flag static is OFF!!\n\n");
        assertEquals("Expected message", expected.toString(), ret);
    }
}

