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
import {Timestamp} from 'common/time/time';
import {AbstractParser} from 'parsers/legacy/abstract_parser';
import {perfetto} from 'protos/perfetto/trace/static';
import root from 'protos/transactions/udc/json';
import {android} from 'protos/transactions/udc/static';
import {TraceType} from 'trace/trace_type';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';

type TraceEntryProto = android.surfaceflinger.proto.ITransactionTraceEntry;

export class ParserTransactions extends AbstractParser<
  PropertyTreeNode,
  TraceEntryProto
> {
  private static readonly MAGIC_NUMBER = [
    0x09, 0x54, 0x4e, 0x58, 0x54, 0x52, 0x41, 0x43, 0x45,
  ]; // .TNXTRACE

  private static readonly TransactionsTraceFileProto = root.lookupType(
    'android.surfaceflinger.TransactionTraceFile',
  );

  private realToMonotonicTimeOffsetNs: bigint | undefined;

  override getTraceType(): TraceType {
    return TraceType.TRANSACTIONS;
  }

  override getMagicNumber(): number[] {
    return ParserTransactions.MAGIC_NUMBER;
  }

  override getRealToBootTimeOffsetNs(): bigint | undefined {
    return undefined;
  }

  override getRealToMonotonicTimeOffsetNs(): bigint | undefined {
    return this.realToMonotonicTimeOffsetNs;
  }

  override decodeTrace(buffer: Uint8Array): TraceEntryProto[] {
    const decodedProto = ParserTransactions.TransactionsTraceFileProto.decode(
      buffer,
    ) as android.surfaceflinger.proto.ITransactionTraceFile;

    const timeOffset = BigInt(
      decodedProto.realToElapsedTimeOffsetNanos?.toString() ?? '0',
    );
    this.realToMonotonicTimeOffsetNs =
      timeOffset !== 0n ? timeOffset : undefined;

    return decodedProto.entry ?? [];
  }

  override canConvertToPerfetto(): boolean {
    return true;
  }

  override convertToPerfettoPackets(
    sequenceId: number,
  ): perfetto.protos.TracePacket[] {
    const packets = [];
    for (const entry of this.decodedEntries) {
      const packet = perfetto.protos.TracePacket.create();
      packet.timestamp = assertDefined(entry.elapsedRealtimeNanos);
      packet.timestampClockId =
        perfetto.protos.ClockSnapshot.Clock.BuiltinClocks.MONOTONIC;
      packet.trustedPacketSequenceId = sequenceId;
      packet.surfaceflingerTransactions =
        perfetto.protos.TransactionTraceEntry.fromObject(entry);
      packets.push(packet);
    }
    return packets;
  }

  protected override getTimestamp(entryProto: TraceEntryProto): Timestamp {
    return this.timestampConverter.makeTimestampFromMonotonicNs(
      BigInt(assertDefined(entryProto.elapsedRealtimeNanos).toString()),
    );
  }
}
