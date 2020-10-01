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


import jsonProtoDefsWm from 'frameworks/base/core/proto/android/server/windowmanagertrace.proto'
import jsonProtoDefsProtoLog from 'frameworks/base/core/proto/android/server/protolog.proto'
import jsonProtoDefsSf from 'frameworks/native/services/surfaceflinger/layerproto/layerstrace.proto'
import jsonProtoDefsTransaction from 'frameworks/native/cmds/surfacereplayer/proto/src/trace.proto'
import jsonProtoDefsWl from 'WaylandSafePath/waylandtrace.proto'
import jsonProtoDefsSysUi from 'frameworks/base/packages/SystemUI/src/com/android/systemui/tracing/sysui_trace.proto'
import jsonProtoDefsLauncher from 'packages/apps/Launcher3/protos/launcher_trace_file.proto'
import protobuf from 'protobufjs'
import { transform_layers, transform_layers_trace } from './transform_sf.js'
import { transform_window_service, transform_window_trace } from './transform_wm.js'
import { transform_transaction_trace } from './transform_transaction.js'
import { transform_wl_outputstate, transform_wayland_trace } from './transform_wl.js'
import { transform_protolog } from './transform_protolog.js'
import { transform_sysui_trace } from './transform_sys_ui.js'
import { transform_launcher_trace } from './transform_launcher.js'
import { fill_transform_data } from './matrix_utils.js'
import { mp4Decoder } from './decodeVideo.js'

var WmTraceMessage = lookup_type(jsonProtoDefsWm, "com.android.server.wm.WindowManagerTraceFileProto");
var WmDumpMessage = lookup_type(jsonProtoDefsWm, "com.android.server.wm.WindowManagerServiceDumpProto");
var SfTraceMessage = lookup_type(jsonProtoDefsSf, "android.surfaceflinger.LayersTraceFileProto");
var SfDumpMessage = lookup_type(jsonProtoDefsSf, "android.surfaceflinger.LayersProto");
var SfTransactionTraceMessage = lookup_type(jsonProtoDefsTransaction, "Trace");
var WaylandTraceMessage = lookup_type(jsonProtoDefsWl, "org.chromium.arc.wayland_composer.TraceFileProto");
var WaylandDumpMessage = lookup_type(jsonProtoDefsWl, "org.chromium.arc.wayland_composer.OutputStateProto");
var ProtoLogMessage = lookup_type(jsonProtoDefsProtoLog, "com.android.server.protolog.ProtoLogFileProto");
var SystemUiTraceMessage = lookup_type(jsonProtoDefsSysUi, "com.android.systemui.tracing.SystemUiTraceFileProto");
var LauncherTraceMessage = lookup_type(jsonProtoDefsLauncher, "com.android.launcher3.tracing.LauncherTraceFileProto");

const LAYER_TRACE_MAGIC_NUMBER = [0x09, 0x4c, 0x59, 0x52, 0x54, 0x52, 0x41, 0x43, 0x45] // .LYRTRACE
const WINDOW_TRACE_MAGIC_NUMBER = [0x09, 0x57, 0x49, 0x4e, 0x54, 0x52, 0x41, 0x43, 0x45] // .WINTRACE
const MPEG4_MAGIC_NMBER = [0x00, 0x00, 0x00, 0x18, 0x66, 0x74, 0x79, 0x70, 0x6d, 0x70, 0x34, 0x32] // ....ftypmp42
const WAYLAND_TRACE_MAGIC_NUMBER = [0x09, 0x57, 0x59, 0x4c, 0x54, 0x52, 0x41, 0x43, 0x45] // .WYLTRACE
const PROTO_LOG_MAGIC_NUMBER = [0x09, 0x50, 0x52, 0x4f, 0x54, 0x4f, 0x4c, 0x4f, 0x47] // .PROTOLOG
const SYSTEM_UI_MAGIC_NUMBER = [0x09, 0x53, 0x59, 0x53, 0x55, 0x49, 0x54, 0x52, 0x43] // .SYSUITRC
const LAUNCHER_MAGIC_NUMBER = [0x09, 0x4C, 0x4E, 0x43, 0x48, 0x52, 0x54, 0x52, 0x43] // .LNCHRTRC

