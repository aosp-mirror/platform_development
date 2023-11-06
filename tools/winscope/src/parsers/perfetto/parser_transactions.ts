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
import {TransactionsTraceFileProto, winscopeJson} from 'parsers/proto_types';
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
import {FakeProto} from './fake_proto_builder';
import {FakeProtoTransformer} from './fake_proto_transformer';
import {Utils} from './utils';

export class ParserTransactions extends AbstractParser<object> {
  private protoTransformer = new FakeProtoTransformer(
    winscopeJson,
    'WinscopeTraceData',
    'transactions'
  );

  constructor(traceFile: TraceFile, traceProcessor: WasmEngineProxy) {
    super(traceFile, traceProcessor);
  }

  override getTraceType(): TraceType {
    return TraceType.TRANSACTIONS;
  }

  override async getEntry(index: number, timestampType: TimestampType): Promise<object> {
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

  private decodeWhatBitsetFields(transactionTraceEntry: FakeProto): FakeProto {
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

    transactionTraceEntry.transactions.forEach((transactionState: any) => {
      transactionState.layerChanges.forEach((layerState: any) => {
        layerState.what = concatBitsetTokens(
          decodeBitset32(Number(layerState.what), LayerStateChangesLsbEnum).concat(
            decodeBitset32(Number(layerState.what >> 32n), LayerStateChangesMsbEnum)
          )
        );
      });

      transactionState.displayChanges.forEach((displayState: any) => {
        displayState.what = concatBitsetTokens(
          decodeBitset32(Number(displayState.what), DisplayStateChangesEnum)
        );
      });
    });

    transactionTraceEntry?.addedDisplays?.forEach((displayState: any) => {
      displayState.what = concatBitsetTokens(
        decodeBitset32(Number(displayState.what), DisplayStateChangesEnum)
      );
    });

    return transactionTraceEntry;
  }
}
