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

import {assertBigInt, assertDefined, assertString} from 'common/assert_utils';
import {ParserTimestampConverter} from 'common/time/timestamp_converter';
import {HierarchyTreeBuilderLog} from 'parsers/hierarchy_tree_builder_log';
import {AddDefaults} from 'parsers/operations/add_defaults';
import {SetFormatters} from 'parsers/operations/set_formatters';
import {AbstractParser} from 'parsers/perfetto/abstract_parser';
import {FakeProtoTransformer} from 'parsers/perfetto/fake_proto_transformer';
import {
  getDistinctValues,
  queryArgs,
  queryVsyncId,
} from 'parsers/perfetto/utils';
import {PropertyTreeBuilderFromProto} from 'parsers/property_tree_builder_from_proto';
import {PropertyTreeBuilderFromQueryRow} from 'parsers/property_tree_builder_from_query_row';
import {
  TamperedProtoField,
  TAMPERED_TRACE_PACKET,
} from 'parsers/tampered_message_type';
import {perfetto} from 'protos/perfetto/trace/static';
import {
  CustomQueryParamTypeMap,
  CustomQueryParserResultTypeMap,
  CustomQueryType,
  VisitableParserCustomQuery,
} from 'trace/custom_query';
import {EntriesRange} from 'trace/index_types';
import {TraceFile} from 'trace/trace_file';
import {TraceType} from 'trace/trace_type';
import {TransactionColumnType} from 'trace/transactions/transaction_column_type';
import {TransactionType} from 'trace/transactions/transaction_type';
import {
  EnumFormatter,
  FixedStringFormatter,
  PropertyFormatter,
} from 'trace/tree_node/formatters';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {Operation} from 'trace/tree_node/operations/operation';
import {PropertiesProvider} from 'trace/tree_node/properties_provider';
import {PropertiesProviderBuilder} from 'trace/tree_node/properties_provider_builder';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {QueryResult, Row, RowIteratorBase} from 'trace_processor/query_result';
import {TraceProcessor} from 'trace_processor/trace_processor';

export class ParserTransactions extends AbstractParser<HierarchyTreeNode> {
  private static readonly TransactionsTraceEntryField =
    TAMPERED_TRACE_PACKET.fields['surfaceflingerTransactions'];

  private flags: {[key: number]: string} | undefined;

  constructor(
    traceFile: TraceFile,
    traceProcessor: TraceProcessor,
    timestampConverter: ParserTimestampConverter,
  ) {
    super(traceFile, traceProcessor, timestampConverter);
  }

  override getTraceType(): TraceType {
    return TraceType.TRANSACTIONS;
  }

  override async getEntry(index: number): Promise<HierarchyTreeNode> {
    const sql = `SELECT
      sft.transaction_id,
      sft.pid,
      sft.uid,
      sft.process_name,
      sft.layer_id,
      sft.display_id,
      sft.flags_id,
      sft.transaction_type,
      sft.arg_set_id,
      sfs.vsync_id
    FROM __transaction_with_process AS sft
    INNER JOIN surfaceflinger_transactions AS sfs
      ON sfs.id = ${this.entryIndexToRowIdMap[index]}
      AND sfs.id = sft.snapshot_id`;

    const queryResult = await this.traceProcessor.query(sql);

    if (this.flags === undefined) {
      const flags = await this.queryFlags();
      this.flags = {};
      flags.forEach(
        (flags, flagId) => (assertDefined(this.flags)[flagId] = flags),
      );
    }

    return this.makeHierarchyTree(queryResult);
  }

