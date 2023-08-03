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

import android.platform.test.annotations.RequiresFlagsDisabled;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import com.example.android.aconfig.demo.flags.Flags;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for how combination of flags states effects execution */
@RunWith(JUnit4.class)
public final class FlagStateComboTests {
    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_AWESOME_FLAG_1)
    public void requires_f1on_pass_or_skip() {}

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_AWESOME_FLAG_2)
    public void requires_f2off_pass_or_skip() {}

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_AWESOME_FLAG_1)
    public void requires_f1on_throw_or_skip() {
        throw new RuntimeException("This exception is expected if "
            + Flags.FLAG_AWESOME_FLAG_1 + " is enabled");
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_AWESOME_FLAG_2)
    public void requires_f2off_throw_or_skip() {
        throw new RuntimeException("This exception is expected if "
            + Flags.FLAG_AWESOME_FLAG_2 + " is disabled");
    }

    @Test
    @RequiresFlagsEnabled({Flags.FLAG_AWESOME_FLAG_1, Flags.FLAG_AWESOME_FLAG_2})
    public void requires_f1on_f2on_pass_or_skip() {}

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_AWESOME_FLAG_1)
    @RequiresFlagsDisabled(Flags.FLAG_AWESOME_FLAG_2)
    public void requires_f1on_f2off_throw_or_skip() {
        throw new RuntimeException("This exception is expected if "
            + Flags.FLAG_AWESOME_FLAG_1 + " is enabled and "
            + Flags.FLAG_AWESOME_FLAG_2 + " is disabled");
    }
}
