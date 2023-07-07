/*
 * Copyright (C) 2022 The Android Open Source Project
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
import {Cuj, Event, Transition} from 'trace/flickerlib/common';
import {LayerTraceEntry} from './flickerlib/layers/LayerTraceEntry';
import {WindowManagerState} from './flickerlib/windows/WindowManagerState';
import {LogMessage} from './protolog';
import {ScreenRecordingTraceEntry} from './screen_recording';

export enum TraceType {
  ACCESSIBILITY,
  WINDOW_MANAGER,
  SURFACE_FLINGER,
  SCREEN_RECORDING,
  TRANSACTIONS,
  TRANSACTIONS_LEGACY,
  WAYLAND,
  WAYLAND_DUMP,
  PROTO_LOG,
  SYSTEM_UI,
  LAUNCHER,
  INPUT_METHOD_CLIENTS,
  INPUT_METHOD_MANAGER_SERVICE,
  INPUT_METHOD_SERVICE,
  EVENT_LOG,
  WM_TRANSITION,
  SHELL_TRANSITION,
  TRANSITION,
  CUJS,
  TAG,
  ERROR,
  TEST_TRACE_STRING,
  TEST_TRACE_NUMBER,
}

export interface TraceEntryTypeMap {
  [TraceType.ACCESSIBILITY]: object;
  [TraceType.LAUNCHER]: object;
  [TraceType.PROTO_LOG]: LogMessage;
  [TraceType.SURFACE_FLINGER]: LayerTraceEntry;
  [TraceType.SCREEN_RECORDING]: ScreenRecordingTraceEntry;
  [TraceType.SYSTEM_UI]: object;
  [TraceType.TRANSACTIONS]: object;
  [TraceType.TRANSACTIONS_LEGACY]: object;
  [TraceType.WAYLAND]: object;
  [TraceType.WAYLAND_DUMP]: object;
  [TraceType.WINDOW_MANAGER]: WindowManagerState;
  [TraceType.INPUT_METHOD_CLIENTS]: object;
  [TraceType.INPUT_METHOD_MANAGER_SERVICE]: object;
  [TraceType.INPUT_METHOD_SERVICE]: object;
  [TraceType.EVENT_LOG]: Event;
  [TraceType.WM_TRANSITION]: object;
  [TraceType.SHELL_TRANSITION]: object;
  [TraceType.TRANSITION]: Transition;
  [TraceType.CUJS]: Cuj;
  [TraceType.TAG]: object;
  [TraceType.ERROR]: object;
  [TraceType.TEST_TRACE_STRING]: string;
  [TraceType.TEST_TRACE_NUMBER]: number;
}
