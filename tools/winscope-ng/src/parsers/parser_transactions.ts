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
import {TransactionsTraceFileProto} from "./proto_types";

class ParserTransactions extends Parser {
  constructor(trace: Blob) {
    super(trace);
  }

  override getTraceTypeId(): TraceTypeId {
    return TraceTypeId.TRANSACTIONS;
  }

  override getMagicNumber(): number[] {
    return ParserTransactions.MAGIC_NUMBER;
  }

  override decodeTrace(buffer: Uint8Array): any[] {
    return (<any>TransactionsTraceFileProto.decode(buffer)).entry;
  }

  override getTimestamp(entryProto: any): number {
    return Number(entryProto.elapsedRealtimeNanos);
  }

  override processDecodedEntry(entryProto: any): any {
    return entryProto;
  }

  private static readonly MAGIC_NUMBER = [0x09, 0x54, 0x4e, 0x58, 0x54, 0x52, 0x41, 0x43, 0x45]; // .TNXTRACE
}

export {ParserTransactions};
