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

const WindowManagerTrace = require('flicker').com.android.server.wm.traces.common.
    windowmanager.WindowManagerTrace;
const WindowManagerState = require('flicker').com.android.server.wm.traces.common.
    windowmanager.WindowManagerState;

const Activity = require('flicker').com.android.server.wm.traces.common.
    windowmanager.windows.Activity;
const ActivityTask = require('flicker').com.android.server.wm.traces.common.
    windowmanager.windows.ActivityTask;
const Configuration = require('flicker').com.android.server.wm.traces.common.
    windowmanager.windows.Configuration;
const ConfigurationContainer = require('flicker').com.android.server.wm.traces.common.
    windowmanager.windows.ConfigurationContainer;
const DisplayArea = require('flicker').com.android.server.wm.traces.common.
    windowmanager.windows.DisplayArea;
const DisplayContent = require('flicker').com.android.server.wm.traces.common.
    windowmanager.windows.DisplayContent;
const KeyguardControllerState = require('flicker').com.android.server.wm.traces.common.
    windowmanager.windows.KeyguardControllerState;
const RootWindowContainer = require('flicker').com.android.server.wm.traces.common.
    windowmanager.windows.RootWindowContainer;
const WindowConfiguration = require('flicker').com.android.server.wm.traces.common.
    windowmanager.windows.WindowConfiguration;
const WindowContainer = require('flicker').com.android.server.wm.traces.common.
    windowmanager.windows.WindowContainer;
const WindowManagerPolicy = require('flicker').com.android.server.wm.traces.common.
    windowmanager.windows.WindowManagerPolicy;
const WindowState = require('flicker').com.android.server.wm.traces.common.
    windowmanager.windows.WindowState;
const WindowToken = require('flicker').com.android.server.wm.traces.common.
    windowmanager.windows.WindowToken;

const Rect = require('flicker').com.android.server.wm.traces.common.Rect;
const Bounds = require('flicker').com.android.server.wm.traces.common.Bounds;

function toRect(proto) {
    if (proto == null) {
        return null
    }
    return new Rect(proto.left, proto.top, proto.right, proto.bottom)
}

export {
    Activity,
    ActivityTask,
    Configuration,
    ConfigurationContainer,
    DisplayArea,
    DisplayContent,
    KeyguardControllerState,
    RootWindowContainer,
    WindowConfiguration,
    WindowContainer,
    WindowState,
    WindowToken,
    WindowManagerPolicy,
    WindowManagerTrace,
    WindowManagerState,
    Rect,
    Bounds,
    toRect
};
