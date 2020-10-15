/*
 * Copyright 2017, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* eslint-disable camelcase */
/* eslint-disable max-len */

import jsonProtoDefsWm from 'frameworks/base/core/proto/android/server/windowmanagertrace.proto';
import jsonProtoDefsProtoLog from 'frameworks/base/core/proto/android/internal/protolog.proto';
import jsonProtoDefsSf from 'frameworks/native/services/surfaceflinger/layerproto/layerstrace.proto';
import jsonProtoDefsTransaction from 'frameworks/native/cmds/surfacereplayer/proto/src/trace.proto';
import jsonProtoDefsWl from 'WaylandSafePath/waylandtrace.proto';
import jsonProtoDefsSysUi from 'frameworks/base/packages/SystemUI/src/com/android/systemui/tracing/sysui_trace.proto';
import jsonProtoDefsLauncher from 'packages/apps/Launcher3/protos/launcher_trace_file.proto';
import jsonProtoDefsIme from 'frameworks/base/core/proto/android/view/inputmethod/inputmethodeditortrace.proto';
import protobuf from 'protobufjs';
import {transformLayers, transformLayersTrace} from './transform_sf.js';
import {transform_transaction_trace} from './transform_transaction.js';
import {transform_wl_outputstate, transform_wayland_trace} from './transform_wl.js';
import {transformProtolog} from './transform_protolog.js';
import {transform_sysui_trace} from './transform_sys_ui.js';
import {transform_launcher_trace} from './transform_launcher.js';
import {transform_ime_trace} from './transform_ime.js';
import {fill_transform_data} from './matrix_utils.js';
import {mp4Decoder} from './decodeVideo.js';

import SurfaceFlingerTrace from '@/traces/SurfaceFlinger.ts';
import WindowManagerTrace from '@/traces/WindowManager.ts';
import TransactionsTrace from '@/traces/Transactions.ts';
import ScreenRecordingTrace from '@/traces/ScreenRecording.ts';
import WaylandTrace from '@/traces/Wayland.ts';
import ProtoLogTrace from '@/traces/ProtoLog.ts';
import SystemUITrace from '@/traces/SystemUI.ts';
import LauncherTrace from '@/traces/Launcher.ts';
import ImeTraceClients from '@/traces/InputMethodClients.ts';

import SurfaceFlingerDump from '@/dumps/SurfaceFlinger.ts';
import WindowManagerDump from '@/dumps/WindowManager.ts';
import WaylandDump from '@/dumps/Wayland.ts';

const WmTraceMessage = lookup_type(jsonProtoDefsWm, 'com.android.server.wm.WindowManagerTraceFileProto');
const WmDumpMessage = lookup_type(jsonProtoDefsWm, 'com.android.server.wm.WindowManagerServiceDumpProto');
const SfTraceMessage = lookup_type(jsonProtoDefsSf, 'android.surfaceflinger.LayersTraceFileProto');
const SfDumpMessage = lookup_type(jsonProtoDefsSf, 'android.surfaceflinger.LayersProto');
const SfTransactionTraceMessage = lookup_type(jsonProtoDefsTransaction, 'Trace');
const WaylandTraceMessage = lookup_type(jsonProtoDefsWl, 'org.chromium.arc.wayland_composer.TraceFileProto');
const WaylandDumpMessage = lookup_type(jsonProtoDefsWl, 'org.chromium.arc.wayland_composer.OutputStateProto');
const ProtoLogMessage = lookup_type(jsonProtoDefsProtoLog, 'com.android.internal.protolog.ProtoLogFileProto');
const SystemUiTraceMessage = lookup_type(jsonProtoDefsSysUi, 'com.android.systemui.tracing.SystemUiTraceFileProto');
const LauncherTraceMessage = lookup_type(jsonProtoDefsLauncher, 'com.android.launcher3.tracing.LauncherTraceFileProto');
const InputMethodClientsTraceMessage = lookup_type(jsonProtoDefsIme, "android.view.inputmethod.InputMethodClientsTraceFileProto");

