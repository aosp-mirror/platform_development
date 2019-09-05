/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.example.android.intentplayground;

/**
 * Created by wvk on 7/25/17.
 */

enum ActivityFlag {
    CLEAR_TASK_ON_LAUNCH, ALLOW_TASK_REPARENTING, LAUNCH_MODE_STANDARD, LAUNCH_MODE_SINGLE_TOP,
    LAUNCH_MODE_SINGLE_TASK, LAUNCH_MODE_SINGLE_INSTANCE, DOCUMENT_LAUNCH_MODE_INTO_EXISTING,
    DOCUMENT_LAUNCH_MODE_ALWAYS, DOCUMENT_LAUNCH_MODE_NONE, DOCUMENT_LAUNCH_MODE_NEVER;
}
