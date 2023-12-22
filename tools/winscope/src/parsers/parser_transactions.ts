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
import root from 'protos/transactions/udc/root';
import {android} from 'protos/transactions/udc/types';
import {
  CustomQueryParserResultTypeMap,
  CustomQueryType,
  VisitableParserCustomQuery,
} from 'trace/custom_query';
import {EntriesRange} from 'trace/trace';
import {TraceFile} from 'trace/trace_file';
import {TraceType} from 'trace/trace_type';
import {AbstractParser} from './abstract_parser';

class ParserTransactions extends AbstractParser {
  private static readonly TransactionsTraceFileProto = root.lookupType(
    'android.surfaceflinger.TransactionTraceFile'
  );

  constructor(trace: TraceFile) {
    super(trace);
    this.realToElapsedTimeOffsetNs = undefined;
  }

  override getTraceType(): TraceType {
    return TraceType.TRANSACTIONS;
  }

  override getMagicNumber(): number[] {
    return ParserTransactions.MAGIC_NUMBER;
  }

  override decodeTrace(buffer: Uint8Array): android.surfaceflinger.proto.ITransactionTraceEntry[] {
    const decodedProto = ParserTransactions.TransactionsTraceFileProto.decode(
      buffer
    ) as android.surfaceflinger.proto.ITransactionTraceFile;

    this.decodeWhatFields(decodedProto);

    const timeOffset = BigInt(decodedProto.realToElapsedTimeOffsetNanos?.toString() ?? '0');
    this.realToElapsedTimeOffsetNs = timeOffset !== 0n ? timeOffset : undefined;

    return decodedProto.entry ?? [];
  }

  private decodeWhatFields(decodedProto: android.surfaceflinger.proto.ITransactionTraceFile) {
    const decodeBitset32 = (bitset: number, EnumProto: any) => {
      return Object.keys(EnumProto).filter((key) => {
        const value = EnumProto[key];
        return (bitset & value) !== 0;
      });
    };

    const concatBitsetTokens = (tokens: string[]) => {
      if (tokens.length === 0) {
        return '0';
      }
      return tokens.join(' | ');
    };

    const LayerStateChangesLsbEnum = (ParserTransactions.TransactionsTraceFileProto?.parent as any)
      .LayerState.ChangesLsb;
    const LayerStateChangesMsbEnum = (ParserTransactions.TransactionsTraceFileProto?.parent as any)
      .LayerState.ChangesMsb;
    const DisplayStateChangesEnum = (ParserTransactions.TransactionsTraceFileProto?.parent as any)
      .DisplayState.Changes;

    decodedProto.entry?.forEach((transactionTraceEntry) => {
      transactionTraceEntry.transactions?.forEach((transactionState) => {
        transactionState.layerChanges?.forEach((layerState) => {
          //TODO(priyankaspatel): modify properties tree instead of tampering the proto
          (layerState.what as unknown as string) = concatBitsetTokens(
            decodeBitset32(assertDefined(layerState.what).low, LayerStateChangesLsbEnum).concat(
              decodeBitset32(assertDefined(layerState.what).high, LayerStateChangesMsbEnum)
            )
          );
        });

        transactionState.displayChanges?.forEach((displayState) => {
          //TODO(priyankaspatel): modify properties tree instead of tampering the proto
          (displayState.what as unknown as string) = concatBitsetTokens(
            decodeBitset32(assertDefined(displayState.what), DisplayStateChangesEnum)
          );
        });
      });

      transactionTraceEntry.addedDisplays?.forEach((displayState) => {
        //TODO(priyankaspatel): modify properties tree instead of tampering the proto
        (displayState.what as unknown as string) = concatBitsetTokens(
          decodeBitset32(assertDefined(displayState.what), DisplayStateChangesEnum)
        );
      });
    });
  }

  override getTimestamp(
    type: TimestampType,
    entryProto: android.surfaceflinger.proto.ITransactionTraceEntry
  ): undefined | Timestamp {
    if (type === TimestampType.ELAPSED) {
      return new Timestamp(type, BigInt(assertDefined(entryProto.elapsedRealtimeNanos).toString()));
    } else if (type === TimestampType.REAL && this.realToElapsedTimeOffsetNs !== undefined) {
      return new Timestamp(
        type,
        this.realToElapsedTimeOffsetNs +
          BigInt(assertDefined(entryProto.elapsedRealtimeNanos).toString())
      );
    }
    return undefined;
  }

  override processDecodedEntry(
    index: number,
    timestampType: TimestampType,
    entryProto: android.surfaceflinger.proto.ITransactionTraceEntry
  ): android.surfaceflinger.proto.ITransactionTraceEntry {
    return entryProto;
  }

  override customQuery<Q extends CustomQueryType>(
    type: Q,
    entriesRange: EntriesRange
  ): Promise<CustomQueryParserResultTypeMap[Q]> {
    return new VisitableParserCustomQuery(type)
      .visit(CustomQueryType.VSYNCID, async () => {
        return this.decodedEntries.slice(entriesRange.start, entriesRange.end).map((entry) => {
          return BigInt(entry.vsyncId.toString()); // convert Long to bigint
        });
      })
      .getResult();
  }

  private realToElapsedTimeOffsetNs: undefined | bigint;
  private static readonly MAGIC_NUMBER = [0x09, 0x54, 0x4e, 0x58, 0x54, 0x52, 0x41, 0x43, 0x45]; // .TNXTRACE
}

export {ParserTransactions};