const LAYER_TRACE_MAGIC_NUMBER = [0x09, 0x4c, 0x59, 0x52, 0x54, 0x52, 0x41, 0x43, 0x45]; // .LYRTRACE
const WINDOW_TRACE_MAGIC_NUMBER = [0x09, 0x57, 0x49, 0x4e, 0x54, 0x52, 0x41, 0x43, 0x45]; // .WINTRACE
const MPEG4_MAGIC_NMBER = [0x00, 0x00, 0x00, 0x18, 0x66, 0x74, 0x79, 0x70, 0x6d, 0x70, 0x34, 0x32]; // ....ftypmp42
const WAYLAND_TRACE_MAGIC_NUMBER = [0x09, 0x57, 0x59, 0x4c, 0x54, 0x52, 0x41, 0x43, 0x45]; // .WYLTRACE
const PROTO_LOG_MAGIC_NUMBER = [0x09, 0x50, 0x52, 0x4f, 0x54, 0x4f, 0x4c, 0x4f, 0x47]; // .PROTOLOG
const SYSTEM_UI_MAGIC_NUMBER = [0x09, 0x53, 0x59, 0x53, 0x55, 0x49, 0x54, 0x52, 0x43]; // .SYSUITRC
const LAUNCHER_MAGIC_NUMBER = [0x09, 0x4C, 0x4E, 0x43, 0x48, 0x52, 0x54, 0x52, 0x43]; // .LNCHRTRC
const IMC_TRACE_MAGIC_NUMBER = [0x09, 0x49, 0x4d, 0x43, 0x54, 0x52, 0x41, 0x43, 0x45] //.IMCTRACE

const FILE_TYPES = Object.freeze({
  WINDOW_MANAGER_TRACE: 'WindowManagerTrace',
  SURFACE_FLINGER_TRACE: 'SurfaceFlingerTrace',
  WINDOW_MANAGER_DUMP: 'WindowManagerDump',
  SURFACE_FLINGER_DUMP: 'SurfaceFlingerDump',
  SCREEN_RECORDING: 'ScreenRecording',
  TRANSACTIONS_TRACE: 'TransactionsTrace',
  WAYLAND_TRACE: 'WaylandTrace',
  WAYLAND_DUMP: 'WaylandDump',
  PROTO_LOG: 'ProtoLog',
  SYSTEM_UI: 'SystemUI',
  LAUNCHER: 'Launcher',
  IME_TRACE_CLIENTS: 'ImeTraceClients',
});

const WINDOW_MANAGER_ICON = 'view_compact';
const SURFACE_FLINGER_ICON = 'filter_none';
const SCREEN_RECORDING_ICON = 'videocam';
const TRANSACTION_ICON = 'timeline';
const WAYLAND_ICON = 'filter_none';
const PROTO_LOG_ICON = 'notes';
const SYSTEM_UI_ICON = 'filter_none';
const LAUNCHER_ICON = 'filter_none';
const IME_ICON = 'keyboard';

const FILE_ICONS = {
  [FILE_TYPES.WINDOW_MANAGER_TRACE]: WINDOW_MANAGER_ICON,
  [FILE_TYPES.SURFACE_FLINGER_TRACE]: SURFACE_FLINGER_ICON,
  [FILE_TYPES.WINDOW_MANAGER_DUMP]: WINDOW_MANAGER_ICON,
  [FILE_TYPES.SURFACE_FLINGER_DUMP]: SURFACE_FLINGER_ICON,
  [FILE_TYPES.SCREEN_RECORDING]: SCREEN_RECORDING_ICON,
  [FILE_TYPES.TRANSACTIONS_TRACE]: TRANSACTION_ICON,
  [FILE_TYPES.WAYLAND_TRACE]: WAYLAND_ICON,
  [FILE_TYPES.WAYLAND_DUMP]: WAYLAND_ICON,
  [FILE_TYPES.PROTO_LOG]: PROTO_LOG_ICON,
  [FILE_TYPES.SYSTEM_UI]: SYSTEM_UI_ICON,
  [FILE_TYPES.LAUNCHER]: LAUNCHER_ICON,
  [FILE_TYPES.IME_TRACE_CLIENTS]: IME_ICON,
};

