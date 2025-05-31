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

import {
  assertBigIntOrUndefined,
  assertDefined,
  assertString,
} from 'common/assert_utils';
import {MakeTimestampStrategyType} from 'common/time/time';
import {ParserTimestampConverter} from 'common/time/timestamp_converter';
import {HierarchyTreeBuilderLog} from 'parsers/hierarchy_tree_builder_log';
import {AddDefaults} from 'parsers/operations/add_defaults';
import {SetFormatters} from 'parsers/operations/set_formatters';
import {TransformToTimestamp} from 'parsers/operations/transform_to_timestamp';
import {TranslateIntDef} from 'parsers/operations/translate_intdef';
import {AbstractParser} from 'parsers/perfetto/abstract_parser';
import {FakeProtoTransformer} from 'parsers/perfetto/fake_proto_transformer';
import {queryArgs} from 'parsers/perfetto/utils';
import {PropertyTreeBuilderFromProto} from 'parsers/property_tree_builder_from_proto';
import {PropertyTreeBuilderFromQueryRow} from 'parsers/property_tree_builder_from_query_row';
import {TAMPERED_TRACE_PACKET} from 'parsers/tampered_message_type';
import {TransformDuration} from 'parsers/transitions/operations/transform_duration';
import {TransitionType} from 'parsers/transitions/transition_type';
import {TraceType} from 'trace/trace_type';
import {
  EnumFormatter,
  PropertyFormatter,
  TIMESTAMP_NODE_FORMATTER,
  UPPER_CASE_FORMATTER,
} from 'trace/tree_node/formatters';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {Operation} from 'trace/tree_node/operations/operation';
import {PropertiesProvider} from 'trace/tree_node/properties_provider';
import {PropertiesProviderBuilder} from 'trace/tree_node/properties_provider_builder';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {
  QueryResult,
  RowIteratorBase,
  SqlValue,
} from 'trace_processor/query_result';

export class ParserTransitions extends AbstractParser<HierarchyTreeNode> {
  private static readonly TRANSITION_FIELD =
    TAMPERED_TRACE_PACKET.fields['shellTransition'];
  private static readonly PROTO_TRANSFORMER = new FakeProtoTransformer(
    assertDefined(ParserTransitions.TRANSITION_FIELD.tamperedMessageType),
  );
  private static readonly EAGER_COLUMNS = [
    'transition_id',
    'arg_set_id',
    'transition_type',
    'send_time_ns',
    'dispatch_time_ns',
    'duration_ns',
    'handler',
    'status',
    'flags',
  ];
  private static readonly EAGER_TIMESTAMP_PROPERTIES = [
    'wmAbortTimeNs',
    'shellAbortTimeNs',
    'sendTimeNs',
    'dispatchTimeNs',
  ];
  private static readonly LAZY_TIMESTAMP_PROPERTIES = [
    'wmAbortTimeNs',
    'shellAbortTimeNs',
    'createTimeNs',
    'sendTimeNs',
    'finishTimeNs',
    'startingWindowRemoveTimeNs',
    'dispatchTimeNs',
    'mergeRequestTimeNs',
    'mergeTimeNs',
  ];
  private static readonly TRANSFORM_DURATION_OPERATION =
    new TransformDuration();
  private static readonly TRANSLATE_INTDEF_OPERATION = new TranslateIntDef(
    ParserTransitions.TRANSITION_FIELD,
  );
  private static readonly ADD_DEFAULTS_OPERATION = new AddDefaults(
    ParserTransitions.TRANSITION_FIELD,
    ['type', 'targets'],
  );
  private static readonly TRANSITION_TYPE_FORMATTER = new EnumFormatter(
    TransitionType,
  );

  private handlerIdToName: {[id: number]: string} | undefined = undefined;

  override getTraceType(): TraceType {
    return TraceType.TRANSITION;
  }

  override async getEntry(index: number): Promise<HierarchyTreeNode> {
    const columns = ParserTransitions.EAGER_COLUMNS.map(
      (column) => `transitions.${column}`,
    ).join(', ');
    const sql =
      `SELECT ${columns} FROM ${this.getTableName()} as transitions` +
      ` WHERE transitions.id = ${this.entryIndexToRowIdMap[index]};`;
    const queryResult = await this.traceProcessor.query(sql);

    if (this.handlerIdToName === undefined) {
      const handlers = await this.queryHandlers();
      this.handlerIdToName = {};
      handlers.forEach(
        (it) => (assertDefined(this.handlerIdToName)[it.id] = it.name),
      );
    }
    return this.makeHierarchyTree(queryResult);
  }

  protected override getTableName(): string {
    return 'window_manager_shell_transitions';
  }

  protected override getStdLibModuleName(): string {
    return 'android.winscope.transitions';
  }

  private async queryHandlers(): Promise<TransitionHandler[]> {
    const sql =
      'SELECT handler_id, handler_name FROM window_manager_shell_transition_handlers;';
    const result = await this.traceProcessor.query(sql);

    const handlers: TransitionHandler[] = [];
    for (const it = result.iter({}); it.valid(); it.next()) {
      const handlerid = assertBigIntOrUndefined(it.get('handler_id'));
      if (handlerid === undefined) continue;
      handlers.push({
        id: Number(handlerid),
        name: assertString(it.get('handler_name')),
      });
    }

    return handlers;
  }

