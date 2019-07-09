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


import jsonProtoDefs from 'frameworks/base/core/proto/android/server/windowmanagertrace.proto'
import jsonProtoDefsSF from 'frameworks/native/services/surfaceflinger/layerproto/layerstrace.proto'
import protobuf from 'protobufjs'
import { transform_layers, transform_layers_trace } from './transform_sf.js'
import { transform_window_service, transform_window_trace } from './transform_wm.js'

var protoDefs = protobuf.Root.fromJSON(jsonProtoDefs)
  .addJSON(jsonProtoDefsSF.nested);

var WindowTraceMessage = protoDefs.lookupType(
  "com.android.server.wm.WindowManagerTraceFileProto");
var WindowMessage = protoDefs.lookupType(
  "com.android.server.wm.WindowManagerServiceDumpProto");
var LayersMessage = protoDefs.lookupType("android.surfaceflinger.LayersProto");
var LayersTraceMessage = protoDefs.lookupType("android.surfaceflinger.LayersTraceFileProto");


const LAYER_TRACE_MAGIC_NUMBER = [0x09, 0x4c, 0x59, 0x52, 0x54, 0x52, 0x41, 0x43, 0x45] // .LYRTRACE
const WINDOW_TRACE_MAGIC_NUMBER = [0x09, 0x57, 0x49, 0x4e, 0x54, 0x52, 0x41, 0x43, 0x45] // .WINTRACE

const DATA_TYPES = {
  WINDOW_MANAGER: {
    name: "WindowManager",
    icon: "view_compact",
  },
  SURFACE_FLINGER: {
    name: "SurfaceFlinger",
    icon: "filter_none",
  },
}

const FILE_TYPES = {
  'window_trace': {
    protoType: WindowTraceMessage,
    transform: transform_window_trace,
    name: "WindowManager trace",
    timeline: true,
    dataType: DATA_TYPES.WINDOW_MANAGER,
  },
  'layers_trace': {
    protoType: LayersTraceMessage,
    transform: transform_layers_trace,
    name: "SurfaceFlinger trace",
    timeline: true,
    dataType: DATA_TYPES.SURFACE_FLINGER,
  },
  'layers_dump': {
    protoType: LayersMessage,
    transform: transform_layers,
    name: "SurfaceFlinger dump",
    timeline: false,
    dataType: DATA_TYPES.SURFACE_FLINGER,
  },
  'window_dump': {
    protoType: WindowMessage,
    transform: transform_window_service,
    name: "WindowManager dump",
    timeline: false,
    dataType: DATA_TYPES.WINDOW_MANAGER,
  },
};

function dataFile(filename, timeline, type) {
  return {
    filename: filename,
    timeline: timeline,
    type: type,
    selectedIndex: 0,
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

function decodedFile(filename, buffer) {
  var decoded = FILE_TYPES[filename].protoType.decode(buffer);
  return [FILE_TYPES[filename], decoded];
}

function detectFile(buffer) {
  if (arrayStartsWith(buffer, LAYER_TRACE_MAGIC_NUMBER)) {
    return decodedFile('layers_trace', buffer);
  }
  if (arrayStartsWith(buffer, WINDOW_TRACE_MAGIC_NUMBER)) {
    return decodedFile('window_trace', buffer);
  }
  for (var filename of ['layers_dump', 'window_dump']) {
    try {
      var [filetype, decoded] = decodedFile(filename, buffer);
      var transformed = filetype.transform(decoded);
      return [FILE_TYPES[filename], decoded];
    } catch (ex) {
      // ignore exception and try next filetype
    }
  }
  throw new Error('Unable to detect file');
}

export { detectFile, dataFile, DATA_TYPES, FILE_TYPES };