function oneOf(dataType) {
  return {oneOf: true, type: dataType};
}

function manyOf(dataType, fold = null) {
  return {manyOf: true, type: dataType, fold};
}

const TRACE_TYPES = Object.freeze({
  WINDOW_MANAGER: 'WindowManagerTrace',
  SURFACE_FLINGER: 'SurfaceFlingerTrace',
  SCREEN_RECORDING: 'ScreenRecording',
  TRANSACTION: 'Transaction',
  WAYLAND: 'Wayland',
  PROTO_LOG: 'ProtoLog',
  SYSTEM_UI: 'SystemUI',
  LAUNCHER: 'Launcher',
  IME_CLIENTS: 'ImeTraceClients',
});

const TRACE_INFO = {
  [TRACE_TYPES.WINDOW_MANAGER]: {
    name: 'WindowManager',
    icon: WINDOW_MANAGER_ICON,
    files: [oneOf(FILE_TYPES.WINDOW_MANAGER_TRACE)],
    constructor: WindowManagerTrace,
  },
  [TRACE_TYPES.SURFACE_FLINGER]: {
    name: 'SurfaceFlinger',
    icon: SURFACE_FLINGER_ICON,
    files: [oneOf(FILE_TYPES.SURFACE_FLINGER_TRACE)],
    constructor: SurfaceFlingerTrace,
  },
  [TRACE_TYPES.SCREEN_RECORDING]: {
    name: 'Screen recording',
    icon: SCREEN_RECORDING_ICON,
    files: [oneOf(FILE_TYPES.SCREEN_RECORDING)],
    constructor: ScreenRecordingTrace,
  },
  [TRACE_TYPES.TRANSACTION]: {
    name: 'Transaction',
    icon: TRANSACTION_ICON,
    files: [
      oneOf(FILE_TYPES.TRANSACTIONS_TRACE),
    ],
    constructor: TransactionsTrace,
  },
  [TRACE_TYPES.WAYLAND]: {
    name: 'Wayland',
    icon: WAYLAND_ICON,
    files: [oneOf(FILE_TYPES.WAYLAND_TRACE)],
    constructor: WaylandTrace,
  },
  [TRACE_TYPES.PROTO_LOG]: {
    name: 'ProtoLog',
    icon: PROTO_LOG_ICON,
    files: [oneOf(FILE_TYPES.PROTO_LOG)],
    constructor: ProtoLogTrace,
  },
  [TRACE_TYPES.SYSTEM_UI]: {
    name: 'SystemUI',
    icon: SYSTEM_UI_ICON,
    files: [oneOf(FILE_TYPES.SYSTEM_UI)],
    constructor: SystemUITrace,
  },
  [TRACE_TYPES.LAUNCHER]: {
    name: 'Launcher',
    icon: LAUNCHER_ICON,
    files: [oneOf(FILE_TYPES.LAUNCHER)],
    constructor: LauncherTrace,
  },
  [TRACE_TYPES.IME_CLIENTS]: {
    name: 'InputMethodClients',
    icon: IME_ICON,
    files: [oneOf(FILE_TYPES.IME_TRACE_CLIENTS)],
    constructor: ImeTraceClients,
  },
};

const DUMP_TYPES = Object.freeze({
  WINDOW_MANAGER: 'WindowManagerDump',
  SURFACE_FLINGER: 'SurfaceFlingerDump',
  WAYLAND: 'WaylandDump',
});

const DUMP_INFO = {
  [DUMP_TYPES.WINDOW_MANAGER]: {
    name: 'WindowManager',
    icon: WINDOW_MANAGER_ICON,
    files: [oneOf(FILE_TYPES.WINDOW_MANAGER_DUMP)],
    constructor: WindowManagerDump,
  },
  [DUMP_TYPES.SURFACE_FLINGER]: {
    name: 'SurfaceFlinger',
    icon: SURFACE_FLINGER_ICON,
    files: [oneOf(FILE_TYPES.SURFACE_FLINGER_DUMP)],
    constructor: SurfaceFlingerDump,
  },
  [DUMP_TYPES.WAYLAND]: {
    name: 'Wayland',
    icon: WAYLAND_ICON,
    files: [oneOf(FILE_TYPES.WAYLAND_DUMP)],
    constructor: WaylandDump,
  },
};

