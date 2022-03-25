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

// WM
const WindowManagerTrace = require('flicker').com.android.server.wm.traces.
    common.windowmanager.WindowManagerTrace;
const WindowManagerState = require('flicker').com.android.server.wm.traces.
    common.windowmanager.WindowManagerState;
const Activity = require('flicker').com.android.server.wm.traces.common.
    windowmanager.windows.Activity;
const Configuration = require('flicker').com.android.server.wm.traces.common.
    windowmanager.windows.Configuration;
const ConfigurationContainer = require('flicker').com.android.server.wm.traces.
    common.windowmanager.windows.ConfigurationContainer;
const DisplayArea = require('flicker').com.android.server.wm.traces.common.
    windowmanager.windows.DisplayArea;
const DisplayContent = require('flicker').com.android.server.wm.traces.common.
    windowmanager.windows.DisplayContent;
const KeyguardControllerState = require('flicker').com.android.server.wm.
    traces.common.windowmanager.windows.KeyguardControllerState;
const RootWindowContainer = require('flicker').com.android.server.wm.traces.
    common.windowmanager.windows.RootWindowContainer;
const Task = require('flicker').com.android.server.wm.traces.common.
    windowmanager.windows.Task;
const TaskFragment = require('flicker').com.android.server.wm.traces.common.
    windowmanager.windows.TaskFragment;
const WindowConfiguration = require('flicker').com.android.server.wm.traces.
    common.windowmanager.windows.WindowConfiguration;
const WindowContainer = require('flicker').com.android.server.wm.traces.common.
    windowmanager.windows.WindowContainer;
const WindowLayoutParams= require('flicker').com.android.server.wm.traces.
    common.windowmanager.windows.WindowLayoutParams;
const WindowManagerPolicy = require('flicker').com.android.server.wm.traces.
    common.windowmanager.windows.WindowManagerPolicy;
const WindowState = require('flicker').com.android.server.wm.traces.common.
    windowmanager.windows.WindowState;
const WindowToken = require('flicker').com.android.server.wm.traces.common.
    windowmanager.windows.WindowToken;

// SF
const Layer = require('flicker').com.android.server.wm.traces.common.
    layers.Layer;
const BaseLayerTraceEntry = require('flicker').com.android.server.wm.traces.common.
    layers.BaseLayerTraceEntry;
const LayerTraceEntry = require('flicker').com.android.server.wm.traces.common.
    layers.LayerTraceEntry;
const LayerTraceEntryBuilder = require('flicker').com.android.server.wm.traces.
    common.layers.LayerTraceEntryBuilder;
const LayersTrace = require('flicker').com.android.server.wm.traces.common.
    layers.LayersTrace;
const Matrix22 = require('flicker').com.android.server.wm.traces.common
    .Matrix22;
const Matrix33 = require('flicker').com.android.server.wm.traces.common
    .Matrix33;
const Transform = require('flicker').com.android.server.wm.traces.common.
    layers.Transform;
const Display = require('flicker').com.android.server.wm.traces.common.
    layers.Display;

// Common
const Size = require('flicker').com.android.server.wm.traces.common.Size;
const ActiveBuffer = require('flicker').com.android.server.wm.traces.common
    .ActiveBuffer;
const Color3 = require('flicker').com.android.server.wm.traces.common.Color3;
const Color = require('flicker').com.android.server.wm.traces.common.Color;
const Point = require('flicker').com.android.server.wm.traces.common.Point;
const Rect = require('flicker').com.android.server.wm.traces.common.Rect;
const RectF = require('flicker').com.android.server.wm.traces.common.RectF;
const Region = require('flicker').com.android.server.wm.traces.common.region.Region;

//Tags
const Tag = require('flicker').com.android.server.wm.traces.common.tags.Tag;
const TagState = require('flicker').com.android.server.wm.traces.common.tags.TagState;
const TagTrace = require('flicker').com.android.server.wm.traces.common.tags.TagTrace;

//Errors
const Error = require('flicker').com.android.server.wm.traces.common.errors.Error;
const ErrorState = require('flicker').com.android.server.wm.traces.common.errors.ErrorState;
const ErrorTrace = require('flicker').com.android.server.wm.traces.common.errors.ErrorTrace;

// Service
const TaggingEngine = require('flicker').com.android.server.wm.traces.common.service.TaggingEngine;

const EMPTY_BUFFER = new ActiveBuffer(0, 0, 0, 0);
const EMPTY_COLOR3 = new Color3(-1, -1, -1);
const EMPTY_COLOR = new Color(-1, -1, -1, 0);
const EMPTY_RECT = new Rect(0, 0, 0, 0);
const EMPTY_RECTF = new RectF(0, 0, 0, 0);
const EMPTY_POINT = new Point(0, 0);
const EMPTY_MATRIX22 = new Matrix22(0, 0, 0, 0, 0, 0);
const EMPTY_MATRIX33 = new Matrix33(0, 0, 0, 0, 0, 0);
const EMPTY_TRANSFORM = new Transform(0, EMPTY_MATRIX33);

