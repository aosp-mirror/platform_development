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

import {assertDefined} from 'common/assert_utils';
import {ElapsedTimestamp, RealTimestamp, Timestamp, TimestampType} from 'common/time';
import {CrossPlatform, ShellTransitionData, Transition, WmTransitionData} from 'flickerlib/common';
import root from 'protos/transitions/udc/root';
import {com} from 'protos/transitions/udc/types';
import {TraceFile} from 'trace/trace_file';
import {TraceType} from 'trace/trace_type';
import {AbstractParser} from './abstract_parser';

export class ParserTransitionsShell extends AbstractParser {
  private static readonly WmShellTransitionsTraceProto = (root as any).lookupType(
    'com.android.wm.shell.WmShellTransitionTraceProto'
  );
  private realToElapsedTimeOffsetNs: undefined | bigint;
  private handlerMapping: undefined | Map<number, string>;

  constructor(trace: TraceFile) {
    super(trace);
  }

  override getTraceType(): TraceType {
    return TraceType.SHELL_TRANSITION;
  }

  override decodeTrace(traceBuffer: Uint8Array): com.android.wm.shell.ITransition[] {
    const decodedProto = ParserTransitionsShell.WmShellTransitionsTraceProto.decode(
      traceBuffer
    ) as com.android.wm.shell.IWmShellTransitionTraceProto;

    const timeOffset = BigInt(decodedProto.realToElapsedTimeOffsetNanos?.toString() ?? '0');
    this.realToElapsedTimeOffsetNs = timeOffset !== 0n ? timeOffset : undefined;

    this.handlerMapping = new Map<number, string>();
    for (const mapping of decodedProto.handlerMappings ?? []) {
      this.handlerMapping.set(mapping.id, mapping.name);
    }

    return decodedProto.transitions ?? [];
  }

  override processDecodedEntry(
    index: number,
    timestampType: TimestampType,
    entryProto: com.android.wm.shell.ITransition
  ): Transition {
    return this.parseShellTransitionEntry(entryProto);
  }

  private parseShellTransitionEntry(entry: com.android.wm.shell.ITransition): Transition {
    this.validateShellTransitionEntry(entry);

    if (this.realToElapsedTimeOffsetNs === undefined) {
      throw new Error('missing realToElapsedTimeOffsetNs');
    }

    let dispatchTime = null;
    if (entry.dispatchTimeNs && BigInt(entry.dispatchTimeNs.toString()) !== 0n) {
      const unixNs = BigInt(entry.dispatchTimeNs.toString()) + this.realToElapsedTimeOffsetNs;
      dispatchTime = CrossPlatform.timestamp.fromString(
        entry.dispatchTimeNs.toString(),
        null,
        unixNs.toString()
      );
    }

    let mergeRequestTime = null;
    if (entry.mergeRequestTimeNs && BigInt(entry.mergeRequestTimeNs.toString()) !== 0n) {
      const unixNs = BigInt(entry.mergeRequestTimeNs.toString()) + this.realToElapsedTimeOffsetNs;
      mergeRequestTime = CrossPlatform.timestamp.fromString(
        entry.mergeRequestTimeNs.toString(),
        null,
        unixNs.toString()
      );
    }

    let mergeTime = null;
    if (entry.mergeTimeNs && BigInt(entry.mergeTimeNs.toString()) !== 0n) {
      const unixNs = BigInt(entry.mergeTimeNs.toString()) + this.realToElapsedTimeOffsetNs;
      mergeTime = CrossPlatform.timestamp.fromString(
        entry.mergeTimeNs.toString(),
        null,
        unixNs.toString()
      );
    }

    let abortTime = null;
    if (entry.abortTimeNs && BigInt(entry.abortTimeNs.toString()) !== 0n) {
      const unixNs = BigInt(entry.abortTimeNs.toString()) + this.realToElapsedTimeOffsetNs;
      abortTime = CrossPlatform.timestamp.fromString(
        entry.abortTimeNs.toString(),
        null,
        unixNs.toString()
      );
    }

    const mergeTarget = entry.mergeTarget ? entry.mergeTarget : null;

    if (this.handlerMapping === undefined) {
      throw new Error('Missing handler mapping!');
    }

    return new Transition(
      entry.id,
      new WmTransitionData(),
      new ShellTransitionData(
        dispatchTime,
        mergeRequestTime,
        mergeTime,
        abortTime,
        this.handlerMapping.get(assertDefined(entry.handler)),
        mergeTarget
      )
    );
  }

  private validateShellTransitionEntry(entry: com.android.wm.shell.ITransition) {
    if (entry.id === 0) {
      throw new Error('Entry need a non null id');
    }
    if (
      !entry.dispatchTimeNs &&
      !entry.mergeRequestTimeNs &&
      !entry.mergeTimeNs &&
      !entry.abortTimeNs
    ) {
      throw new Error('Requires at least one non-null timestamp');
    }
  }

  protected getMagicNumber(): number[] | undefined {
    return [0x09, 0x57, 0x4d, 0x53, 0x54, 0x52, 0x41, 0x43, 0x45]; // .WMSTRACE
  }

  override getTimestamp(type: TimestampType, decodedEntry: Transition): undefined | Timestamp {
    decodedEntry = this.parseShellTransitionEntry(decodedEntry);

    if (type === TimestampType.ELAPSED) {
      return new ElapsedTimestamp(BigInt(decodedEntry.timestamp.elapsedNanos.toString()));
    }

    if (type === TimestampType.REAL) {
      return new RealTimestamp(BigInt(decodedEntry.timestamp.unixNanos.toString()));
    }

    throw new Error('Timestamp type unsupported');
  }
}
