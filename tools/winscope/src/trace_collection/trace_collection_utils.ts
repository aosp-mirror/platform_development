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

export interface TraceConfiguration {
  name: string | undefined;
  run: boolean | undefined;
  isTraceCollection: boolean | undefined;
  config: ConfigurationOptions | undefined;
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

const traceConfigurations: TraceConfigurationMap = {
  layers_trace: {
    name: 'Surface Flinger',
    run: true,
    isTraceCollection: undefined,
    config: {
      enableConfigs: sfTraceEnableConfigs,
      selectionConfigs: sfTraceSelectionConfigs,
    },
  },
  window_trace: {
    name: 'Window Manager',
    run: true,
    isTraceCollection: undefined,
    config: {
      enableConfigs: [],
      selectionConfigs: wmTraceSelectionConfigs,
    },
  },
  screen_recording: {
    name: 'Screen Recording',
    isTraceCollection: undefined,
    run: true,
    config: undefined,
  },
  ime_tracing: {
    name: 'IME Tracing',
    run: true,
    isTraceCollection: true,
    config: {
      enableConfigs: [
        {
          name: 'Input Method Clients',
          key: 'ime_trace_clients',
          enabled: true,
        },
        {
          name: 'Input Method Service',
          key: 'ime_trace_service',
          enabled: true,
        },
        {
          name: 'Input Method Manager Service',
          key: 'ime_trace_managerservice',
          enabled: true,
        },
      ],
      selectionConfigs: [],
    },
  },
  ime_trace_clients: {
    name: 'Input Method Clients',
    isTraceCollection: undefined,
    run: true,
    config: undefined,
  },
  ime_trace_service: {
    name: 'Input Method Service',
    isTraceCollection: undefined,
    run: true,
    config: undefined,
  },
  ime_trace_managerservice: {
    name: 'Input Method Manager Service',
    isTraceCollection: undefined,
    run: true,
    config: undefined,
  },
  transactions: {
    name: 'Transaction',
    isTraceCollection: undefined,
    run: false,
    config: undefined,
  },
  proto_log: {
    name: 'ProtoLog',
    isTraceCollection: undefined,
    run: false,
    config: undefined,
  },
  wayland_trace: {
    name: 'Wayland',
    isTraceCollection: undefined,
    run: false,
    config: undefined,
  },
  eventlog: {
    name: 'Event Log',
    isTraceCollection: undefined,
    run: false,
    config: undefined,
  },
  transition_traces: {
    name: 'Shell Transitions',
    isTraceCollection: undefined,
    run: false,
    config: undefined,
  },
  view_capture_traces: {
    name: 'View Capture',
    isTraceCollection: undefined,
    run: false,
    config: undefined,
  },
};

export const TRACES: {[key: string]: TraceConfigurationMap} = {
  default: {
    window_trace: traceConfigurations['window_trace'],
    layers_trace: traceConfigurations['layers_trace'],
    transactions: traceConfigurations['transactions'],
    proto_log: traceConfigurations['proto_log'],
    screen_recording: traceConfigurations['screen_recording'],
    ime_tracing: traceConfigurations['ime_tracing'],
    eventlog: traceConfigurations['eventlog'],
    transition_traces: traceConfigurations['transition_traces'],
    view_capture_trace: traceConfigurations['view_capture_traces'],
  },
  arc: {
    wayland_trace: traceConfigurations['wayland_trace'],
  },
};
