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

import {TRACE_INFO} from 'trace/trace_info';
import {TraceType} from 'trace/trace_type';

export interface TraceConfiguration {
  name: string;
  enabled: boolean;
  config: ConfigurationOptions | undefined;
  available: boolean;
  types: TraceType[];
}

export interface TraceConfigurationMap {
  [key: string]: TraceConfiguration;
}

export interface ConfigurationOptions {
  enableConfigs: EnableConfiguration[];
  selectionConfigs: SelectionConfiguration[];
}

export interface EnableConfiguration {
  name: string;
  key: string;
  enabled: boolean;
}

export interface SelectionConfiguration {
  key: string;
  name: string;
  options: string[];
  value: string | string[];
  desc?: string;
  optional?: boolean;
  wideField?: boolean;
}

export interface ConfigMap {
  [key: string]: string[] | string;
}

const wmTraceSelectionConfigs: SelectionConfiguration[] = [
  {
    key: 'wmbuffersize',
    name: 'buffer size (KB)',
    options: ['4000', '8000', '16000', '32000'],
    value: '32000',
  },
  {
    key: 'tracingtype',
    name: 'tracing type',
    options: ['frame', 'transaction'],
    value: 'frame',
  },
  {
    key: 'tracinglevel',
    name: 'tracing level',
    options: ['verbose', 'debug', 'critical'],
    value: 'verbose',
  },
];

const sfTraceEnableConfigs: EnableConfiguration[] = [
  {
    name: 'input',
    key: 'input',
    enabled: true,
  },
  {
    name: 'composition',
    key: 'composition',
    enabled: true,
  },
  {
    name: 'metadata',
    key: 'metadata',
    enabled: false,
  },
  {
    name: 'hwc',
    key: 'hwc',
    enabled: false,
  },
  {
    name: 'trace buffers',
    key: 'tracebuffers',
    enabled: false,
  },
  {
    name: 'virtual displays',
    key: 'virtualdisplays',
    enabled: false,
  },
];

const sfTraceSelectionConfigs: SelectionConfiguration[] = [
  {
    key: 'sfbuffersize',
    name: 'buffer size (KB)',
    options: ['4000', '8000', '16000', '32000'],
    value: '32000',
  },
];
const screenshotConfigs: SelectionConfiguration[] = [
  {
    key: 'displays',
    name: 'displays',
    options: [],
    value: [],
    desc: 'Leave empty to capture active display',
    wideField: true,
  },
];

export function makeScreenRecordingConfigs(
  options: string[],
  initialValue: string | string[],
): SelectionConfiguration[] {
  return [
    {
      key: 'displays',
      name: 'displays',
      options,
      value: initialValue,
      optional: true,
      desc: 'Leave empty to capture active display',
      wideField: true,
    },
  ];
}

const traceDefaultConfig: TraceConfigurationMap = {
  layers_trace: {
    name: TRACE_INFO[TraceType.SURFACE_FLINGER].name,
    enabled: true,
    config: {
      enableConfigs: sfTraceEnableConfigs,
      selectionConfigs: sfTraceSelectionConfigs,
    },
    available: true,
    types: [TraceType.SURFACE_FLINGER],
  },
  window_trace: {
    name: TRACE_INFO[TraceType.WINDOW_MANAGER].name,
    enabled: true,
    config: {
      enableConfigs: [],
      selectionConfigs: wmTraceSelectionConfigs,
    },
    available: true,
    types: [TraceType.WINDOW_MANAGER],
  },
  screen_recording: {
    name: TRACE_INFO[TraceType.SCREEN_RECORDING].name,
    enabled: true,
    config: {
      enableConfigs: [],
      selectionConfigs: makeScreenRecordingConfigs([], ''),
    },
    available: true,
    types: [TraceType.SCREEN_RECORDING],
  },
  ime: {
    name: 'IME',
    enabled: true,
    config: undefined,
    available: true,
    types: [
      TraceType.INPUT_METHOD_CLIENTS,
      TraceType.INPUT_METHOD_SERVICE,
      TraceType.INPUT_METHOD_MANAGER_SERVICE,
    ],
  },
  transactions: {
    name: TRACE_INFO[TraceType.TRANSACTIONS].name,
    enabled: true,
    config: undefined,
    available: true,
    types: [TraceType.TRANSACTIONS, TraceType.TRANSACTIONS_LEGACY],
  },
  proto_log: {
    name: TRACE_INFO[TraceType.PROTO_LOG].name,
    enabled: false,
    config: undefined,
    available: true,
    types: [TraceType.PROTO_LOG],
  },
  wayland_trace: {
    name: TRACE_INFO[TraceType.WAYLAND].name,
    enabled: false,
    config: undefined,
    available: false,
    types: [TraceType.WAYLAND, TraceType.WAYLAND_DUMP],
  },
  eventlog: {
    name: TRACE_INFO[TraceType.EVENT_LOG].name,
    enabled: false,
    config: undefined,
    available: true,
    types: [TraceType.EVENT_LOG, TraceType.CUJS],
  },
  transition_traces: {
    name: TRACE_INFO[TraceType.SHELL_TRANSITION].name,
    enabled: false,
    config: undefined,
    available: true,
    types: [
      TraceType.SHELL_TRANSITION,
      TraceType.WM_TRANSITION,
      TraceType.TRANSITION,
    ],
  },
  view_capture_traces: {
    name: TRACE_INFO[TraceType.VIEW_CAPTURE].name,
    enabled: false,
    config: undefined,
    available: true,
    types: [TraceType.VIEW_CAPTURE],
  },
  input: {
    name: 'Input',
    enabled: false,
    config: undefined,
    available: true,
    types: [
      TraceType.INPUT_KEY_EVENT,
      TraceType.INPUT_MOTION_EVENT,
      TraceType.INPUT_EVENT_MERGED,
    ],
  },
};

export function makeDefaultTraceConfigMap(): TraceConfigurationMap {
  return structuredClone({
    window_trace: traceDefaultConfig['window_trace'],
    layers_trace: traceDefaultConfig['layers_trace'],
    transactions: traceDefaultConfig['transactions'],
    proto_log: traceDefaultConfig['proto_log'],
    screen_recording: traceDefaultConfig['screen_recording'],
    ime: traceDefaultConfig['ime'],
    eventlog: traceDefaultConfig['eventlog'],
    transition_traces: traceDefaultConfig['transition_traces'],
    view_capture_trace: traceDefaultConfig['view_capture_traces'],
    input: traceDefaultConfig['input'],
    wayland_trace: traceDefaultConfig['wayland_trace'],
  });
}

export function makeDefaultDumpConfigMap(): TraceConfigurationMap {
  return structuredClone({
    window_dump: {
      name: 'Window Manager',
      enabled: true,
      config: undefined,
      available: true,
      types: [TraceType.WINDOW_MANAGER],
    },
    layers_dump: {
      name: 'Surface Flinger',
      enabled: true,
      config: undefined,
      available: true,
      types: [TraceType.SURFACE_FLINGER],
    },
    screenshot: {
      name: 'Screenshot',
      enabled: true,
      config: {
        enableConfigs: [],
        selectionConfigs: screenshotConfigs,
      },
      available: true,
      types: [TraceType.SCREENSHOT],
    },
  });
}
