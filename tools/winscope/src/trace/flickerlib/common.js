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
const WindowManagerTrace = require('flicker').android.tools.common.traces.wm.WindowManagerTrace;
const WindowManagerState = require('flicker').android.tools.common.traces.wm.WindowManagerState;
const WindowManagerTraceEntryBuilder =
  require('flicker').android.tools.common.traces.wm.WindowManagerTraceEntryBuilder;
const Activity = require('flicker').android.tools.common.traces.wm.Activity;
const Configuration = require('flicker').android.tools.common.traces.wm.Configuration;
const ConfigurationContainer =
  require('flicker').android.tools.common.traces.wm.ConfigurationContainer;
const DisplayArea = require('flicker').android.tools.common.traces.wm.DisplayArea;
const DisplayContent = require('flicker').android.tools.common.traces.wm.DisplayContent;
const DisplayCutout = require('flicker').android.tools.common.traces.wm.DisplayCutout;
const KeyguardControllerState =
  require('flicker').android.tools.common.traces.wm.KeyguardControllerState;
const RootWindowContainer = require('flicker').android.tools.common.traces.wm.RootWindowContainer;
const Task = require('flicker').android.tools.common.traces.wm.Task;
const TaskFragment = require('flicker').android.tools.common.traces.wm.TaskFragment;
const WindowConfiguration = require('flicker').android.tools.common.traces.wm.WindowConfiguration;
const WindowContainer = require('flicker').android.tools.common.traces.wm.WindowContainer;
const WindowLayoutParams = require('flicker').android.tools.common.traces.wm.WindowLayoutParams;
const WindowManagerPolicy = require('flicker').android.tools.common.traces.wm.WindowManagerPolicy;
const WindowState = require('flicker').android.tools.common.traces.wm.WindowState;
const WindowToken = require('flicker').android.tools.common.traces.wm.WindowToken;

// SF
const HwcCompositionType =
  require('flicker').android.tools.common.traces.surfaceflinger.HwcCompositionType;
const Layer = require('flicker').android.tools.common.traces.surfaceflinger.Layer;
const LayerProperties =
  require('flicker').android.tools.common.traces.surfaceflinger.LayerProperties;
const LayerTraceEntry =
  require('flicker').android.tools.common.traces.surfaceflinger.LayerTraceEntry;
const LayerTraceEntryBuilder =
  require('flicker').android.tools.common.traces.surfaceflinger.LayerTraceEntryBuilder;
const LayersTrace = require('flicker').android.tools.common.traces.surfaceflinger.LayersTrace;
const Transform = require('flicker').android.tools.common.traces.surfaceflinger.Transform;
const Display = require('flicker').android.tools.common.traces.surfaceflinger.Display;
const Region = require('flicker').android.tools.common.datatypes.Region;

// Event Log
const EventLog = require('flicker').android.tools.common.traces.events.EventLog;
const CujEvent = require('flicker').android.tools.common.traces.events.CujEvent;
const CujType = require('flicker').android.tools.common.traces.events.CujType;
const Event = require('flicker').android.tools.common.traces.events.Event;
const FlickerEvent = require('flicker').android.tools.common.traces.events.FlickerEvent;
const FocusEvent = require('flicker').android.tools.common.traces.events.FocusEvent;
const EventLogParser = require('flicker').android.tools.common.parsers.events.EventLogParser;
const CujTrace = require('flicker').android.tools.common.parsers.events.CujTrace;
const Cuj = require('flicker').android.tools.common.parsers.events.Cuj;

// Transitions
const Transition = require('flicker').android.tools.common.traces.wm.Transition;
const TransitionType = require('flicker').android.tools.common.traces.wm.TransitionType;
const TransitionChange = require('flicker').android.tools.common.traces.wm.TransitionChange;
const TransitionsTrace = require('flicker').android.tools.common.traces.wm.TransitionsTrace;
const ShellTransitionData = require('flicker').android.tools.common.traces.wm.ShellTransitionData;
const WmTransitionData = require('flicker').android.tools.common.traces.wm.WmTransitionData;

// Common
const Size = require('flicker').android.tools.common.datatypes.Size;
const ActiveBuffer = require('flicker').android.tools.common.datatypes.ActiveBuffer;
const Color = require('flicker').android.tools.common.datatypes.Color;
const Insets = require('flicker').android.tools.common.datatypes.Insets;
const Matrix33 = require('flicker').android.tools.common.datatypes.Matrix33;
const PlatformConsts = require('flicker').android.tools.common.PlatformConsts;
const Rotation = require('flicker').android.tools.common.Rotation;
const Point = require('flicker').android.tools.common.datatypes.Point;
const PointF = require('flicker').android.tools.common.datatypes.PointF;
const Rect = require('flicker').android.tools.common.datatypes.Rect;
const RectF = require('flicker').android.tools.common.datatypes.RectF;
const WindowingMode = require('flicker').android.tools.common.traces.wm.WindowingMode;
const CrossPlatform = require('flicker').android.tools.common.CrossPlatform;
const TimestampFactory = require('flicker').android.tools.common.TimestampFactory;

