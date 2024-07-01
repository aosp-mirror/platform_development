/*
 * Copyright (C) 2024 The Android Open Source Project
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
import {ParserTimestampConverter} from 'common/timestamp_converter';
import {SetFormatters} from 'parsers/operations/set_formatters';
import {TranslateIntDef} from 'parsers/operations/translate_intdef';
import {AbstractParser} from 'parsers/perfetto/abstract_parser';
import {FakeProtoBuilder} from 'parsers/perfetto/fake_proto_builder';
import {FakeProtoTransformer} from 'parsers/perfetto/fake_proto_transformer';
import {Utils} from 'parsers/perfetto/utils';
import {TamperedMessageType} from 'parsers/tampered_message_type';
import root from 'protos/input/latest/json';
import {perfetto} from 'protos/input/latest/static';
import {
  CustomQueryParserResultTypeMap,
  CustomQueryType,
  VisitableParserCustomQuery,
} from 'trace/custom_query';
import {EntriesRange} from 'trace/index_types';
import {TraceFile} from 'trace/trace_file';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {WasmEngineProxy} from 'trace_processor/wasm_engine_proxy';

export abstract class AbstractInputEventParser extends AbstractParser<PropertyTreeNode> {
  protected static readonly WrapperProto = TamperedMessageType.tamper(
    root.lookupType('perfetto.protos.InputEventWrapper'),
  );

  private static readonly DispatchEventsField =
    AbstractInputEventParser.WrapperProto.fields['windowDispatchEvents'];

  private static readonly DISPATCH_EVENT_OPS = [
    new SetFormatters(AbstractInputEventParser.DispatchEventsField),
    new TranslateIntDef(AbstractInputEventParser.DispatchEventsField),
  ];

  private static readonly DispatchTableName = 'android_input_event_dispatch';

  private dispatchEventTransformer: FakeProtoTransformer;

  protected constructor(
    traceFile: TraceFile,
    traceProcessor: WasmEngineProxy,
    timestampConverter: ParserTimestampConverter,
  ) {
    super(traceFile, traceProcessor, timestampConverter);

    this.dispatchEventTransformer = new FakeProtoTransformer(
      assertDefined(
        AbstractInputEventParser.DispatchEventsField.tamperedMessageType,
      ),
    );
  }

  protected async getDispatchEvents(
    eventId: number,
  ): Promise<perfetto.protos.AndroidWindowInputDispatchEvent[]> {
    const sql = `
        SELECT d.id,
               args.key,
               args.value_type,
               args.int_value,
               args.string_value,
               args.real_value
        FROM ${AbstractInputEventParser.DispatchTableName} AS d
                 INNER JOIN args ON d.arg_set_id = args.arg_set_id
        WHERE d.event_id = ${eventId}
        ORDER BY d.id;
    `;
    const result = await this.traceProcessor.query(sql).waitAllRows();

    const dispatchEvents: perfetto.protos.AndroidWindowInputDispatchEvent[] =
      [];
    for (const it = result.iter({}); it.valid(); ) {
      const builder = new FakeProtoBuilder();
      const prevId = it.get('id');
      while (it.valid() && it.get('id') === prevId) {
        builder.addArg(
          it.get('key') as string,
          it.get('value_type') as string,
          it.get('int_value') as bigint | undefined,
          it.get('real_value') as number | undefined,
          it.get('string_value') as string | undefined,
        );
        it.next();
      }
      dispatchEvents.push(builder.build());
    }
    return dispatchEvents;
  }

  protected override getStdLibModuleName(): string | undefined {
    return 'android.input';
  }

  protected processDispatchEventsTree(tree: PropertyTreeNode) {
    AbstractInputEventParser.DISPATCH_EVENT_OPS.forEach((operation) => {
      operation.apply(tree);
    });
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
          AbstractInputEventParser.createVsyncIdQuery,
        );
      })
      .getResult();
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
        args.key,
        args.value_type,
        args.int_value
      FROM ${tableName} AS tbl
      INNER JOIN ${AbstractInputEventParser.DispatchTableName} AS d
          ON tbl.event_id = d.event_id
      INNER JOIN args ON d.arg_set_id = args.arg_set_id
      WHERE
        tbl.id BETWEEN ${minRowId} AND ${maxRowId}
        AND args.key = 'vsync_id'
      GROUP BY tbl.id
      ORDER BY tbl.id;
    `;
  }
}
