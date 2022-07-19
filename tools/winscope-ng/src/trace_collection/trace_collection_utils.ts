
interface TraceConfiguration {
  name: string,
  defaultCheck?: boolean,
  config?: ConfigurationOptions
}

export interface ConfigurationOptions {
  enableConfigs: Array<EnableConfiguration>,
  selectionConfigs: Array<SelectionConfiguration>
}

export interface EnableConfiguration {
  name: string,
  defaultCheck: boolean,
}

export interface SelectionConfiguration {
  name: string,
  options: Array<string>,
  value: string
}

export type configMap = {
[key: string]: Array<string> | string;
}

export const TRACES = {
  "default": {
    "window_trace": {
      name: "Window Manager",
    },
    "accessibility_trace": {
      name: "Accessibility",
    },
    "layers_trace": {
      name: "Surface Flinger",
    },
    "transactions": {
      name: "Transaction",
    },
    "proto_log": {
      name: "ProtoLog",
    },
    "screen_recording": {
      name: "Screen Recording",
    },
    "ime_trace_clients": {
      name: "Input Method Clients",
    },
    "ime_trace_service": {
      name: "Input Method Service",
    },
    "ime_trace_managerservice": {
      name: "Input Method Manager Service",
    },
  },
  "arc": {
    "wayland_trace": {
      name: "Wayland",
    },
  },
};

const wmTraceSelectionConfigs = [
  {
    name: "wmbuffersize (KB)",
    options: [
      "4000",
      "8000",
      "16000",
      "32000",
    ],
    value: "4000"
  },
  {
    name: "tracingtype",
    options: [
      "frame",
      "transaction",
    ],
    value: "frame"
  },
  {
    name: "tracinglevel",
    options: [
      "verbose",
      "debug",
      "critical",
    ],
    value: "verbose"
  },
];


export const traceConfigurations: Array<TraceConfiguration> = [
  {
    name: "Surface Flinger",
    defaultCheck: true,
    config: {
      enableConfigs: [
        {name: "composition", defaultCheck: false},
        {name: "metadata", defaultCheck: false},
        {name: "hwc", defaultCheck: false},
        {name: "tracebuffers", defaultCheck: false}
      ],
      selectionConfigs: [
        {
          name: "sfbuffersize (KB)",
          options: ["4000","8000","16000","32000",],
          value: "4000"
        }
      ]
    }
  },
  {
    name: "Window Manager",
    defaultCheck: true,
    config: {
      enableConfigs: [],
      selectionConfigs: wmTraceSelectionConfigs,
    }
  },
  {
    name: "Screen Recording",
  },
  {
    name: "Accessibility",
  },
  {
    name: "Transaction",
  },
  {
    name: "IME Tracing",
    defaultCheck: true,
    config: {
      enableConfigs: [
        {name: "Input Method Clients", defaultCheck: true},
        {name: "Input Method Service", defaultCheck: true},
        {name: "Input Method Manager Service", defaultCheck: true},
      ],
      selectionConfigs: []
    }
  },
];
