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
import {AbstractParser} from 'parsers/perfetto/abstract_parser';
import {FakeProtoBuilder} from 'parsers/perfetto/fake_proto_builder';
import {EntryPropertiesTreeFactory} from 'parsers/transitions/entry_properties_tree_factory';
import {perfetto} from 'protos/transitions/latest/static';
import {TraceType} from 'trace/trace_type';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';

export class ParserTransitions extends AbstractParser<PropertyTreeNode> {
  private handlerIdToName: {[id: number]: string} | undefined = undefined;
  private readonly internalTableName = 'window_manager_shell_transitions';

  protected override async preProcessTrace() {
    // Entry timestamps are defined as shell dispatch time, which corresponds
    // to the ts column of the internal Perfetto table - if this is 0 and
    // send time is not null we fall back on send time
    const sql = `
      CREATE PERFETTO TABLE ${this.getTableName()} AS
      SELECT
        STATE.id as id,
        CASE
          WHEN (STATE.ts = 0 AND TRANS.int_value IS NOT NULL) THEN TRANS.int_value
          ELSE STATE.ts END
        AS ts,
        STATE.transition_id,
        STATE.arg_set_id,
        STATE.base64_proto,
        STATE.base64_proto_id
      FROM ${this.internalTableName} STATE
      LEFT JOIN args TRANS
        ON TRANS.arg_set_id = STATE.arg_set_id AND TRANS.key = 'send_time_ns'
      ORDER BY id;
   `;
    await this.traceProcessor.query(sql).waitAllRows();
  }

  override getTraceType(): TraceType {
    return TraceType.TRANSITION;
  }

  override async getEntry(index: number): Promise<PropertyTreeNode> {
    const transitionProto = await this.queryEntry(index);
    if (this.handlerIdToName === undefined) {
      const handlers = await this.queryHandlers();
      this.handlerIdToName = {};
      handlers.forEach(
        (it) => (assertDefined(this.handlerIdToName)[it.id] = it.name),
      );
    }
    return this.makePropertiesTree(transitionProto);
  }

  protected override getTableName(): string {
    return 'transitions_with_updated_ts';
  }

  private async queryEntry(
    index: number,
  ): Promise<perfetto.protos.ShellTransition> {
    const protoBuilder = new FakeProtoBuilder();

    const sql = `
      SELECT
        transitions.transition_id,
        args.key,
        args.value_type,
        args.int_value,
        args.string_value,
        args.real_value
      FROM
        ${this.getTableName()} as transitions
        INNER JOIN args ON transitions.arg_set_id = args.arg_set_id
      WHERE transitions.id = ${this.entryIndexToRowIdMap[index]};
    `;
    const result = await this.traceProcessor.query(sql).waitAllRows();

    for (const it = result.iter({}); it.valid(); it.next()) {
      protoBuilder.addArg(
        it.get('key') as string,
        it.get('value_type') as string,
        it.get('int_value') as bigint | undefined,
        it.get('real_value') as number | undefined,
        it.get('string_value') as string | undefined,
      );
    }

    return protoBuilder.build();
  }

  private makePropertiesTree(
    transitionProto: perfetto.protos.ShellTransition,
  ): PropertyTreeNode {
    this.validatePerfettoTransition(transitionProto);

    const perfettoTransitionInfo = {
      entry: transitionProto,
      realToBootTimeOffsetNs: undefined,
      handlerMapping: this.handlerIdToName,
      timestampConverter: this.timestampConverter,
    };

    const shellEntryTree = EntryPropertiesTreeFactory.makeShellPropertiesTree(
      perfettoTransitionInfo,
      [
        'createTimeNs',
        'sendTimeNs',
        'wmAbortTimeNs',
        'finishTimeNs',
        'startTransactionId',
        'finishTransactionId',
        'type',
        'targets',
        'flags',
        'startingWindowRemoveTimeNs',
      ],
    );
    const wmEntryTree = EntryPropertiesTreeFactory.makeWmPropertiesTree(
      perfettoTransitionInfo,
      [
        'dispatchTimeNs',
        'mergeTimeNs',
        'mergeRequestTimeNs',
        'shellAbortTimeNs',
        'handler',
        'mergeTarget',
      ],
    );

    return EntryPropertiesTreeFactory.makeTransitionPropertiesTree(
      shellEntryTree,
      wmEntryTree,
    );
  }

  private async queryHandlers(): Promise<TransitionHandler[]> {
    const sql =
      'SELECT handler_id, handler_name FROM window_manager_shell_transition_handlers;';
    const result = await this.traceProcessor.query(sql).waitAllRows();

    const handlers: TransitionHandler[] = [];
    for (const it = result.iter({}); it.valid(); it.next()) {
      handlers.push({
        id: it.get('handler_id') as number,
        name: it.get('handler_name') as string,
      });
    }

    return handlers;
  }

  private validatePerfettoTransition(
    transition: perfetto.protos.IShellTransition,
  ) {
    if (transition.id === 0) {
      throw new Error('Transitions entry need a non null id');
    }
    if (
      !transition.createTimeNs &&
      !transition.sendTimeNs &&
      !transition.wmAbortTimeNs &&
      !transition.finishTimeNs &&
      !transition.dispatchTimeNs &&
      !transition.mergeRequestTimeNs &&
      !transition.mergeTimeNs &&
      !transition.shellAbortTimeNs
    ) {
      throw new Error(
        'Transitions entry requires at least one non-null timestamp',
      );
    }
  }
}

interface TransitionHandler {
  id: number;
  name: string;
}
