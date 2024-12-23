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

import {PersistentStoreProxy} from 'common/store/persistent_store_proxy';
import {Store} from 'common/store/store';
import {TRACE_INFO} from 'trace/trace_info';
import {TraceType} from 'trace/trace_type';

/**
 * Represents a trace target and its required/optional configuration.
 */
export interface TraceConfiguration {
  name: string;
  config: ConfigurationOptions;
  available: boolean;
  types: TraceType[];
}

/**
 * Maps trace targets to their configuration.
 */
export interface TraceConfigurationMap {
  [key: string]: TraceConfiguration;
}

/**
 * Contains all config options for a trace target that should be stored
 * between sessions
 * @prop {boolean} enabled indicates whether a trace target should be run
 * @prop {CheckboxConfiguration} checkboxConfigs List of configs that can be optionally enabled
 * @prop {SelectionConfiguration} selectionConfigs List of configs whose values are set from a list of options
 */
export interface ConfigurationOptions {
  enabled: boolean;
  checkboxConfigs: CheckboxConfiguration[];
  selectionConfigs: SelectionConfiguration[];
}

interface AdvancedConfiguration {
  name: string;
  key: string;
}

export interface CheckboxConfiguration extends AdvancedConfiguration {
  enabled: boolean;
}

export interface SelectionConfiguration extends AdvancedConfiguration {
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

const sfTraceCheckboxConfigs: CheckboxConfiguration[] = [
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

export function makeScreenRecordingSelectionConfigs(
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
    config: {
      enabled: true,
      checkboxConfigs: sfTraceCheckboxConfigs,
      selectionConfigs: sfTraceSelectionConfigs,
    },
    available: true,
    types: [TraceType.SURFACE_FLINGER],
  },
  window_trace: {
    name: TRACE_INFO[TraceType.WINDOW_MANAGER].name,
    config: {
      enabled: true,
      checkboxConfigs: [],
      selectionConfigs: wmTraceSelectionConfigs,
    },
    available: true,
    types: [TraceType.WINDOW_MANAGER],
  },
  screen_recording: {
    name: TRACE_INFO[TraceType.SCREEN_RECORDING].name,
    config: {
      enabled: true,
      checkboxConfigs: [
        {
          name: 'pointer location and touches',
          key: 'pointer_and_touches',
          enabled: true,
        },
      ],
      selectionConfigs: makeScreenRecordingSelectionConfigs([], ''),
    },
    available: true,
    types: [TraceType.SCREEN_RECORDING],
  },
  ime: {
    name: 'IME',
    config: {
      enabled: true,
      checkboxConfigs: [],
      selectionConfigs: [],
    },
    available: true,
    types: [
      TraceType.INPUT_METHOD_CLIENTS,
      TraceType.INPUT_METHOD_SERVICE,
      TraceType.INPUT_METHOD_MANAGER_SERVICE,
    ],
  },
  transactions: {
    name: TRACE_INFO[TraceType.TRANSACTIONS].name,
    config: {
      enabled: true,
      checkboxConfigs: [],
      selectionConfigs: [],
    },
    available: true,
    types: [TraceType.TRANSACTIONS, TraceType.TRANSACTIONS_LEGACY],
  },
  proto_log: {
    name: TRACE_INFO[TraceType.PROTO_LOG].name,
    config: {
      enabled: false,
      checkboxConfigs: [],
      selectionConfigs: [],
    },
    available: true,
    types: [TraceType.PROTO_LOG],
  },
  wayland_trace: {
    name: TRACE_INFO[TraceType.WAYLAND].name,
    config: {
      enabled: false,
      checkboxConfigs: [],
      selectionConfigs: [],
    },
    available: false,
    types: [TraceType.WAYLAND, TraceType.WAYLAND_DUMP],
  },
  eventlog: {
    name: TRACE_INFO[TraceType.EVENT_LOG].name,
    config: {
      enabled: false,
      checkboxConfigs: [],
      selectionConfigs: [],
    },
    available: true,
    types: [TraceType.EVENT_LOG, TraceType.CUJS],
  },
  transition_traces: {
    name: TRACE_INFO[TraceType.SHELL_TRANSITION].name,
    config: {
      enabled: false,
      checkboxConfigs: [],
      selectionConfigs: [],
    },
    available: true,
    types: [
      TraceType.SHELL_TRANSITION,
      TraceType.WM_TRANSITION,
      TraceType.TRANSITION,
    ],
  },
  view_capture_traces: {
    name: TRACE_INFO[TraceType.VIEW_CAPTURE].name,
    config: {
      enabled: false,
      checkboxConfigs: [],
      selectionConfigs: [],
    },
    available: true,
    types: [TraceType.VIEW_CAPTURE],
  },
  input: {
    name: 'Input',
    config: {
      enabled: false,
      checkboxConfigs: [],
      selectionConfigs: [],
    },
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
      config: {
        enabled: true,
        checkboxConfigs: [],
        selectionConfigs: [],
      },
      available: true,
      types: [TraceType.WINDOW_MANAGER],
    },
    layers_dump: {
      name: 'Surface Flinger',
      config: {
        enabled: true,
        checkboxConfigs: [],
        selectionConfigs: [],
      },
      available: true,
      types: [TraceType.SURFACE_FLINGER],
    },
    screenshot: {
      name: 'Screenshot',
      config: {
        enabled: true,
        checkboxConfigs: [],
        selectionConfigs: screenshotConfigs,
      },
      available: true,
      types: [TraceType.SCREENSHOT],
    },
  });
}

