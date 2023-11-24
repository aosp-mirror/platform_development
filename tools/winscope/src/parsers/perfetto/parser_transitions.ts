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
import {TimestampType} from 'common/time';
import {
  CrossPlatform,
  ShellTransitionData,
  Timestamp,
  Transition,
  TransitionChange,
  TransitionType,
  WmTransitionData,
} from 'flickerlib/common';
import {LayerTraceEntry} from 'flickerlib/layers/LayerTraceEntry';
import {TraceFile} from 'trace/trace_file';
import {TraceType} from 'trace/trace_type';
import {WasmEngineProxy} from 'trace_processor/wasm_engine_proxy';
import {AbstractParser} from './abstract_parser';
import {FakeProto, FakeProtoBuilder} from './fake_proto_builder';

export class ParserTransitions extends AbstractParser<Transition> {
  constructor(traceFile: TraceFile, traceProcessor: WasmEngineProxy) {
    super(traceFile, traceProcessor);
  }

  override getTraceType(): TraceType {
    return TraceType.TRANSITION;
  }

  override async getEntry(index: number, timestampType: TimestampType): Promise<LayerTraceEntry> {
    const transitionProto = await this.queryTransition(index);

    if (this.handlerIdToName === undefined) {
      const handlers = await this.queryHandlers();
      this.handlerIdToName = {};
      handlers.forEach((it) => (assertDefined(this.handlerIdToName)[it.id] = it.name));
    }

    return new Transition(
      Number(transitionProto.id),
      new WmTransitionData(
        this.toTimestamp(transitionProto.createTimeNs),
        this.toTimestamp(transitionProto.sendTimeNs),
        this.toTimestamp(transitionProto.wmAbortTimeNs),
        this.toTimestamp(transitionProto.finishTimeNs),
        this.toTimestamp(transitionProto.startingWindowRemoveTimeNs),
        transitionProto.startTransactionId.toString(),
        transitionProto.finishTransactionId.toString(),
        TransitionType.Companion.fromInt(Number(transitionProto.type)),
        transitionProto.targets.map(
          (it: any) =>
            new TransitionChange(
              TransitionType.Companion.fromInt(Number(it.mode)),
              Number(it.layerId),
              Number(it.windowId)
            )
        )
      ),
      new ShellTransitionData(
        this.toTimestamp(transitionProto.dispatchTimeNs),
        this.toTimestamp(transitionProto.mergeRequestTimeNs),
        this.toTimestamp(transitionProto.mergeTimeNs),
        this.toTimestamp(transitionProto.shellAbortTimeNs),
        this.handlerIdToName[Number(transitionProto.handler)],
        transitionProto.mergeTarget ? Number(transitionProto.mergeTarget) : null
      )
    );
  }

  private toTimestamp(n: BigInt | undefined | null): Timestamp | null {
    if (n === undefined || n === null) {
      return null;
    }

    const realToElapsedTimeOffsetNs = assertDefined(this.realToElapsedTimeOffsetNs);
    const unixNs = BigInt(n.toString()) + realToElapsedTimeOffsetNs;

    return CrossPlatform.timestamp.fromString(n.toString(), null, unixNs.toString());
  }

  protected override getTableName(): string {
    return 'window_manager_shell_transitions';
  }

  private async queryTransition(index: number): Promise<FakeProto> {
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
        window_manager_shell_transitions as transitions
        INNER JOIN args ON transitions.arg_set_id = args.arg_set_id
      WHERE transitions.id = ${index};
    `;
    const result = await this.traceProcessor.query(sql).waitAllRows();

    for (const it = result.iter({}); it.valid(); it.next()) {
      protoBuilder.addArg(
        it.get('key') as string,
        it.get('value_type') as string,
        it.get('int_value') as bigint | undefined,
        it.get('real_value') as number | undefined,
        it.get('string_value') as string | undefined
      );
    }

    return protoBuilder.build();
  }

  private async queryHandlers(): Promise<TransitionHandler[]> {
    const sql = 'SELECT handler_id, handler_name FROM window_manager_shell_transition_handlers;';
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

  private handlerIdToName: {[id: number]: string} | undefined = undefined;
}

interface TransitionHandler {
  id: number;
  name: string;
}
