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
import {TimestampType} from 'common/time';
import root from 'protos/transactions/latest/json';
import {perfetto} from 'protos/transactions/latest/static';
import {
  CustomQueryParserResultTypeMap,
  CustomQueryType,
  VisitableParserCustomQuery,
} from 'trace/custom_query';
import {EntriesRange} from 'trace/index_types';
import {TraceFile} from 'trace/trace_file';
import {TraceType} from 'trace/trace_type';
import {WasmEngineProxy} from 'trace_processor/wasm_engine_proxy';
import {AbstractParser} from './abstract_parser';
import {FakeProtoTransformer} from './fake_proto_transformer';
import {Utils} from './utils';

export class ParserTransactions extends AbstractParser<object> {
  private static readonly LayerState = root.lookupType('perfetto.protos.LayerState');
  private static readonly DisplayState = root.lookupType('perfetto.protos.DisplayState');
  private protoTransformer = new FakeProtoTransformer(
    root.lookupType('perfetto.protos.TransactionTraceEntry')
  );

  constructor(traceFile: TraceFile, traceProcessor: WasmEngineProxy) {
    super(traceFile, traceProcessor);
  }

  override getTraceType(): TraceType {
    return TraceType.TRANSACTIONS;
  }

  override async getEntry(
    index: number,
    timestampType: TimestampType
  ): Promise<perfetto.protos.ITransactionTraceEntry> {
    let entryProto = await Utils.queryEntry(this.traceProcessor, this.getTableName(), index);
    entryProto = this.protoTransformer.transform(entryProto);
    entryProto = this.decodeWhatBitsetFields(entryProto);
    return entryProto;
  }

  protected override getTableName(): string {
    return 'surfaceflinger_transactions';
  }

  override async customQuery<Q extends CustomQueryType>(
    type: Q,
    entriesRange: EntriesRange
  ): Promise<CustomQueryParserResultTypeMap[Q]> {
    return new VisitableParserCustomQuery(type)
      .visit(CustomQueryType.VSYNCID, async () => {
        return Utils.queryVsyncId(this.traceProcessor, this.getTableName(), entriesRange);
      })
      .getResult();
  }

  private decodeWhatBitsetFields(
    transactionTraceEntry: perfetto.protos.ITransactionTraceEntry
  ): perfetto.protos.ITransactionTraceEntry {
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

    const LayerStateChangesLsbEnum = (ParserTransactions.LayerState as any).ChangesLsb;
    const LayerStateChangesMsbEnum = (ParserTransactions.LayerState as any).ChangesMsb;
    const DisplayStateChangesEnum = (ParserTransactions.DisplayState as any).Changes;

    transactionTraceEntry.transactions?.forEach((transactionState) => {
      transactionState?.layerChanges?.forEach((layerState) => {
        const originalValue = BigInt(layerState.what?.toString() ?? 0n);
        (layerState.what as unknown as string) = concatBitsetTokens(
          decodeBitset32(Number(originalValue), LayerStateChangesLsbEnum).concat(
            decodeBitset32(Number(originalValue >> 32n), LayerStateChangesMsbEnum)
          )
        );
      });

      transactionState.displayChanges?.forEach((displayState) => {
        (displayState.what as unknown as string) = concatBitsetTokens(
          decodeBitset32(Number(displayState.what), DisplayStateChangesEnum)
        );
      });
    });

    transactionTraceEntry?.addedDisplays?.forEach((displayState) => {
      (displayState.what as unknown as string) = concatBitsetTokens(
        decodeBitset32(Number(displayState.what), DisplayStateChangesEnum)
      );
    });

    return transactionTraceEntry;
  }
}
