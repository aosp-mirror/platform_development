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
}

export interface TraceConfigurationMap {
  [key: string]: TraceConfiguration;
}

interface ConfigurationOptions {
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
  value: string;
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
    enabled: true,
  },
  {
    name: 'trace buffers',
    key: 'tracebuffers',
    enabled: true,
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

const traceDefaultConfig: TraceConfigurationMap = {
  layers_trace: {
    name: TRACE_INFO[TraceType.SURFACE_FLINGER].name,
    enabled: true,
    config: {
      enableConfigs: sfTraceEnableConfigs,
      selectionConfigs: sfTraceSelectionConfigs,
    },
    available: true,
  },
  window_trace: {
    name: TRACE_INFO[TraceType.WINDOW_MANAGER].name,
    enabled: true,
    config: {
      enableConfigs: [],
      selectionConfigs: wmTraceSelectionConfigs,
    },
    available: true,
  },
  screen_recording: {
    name: TRACE_INFO[TraceType.SCREEN_RECORDING].name,
    enabled: true,
    config: undefined,
    available: true,
  },
  ime: {
    name: 'IME',
    enabled: true,
    config: undefined,
    available: true,
  },
  transactions: {
    name: TRACE_INFO[TraceType.TRANSACTIONS].name,
    enabled: true,
    config: undefined,
    available: true,
  },
  proto_log: {
    name: TRACE_INFO[TraceType.PROTO_LOG].name,
    enabled: false,
    config: undefined,
    available: true,
  },
  wayland_trace: {
    name: TRACE_INFO[TraceType.WAYLAND].name,
    enabled: false,
    config: undefined,
    available: false,
  },
  eventlog: {
    name: TRACE_INFO[TraceType.EVENT_LOG].name,
    enabled: false,
    config: undefined,
    available: true,
  },
  transition_traces: {
    name: TRACE_INFO[TraceType.SHELL_TRANSITION].name,
    enabled: false,
    config: undefined,
    available: true,
  },
  view_capture_traces: {
    name: 'View Capture',
    enabled: false,
    config: undefined,
    available: true,
  },
  input: {
    name: 'Input',
    enabled: false,
    config: undefined,
    available: true,
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
    },
    layers_dump: {
      name: 'Surface Flinger',
      enabled: true,
      config: undefined,
      available: true,
    },
    screenshot: {
      name: 'Screenshot',
      enabled: true,
      config: undefined,
      available: true,
    },
  });
}
