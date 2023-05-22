/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import Long from 'long';
import * as protobuf from 'protobufjs';

protobuf.util.Long = Long; // otherwise 64-bit types would be decoded as javascript number (only 53-bits precision)
protobuf.configure();

import protoLogJson from 'frameworks/base/core/proto/android/internal/protolog.proto';
import accessibilityJson from 'frameworks/base/core/proto/android/server/accessibilitytrace.proto';
import windowManagerJson from 'frameworks/base/core/proto/android/server/windowmanagertrace.proto';
import wmTransitionsJson from 'frameworks/base/core/proto/android/server/windowmanagertransitiontrace.proto';
import inputMethodClientsJson from 'frameworks/base/core/proto/android/view/inputmethod/inputmethodeditortrace.proto';
import shellTransitionsJson from 'frameworks/base/libs/WindowManager/Shell/proto/wm_shell_transition_trace.proto';
import layersJson from 'frameworks/native/services/surfaceflinger/layerproto/layerstrace.proto';
import transactionsJson from 'frameworks/native/services/surfaceflinger/layerproto/transactions.proto';

const AccessibilityTraceFileProto = protobuf.Root.fromJSON(accessibilityJson).lookupType(
  'com.android.server.accessibility.AccessibilityTraceFileProto'
);
const InputMethodClientsTraceFileProto = protobuf.Root.fromJSON(inputMethodClientsJson).lookupType(
  'android.view.inputmethod.InputMethodClientsTraceFileProto'
);
const InputMethodManagerServiceTraceFileProto = protobuf.Root.fromJSON(
  inputMethodClientsJson
).lookupType('android.view.inputmethod.InputMethodManagerServiceTraceFileProto');
const InputMethodServiceTraceFileProto = protobuf.Root.fromJSON(inputMethodClientsJson).lookupType(
  'android.view.inputmethod.InputMethodServiceTraceFileProto'
);
const LayersTraceFileProto = protobuf.Root.fromJSON(layersJson).lookupType(
  'android.surfaceflinger.LayersTraceFileProto'
);
const ProtoLogFileProto = protobuf.Root.fromJSON(protoLogJson).lookupType(
  'com.android.internal.protolog.ProtoLogFileProto'
);
const TransactionsTraceFileProto = protobuf.Root.fromJSON(transactionsJson).lookupType(
  'android.surfaceflinger.proto.TransactionTraceFile'
);
const WindowManagerServiceDumpProto = protobuf.Root.fromJSON(windowManagerJson).lookupType(
  'com.android.server.wm.WindowManagerServiceDumpProto'
);
const WindowManagerTraceFileProto = protobuf.Root.fromJSON(windowManagerJson).lookupType(
  'com.android.server.wm.WindowManagerTraceFileProto'
);
const WmTransitionsTraceFileProto = protobuf.Root.fromJSON(wmTransitionsJson).lookupType(
  'com.android.server.wm.shell.TransitionTraceProto'
);
const ShellTransitionsTraceFileProto = protobuf.Root.fromJSON(shellTransitionsJson).lookupType(
  'com.android.wm.shell.WmShellTransitionTraceProto'
);

export {
  AccessibilityTraceFileProto,
  InputMethodClientsTraceFileProto,
  InputMethodManagerServiceTraceFileProto,
  InputMethodServiceTraceFileProto,
  LayersTraceFileProto,
  ProtoLogFileProto,
  TransactionsTraceFileProto,
  WindowManagerServiceDumpProto,
  WindowManagerTraceFileProto,
  WmTransitionsTraceFileProto,
  ShellTransitionsTraceFileProto,
};
