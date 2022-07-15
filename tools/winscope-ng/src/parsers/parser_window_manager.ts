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
import {TraceTypeId} from "common/trace/type_id";
import {Parser} from "./parser";
import {WindowManagerTraceFileProto} from "./proto_types";
import {WindowManagerState} from "common/trace/flickerlib/windows/WindowManagerState";

class ParserWindowManager extends Parser {
  constructor(trace: Blob) {
    super(trace);
  }

  override getTraceTypeId(): TraceTypeId {
    return TraceTypeId.WINDOW_MANAGER;
  }

  override getMagicNumber(): number[] {
    return ParserWindowManager.MAGIC_NUMBER;
  }

  override decodeTrace(buffer: Uint8Array): any[] {
    return (<any>WindowManagerTraceFileProto.decode(buffer)).entry;
  }

  override getTimestamp(entryProto: any): number {
    return Number(entryProto.elapsedRealtimeNanos);
  }

  override processDecodedEntry(entryProto: any): WindowManagerState {
    return WindowManagerState.fromProto(entryProto.windowManagerService, entryProto.elapsedRealtimeNanos, entryProto.where);
  }

  private static readonly MAGIC_NUMBER = [0x09, 0x57, 0x49, 0x4e, 0x54, 0x52, 0x41, 0x43, 0x45]; // .WINTRACE
}

export {ParserWindowManager};