export const TRACE_ICONS = {
  [TRACE_TYPES.WINDOW_MANAGER]: WINDOW_MANAGER_ICON,
  [TRACE_TYPES.SURFACE_FLINGER]: SURFACE_FLINGER_ICON,
  [TRACE_TYPES.SCREEN_RECORDING]: SCREEN_RECORDING_ICON,
  [TRACE_TYPES.TRANSACTION]: TRANSACTION_ICON,
  [TRACE_TYPES.WAYLAND]: WAYLAND_ICON,
  [TRACE_TYPES.PROTO_LOG]: PROTO_LOG_ICON,
  [TRACE_TYPES.SYSTEM_UI]: SYSTEM_UI_ICON,
  [TRACE_TYPES.LAUNCHER]: LAUNCHER_ICON,
  [TRACE_TYPES.IME_CLIENTS]: IME_ICON,

  [DUMP_TYPES.WINDOW_MANAGER]: WINDOW_MANAGER_ICON,
  [DUMP_TYPES.SURFACE_FLINGER]: SURFACE_FLINGER_ICON,
  [DUMP_TYPES.WAYLAND]: WAYLAND_ICON,
};

// TODO: Rename name to defaultName
const FILE_DECODERS = {
  [FILE_TYPES.WINDOW_MANAGER_TRACE]: {
    name: 'WindowManager trace',
    decoder: protoDecoder,
    decoderParams: {
      type: FILE_TYPES.WINDOW_MANAGER_TRACE,
      protoType: WmTraceMessage,
      transform: WindowManagerTrace.fromProto,
      timeline: true,
    },
  },
  [FILE_TYPES.SURFACE_FLINGER_TRACE]: {
    name: 'SurfaceFlinger trace',
    decoder: protoDecoder,
    decoderParams: {
      type: FILE_TYPES.SURFACE_FLINGER_TRACE,
      mime: 'application/octet-stream',
      protoType: SfTraceMessage,
      transform: transformLayersTrace,
      timeline: true,
    },
  },
  [FILE_TYPES.WAYLAND_TRACE]: {
    name: 'Wayland trace',
    decoder: protoDecoder,
    decoderParams: {
      type: FILE_TYPES.WAYLAND_TRACE,
      mime: 'application/octet-stream',
      protoType: WaylandTraceMessage,
      transform: transform_wayland_trace,
      timeline: true,
    },
  },
  [FILE_TYPES.SURFACE_FLINGER_DUMP]: {
    name: 'SurfaceFlinger dump',
    decoder: protoDecoder,
    decoderParams: {
      type: FILE_TYPES.SURFACE_FLINGER_DUMP,
      mime: 'application/octet-stream',
      protoType: SfDumpMessage,
      transform: (decoded) => transformLayers(true /* includesCompositionState*/, decoded),
      timeline: false,
    },
  },
  [FILE_TYPES.WINDOW_MANAGER_DUMP]: {
    name: 'WindowManager dump',
    decoder: protoDecoder,
    decoderParams: {
      type: FILE_TYPES.WINDOW_MANAGER_DUMP,
      mime: 'application/octet-stream',
      protoType: WmDumpMessage,
      transform: WindowManagerDump.fromProto,
      timeline: false,
    },
  },
  [FILE_TYPES.WAYLAND_DUMP]: {
    name: 'Wayland dump',
    decoder: protoDecoder,
    decoderParams: {
      type: FILE_TYPES.WAYLAND_DUMP,
      mime: 'application/octet-stream',
      protoType: WaylandDumpMessage,
      transform: transform_wl_outputstate,
      timeline: false,
    },
  },
  [FILE_TYPES.SCREEN_RECORDING]: {
    name: 'Screen recording',
    decoder: videoDecoder,
    decoderParams: {
      type: FILE_TYPES.SCREEN_RECORDING,
      mime: 'video/mp4',
      videoDecoder: mp4Decoder,
    },
  },
  [FILE_TYPES.TRANSACTIONS_TRACE]: {
    name: 'Transaction',
    decoder: protoDecoder,
    decoderParams: {
      type: FILE_TYPES.TRANSACTIONS_TRACE,
      mime: 'application/octet-stream',
      protoType: SfTransactionTraceMessage,
      transform: transform_transaction_trace,
      timeline: true,
    },
  },
  [FILE_TYPES.PROTO_LOG]: {
    name: 'ProtoLog',
    decoder: protoDecoder,
    decoderParams: {
      type: FILE_TYPES.PROTO_LOG,
      mime: 'application/octet-stream',
      protoType: ProtoLogMessage,
      transform: transformProtolog,
      timeline: true,
    },
  },
  [FILE_TYPES.SYSTEM_UI]: {
    name: 'SystemUI trace',
    decoder: protoDecoder,
    decoderParams: {
      type: FILE_TYPES.SYSTEM_UI,
      mime: 'application/octet-stream',
      protoType: SystemUiTraceMessage,
      transform: transform_sysui_trace,
      timeline: true,
    },
  },
  [FILE_TYPES.LAUNCHER]: {
    name: 'Launcher trace',
    decoder: protoDecoder,
    decoderParams: {
      type: FILE_TYPES.LAUNCHER,
      mime: 'application/octet-stream',
      protoType: LauncherTraceMessage,
      transform: transform_launcher_trace,
      timeline: true,
    },
  },
  [FILE_TYPES.IME_TRACE_CLIENTS]: {
    name: 'InputMethodClients trace',
    decoder: protoDecoder,
    decoderParams: {
      type: FILE_TYPES.IME_TRACE_CLIENTS,
      mime: 'application/octet-stream',
      protoType: InputMethodClientsTraceMessage,
      transform: transform_ime_trace,
      timeline: true,
    },
  },
};