const EMPTY_SIZE = Size.Companion.EMPTY;
const EMPTY_BUFFER = ActiveBuffer.Companion.EMPTY;
const EMPTY_COLOR = Color.Companion.EMPTY;
const EMPTY_INSETS = Insets.Companion.EMPTY;
const EMPTY_RECT = Rect.Companion.EMPTY;
const EMPTY_RECTF = RectF.Companion.EMPTY;
const EMPTY_POINT = Point.Companion.EMPTY;
const EMPTY_POINTF = PointF.Companion.EMPTY;
const EMPTY_MATRIX33 = Matrix33.Companion.identity(0, 0);
const EMPTY_TRANSFORM = new Transform(0, EMPTY_MATRIX33);

function toSize(proto) {
  if (proto == null) {
    return EMPTY_SIZE;
  }
  const width = proto.width ?? proto.w ?? 0;
  const height = proto.height ?? proto.h ?? 0;
  if (width || height) {
    return new Size(width, height);
  }
  return EMPTY_SIZE;
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

function toColor(proto, hasAlpha = true) {
  if (proto == null) {
    return EMPTY_COLOR;
  }
  const r = proto.r ?? 0;
  const g = proto.g ?? 0;
  const b = proto.b ?? 0;
  let a = proto.a;
  if (a === null && !hasAlpha) {
    a = 1;
  }
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

function toPointF(proto) {
  if (proto == null) {
    return null;
  }
  const x = proto.x ?? 0;
  const y = proto.y ?? 0;
  if (x || y) {
    return new PointF(x, y);
  }
  return EMPTY_POINTF;
}

function toInsets(proto) {
  if (proto == null) {
    return EMPTY_INSETS;
  }

  const left = proto?.left ?? 0;
  const top = proto?.top ?? 0;
  const right = proto?.right ?? 0;
  const bottom = proto?.bottom ?? 0;
  if (left || top || right || bottom) {
    return new Insets(left, top, right, bottom);
  }
  return EMPTY_INSETS;
}

function toCropRect(proto) {
  if (proto == null) return EMPTY_RECT;

  const right = proto.right || 0;
  const left = proto.left || 0;
  const bottom = proto.bottom || 0;
  const top = proto.top || 0;

  // crop (0,0) (-1,-1) means no crop
  if (right == -1 && left == 0 && bottom == -1 && top == 0) EMPTY_RECT;

  if (right - left <= 0 || bottom - top <= 0) return EMPTY_RECT;

  return Rect.Companion.from(left, top, right, bottom);
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

export {
  Activity,
  Configuration,
  ConfigurationContainer,
  DisplayArea,
  DisplayContent,
  KeyguardControllerState,
  DisplayCutout,
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
  WindowManagerTraceEntryBuilder,
  // SF
  HwcCompositionType,
  Layer,
  LayerProperties,
  LayerTraceEntry,
  LayerTraceEntryBuilder,
  LayersTrace,
  Transform,
  Matrix33,
  Display,
  // Eventlog
  EventLog,
  CujEvent,
  CujType,
  Event,
  FlickerEvent,
  FocusEvent,
  EventLogParser,
  CujTrace,
  Cuj,
  // Transitions
  Transition,
  TransitionType,
  TransitionChange,
  TransitionsTrace,
  ShellTransitionData,
  WmTransitionData,
  // Common
  Size,
  ActiveBuffer,
  Color,
  Insets,
  PlatformConsts,
  Point,
  Rect,
  RectF,
  Region,
  Rotation,
  WindowingMode,
  CrossPlatform,
  TimestampFactory,
  // Service
  toSize,
  toActiveBuffer,
  toColor,
  toCropRect,
  toInsets,
  toPoint,
  toPointF,
  toRect,
  toRectF,
  toRegion,
  toTransform,
  // Constants
  EMPTY_BUFFER,
  EMPTY_COLOR,
  EMPTY_RECT,
  EMPTY_RECTF,
  EMPTY_POINT,
  EMPTY_POINTF,
  EMPTY_MATRIX33,
  EMPTY_TRANSFORM,
};