  override async customQuery<Q extends CustomQueryType>(
    type: Q,
    entriesRange: EntriesRange,
    param?: CustomQueryParamTypeMap[Q],
  ): Promise<CustomQueryParserResultTypeMap[Q]> {
    return new VisitableParserCustomQuery(type)
      .visit(CustomQueryType.VSYNCID, async () => {
        return queryVsyncId(
          this.traceProcessor,
          this.getTableName(),
          this.entryIndexToRowIdMap,
          entriesRange,
          ParserTransactions.createVsyncIdQuery,
        );
      })
      .visit(CustomQueryType.LOG_TABLE_FILTER_VALUES, async () => {
        let tableName = this.getTransactionTableName();
        let columns: string[];
        switch (param) {
          case TransactionColumnType.TRANSACTION_ID:
            columns = ['transaction_id'];
            break;
          case TransactionColumnType.VSYNC_ID:
            tableName = this.getTableName();
            columns = ['vsync_id'];
            break;
          case TransactionColumnType.PID:
            columns = ['pid'];
            break;
          case TransactionColumnType.UID:
            columns = ['uid'];
            break;
          case TransactionColumnType.PROCESS:
            columns = ['process_name'];
            break;
          case TransactionColumnType.TRANSACTION_TYPE:
            columns = ['transaction_type'];
            break;
          case TransactionColumnType.LAYER_OR_DISPLAY_ID:
            columns = ['layer_id', 'display_id'];
            break;
          case TransactionColumnType.FLAGS:
            tableName = this.getFlagTableName();
            columns = ['flag'];
            break;

          default:
            throw new Error('unexpected transaction column type requested');
        }
        return getDistinctValues(this.traceProcessor, tableName, columns);
      })
      .getResult();
  }

  protected override async preProcessTrace(): Promise<void> {
    const sql = `
CREATE PERFETTO TABLE ${this.getTransactionTableName()} AS
  WITH process_matches AS (
  SELECT
      sft.id as row_id,
      processes.name AS process_name,
      0 AS match_priority
  FROM __intrinsic_surfaceflinger_transaction AS sft
  INNER JOIN process AS processes
      ON sft.pid = processes.pid AND sft.uid = processes.uid
  WHERE
      (sft.pid IS NOT NULL AND sft.pid != 0)
      AND (sft.uid IS NOT NULL AND sft.uid != 0)

  UNION ALL

  SELECT
      sft.id as row_id,
      processes.name AS process_name,
      1 AS match_priority
  FROM __intrinsic_surfaceflinger_transaction AS sft
  INNER JOIN process AS processes
      ON sft.pid = processes.pid
  WHERE
      (sft.uid IS NULL OR sft.uid = 0)
      AND (sft.pid IS NOT NULL AND sft.pid != 0)

  UNION ALL

  SELECT
      sft.id as row_id,
      processes.name AS process_name,
      2 AS match_priority
  FROM __intrinsic_surfaceflinger_transaction AS sft
  INNER JOIN process AS processes
      ON sft.uid = processes.uid
  WHERE
      (sft.pid IS NULL OR sft.pid = 0)
      AND (sft.uid IS NOT NULL AND sft.uid != 0)
),
ranked_process_matches AS (
    SELECT
        row_id,
        process_name,
        match_priority,
        COUNT(*) OVER (PARTITION BY row_id, match_priority) as num_matches_at_priority,
        ROW_NUMBER() OVER (PARTITION BY row_id ORDER BY match_priority ASC) as row_number
    FROM process_matches
)
SELECT
    sft.snapshot_id,
    sft.transaction_id,
    sft.pid,
    sft.uid,
    CASE
        WHEN rpm.num_matches_at_priority > 1 THEN NULL
        ELSE rpm.process_name
    END AS process_name,
    sft.layer_id,
    sft.display_id,
    sft.flags_id,
    sft.transaction_type,
    sft.arg_set_id
FROM __intrinsic_surfaceflinger_transaction AS sft
LEFT JOIN ranked_process_matches AS rpm
    ON sft.id = rpm.row_id AND rpm.row_number = 1;`;
    await this.traceProcessor.query(sql);
  }

  protected override getTableName(): string {
    return 'surfaceflinger_transactions';
  }

  private getTransactionTableName(): string {
    return '__transaction_with_process';
  }

  private getFlagTableName(): string {
    return '__intrinsic_surfaceflinger_transaction_flag';
  }

  private makeHierarchyTree(result: QueryResult): HierarchyTreeNode {
    const vsyncId =
      result.numRows() > 0 ? result.firstRow<Row>({})['vsync_id'] : undefined;
    const entryProperties = new PropertyTreeBuilderFromProto()
      .setData({vsyncId})
      .setRootId('TransactionsTraceEntry')
      .setRootName('entry')
      .build();
    const entry = new PropertiesProviderBuilder()
      .setEagerProperties(entryProperties)
      .build();

    const transactions: PropertiesProvider[] = [];
    const columns = [
      'transaction_id',
      'pid',
      'uid',
      'process_name',
      'layer_id',
      'display_id',
      'flags_id',
      'transaction_type',
    ];

    for (const it = result.iter({}); it.valid(); it.next()) {
      transactions.push(
        this.makeTransactionPropertiesProvider(
          it,
          columns,
          transactions.length,
        ),
      );
    }

    return new HierarchyTreeBuilderLog()
      .setRoot(entry)
      .setChildren(transactions)
      .build();
  }