function lookup_type(protoPath, type) {
  return protobuf.Root.fromJSON(protoPath).lookupType(type);
}

// Replace enum values with string representation and
// add default values to the proto objects. This function also handles
// a special case with TransformProtos where the matrix may be derived
// from the transform type.
function modifyProtoFields(protoObj, displayDefaults) {
  if (!protoObj || protoObj !== Object(protoObj) || !protoObj.$type) {
    return;
  }

  for (const fieldName in protoObj.$type.fields) {
    if (protoObj.$type.fields.hasOwnProperty(fieldName)) {
      const fieldProperties = protoObj.$type.fields[fieldName];
      const field = protoObj[fieldName];

      if (Array.isArray(field)) {
        field.forEach((item, _) => {
          modifyProtoFields(item, displayDefaults);
        });
        continue;
      }

      if (displayDefaults && !(field)) {
        protoObj[fieldName] = fieldProperties.defaultValue;
      }

      if (fieldProperties.type === 'TransformProto') {
        fill_transform_data(protoObj[fieldName]);
        continue;
      }

      if (fieldProperties.resolvedType && fieldProperties.resolvedType.valuesById) {
        protoObj[fieldName] = fieldProperties.resolvedType.valuesById[protoObj[fieldProperties.name]];
        continue;
      }
      modifyProtoFields(protoObj[fieldName], displayDefaults);
    }
  }
}

function decodeAndTransformProto(buffer, params, displayDefaults) {
  const decoded = params.protoType.decode(buffer);
  modifyProtoFields(decoded, displayDefaults);
  const transformed = params.transform(decoded);

  return transformed;
}

function protoDecoder(buffer, params, fileName, store) {
  const transformed = decodeAndTransformProto(buffer, params, store.displayDefaults);
  let data;
  if (params.timeline) {
    data = transformed.entries ?? transformed.children;
  } else {
    data = [transformed];
  }
  const blobUrl = URL.createObjectURL(new Blob([buffer], {type: params.mime}));
  return dataFile(fileName, data.map((x) => x.timestamp), data, blobUrl, params.type);
}

function videoDecoder(buffer, params, fileName, store) {
  const [data, timeline] = params.videoDecoder(buffer);
  const blobUrl = URL.createObjectURL(new Blob([data], {type: params.mime}));
  return dataFile(fileName, timeline, blobUrl, blobUrl, params.type);
}

