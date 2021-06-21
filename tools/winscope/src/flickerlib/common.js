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
const WindowLayoutParams= require('flicker').com.android.server.wm.traces.common.
    windowmanager.windows.WindowLayoutParams;
const WindowManagerPolicy = require('flicker').com.android.server.wm.traces.common.
    windowmanager.windows.WindowManagerPolicy;
const WindowState = require('flicker').com.android.server.wm.traces.common.
    windowmanager.windows.WindowState;
const WindowToken = require('flicker').com.android.server.wm.traces.common.
    windowmanager.windows.WindowToken;

const Matrix = require('flicker').com.android.server.wm.traces.common.layers.Transform.Matrix;
const Transform = require('flicker').com.android.server.wm.traces.common.layers.Transform;

const Bounds = require('flicker').com.android.server.wm.traces.common.Bounds;
const Buffer = require('flicker').com.android.server.wm.traces.common.Buffer;
const Color = require('flicker').com.android.server.wm.traces.common.Color;
const Point = require('flicker').com.android.server.wm.traces.common.Point;
const Rect = require('flicker').com.android.server.wm.traces.common.Rect;
const RectF = require('flicker').com.android.server.wm.traces.common.RectF;
const Region = require('flicker').com.android.server.wm.traces.common.Region;

function toBounds(proto) {
    if (proto == null) {
        return null
    }
    return new Bounds(proto.width ?? proto.w ?? 0, proto.height ?? proto.h ?? 0)
}

function toBuffer(proto) {
    if (proto == null) {
        return null
    }
    return new Buffer(proto.width ?? 0, proto.height ?? 0, proto.stride ?? 0, proto.format ?? 0)
}

function toColor(proto) {
    if (proto == null) {
        return null
    }
    return new Color(proto.r ?? 0, proto.g ?? 0, proto.b ?? 0, proto.a ?? 0)
}

function toPoint(proto) {
    if (proto == null) {
        return null
    }
    return new Point(proto.x ?? 0, proto.y ?? 0)
}

function toRect(proto) {
    if (proto == null) {
        return null
    }
    return new Rect(proto.left ?? 0, proto.top ?? 0, proto.right ?? 0, proto.bottom ?? 0)
}

function toRectF(proto) {
    if (proto == null) {
        return null
    }
    return new RectF(proto.left ?? 0, proto.top ?? 0, proto.right ?? 0, proto.bottom ?? 0)
}

function toRegion(proto) {
    if (proto == null) {
        return null
    }

    let rects = []
    for (let rectNr in proto.rect) {
        const rect = proto.rect[rectNr]
        const parsedRect = toRect(rect)
        rects.push(parsedRect)
    }

    return new Region(rects)
}

function toTransform(proto) {
    if (proto == null) {
        return null
    }
    const matrix = new Matrix(proto.dsdx ?? 0, proto.dtdx ?? 0,
        proto.tx ?? 0, proto.dsdy ?? 0, proto.dtdy ?? 0, proto.ty ?? 0)
    return new Transform(proto.type ?? 0, matrix)
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
    WindowLayoutParams,
    WindowManagerPolicy,
    WindowManagerTrace,
    WindowManagerState,
    Bounds,
    Buffer,
    Color,
    Point,
    Rect,
    RectF,
    Region,
    toBounds,
    toBuffer,
    toColor,
    toPoint,
    toRect,
    toRectF,
    toRegion,
    toTransform
};
