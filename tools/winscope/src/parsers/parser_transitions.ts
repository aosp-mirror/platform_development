/*
 * Copyright (C) 2023 The Android Open Source Project
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

import {
  CrossPlatform,
  Transition,
  TransitionChange,
  TransitionType,
  WindowingMode,
} from 'trace/flickerlib/common';
import {ElapsedTimestamp, RealTimestamp, Timestamp, TimestampType} from 'trace/timestamp';
import {TraceFile} from 'trace/trace_file';
import {TraceType} from 'trace/trace_type';
import {AbstractParser} from './abstract_parser';
import {TransitionsTraceFileProto} from './proto_types';

export class ParserTransitions extends AbstractParser {
  constructor(trace: TraceFile) {
    super(trace);
    this.realToElapsedTimeOffsetNs = undefined;
  }

  override getTraceType(): TraceType {
    return TraceType.TRANSITION;
  }

  override getMagicNumber(): number[] {
    return ParserTransitions.MAGIC_NUMBER;
  }

  override decodeTrace(buffer: Uint8Array): any[] {
    const decodedProto = TransitionsTraceFileProto.decode(buffer) as any;
    this.realToElapsedTimeOffsetNs = BigInt(decodedProto.realToElapsedTimeOffsetNanos);

    return decodedProto.transitions;
  }

  override getTimestamp(type: TimestampType, entryProto: any): undefined | Timestamp {
    if (type === TimestampType.ELAPSED) {
      return new ElapsedTimestamp(BigInt(entryProto.createTimeNs));
    }
    if (type === TimestampType.REAL && this.realToElapsedTimeOffsetNs !== undefined) {
      return new RealTimestamp(this.realToElapsedTimeOffsetNs + BigInt(entryProto.createTimeNs));
    }
    return undefined;
  }

  override processDecodedEntry(
    index: number,
    timestampType: TimestampType,
    entryProto: any
  ): Transition {
    if (!this.transitions) {
      const transitions = this.decodedEntries.map((it) => this.parseProto(it));
      this.transitions = transitions;
    }
    return this.transitions[index];
  }

  private parseProto(entryProto: any): Transition {
    const changes = entryProto.targets.map((it: any) => {
      const windowingMode = WindowingMode.WINDOWING_MODE_UNDEFINED; // TODO: Get the windowing mode

      return new TransitionChange(
        TransitionType.Companion.fromInt(it.mode),
        it.layerId,
        it.windowId,
        windowingMode
      );
    });

    const createTime = CrossPlatform.timestamp.fromString(
      entryProto.createTimeNs.toString(),
      null,
      null
    );
    const sendTime = CrossPlatform.timestamp.fromString(
      entryProto.sendTimeNs.toString(),
      null,
      null
    );
    const finishTime = CrossPlatform.timestamp.fromString(
      entryProto.finishTimeNs.toString(),
      null,
      null
    );
    const startTransactionId = entryProto.startTransactionId;
    const finishTransactionId = entryProto.finishTransactionId;
    const type = TransitionType.Companion.fromInt(entryProto.type);
    const played = entryProto.finishTimeNs > 0;
    const aborted = entryProto.sendTimeNs === 0;

    return new Transition(
      createTime,
      sendTime,
      finishTime,
      startTransactionId,
      finishTransactionId,
      type,
      changes,
      played,
      aborted
    );
  }

  private transitions: Transition[] | undefined;
  private realToElapsedTimeOffsetNs: undefined | bigint;
  private static readonly MAGIC_NUMBER = [0x09, 0x54, 0x52, 0x4e, 0x54, 0x52, 0x41, 0x43, 0x45]; // .TRNTRACE
}