  private makeTransactionPropertiesProvider(
    row: RowIteratorBase,
    columns: string[],
    index: number,
  ): PropertiesProvider {
    const argSetId = row.get('arg_set_id') ?? undefined;

    let field: TamperedProtoField | undefined;
    const transactionType = assertString(row.get('transaction_type'));
    const entryProtoType = assertDefined(
      ParserTransactions.TransactionsTraceEntryField.tamperedMessageType,
    );
    switch (transactionType) {
      case TransactionType.DISPLAY_ADDED:
      case TransactionType.DISPLAY_CHANGED:
        field = entryProtoType.fields['addedDisplays'];
        break;
      case TransactionType.LAYER_ADDED:
        field = entryProtoType.fields['addedLayers'];
        break;
      case TransactionType.LAYER_CHANGED:
        field = assertDefined(
          entryProtoType.fields['transactions']?.tamperedMessageType?.fields[
            'layerChanges'
          ],
        );
        break;
      default:
        if (argSetId !== undefined) {
          throw new Error('unexpected transaction type found with arg set id');
        }
    }

    const eagerProperties = new PropertyTreeBuilderFromQueryRow()
      .setData(row)
      .setColumns(columns)
      .setRootId(index)
      .setRootName(field?.type ?? transactionType)
      .build();

    const flagsIdFormatter = new EnumFormatter(assertDefined(this.flags));
    const builder = new PropertiesProviderBuilder()
      .setEagerProperties(eagerProperties)
      .setEagerOperations([
        new SetFormatters(
          undefined,
          new Map<string, PropertyFormatter>([['flagsId', flagsIdFormatter]]),
        ),
      ]);

    if (argSetId !== undefined && field !== undefined) {
      const customFormatters = new Map<string, PropertyFormatter>([
        ['flags', new EnumFormatter(perfetto.protos.LayerState.Flags)],
      ]);
      const flagsId = eagerProperties.getChildByName('flagsId');
      if (flagsId !== undefined) {
        const whatTranslation = flagsIdFormatter.format(flagsId);
        customFormatters.set('what', new FixedStringFormatter(whatTranslation));
      }
      const lazyOperations: Array<Operation<PropertyTreeNode>> = [
        new AddDefaults(field),
        new SetFormatters(field, customFormatters),
      ];

      const lazyPropertiesStrategy = async () => {
        let data = await queryArgs(this.traceProcessor, Number(argSetId));
        const transformer = new FakeProtoTransformer(
          assertDefined(field?.tamperedMessageType),
        );
        data = transformer.transform(data);

        return new PropertyTreeBuilderFromProto()
          .setData(data)
          .setRootId(index)
          .setRootName(assertDefined(field).name)
          .build();
      };

      builder
        .setLazyOperations(lazyOperations)
        .setLazyPropertiesStrategy(lazyPropertiesStrategy);
    }

    return builder.build();
  }

  private async queryFlags(): Promise<Map<number, string>> {
    const sql = `SELECT flags_id, flag FROM ${this.getFlagTableName()};`;
    const result = await this.traceProcessor.query(sql);

    const flags = new Map<number, string>();
    for (const it = result.iter({}); it.valid(); it.next()) {
      const flagId = Number(assertBigInt(it.get('flags_id')));
      const flag = assertString(it.get('flag'));
      if (flags.has(flagId)) {
        flags.set(flagId, flags.get(flagId) + ' | ' + flag);
      } else {
        flags.set(flagId, flag);
      }
    }
    return flags;
  }

  // Use a custom sql query to get the vsync_id of the first dispatch
  // entry associated with an input event, if any.
  private static createVsyncIdQuery(
    tableName: string,
    minRowId: number,
    maxRowId: number,
  ): string {
    return `
      SELECT
        tbl.id AS id,
        vsync_id as int_value,
        'uint' as value_type
      FROM ${tableName} AS tbl
      WHERE
        tbl.id BETWEEN ${minRowId} AND ${maxRowId}
      GROUP BY tbl.id
      ORDER BY tbl.id;
    `;
  }
}