const DATA_TYPES = {
  WINDOW_MANAGER: {
    name: "WindowManager",
    icon: "view_compact",
    mime: "application/octet-stream",
  },
  SURFACE_FLINGER: {
    name: "SurfaceFlinger",
    icon: "filter_none",
    mime: "application/octet-stream",
  },
  SCREEN_RECORDING: {
    name: "Screen recording",
    icon: "videocam",
    mime: "video/mp4",
  },
  TRANSACTION: {
    name: "Transaction",
    icon: "timeline",
    mime: "application/octet-stream",
  },
  WAYLAND: {
    name: "Wayland",
    icon: "filter_none",
    mime: "application/octet-stream",
  },
  PROTO_LOG: {
    name: "ProtoLog",
    icon: "notes",
    mime: "application/octet-stream",
  },
  SYSTEM_UI: {
    name: "SystemUI",
    icon: "filter_none",
    mime: "application/octet-stream",
  },
  LAUNCHER: {
    name: "Launcher",
    icon: "filter_none",
    mime: "application/octet-stream",
  },
}

const FILE_TYPES = {
  'window_trace': {
    name: "WindowManager trace",
    dataType: DATA_TYPES.WINDOW_MANAGER,
    decoder: protoDecoder,
    decoderParams: {
      protoType: WmTraceMessage,
      transform: transform_window_trace,
      timeline: true,
    },
  },
  'layers_trace': {
    name: "SurfaceFlinger trace",
    dataType: DATA_TYPES.SURFACE_FLINGER,
    decoder: protoDecoder,
    decoderParams: {
      protoType: SfTraceMessage,
      transform: transform_layers_trace,
      timeline: true,
    },
  },
  'wl_trace': {
    name: "Wayland trace",
    dataType: DATA_TYPES.WAYLAND,
    decoder: protoDecoder,
    decoderParams: {
      protoType: WaylandTraceMessage,
      transform: transform_wayland_trace,
      timeline: true,
    },
  },
  'layers_dump': {
    name: "SurfaceFlinger dump",
    dataType: DATA_TYPES.SURFACE_FLINGER,
    decoder: protoDecoder,
    decoderParams: {
      protoType: SfDumpMessage,
      transform: (decoded) => transform_layers(true /*includesCompositionState*/, decoded),
      timeline: false,
    },
  },
  'window_dump': {
    name: "WindowManager dump",
    dataType: DATA_TYPES.WINDOW_MANAGER,
    decoder: protoDecoder,
    decoderParams: {
      protoType: WmDumpMessage,
      transform: transform_window_service,
      timeline: false,
    },
  },
  'wl_dump': {
    name: "Wayland dump",
    dataType: DATA_TYPES.WAYLAND,
    decoder: protoDecoder,
    decoderParams: {
      protoType: WaylandDumpMessage,
      transform: transform_wl_outputstate,
      timeline: false,
    },
  },
  'screen_recording': {
    name: "Screen recording",
    dataType: DATA_TYPES.SCREEN_RECORDING,
    decoder: videoDecoder,
    decoderParams: {
      videoDecoder: mp4Decoder,
    },
  },
  'transaction': {
    name: "Transaction",
    dataType: DATA_TYPES.TRANSACTION,
    decoder: protoDecoder,
    decoderParams: {
      protoType: SfTransactionTraceMessage,
      transform: transform_transaction_trace,
      timeline: true,
    }
  },
  'proto_log': {
    name: "ProtoLog",
    dataType: DATA_TYPES.PROTO_LOG,
    decoder: protoDecoder,
    decoderParams: {
      protoType: ProtoLogMessage,
      transform: transform_protolog,
      timeline: true,
    }
  },
  'system_ui_trace': {
    name: "SystemUI trace",
    dataType: DATA_TYPES.SYSTEM_UI,
    decoder: protoDecoder,
    decoderParams: {
      protoType: SystemUiTraceMessage,
      transform: transform_sysui_trace,
      timeline: true,
    }
  },
  'launcher_trace': {
    name: "Launcher trace",
    dataType: DATA_TYPES.LAUNCHER,
    decoder: protoDecoder,
    decoderParams: {
      protoType: LauncherTraceMessage,
      transform: transform_launcher_trace,
      timeline: true,
    }
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
  for (var fieldName in protoObj.$type.fields) {
    var fieldProperties = protoObj.$type.fields[fieldName];
    var field = protoObj[fieldName];

    if (Array.isArray(field)) {
      field.forEach((item, _) => {
        modifyProtoFields(item, displayDefaults);
      })
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

function protoDecoder(buffer, fileType, fileName, store) {
  var decoded = fileType.decoderParams.protoType.decode(buffer);
  modifyProtoFields(decoded, store.displayDefaults);
  var transformed = fileType.decoderParams.transform(decoded);
  var data
  if (fileType.decoderParams.timeline) {
    data = transformed.children;
  } else {
    data = [transformed];
  }
  let blobUrl = URL.createObjectURL(new Blob([buffer], { type: fileType.dataType.mime }));
  return dataFile(fileName, data.map(x => x.timestamp), data, blobUrl, fileType.dataType);
}

function videoDecoder(buffer, fileType, fileName, store) {
  let [data, timeline] = fileType.decoderParams.videoDecoder(buffer);
  let blobUrl = URL.createObjectURL(new Blob([data], { type: fileType.dataType.mime }));
  return dataFile(fileName, timeline, blobUrl, blobUrl, fileType.dataType);
}

function dataFile(filename, timeline, data, blobUrl, type) {
  return {
    filename: filename,
    timeline: timeline,
    data: data,
    blobUrl: blobUrl,
    type: type,
    selectedIndex: 0,
    destroy() {
      URL.revokeObjectURL(this.blobUrl);
    },
  }
}

function arrayEquals(a, b) {
  if (a.length !== b.length) {
    return false;
  }
  for (var i = 0; i < a.length; i++) {
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
  return [fileType, fileType.decoder(buffer, fileType, fileName, store)];
}

function detectAndDecode(buffer, fileName, store) {
  if (arrayStartsWith(buffer, LAYER_TRACE_MAGIC_NUMBER)) {
    return decodedFile(FILE_TYPES['layers_trace'], buffer, fileName, store);
  }
  if (arrayStartsWith(buffer, WINDOW_TRACE_MAGIC_NUMBER)) {
    return decodedFile(FILE_TYPES['window_trace'], buffer, fileName, store);
  }
  if (arrayStartsWith(buffer, MPEG4_MAGIC_NMBER)) {
    return decodedFile(FILE_TYPES['screen_recording'], buffer, fileName, store);
  }
  if (arrayStartsWith(buffer, WAYLAND_TRACE_MAGIC_NUMBER)) {
    return decodedFile(FILE_TYPES['wl_trace'], buffer, fileName, store);
  }
  if (arrayStartsWith(buffer, PROTO_LOG_MAGIC_NUMBER)) {
    return decodedFile(FILE_TYPES['proto_log'], buffer, fileName, store);
  }
  if (arrayStartsWith(buffer, SYSTEM_UI_MAGIC_NUMBER)) {
    return decodedFile(FILE_TYPES['system_ui_trace'], buffer, fileName, store);
  }
  if (arrayStartsWith(buffer, LAUNCHER_MAGIC_NUMBER)) {
    return decodedFile(FILE_TYPES['launcher_trace'], buffer, fileName, store);
  }
  for (var name of ['transaction', 'layers_dump', 'window_dump', 'wl_dump']) {
    try {
      return decodedFile(FILE_TYPES[name], buffer, fileName, store);
    } catch (ex) {
      // ignore exception and try next filetype
    }
  }
  throw new Error('Unable to detect file');
}

export { detectAndDecode, DATA_TYPES, FILE_TYPES };
