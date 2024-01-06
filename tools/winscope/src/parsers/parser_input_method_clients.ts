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

import {assertDefined} from 'common/assert_utils';
import {Timestamp, TimestampType} from 'common/time';
import {TimeUtils} from 'common/time_utils';
import root from 'protos/ime/latest/root';
import {android} from 'protos/ime/latest/types';
import {TraceFile} from 'trace/trace_file';
import {TraceTreeNode} from 'trace/trace_tree_node';
import {TraceType} from 'trace/trace_type';
import {ImeUtils} from 'viewers/common/ime_utils';
import {AbstractParser} from './abstract_parser';

class ParserInputMethodClients extends AbstractParser {
  private static readonly InputMethodClientsTraceFileProto = root.lookupType(
    'android.view.inputmethod.InputMethodClientsTraceFileProto'
  );

  constructor(trace: TraceFile) {
    super(trace);
    this.realToElapsedTimeOffsetNs = undefined;
  }

  getTraceType(): TraceType {
    return TraceType.INPUT_METHOD_CLIENTS;
  }

  override getMagicNumber(): number[] {
    return ParserInputMethodClients.MAGIC_NUMBER;
  }

  override decodeTrace(
    buffer: Uint8Array
  ): android.view.inputmethod.IInputMethodClientsTraceProto[] {
    const decoded = ParserInputMethodClients.InputMethodClientsTraceFileProto.decode(
      buffer
    ) as android.view.inputmethod.IInputMethodClientsTraceFileProto;
    const timeOffset = BigInt(decoded.realToElapsedTimeOffsetNanos?.toString() ?? '0');
    this.realToElapsedTimeOffsetNs = timeOffset !== 0n ? timeOffset : undefined;
    return decoded.entry ?? [];
  }

  override getTimestamp(
    type: TimestampType,
    entry: android.view.inputmethod.IInputMethodClientsTraceProto
  ): undefined | Timestamp {
    const elapsedRealtimeNanos = BigInt(assertDefined(entry.elapsedRealtimeNanos).toString());
    if (type === TimestampType.ELAPSED) {
      return new Timestamp(type, elapsedRealtimeNanos);
    } else if (type === TimestampType.REAL && this.realToElapsedTimeOffsetNs !== undefined) {
      return new Timestamp(type, elapsedRealtimeNanos + this.realToElapsedTimeOffsetNs);
    }
    return undefined;
  }

  override processDecodedEntry(
    index: number,
    timestampType: TimestampType,
    entry: android.view.inputmethod.IInputMethodClientsTraceProto
  ): TraceTreeNode {
    if (entry.elapsedRealtimeNanos === undefined || entry.elapsedRealtimeNanos === null) {
      throw Error('Missing elapsedRealtimeNanos on entry');
    }

    const elapsedRealtimeNanos = BigInt(entry.elapsedRealtimeNanos.toString());

    let clockTimeNanos: bigint | undefined = undefined;
    if (this.realToElapsedTimeOffsetNs !== undefined) {
      clockTimeNanos = elapsedRealtimeNanos + this.realToElapsedTimeOffsetNs;
    }

    const timestamp = Timestamp.from(
      timestampType,
      elapsedRealtimeNanos,
      this.realToElapsedTimeOffsetNs
    );

    return {
      name: TimeUtils.format(timestamp) + ' - ' + entry.where,
      kind: 'InputMethodClient entry',
      children: [
        {
          obj: ImeUtils.transformInputConnectionCall(entry.client),
          kind: 'Client',
          name: entry.client?.viewRootImpl?.view ?? '',
          children: [],
          stableId: 'client',
          id: 'client',
        },
      ],
      obj: entry,
      stableId: 'entry',
      id: 'entry',
      elapsedRealtimeNanos,
      clockTimeNanos,
    };
  }

  private realToElapsedTimeOffsetNs: undefined | bigint;
  private static readonly MAGIC_NUMBER = [0x09, 0x49, 0x4d, 0x43, 0x54, 0x52, 0x41, 0x43, 0x45]; // .IMCTRACE
}

export {ParserInputMethodClients};