function toSize(proto) {
  if (proto == null) {
    return EMPTY_BOUNDS;
  }
  const width = proto.width ?? proto.w ?? 0;
  const height = proto.height ?? proto.h ?? 0;
  if (width || height) {
    return new Size(width, height);
  }
  return EMPTY_BOUNDS;
}

function toActiveBuffer(proto) {
  const width = proto?.width ?? 0;
  const height = proto?.height ?? 0;
  const stride = proto?.stride ?? 0;
  const format = proto?.format ?? 0;

  if (width || height || stride || format) {
    return new ActiveBuffer(width, height, stride, format);
  }
  return EMPTY_BUFFER;
}

function toColor3(proto) {
  if (proto == null) {
    return EMPTY_COLOR;
  }
  const r = proto.r ?? 0;
  const g = proto.g ?? 0;
  const b = proto.b ?? 0;
  if (r || g || b) {
    return new Color3(r, g, b);
  }
  return EMPTY_COLOR3;
}

function toColor(proto) {
  if (proto == null) {
    return EMPTY_COLOR;
  }
  const r = proto.r ?? 0;
  const g = proto.g ?? 0;
  const b = proto.b ?? 0;
  const a = proto.a ?? 0;
  if (r || g || b || a) {
    return new Color(r, g, b, a);
  }
  return EMPTY_COLOR;
}

function toPoint(proto) {
  if (proto == null) {
    return null;
  }
  const x = proto.x ?? 0;
  const y = proto.y ?? 0;
  if (x || y) {
    return new Point(x, y);
  }
  return EMPTY_POINT;
}

function toRect(proto) {
  if (proto == null) {
    return EMPTY_RECT;
  }

  const left = proto?.left ?? 0;
  const top = proto?.top ?? 0;
  const right = proto?.right ?? 0;
  const bottom = proto?.bottom ?? 0;
  if (left || top || right || bottom) {
    return new Rect(left, top, right, bottom);
  }
  return EMPTY_RECT;
}

function toRectF(proto) {
  if (proto == null) {
    return EMPTY_RECTF;
  }

  const left = proto?.left ?? 0;
  const top = proto?.top ?? 0;
  const right = proto?.right ?? 0;
  const bottom = proto?.bottom ?? 0;
  if (left || top || right || bottom) {
    return new RectF(left, top, right, bottom);
  }
  return EMPTY_RECTF;
}

function toRegion(proto) {
  if (proto == null) {
    return null;
  }

  const rects = [];
  for (let x = 0; x < proto.rect.length; x++) {
    const rect = proto.rect[x];
    const parsedRect = toRect(rect);
    rects.push(parsedRect);
  }

  return new Region(rects);
}

function toTransform(proto) {
  if (proto == null) {
    return EMPTY_TRANSFORM;
  }
  const dsdx = proto.dsdx ?? 0;
  const dtdx = proto.dtdx ?? 0;
  const tx = proto.tx ?? 0;
  const dsdy = proto.dsdy ?? 0;
  const dtdy = proto.dtdy ?? 0;
  const ty = proto.ty ?? 0;

  if (dsdx || dtdx || tx || dsdy || dtdy || ty) {
    const matrix = new Matrix33(dsdx, dtdx, tx, dsdy, dtdy, ty);
    return new Transform(proto.type ?? 0, matrix);
  }

  if (proto.type) {
    return new Transform(proto.type ?? 0, EMPTY_MATRIX33);
  }
  return EMPTY_TRANSFORM;
}

function toMatrix22(proto) {
  if (proto == null) {
    return EMPTY_MATRIX22;
  }
  const dsdx = proto.dsdx ?? 0;
  const dtdx = proto.dtdx ?? 0;
  const dsdy = proto.dsdy ?? 0;
  const dtdy = proto.dtdy ?? 0;

  if (dsdx || dtdx || dsdy || dtdy) {
    return new Matrix22(dsdx, dtdx, dsdy, dtdy);
  }

  return EMPTY_MATRIX22;
}

export {
  Activity,
  Configuration,
  ConfigurationContainer,
  DisplayArea,
  DisplayContent,
  KeyguardControllerState,
  RootWindowContainer,
  Task,
  TaskFragment,
  WindowConfiguration,
  WindowContainer,
  WindowState,
  WindowToken,
  WindowLayoutParams,
  WindowManagerPolicy,
  WindowManagerTrace,
  WindowManagerState,
  // SF
  BaseLayerTraceEntry,
  Layer,
  LayerTraceEntry,
  LayerTraceEntryBuilder,
  LayersTrace,
  Transform,
  Matrix22,
  Matrix33,
  Display,
  // Tags
  Tag,
  TagState,
  TagTrace,
  // Errors
  Error,
  ErrorState,
  ErrorTrace,
  // Common
  Size,
  ActiveBuffer,
  Color,
  Color3,
  Point,
  Rect,
  RectF,
  Region,
  // Service
  TaggingEngine,
  toSize,
  toActiveBuffer,
  toColor,
  toColor3,
  toPoint,
  toRect,
  toRectF,
  toRegion,
  toMatrix22,
  toTransform,
};
