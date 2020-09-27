/*
 * Copyright 2020, The Android Open Source Project
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

// Imports all the compiled common Flicker library classes and exports them
// as clean es6 modules rather than having them be commonjs modules

const WindowManagerTrace = require('flicker').com.android.server.wm.flicker
    .common.traces.windowmanager.WindowManagerTrace;
const WindowManagerTraceEntry = require('flicker').com.android.server.wm.
    flicker.common.traces.windowmanager.WindowManagerTraceEntry;

const WindowContainer = require('flicker').com.android.server.wm.flicker.common.
    traces.windowmanager.windows.WindowContainer;
const WindowState = require('flicker').com.android.server.wm.flicker.common.
    traces.windowmanager.windows.WindowState;
const DisplayContent = require('flicker').com.android.server.wm.flicker.common.
    traces.windowmanager.windows.DisplayContent;
const ActivityRecord = require('flicker').com.android.server.wm.flicker.common.
    traces.windowmanager.windows.ActivityRecord;
const WindowToken = require('flicker').com.android.server.wm.flicker.common.
    traces.windowmanager.windows.WindowToken;
const DisplayArea = require('flicker').com.android.server.wm.flicker.common.
    traces.windowmanager.windows.DisplayArea;
const RootDisplayArea = require('flicker').com.android.server.wm.flicker.common.
    traces.windowmanager.windows.RootDisplayArea;
const Task = require('flicker').com.android.server.wm.flicker.common.traces.
    windowmanager.windows.Task;

const Rect = require('flicker').com.android.server.wm.flicker.common.Rect;
const Bounds = require('flicker').com.android.server.wm.flicker.common.Bounds;

export {
  WindowManagerTrace,
  WindowManagerTraceEntry,
  WindowContainer,
  WindowState,
  DisplayContent,
  ActivityRecord,
  WindowToken,
  DisplayArea,
  RootDisplayArea,
  Task,
  Rect,
  Bounds,
};
