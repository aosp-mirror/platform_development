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

import {Timestamp, TimestampType} from 'trace/timestamp';
import {TraceFile} from 'trace/trace_file';
import {TraceType} from 'trace/trace_type';
import {AbstractParser} from './abstract_parser';
import {AccessibilityTraceFileProto} from './proto_types';

class ParserAccessibility extends AbstractParser {
  constructor(trace: TraceFile) {
    super(trace);
    this.realToElapsedTimeOffsetNs = undefined;
  }

  override getTraceType(): TraceType {
    return TraceType.ACCESSIBILITY;
  }

  override getMagicNumber(): number[] {
    return ParserAccessibility.MAGIC_NUMBER;
  }

  override decodeTrace(buffer: Uint8Array): any[] {
    const decoded = AccessibilityTraceFileProto.decode(buffer) as any;
    if (Object.prototype.hasOwnProperty.call(decoded, 'realToElapsedTimeOffsetNanos')) {
      this.realToElapsedTimeOffsetNs = BigInt(decoded.realToElapsedTimeOffsetNanos);
    } else {
      this.realToElapsedTimeOffsetNs = undefined;
    }
    return decoded.entry;
  }

  override getTimestamp(type: TimestampType, entryProto: any): undefined | Timestamp {
    if (type === TimestampType.ELAPSED) {
      return new Timestamp(type, BigInt(entryProto.elapsedRealtimeNanos));
    } else if (type === TimestampType.REAL && this.realToElapsedTimeOffsetNs !== undefined) {
      return new Timestamp(
        type,
        this.realToElapsedTimeOffsetNs + BigInt(entryProto.elapsedRealtimeNanos)
      );
    }
    return undefined;
  }

  override processDecodedEntry(index: number, timestampType: TimestampType, entryProto: any): any {
    return entryProto;
  }

  private realToElapsedTimeOffsetNs: undefined | bigint;
  private static readonly MAGIC_NUMBER = [0x09, 0x41, 0x31, 0x31, 0x59, 0x54, 0x52, 0x41, 0x43]; // .A11YTRAC
}

export {ParserAccessibility};