  private async makeHierarchyTree(
    result: QueryResult,
  ): Promise<HierarchyTreeNode> {
    const transitionRow = result.iter({});
    const transition = await this.makeTransitionsPropertiesProvider(
      transitionRow,
    );
    return new HierarchyTreeBuilderLog()
      .setRoot(transition)
      .setChildren([])
      .build();
  }

  private async makeTransitionsPropertiesProvider(
    transitionRow: RowIteratorBase,
  ): Promise<PropertiesProvider> {
    const eagerProperties = await this.makeEagerPropertiesTree(transitionRow);

    const builder = new PropertiesProviderBuilder()
      .setEagerProperties(eagerProperties)
      .setEagerOperations(this.getEagerOperations());

    const argSetId = transitionRow.get('arg_set_id') ?? undefined;
    if (argSetId !== undefined) {
      builder
        .setLazyPropertiesStrategy(this.makeLazyPropertiesStrategy(argSetId))
        .setLazyOperations(this.getLazyOperations());
    }

    return builder.build();
  }

  private async makeEagerPropertiesTree(
    transitionRow: RowIteratorBase,
  ): Promise<PropertyTreeNode> {
    const eagerProperties = new PropertyTreeBuilderFromQueryRow()
      .setData(transitionRow)
      .setColumns(ParserTransitions.EAGER_COLUMNS)
      .setRootId('TransitionTraceEntry')
      .setRootName('Transition')
      .build();

    const participants = await this.makeParticipants(transitionRow);
    eagerProperties.addOrReplaceChild(
      assertDefined(participants.getChildByName('layers')),
    );
    eagerProperties.addOrReplaceChild(
      assertDefined(participants.getChildByName('windows')),
    );
    return eagerProperties;
  }

  private async makeParticipants(
    transitionRow: RowIteratorBase,
  ): Promise<PropertyTreeNode> {
    const transitionId = assertDefined(
      transitionRow.get('transition_id'),
      () => 'transition requires non-null id',
    );

    const participantsSql =
      'SELECT DISTINCT window_id, layer_id from android_window_manager_shell_transition_participants' +
      ` WHERE transition_id = ${transitionId}`;
    const participantsRes = await this.traceProcessor.query(participantsSql);

    const layers = [];
    const windows = [];
    for (const it = participantsRes.iter({}); it.valid(); it.next()) {
      const layer = it.get('layer_id') ?? undefined;
      if (layer !== undefined) {
        layers.push(layer);
      }
      const window = it.get('window_id') ?? undefined;
      if (window !== undefined) {
        windows.push(window);
      }
    }
    return new PropertyTreeBuilderFromProto()
      .setData({layers, windows})
      .setRootId('TransitionTraceEntry')
      .setRootName('Transition')
      .build();
  }

  private getEagerOperations(): Array<Operation<PropertyTreeNode>> {
    const transformToTimestampEager = new TransformToTimestamp(
      ParserTransitions.EAGER_TIMESTAMP_PROPERTIES,
      ParserTransitions.makeTimestampStrategy(this.timestampConverter),
    );

    const customFormattersEager = new Map<string, PropertyFormatter>([
      ['transitionType', ParserTransitions.TRANSITION_TYPE_FORMATTER],
      ['handler', new EnumFormatter(assertDefined(this.handlerIdToName))],
      ['status', UPPER_CASE_FORMATTER],
      ['durationNs', TIMESTAMP_NODE_FORMATTER],
    ]);

    return [
      transformToTimestampEager,
      ParserTransitions.TRANSFORM_DURATION_OPERATION,
      new SetFormatters(
        ParserTransitions.TRANSITION_FIELD,
        customFormattersEager,
      ),
      ParserTransitions.TRANSLATE_INTDEF_OPERATION,
    ];
  }

  private getLazyOperations(): Array<Operation<PropertyTreeNode>> {
    const transformToTimestamp = new TransformToTimestamp(
      ParserTransitions.LAZY_TIMESTAMP_PROPERTIES,
      ParserTransitions.makeTimestampStrategy(this.timestampConverter),
    );

    const customFormatters = new Map<string, PropertyFormatter>([
      ['type', ParserTransitions.TRANSITION_TYPE_FORMATTER],
      ['mode', ParserTransitions.TRANSITION_TYPE_FORMATTER],
      ['handler', new EnumFormatter(assertDefined(this.handlerIdToName))],
    ]);

    return [
      ParserTransitions.ADD_DEFAULTS_OPERATION,
      transformToTimestamp,
      new SetFormatters(ParserTransitions.TRANSITION_FIELD, customFormatters),
      ParserTransitions.TRANSLATE_INTDEF_OPERATION,
    ];
  }

  private makeLazyPropertiesStrategy(argSetId: SqlValue) {
    return async () => {
      const data = await queryArgs(this.traceProcessor, Number(argSetId));
      return new PropertyTreeBuilderFromProto()
        .setData(ParserTransitions.PROTO_TRANSFORMER.transform(data))
        .setRootId('TransitionTraceEntry')
        .setRootName('Transition')
        .build();
    };
  }

  private static makeTimestampStrategy(
    timestampConverter: ParserTimestampConverter,
  ): MakeTimestampStrategyType {
    return (valueNs: bigint) => {
      return timestampConverter.makeTimestampFromBootTimeNs(valueNs);
    };
  }
}

interface TransitionHandler {
  id: number;
  name: string;
}
