import { DATA_TYPES } from '../decode.js'

const mixin = {
  isTrace: function (file) {
    return file.type == DATA_TYPES.WINDOW_MANAGER ||
      file.type == DATA_TYPES.SURFACE_FLINGER ||
      file.type == DATA_TYPES.WAYLAND ||
      file.type == DATA_TYPES.SYSTEM_UI ||
      file.type == DATA_TYPES.LAUNCHER
  },
  isVideo(file) {
    return file.type == DATA_TYPES.SCREEN_RECORDING;
  },
  isTransactions(file) {
    return file.type == DATA_TYPES.TRANSACTION;
  },
  isLog(file) {
    return file.type == DATA_TYPES.PROTO_LOG
  },
  hasDataView(file) {
    return this.isLog(file) || this.isTrace(file) || this.isTransactions(file);
  },
  getDataTypeByName(name) {
    for (const dataType in DATA_TYPES) {
      if (DATA_TYPES.hasOwnProperty(dataType)
        && DATA_TYPES[dataType].name === name) {
        return DATA_TYPES[dataType];
      }
    }

    throw new Error(`No data type with name ${name} found`);
  },
}

export { mixin }

export default {
  name: 'FileType',
  methods: mixin
}