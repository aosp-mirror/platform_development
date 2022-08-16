import { TraceType } from "common/trace/trace_type";

const WINDOW_MANAGER_ICON = "view_compact";
const SURFACE_FLINGER_ICON = "filter_none";
const SCREEN_RECORDING_ICON = "videocam";
const TRANSACTION_ICON = "timeline";
const WAYLAND_ICON = "filter_none";
const PROTO_LOG_ICON = "notes";
const SYSTEM_UI_ICON = "filter_none";
const LAUNCHER_ICON = "filter_none";
const IME_ICON = "keyboard";
const ACCESSIBILITY_ICON = "accessibility";
const TAG_ICON = "details";
const TRACE_ERROR_ICON = "warning";

type traceInfoMap = {
    [key: number]: {
      name: string,
      icon: string
    };
}

export const TRACE_INFO: traceInfoMap = {
  [TraceType.ACCESSIBILITY]: {
    name: "Accessibility",
    icon: ACCESSIBILITY_ICON
  },
  [TraceType.WINDOW_MANAGER]: {
    name: "Window Manager",
    icon: WINDOW_MANAGER_ICON
  },
  [TraceType.SURFACE_FLINGER]: {
    name: "Surface Flinger",
    icon: SURFACE_FLINGER_ICON
  },
  [TraceType.SCREEN_RECORDING]: {
    name: "Screen Recording",
    icon: SCREEN_RECORDING_ICON
  },
  [TraceType.TRANSACTIONS]: {
    name: "Transactions",
    icon: TRANSACTION_ICON
  },
  [TraceType.TRANSACTIONS_LEGACY]: {
    name: "Transactions Legacy",
    icon: TRANSACTION_ICON
  },
  [TraceType.WAYLAND]: {
    name: "Wayland",
    icon: WAYLAND_ICON
  },
  [TraceType.WAYLAND_DUMP]: {
    name: "Wayland Dump",
    icon: WAYLAND_ICON
  },
  [TraceType.PROTO_LOG]: {
    name: "Proto Log",
    icon: PROTO_LOG_ICON
  },
  [TraceType.SYSTEM_UI]: {
    name: "System UI",
    icon: SYSTEM_UI_ICON
  },
  [TraceType.LAUNCHER]: {
    name: "Launcher",
    icon: LAUNCHER_ICON
  },
  [TraceType.INPUT_METHOD_CLIENTS]: {
    name: "IME Clients",
    icon: IME_ICON
  },
  [TraceType.INPUT_METHOD_SERVICE]: {
    name: "IME Service",
    icon: IME_ICON
  },
  [TraceType.INPUT_METHOD_MANAGER_SERVICE]: {
    name: "IME Manager Service",
    icon: IME_ICON
  },
  [TraceType.TAG]: {
    name: "Tag",
    icon: TAG_ICON
  },
  [TraceType.ERROR]: {
    name: "Error",
    icon: TRACE_ERROR_ICON
  },
};
