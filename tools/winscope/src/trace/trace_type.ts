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
import {Cuj, Event, Transition} from 'flickerlib/common';
import {LayerTraceEntry} from 'flickerlib/layers/LayerTraceEntry';
import {WindowManagerState} from 'flickerlib/windows/WindowManagerState';
import {HierarchyTreeNode, TraceRect, TreeNode} from 'trace/trace_data_utils';
import {LogMessage} from './protolog';
import {ScreenRecordingTraceEntry} from './screen_recording';

export enum TraceType {
  WINDOW_MANAGER,
  SURFACE_FLINGER,
  SCREEN_RECORDING,
  TRANSACTIONS,
  TRANSACTIONS_LEGACY,
  WAYLAND,
  WAYLAND_DUMP,
  PROTO_LOG,
  SYSTEM_UI,
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
  VIEW_CAPTURE,
  VIEW_CAPTURE_LAUNCHER_ACTIVITY,
  VIEW_CAPTURE_TASKBAR_DRAG_LAYER,
  VIEW_CAPTURE_TASKBAR_OVERLAY_DRAG_LAYER,
}

// view capture types
export type ViewNode = any;
export type FrameData = any;
export type WindowData = any;

export interface TreeAndRects {
  tree: HierarchyTreeNode;
  rects: TraceRect[];
}

export interface TraceEntryTypeMap {
  [TraceType.PROTO_LOG]: {new: LogMessage; legacy: LogMessage};
  [TraceType.SURFACE_FLINGER]: {new: TreeAndRects; legacy: LayerTraceEntry};
  [TraceType.SCREEN_RECORDING]: {new: ScreenRecordingTraceEntry; legacy: ScreenRecordingTraceEntry};
  [TraceType.SYSTEM_UI]: {new: object; legacy: object};
  [TraceType.TRANSACTIONS]: {new: TreeNode<any>; legacy: object};
  [TraceType.TRANSACTIONS_LEGACY]: {new: TreeNode<any>; legacy: object};
  [TraceType.WAYLAND]: {new: object; legacy: object};
  [TraceType.WAYLAND_DUMP]: {new: object; legacy: object};
  [TraceType.WINDOW_MANAGER]: {new: TreeAndRects; legacy: WindowManagerState};
  [TraceType.INPUT_METHOD_CLIENTS]: {new: TreeNode<any>; legacy: object};
  [TraceType.INPUT_METHOD_MANAGER_SERVICE]: {new: TreeNode<any>; legacy: object};
  [TraceType.INPUT_METHOD_SERVICE]: {new: TreeNode<any>; legacy: object};
  [TraceType.EVENT_LOG]: {new: TreeNode<any>; legacy: Event};
  [TraceType.WM_TRANSITION]: {new: TreeNode<any>; legacy: object};
  [TraceType.SHELL_TRANSITION]: {new: TreeNode<any>; legacy: object};
  [TraceType.TRANSITION]: {new: TreeNode<any>; legacy: Transition};
  [TraceType.CUJS]: {new: TreeNode<any>; legacy: Cuj};
  [TraceType.TAG]: {new: object; legacy: object};
  [TraceType.ERROR]: {new: object; legacy: object};
  [TraceType.TEST_TRACE_STRING]: {new: string; legacy: string};
  [TraceType.TEST_TRACE_NUMBER]: {new: number; legacy: number};
  [TraceType.VIEW_CAPTURE]: {new: TreeNode<any>; legacy: object};
  [TraceType.VIEW_CAPTURE_LAUNCHER_ACTIVITY]: {
    new: TreeAndRects;
    legacy: FrameData;
  };
  [TraceType.VIEW_CAPTURE_TASKBAR_DRAG_LAYER]: {
    new: TreeAndRects;
    legacy: FrameData;
  };
  [TraceType.VIEW_CAPTURE_TASKBAR_OVERLAY_DRAG_LAYER]: {
    new: TreeAndRects;
    legacy: FrameData;
  };
}

export class TraceTypeUtils {
  private static UI_PIPELINE_ORDER = [
    TraceType.INPUT_METHOD_CLIENTS,
    TraceType.INPUT_METHOD_SERVICE,
    TraceType.INPUT_METHOD_MANAGER_SERVICE,
    TraceType.PROTO_LOG,
    TraceType.WINDOW_MANAGER,
    TraceType.TRANSACTIONS,
    TraceType.SURFACE_FLINGER,
    TraceType.SCREEN_RECORDING,
  ];

  private static DISPLAY_ORDER = [
    TraceType.SCREEN_RECORDING,
    TraceType.SURFACE_FLINGER,
    TraceType.WINDOW_MANAGER,
    TraceType.INPUT_METHOD_CLIENTS,
    TraceType.INPUT_METHOD_MANAGER_SERVICE,
    TraceType.INPUT_METHOD_SERVICE,
    TraceType.TRANSACTIONS,
    TraceType.TRANSACTIONS_LEGACY,
    TraceType.PROTO_LOG,
    TraceType.WM_TRANSITION,
    TraceType.SHELL_TRANSITION,
    TraceType.TRANSITION,
    TraceType.VIEW_CAPTURE,
    TraceType.VIEW_CAPTURE_LAUNCHER_ACTIVITY,
    TraceType.VIEW_CAPTURE_TASKBAR_DRAG_LAYER,
    TraceType.VIEW_CAPTURE_TASKBAR_OVERLAY_DRAG_LAYER,
  ];

  private static TRACES_WITH_VIEWERS = [
    TraceType.SCREEN_RECORDING,
    TraceType.SURFACE_FLINGER,
    TraceType.WINDOW_MANAGER,
    TraceType.INPUT_METHOD_CLIENTS,
    TraceType.INPUT_METHOD_MANAGER_SERVICE,
    TraceType.INPUT_METHOD_SERVICE,
    TraceType.TRANSACTIONS,
    TraceType.TRANSACTIONS_LEGACY,
    TraceType.PROTO_LOG,
    TraceType.TRANSITION,
    TraceType.VIEW_CAPTURE,
    TraceType.VIEW_CAPTURE_LAUNCHER_ACTIVITY,
    TraceType.VIEW_CAPTURE_TASKBAR_DRAG_LAYER,
    TraceType.VIEW_CAPTURE_TASKBAR_OVERLAY_DRAG_LAYER,
  ];

  static isTraceTypeWithViewer(t: TraceType): boolean {
    return TraceTypeUtils.TRACES_WITH_VIEWERS.includes(t);
  }

  static compareByUiPipelineOrder(t: TraceType, u: TraceType) {
    const tIndex = TraceTypeUtils.findIndexInOrder(t, TraceTypeUtils.UI_PIPELINE_ORDER);
    const uIndex = TraceTypeUtils.findIndexInOrder(u, TraceTypeUtils.UI_PIPELINE_ORDER);
    return tIndex >= 0 && uIndex >= 0 && tIndex < uIndex;
  }

  static compareByDisplayOrder(t: TraceType, u: TraceType) {
    const tIndex = TraceTypeUtils.findIndexInOrder(t, TraceTypeUtils.DISPLAY_ORDER);
    const uIndex = TraceTypeUtils.findIndexInOrder(u, TraceTypeUtils.DISPLAY_ORDER);
    return tIndex - uIndex;
  }

  private static findIndexInOrder(traceType: TraceType, order: TraceType[]): number {
    return order.findIndex((type) => {
      return type === traceType;
    });
  }
}
