import { DATA_TYPES } from './decode.js'

export default {
    name: 'FileType',
    methods: {
        isTrace: function (file) {
            return file.type == DATA_TYPES.WINDOW_MANAGER ||
                file.type == DATA_TYPES.SURFACE_FLINGER ||
                file.type == DATA_TYPES.TRANSACTION ||
                file.type == DATA_TYPES.WAYLAND ||
                file.type == DATA_TYPES.SYSTEM_UI ||
                file.type == DATA_TYPES.LAUNCHER
        },
        isVideo(file) {
            return file.type == DATA_TYPES.SCREEN_RECORDING;
        },
        isLog(file) {
            return file.type == DATA_TYPES.PROTO_LOG
        }
    }
}