function dataFile(filename, timeline, data, blobUrl, type) {
  return {
    filename: filename,
    // Object is frozen for performance reasons
    // It will prevent Vue from making it a reactive object which will be very slow as the timeline gets larger.
    timeline: Object.freeze(timeline),
    data: data,
    blobUrl: blobUrl,
    type: type,
    selectedIndex: 0,
    destroy() {
      URL.revokeObjectURL(this.blobUrl);
    },
  };
}

function arrayEquals(a, b) {
  if (a.length !== b.length) {
    return false;
  }
  for (let i = 0; i < a.length; i++) {
    if (a[i] != b[i]) {
      return false;
    }
  }
  return true;
}

function arrayStartsWith(array, prefix) {
  return arrayEquals(array.slice(0, prefix.length), prefix);
}

function decodedFile(fileType, buffer, fileName, store) {
  const fileDecoder = FILE_DECODERS[fileType];
  return [fileType, fileDecoder.decoder(buffer, fileDecoder.decoderParams, fileName, store)];
}

function detectAndDecode(buffer, fileName, store) {
  if (arrayStartsWith(buffer, LAYER_TRACE_MAGIC_NUMBER)) {
    return decodedFile(FILE_TYPES.SURFACE_FLINGER_TRACE, buffer, fileName, store);
  }
  if (arrayStartsWith(buffer, WINDOW_TRACE_MAGIC_NUMBER)) {
    return decodedFile(FILE_TYPES.WINDOW_MANAGER_TRACE, buffer, fileName, store);
  }
  if (arrayStartsWith(buffer, MPEG4_MAGIC_NMBER)) {
    return decodedFile(FILE_TYPES.SCREEN_RECORDING, buffer, fileName, store);
  }
  if (arrayStartsWith(buffer, WAYLAND_TRACE_MAGIC_NUMBER)) {
    return decodedFile(FILE_TYPES.WAYLAND_TRACE, buffer, fileName, store);
  }
  if (arrayStartsWith(buffer, PROTO_LOG_MAGIC_NUMBER)) {
    return decodedFile(FILE_TYPES.PROTO_LOG, buffer, fileName, store);
  }
  if (arrayStartsWith(buffer, SYSTEM_UI_MAGIC_NUMBER)) {
    return decodedFile(FILE_TYPES.SYSTEM_UI, buffer, fileName, store);
  }
  if (arrayStartsWith(buffer, LAUNCHER_MAGIC_NUMBER)) {
    return decodedFile(FILE_TYPES.LAUNCHER, buffer, fileName, store);
  }
  if (arrayStartsWith(buffer, IMC_TRACE_MAGIC_NUMBER)) {
    return decodedFile(FILE_TYPES.IME_TRACE_CLIENTS, buffer, fileName, store);
  }

  // TODO(b/169305853): Add magic number at beginning of file for better auto detection
  for (const [filetype, condition] of [
    [FILE_TYPES.TRANSACTIONS_TRACE, (file) => file.data.length > 0],
    [FILE_TYPES.WAYLAND_DUMP, (file) => (file.data.length > 0 && file.data.children[0] > 0) || file.data.length > 1],
    [FILE_TYPES.WINDOW_MANAGER_DUMP],
    [FILE_TYPES.SURFACE_FLINGER_DUMP],
  ]) {
    try {
      const [, fileData] = decodedFile(filetype, buffer, fileName, store);

      // A generic file will often wrongly be decoded as an empty wayland dump file
      if (condition && !condition(fileData)) {
        // Fall through to next filetype
        continue;
      }

      return [filetype, fileData];
    } catch (ex) {
      // ignore exception and fall through to next filetype
    }
  }
  throw new UndetectableFileType('Unable to detect file');
}

/**
 * Error is raised when detectAndDecode is called but the file can't be
 * automatically detected as being of a compatible file type.
 */
class UndetectableFileType extends Error { }

export {detectAndDecode, decodeAndTransformProto, FILE_TYPES, TRACE_INFO, TRACE_TYPES, DUMP_TYPES, DUMP_INFO, FILE_DECODERS, FILE_ICONS, UndetectableFileType};
