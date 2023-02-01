import {TraceType} from 'trace/trace_type';

const WINDOW_MANAGER_ICON = 'view_compact';
const SURFACE_FLINGER_ICON = 'filter_none';
const SCREEN_RECORDING_ICON = 'videocam';
const TRANSACTION_ICON = 'timeline';
const WAYLAND_ICON = 'filter_none';
const PROTO_LOG_ICON = 'notes';
const SYSTEM_UI_ICON = 'filter_none';
const LAUNCHER_ICON = 'filter_none';
const IME_ICON = 'keyboard';
const ACCESSIBILITY_ICON = 'filter_none';
const TAG_ICON = 'details';
const TRACE_ERROR_ICON = 'warning';

interface IconMap {
  [key: number]: string;
}

export const TRACE_ICONS: IconMap = {
  [TraceType.ACCESSIBILITY]: ACCESSIBILITY_ICON,
  [TraceType.WINDOW_MANAGER]: WINDOW_MANAGER_ICON,
  [TraceType.SURFACE_FLINGER]: SURFACE_FLINGER_ICON,
  [TraceType.SCREEN_RECORDING]: SCREEN_RECORDING_ICON,
  [TraceType.TRANSACTIONS]: TRANSACTION_ICON,
  [TraceType.TRANSACTIONS_LEGACY]: TRANSACTION_ICON,
  [TraceType.WAYLAND]: WAYLAND_ICON,
  [TraceType.WAYLAND_DUMP]: WAYLAND_ICON,
  [TraceType.PROTO_LOG]: PROTO_LOG_ICON,
  [TraceType.SYSTEM_UI]: SYSTEM_UI_ICON,
  [TraceType.LAUNCHER]: LAUNCHER_ICON,
  [TraceType.INPUT_METHOD_CLIENTS]: IME_ICON,
  [TraceType.INPUT_METHOD_SERVICE]: IME_ICON,
  [TraceType.INPUT_METHOD_MANAGER_SERVICE]: IME_ICON,
  [TraceType.TAG]: TAG_ICON,
  [TraceType.ERROR]: TRACE_ERROR_ICON,
};