/**
 * Updates configurations for each trace target from store.
 * Stored configs are used only to update available configs, not to add or remove configs.
 * @param configMap contains available configs with default values set
 * @param store contains previous trace configurations - used to update values in configMap for configs that are still available
 * @param storeKeyPrefix used with the trace target key in configMap to retrieve configs from the store
 */
export function updateConfigsFromStore(
  configMap: TraceConfigurationMap,
  storage: Store,
  storeKeyPrefix: string,
) {
  for (const [key, target] of Object.entries(configMap)) {
    const stored = PersistentStoreProxy.new(
      storeKeyPrefix + key,
      target.config,
      storage,
    );
    stored.checkboxConfigs = mergeConfigs(
      target.config.checkboxConfigs,
      stored.checkboxConfigs,
      (liveConfig, storedConfig) => {
        liveConfig.enabled = storedConfig.enabled;
      },
    );
    stored.selectionConfigs = mergeConfigs(
      target.config.selectionConfigs,
      stored.selectionConfigs,
      (liveConfig, storedConfig) => {
        if (typeof liveConfig.value !== typeof storedConfig.value) {
          // config schema has changed between single and multiple selection
          // so stored value no longer applies
          return;
        }

        // liveConfig contains all currently available config options - stored
        // values are valid if their config option is still available, and only
        // if valid should be used to update the values in liveConfig
        const availableOptions = liveConfig.options;
        const validArr =
          Array.isArray(storedConfig.value) &&
          storedConfig.value.every((v) => availableOptions.includes(v));
        const validStr =
          typeof storedConfig.value === 'string' &&
          availableOptions.includes(storedConfig.value);
        if (validArr || validStr) {
          liveConfig.value = storedConfig.value;
        }
      },
    );
    target.config = stored;
  }
  return configMap;
}

function mergeConfigs<T extends AdvancedConfiguration>(
  liveConfigs: T[],
  storedConfigs: T[],
  updateConfig: (liveConfig: T, storedConfig: T) => void,
): T[] {
  storedConfigs.forEach((storedConfig) => {
    const liveConfig = liveConfigs.find(
      (live) => live.key === storedConfig.key,
    );
    if (liveConfig) {
      updateConfig(liveConfig, storedConfig);
    }
  });
  return liveConfigs;
}
