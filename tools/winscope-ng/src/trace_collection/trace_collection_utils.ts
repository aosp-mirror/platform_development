export interface TraceConfiguration {
  name?: string,
  run?: boolean,
  isTraceCollection?: boolean,
  config?: ConfigurationOptions
}


export interface TraceConfigurationMap {
  [key: string]: TraceConfiguration
}

interface ConfigurationOptions {
  enableConfigs: Array<EnableConfiguration>,
  selectionConfigs: Array<SelectionConfiguration>
}

export interface EnableConfiguration {
  name: string,
  key: string,
  enabled: boolean,
}

export interface SelectionConfiguration {
  key: string,
  name: string,
  options: Array<string>,
  value: string
}

export type configMap = {
[key: string]: Array<string> | string;
}

const wmTraceSelectionConfigs: Array<SelectionConfiguration> = [
  {
    key: "wmbuffersize",
    name: "buffer size (KB)",
    options: [
      "4000",
      "8000",
      "16000",
      "32000",
    ],
    value: "4000"
  },
  {
    key: "tracingtype",
    name: "tracing type",
    options: [
      "frame",
      "transaction",
    ],
    value: "frame"
  },
  {
    key: "tracinglevel",
    name: "tracing level",
    options: [
      "verbose",
      "debug",
      "critical",
    ],
    value: "verbose"
  },
];

const sfTraceEnableConfigs: Array<EnableConfiguration> = [
  {
    name: "composition",
    key: "composition",
    enabled: true
  },
  {
    name: "metadata",
    key: "metadata",
    enabled: true
  },
  {
    name: "hwc",
    key: "hwc",
    enabled: true
  },
  {
    name: "trace buffers",
    key: "tracebuffers",
    enabled: true
  }
];

const sfTraceSelectionConfigs: Array<SelectionConfiguration> = [
  {
    key: "sfbuffersize",
    name: "buffer size (KB)",
    options: ["4000","8000","16000","32000"],
    value: "4000"
  }
];

export const traceConfigurations: TraceConfigurationMap = {
  "layers_trace": {
    name: "Surface Flinger",
    run: true,
    config: {
      enableConfigs: sfTraceEnableConfigs,
      selectionConfigs: sfTraceSelectionConfigs,
    }
  },
  "window_trace": {
    name: "Window Manager",
    run: true,
    config: {
      enableConfigs: [],
      selectionConfigs: wmTraceSelectionConfigs,
    }
  },
  "screen_recording": {
    name: "Screen Recording",
    run: true,
  },
  "ime_tracing": {
    name: "IME Tracing",
    run: true,
    isTraceCollection: true,
    config: {
      enableConfigs: [
        {
          name: "Input Method Clients",
          key: "ime_trace_clients",
          enabled: true,
        },
        {
          name: "Input Method Service",
          key: "ime_trace_service",
          enabled: true,
        },
        {
          name: "Input Method Manager Service",
          key: "ime_trace_managerservice",
          enabled: true,
        },
      ],
      selectionConfigs: []
    }
  },
  "ime_trace_clients": {
    name: "Input Method Clients",
    run: true,
  },
  "ime_trace_service": {
    name: "Input Method Service",
    run: true,
  },
  "ime_trace_managerservice": {
    name: "Input Method Manager Service",
    run: true,
  },
  "accessibility_trace": {
    name: "Accessibility",
    run: false,
  },
  "transactions": {
    name: "Transaction",
    run: false,
  },
  "proto_log": {
    name: "ProtoLog",
    run: false,
  },
  "wayland_trace": {
    name: "Wayland",
    run: false,
  },
};


export const TRACES: { [key: string]: TraceConfigurationMap; } = {
  "default": {
    "window_trace": traceConfigurations["window_trace"],
    "accessibility_trace": traceConfigurations["accessibility_trace"],
    "layers_trace": traceConfigurations["layers_trace"],
    "transactions": traceConfigurations["transactions"],
    "proto_log": traceConfigurations["proto_log"],
    "screen_recording": traceConfigurations["screen_recording"],
    "ime_tracing": traceConfigurations["ime_tracing"],
  },
  "arc": {
    "wayland_trace": traceConfigurations["wayland_trace"],
  },
};
