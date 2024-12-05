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
const SCREENSHOT_ICON = 'image';
const TRANSACTION_ICON = 'show_chart';
const WAYLAND_ICON = 'filter_none';
const PROTO_LOG_ICON = 'notes';
const SYSTEM_UI_ICON = 'filter_none';
const VIEW_CAPTURE_ICON = 'filter_none';
const IME_ICON = 'keyboard_alt';
const EVENT_LOG_ICON = 'description';
const TRANSITION_ICON = 'animation';
const CUJ_ICON = 'label';
const INPUT_ICON = 'touch_app';
const SEARCH_ICON = 'search';

interface TraceInfoMap {
  [key: number]: {
    name: string;
    icon: string;
    color: string;
    downloadArchiveDir: string;
    legacyExt: string;
  };
}

export const TRACE_INFO: TraceInfoMap = {
  [TraceType.WINDOW_MANAGER]: {
    name: 'Window Manager',
    icon: WINDOW_MANAGER_ICON,
    color: '#AF5CF7',
    downloadArchiveDir: 'wm',
    legacyExt: '.winscope',
  },
  [TraceType.SURFACE_FLINGER]: {
    name: 'Surface Flinger',
    icon: SURFACE_FLINGER_ICON,
    color: '#4ECDE6',
    downloadArchiveDir: 'sf',
    legacyExt: '.winscope',
  },
  [TraceType.SCREEN_RECORDING]: {
    name: 'Screen Recording',
    icon: SCREEN_RECORDING_ICON,
    color: '#8A9CF9',
    downloadArchiveDir: '',
    legacyExt: '.mp4',
  },
  [TraceType.SCREENSHOT]: {
    name: 'Screenshot',
    icon: SCREENSHOT_ICON,
    color: '#8A9CF9',
    downloadArchiveDir: '',
    legacyExt: '.png',
  },
  [TraceType.TRANSACTIONS]: {
    name: 'Transactions',
    icon: TRANSACTION_ICON,
    color: '#0D652D',
    downloadArchiveDir: 'sf',
    legacyExt: '.winscope',
  },
  [TraceType.TRANSACTIONS_LEGACY]: {
    name: 'Transactions Legacy',
    icon: TRANSACTION_ICON,
    color: '#0D652D',
    downloadArchiveDir: 'sf',
    legacyExt: '.winscope',
  },
  [TraceType.WAYLAND]: {
    name: 'Wayland',
    icon: WAYLAND_ICON,
    color: '#FDC274',
    downloadArchiveDir: 'wayland',
    legacyExt: '.winscope',
  },
  [TraceType.WAYLAND_DUMP]: {
    name: 'Wayland Dump',
    icon: WAYLAND_ICON,
    color: '#D01884',
    downloadArchiveDir: 'wayland',
    legacyExt: '.winscope',
  },
  [TraceType.PROTO_LOG]: {
    name: 'ProtoLog',
    icon: PROTO_LOG_ICON,
    color: '#34A853',
    downloadArchiveDir: 'protolog',
    legacyExt: '.winscope',
  },
  [TraceType.SYSTEM_UI]: {
    name: 'System UI',
    icon: SYSTEM_UI_ICON,
    color: '#7A86FF',
    downloadArchiveDir: 'sysui',
    legacyExt: '.winscope',
  },
  [TraceType.VIEW_CAPTURE]: {
    name: 'View Capture',
    icon: VIEW_CAPTURE_ICON,
    color: '#59CA77',
    downloadArchiveDir: 'vc',
    legacyExt: '.winscope',
  },
  [TraceType.INPUT_METHOD_CLIENTS]: {
    name: 'IME Clients',
    icon: IME_ICON,
    color: '#FF964B',
    downloadArchiveDir: 'ime',
    legacyExt: '.winscope',
  },
  [TraceType.INPUT_METHOD_SERVICE]: {
    name: 'IME Service',
    icon: IME_ICON,
    color: '#FFC24B',
    downloadArchiveDir: 'ime',
    legacyExt: '.winscope',
  },
  [TraceType.INPUT_METHOD_MANAGER_SERVICE]: {
    name: 'IME system_server',
    icon: IME_ICON,
    color: '#FF6B00',
    downloadArchiveDir: 'ime',
    legacyExt: '.winscope',
  },
  [TraceType.EVENT_LOG]: {
    name: 'Event Log',
    icon: EVENT_LOG_ICON,
    color: '#fdd663',
    downloadArchiveDir: 'eventlog',
    legacyExt: '.winscope',
  },
  [TraceType.WM_TRANSITION]: {
    name: 'WM Transitions',
    icon: TRANSITION_ICON,
    color: '#D01884',
    downloadArchiveDir: 'transition',
    legacyExt: '.winscope',
  },
  [TraceType.SHELL_TRANSITION]: {
    name: 'Shell Transitions',
    icon: TRANSITION_ICON,
    color: '#D01884',
    downloadArchiveDir: 'transition',
    legacyExt: '.winscope',
  },
  [TraceType.TRANSITION]: {
    name: 'Transitions',
    icon: TRANSITION_ICON,
    color: '#D01884',
    downloadArchiveDir: 'transition',
    legacyExt: '.winscope',
  },
  [TraceType.CUJS]: {
    name: 'Jank CUJs',
    icon: CUJ_ICON,
    color: '#FF63B8',
    downloadArchiveDir: 'eventlog',
    legacyExt: '.winscope',
  },
  [TraceType.INPUT_MOTION_EVENT]: {
    name: 'Motion Events',
    icon: INPUT_ICON,
    color: '#8baef4',
    downloadArchiveDir: 'input',
    legacyExt: '.winscope',
  },
  [TraceType.INPUT_KEY_EVENT]: {
    name: 'Key Events',
    icon: INPUT_ICON,
    color: '#8baef4',
    downloadArchiveDir: 'input',
    legacyExt: '.winscope',
  },
  [TraceType.INPUT_EVENT_MERGED]: {
    name: 'Input',
    icon: INPUT_ICON,
    color: '#8baef4',
    downloadArchiveDir: 'input',
    legacyExt: '.winscope',
  },
  [TraceType.SEARCH]: {
    name: 'Search',
    icon: SEARCH_ICON,
    color: '#DEBE13',
    downloadArchiveDir: '',
    legacyExt: '',
  },
};
