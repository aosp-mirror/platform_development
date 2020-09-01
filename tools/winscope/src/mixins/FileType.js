import {TRACE_TYPES, DUMP_TYPES} from '@/decode.js';

const mixin = {
  showInTraceView(file) {
    return file.type == TRACE_TYPES.WINDOW_MANAGER ||
      file.type == TRACE_TYPES.SURFACE_FLINGER ||
      file.type == TRACE_TYPES.WAYLAND ||
      file.type == TRACE_TYPES.SYSTEM_UI ||
      file.type == TRACE_TYPES.LAUNCHER ||
      file.type == DUMP_TYPES.WINDOW_MANAGER ||
      file.type == DUMP_TYPES.SURFACE_FLINGER ||
      file.type == DUMP_TYPES.WAYLAND;
  },
  showInWindowManagerTraceView(file) {
    return file.type == TRACE_TYPES.WINDOW_MANAGER ||
        file.type == DUMP_TYPES.WINDOW_MANAGER;
  },
  showInSurfaceFlingerTraceView(file) {
    return file.type == TRACE_TYPES.SURFACE_FLINGER ||
        file.type == DUMP_TYPES.SURFACE_FLINGER;
  },
  isVideo(file) {
    return file.type == TRACE_TYPES.SCREEN_RECORDING;
  },
  isTransactions(file) {
    return file.type == TRACE_TYPES.TRANSACTION;
  },
  isLog(file) {
    return file.type == TRACE_TYPES.PROTO_LOG;
  },
  hasDataView(file) {
    return this.isLog(file) || this.showInTraceView(file) || this.isTransactions(file);
  },
};

export {mixin};

export default {
  name: 'FileType',
  methods: mixin,
};
