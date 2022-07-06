import * as protobuf from "protobufjs";

import accessibilityJson from "frameworks/base/core/proto/android/server/accessibilitytrace.proto";
import inputMethodClientsJson from "frameworks/base/core/proto/android/view/inputmethod/inputmethodeditortrace.proto";
import layersJson from "frameworks/native/services/surfaceflinger/layerproto/layerstrace.proto";
import protoLogJson from "frameworks/base/core/proto/android/internal/protolog.proto";
import transactionsJson from "frameworks/native/services/surfaceflinger/layerproto/transactions.proto";
import windowManagerJson from "frameworks/base/core/proto/android/server/windowmanagertrace.proto";

const AccessibilityTraceFileProto = protobuf.Root.fromJSON(accessibilityJson).lookupType("com.android.server.accessibility.AccessibilityTraceFileProto");
const InputMethodClientsTraceFileProto = protobuf.Root.fromJSON(inputMethodClientsJson).lookupType("android.view.inputmethod.InputMethodClientsTraceFileProto");
const InputMethodManagerServiceTraceFileProto = protobuf.Root.fromJSON(inputMethodClientsJson).lookupType("android.view.inputmethod.InputMethodManagerServiceTraceFileProto");
const InputMethodServiceTraceFileProto = protobuf.Root.fromJSON(inputMethodClientsJson).lookupType("android.view.inputmethod.InputMethodServiceTraceFileProto");
const LayersTraceFileProto = protobuf.Root.fromJSON(layersJson).lookupType("android.surfaceflinger.LayersTraceFileProto");
const ProtoLogFileProto = protobuf.Root.fromJSON(protoLogJson).lookupType("com.android.internal.protolog.ProtoLogFileProto");
const TransactionsTraceFileProto = protobuf.Root.fromJSON(transactionsJson).lookupType("android.surfaceflinger.proto.TransactionTraceFile");
const WindowManagerServiceDumpProto = protobuf.Root.fromJSON(windowManagerJson).lookupType("com.android.server.wm.WindowManagerServiceDumpProto");
const WindowManagerTraceFileProto = protobuf.Root.fromJSON(windowManagerJson).lookupType("com.android.server.wm.WindowManagerTraceFileProto");

export {
  AccessibilityTraceFileProto,
  InputMethodClientsTraceFileProto,
  InputMethodManagerServiceTraceFileProto,
  InputMethodServiceTraceFileProto,
  LayersTraceFileProto,
  ProtoLogFileProto,
  TransactionsTraceFileProto,
  WindowManagerServiceDumpProto,
  WindowManagerTraceFileProto
};
