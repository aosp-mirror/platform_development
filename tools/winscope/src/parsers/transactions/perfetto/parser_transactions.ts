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
import {ParserTimestampConverter} from 'common/time/timestamp_converter';
import {AddDefaults} from 'parsers/operations/add_defaults';
import {SetFormatters} from 'parsers/operations/set_formatters';
import {AbstractParser} from 'parsers/perfetto/abstract_parser';
import {FakeProtoTransformer} from 'parsers/perfetto/fake_proto_transformer';
import {Utils} from 'parsers/perfetto/utils';
import {TamperedMessageType} from 'parsers/tampered_message_type';
import {TranslateChanges} from 'parsers/transactions/operations/translate_changes';
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
import {PropertyTreeBuilderFromProto} from 'trace/tree_node/property_tree_builder_from_proto';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {WasmEngineProxy} from 'trace_processor/wasm_engine_proxy';

export class ParserTransactions extends AbstractParser<PropertyTreeNode> {
  private static readonly TransactionsTraceFileProto =
    TamperedMessageType.tamper(
      root.lookupType('perfetto.protos.TransactionTraceFile'),
    );
  private static readonly TransactionsTraceEntryField =
    ParserTransactions.TransactionsTraceFileProto.fields['entry'];

  private static readonly OPERATIONS = [
    new AddDefaults(ParserTransactions.TransactionsTraceEntryField),
    new SetFormatters(ParserTransactions.TransactionsTraceEntryField),
    new TranslateChanges(),
  ];

  private protoTransformer: FakeProtoTransformer;

  constructor(
    traceFile: TraceFile,
    traceProcessor: WasmEngineProxy,
    timestampConverter: ParserTimestampConverter,
  ) {
    super(traceFile, traceProcessor, timestampConverter);

    this.protoTransformer = new FakeProtoTransformer(
      assertDefined(
        ParserTransactions.TransactionsTraceEntryField.tamperedMessageType,
      ),
    );
  }

  override getTraceType(): TraceType {
    return TraceType.TRANSACTIONS;
  }

  override async getEntry(index: number): Promise<PropertyTreeNode> {
    let entryProto = await Utils.queryEntry(
      this.traceProcessor,
      this.getTableName(),
      this.entryIndexToRowIdMap,
      index,
    );
    entryProto = this.protoTransformer.transform(entryProto);
    return this.makePropertiesTree(entryProto);
  }

  protected override getTableName(): string {
    return 'surfaceflinger_transactions';
  }

  override async customQuery<Q extends CustomQueryType>(
    type: Q,
    entriesRange: EntriesRange,
  ): Promise<CustomQueryParserResultTypeMap[Q]> {
    return new VisitableParserCustomQuery(type)
      .visit(CustomQueryType.VSYNCID, async () => {
        return Utils.queryVsyncId(
          this.traceProcessor,
          this.getTableName(),
          this.entryIndexToRowIdMap,
          entriesRange,
        );
      })
      .getResult();
  }

  private makePropertiesTree(
    entryProto: perfetto.protos.TransactionTraceEntry,
  ): PropertyTreeNode {
    const tree = new PropertyTreeBuilderFromProto()
      .setData(entryProto)
      .setRootId('TransactionsTraceEntry')
      .setRootName('entry')
      .build();

    ParserTransactions.OPERATIONS.forEach((operation) => {
      operation.apply(tree);
    });
    return tree;
  }
}
