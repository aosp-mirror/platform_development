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
import {TransactionsTraceFileProto} from './proto_types';

class ParserTransactions extends AbstractParser {
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

  override decodeTrace(buffer: Uint8Array): any[] {
    const decodedProto = TransactionsTraceFileProto.decode(buffer) as any;
    this.decodeWhatFields(decodedProto);

    if (Object.prototype.hasOwnProperty.call(decodedProto, 'realToElapsedTimeOffsetNanos')) {
      this.realToElapsedTimeOffsetNs = BigInt(decodedProto.realToElapsedTimeOffsetNanos);
    } else {
      this.realToElapsedTimeOffsetNs = undefined;
    }
    return decodedProto.entry;
  }

  private decodeWhatFields(decodedProto: any) {
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

    const LayerStateChangesLsbEnum = (TransactionsTraceFileProto?.parent as any).LayerState
      .ChangesLsb;
    const LayerStateChangesMsbEnum = (TransactionsTraceFileProto?.parent as any).LayerState
      .ChangesMsb;
    const DisplayStateChangesEnum = (TransactionsTraceFileProto?.parent as any).DisplayState
      .Changes;

    decodedProto.entry.forEach((transactionTraceEntry: any) => {
      transactionTraceEntry.transactions.forEach((transactionState: any) => {
        transactionState.layerChanges.forEach((layerState: any) => {
          layerState.what = concatBitsetTokens(
            decodeBitset32(layerState.what.low, LayerStateChangesLsbEnum).concat(
              decodeBitset32(layerState.what.high, LayerStateChangesMsbEnum)
            )
          );
        });

        transactionState.displayChanges.forEach((displayState: any) => {
          displayState.what = concatBitsetTokens(
            decodeBitset32(displayState.what, DisplayStateChangesEnum)
          );
        });
      });

      transactionTraceEntry.addedDisplays.forEach((displayState: any) => {
        displayState.what = concatBitsetTokens(
          decodeBitset32(displayState.what, DisplayStateChangesEnum)
        );
      });
    });
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

  override processDecodedEntry(
    index: number,
    timestampType: TimestampType,
    entryProto: object
  ): object {
    return entryProto;
  }

  private realToElapsedTimeOffsetNs: undefined | bigint;
  private static readonly MAGIC_NUMBER = [0x09, 0x54, 0x4e, 0x58, 0x54, 0x52, 0x41, 0x43, 0x45]; // .TNXTRACE
}

export {ParserTransactions};
