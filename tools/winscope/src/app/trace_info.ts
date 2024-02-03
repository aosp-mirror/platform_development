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

import {TraceType} from 'trace/trace_type';

const WINDOW_MANAGER_ICON = 'web';
const SURFACE_FLINGER_ICON = 'layers';
const SCREEN_RECORDING_ICON = 'videocam';
const TRANSACTION_ICON = 'show_chart';
const WAYLAND_ICON = 'filter_none';
const PROTO_LOG_ICON = 'notes';
const SYSTEM_UI_ICON = 'filter_none';
const LAUNCHER_ICON = 'filter_none';
const IME_ICON = 'keyboard_alt';
const ACCESSIBILITY_ICON = 'accessibility_new';
const TAG_ICON = 'details';
const TRACE_ERROR_ICON = 'warning';
const EVENT_LOG_ICON = 'description';
const TRANSITION_ICON = 'animation';
const CUJ_ICON = 'label';

interface TraceInfoMap {
  [key: number]: {
    name: string;
    icon: string;
    color: string;
    downloadArchiveDir: string;
  };
}

export const TRACE_INFO: TraceInfoMap = {
  [TraceType.ACCESSIBILITY]: {
    name: 'Accessibility',
    icon: ACCESSIBILITY_ICON,
    color: '#FF63B8',
    downloadArchiveDir: 'accessibility',
  },
  [TraceType.WINDOW_MANAGER]: {
    name: 'Window Manager',
    icon: WINDOW_MANAGER_ICON,
    color: '#AF5CF7',
    downloadArchiveDir: 'wm',
  },
  [TraceType.SURFACE_FLINGER]: {
    name: 'Surface Flinger',
    icon: SURFACE_FLINGER_ICON,
    color: '#4ECDE6',
    downloadArchiveDir: 'sf',
  },
  [TraceType.SCREEN_RECORDING]: {
    name: 'Screen Recording',
    icon: SCREEN_RECORDING_ICON,
    color: '#8A9CF9',
    downloadArchiveDir: '',
  },
  [TraceType.TRANSACTIONS]: {
    name: 'Transactions',
    icon: TRANSACTION_ICON,
    color: '#5BB974',
    downloadArchiveDir: 'sf',
  },
  [TraceType.TRANSACTIONS_LEGACY]: {
    name: 'Transactions Legacy',
    icon: TRANSACTION_ICON,
    color: '#5BB974',
    downloadArchiveDir: 'sf',
  },
  [TraceType.WAYLAND]: {
    name: 'Wayland',
    icon: WAYLAND_ICON,
    color: '#FDC274',
    downloadArchiveDir: 'wayland',
  },
  [TraceType.WAYLAND_DUMP]: {
    name: 'Wayland Dump',
    icon: WAYLAND_ICON,
    color: '#D01884',
    downloadArchiveDir: 'wayland',
  },
  [TraceType.PROTO_LOG]: {
    name: 'ProtoLog',
    icon: PROTO_LOG_ICON,
    color: '#40A58A',
    downloadArchiveDir: 'protolog',
  },
  [TraceType.SYSTEM_UI]: {
    name: 'System UI',
    icon: SYSTEM_UI_ICON,
    color: '#7A86FF',
    downloadArchiveDir: 'sysui',
  },
  [TraceType.LAUNCHER]: {
    name: 'Launcher',
    icon: LAUNCHER_ICON,
    color: '#137333',
    downloadArchiveDir: 'launcher',
  },
  // TODO: Choose ViewCapture icon, color, title name, and download archive directory
  [TraceType.VIEW_CAPTURE]: {
    name: 'View Capture',
    icon: LAUNCHER_ICON,
    color: '#137333',
    downloadArchiveDir: 'launcher',
  },
  [TraceType.INPUT_METHOD_CLIENTS]: {
    name: 'IME Clients',
    icon: IME_ICON,
    color: '#FA903E',
    downloadArchiveDir: 'ime',
  },
  [TraceType.INPUT_METHOD_SERVICE]: {
    name: 'IME Service',
    icon: IME_ICON,
    color: '#F29900',
    downloadArchiveDir: 'ime',
  },
  [TraceType.INPUT_METHOD_MANAGER_SERVICE]: {
    name: 'IME Manager Service',
    icon: IME_ICON,
    color: '#D93025',
    downloadArchiveDir: 'ime',
  },
  [TraceType.TAG]: {
    name: 'Tag',
    icon: TAG_ICON,
    color: '#4575B4',
    downloadArchiveDir: '',
  },
  [TraceType.ERROR]: {
    name: 'Error',
    icon: TRACE_ERROR_ICON,
    color: '#D73027',
    downloadArchiveDir: '',
  },
  [TraceType.EVENT_LOG]: {
    name: 'Event Log',
    icon: EVENT_LOG_ICON,
    color: '#fdd663',
    downloadArchiveDir: 'eventlog',
  },
  [TraceType.WM_TRANSITION]: {
    name: 'WM Transitions',
    icon: TRANSITION_ICON,
    color: '#EC407A',
    downloadArchiveDir: 'transition',
  },
  [TraceType.SHELL_TRANSITION]: {
    name: 'Shell Transitions',
    icon: TRANSITION_ICON,
    color: '#EC407A',
    downloadArchiveDir: 'transition',
  },
  [TraceType.TRANSITION]: {
    name: 'Transitions',
    icon: TRANSITION_ICON,
    color: '#EC407A',
    downloadArchiveDir: 'transition',
  },
  [TraceType.CUJS]: {
    name: 'Cujs',
    icon: CUJ_ICON,
    color: '#EC407A',
    downloadArchiveDir: 'eventlog',
  },
};
