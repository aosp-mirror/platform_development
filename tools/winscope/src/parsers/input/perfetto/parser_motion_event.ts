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
import {TraceFile} from 'trace/trace_file';
import {TraceType} from 'trace/trace_type';
import {PropertyTreeBuilderFromProto} from 'trace/tree_node/property_tree_builder_from_proto';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {WasmEngineProxy} from 'trace_processor/wasm_engine_proxy';

export class ParserMotionEvent extends AbstractParser<PropertyTreeNode> {
  private static readonly MotionEventWrapperProto = TamperedMessageType.tamper(
    root.lookupType('perfetto.protos.MotionEventWrapper'),
  );

  private static readonly MotionEventField =
    ParserMotionEvent.MotionEventWrapperProto.fields['motionEvent'];

  private static readonly DispatchEventsField =
    ParserMotionEvent.MotionEventWrapperProto.fields['windowDispatchEvents'];

  private static readonly MOTION_EVENT_OPS = [
    new SetFormatters(ParserMotionEvent.MotionEventField),
    new TranslateIntDef(ParserMotionEvent.MotionEventField),
  ];

  private static readonly DISPATCH_EVENT_OPS = [
    new SetFormatters(ParserMotionEvent.DispatchEventsField),
    new TranslateIntDef(ParserMotionEvent.DispatchEventsField),
  ];

  private motionEventTransformer: FakeProtoTransformer;
  private dispatchEventTransformer: FakeProtoTransformer;

  constructor(
    traceFile: TraceFile,
    traceProcessor: WasmEngineProxy,
    timestampConverter: ParserTimestampConverter,
  ) {
    super(traceFile, traceProcessor, timestampConverter);

    this.motionEventTransformer = new FakeProtoTransformer(
      assertDefined(ParserMotionEvent.MotionEventField.tamperedMessageType),
    );
    this.dispatchEventTransformer = new FakeProtoTransformer(
      assertDefined(ParserMotionEvent.DispatchEventsField.tamperedMessageType),
    );
  }

  override getTraceType(): TraceType {
    return TraceType.INPUT_MOTION_EVENT;
  }

  override async getEntry(index: number): Promise<PropertyTreeNode> {
    const motionEvent = await this.getMotionEventProto(index);
    const events = perfetto.protos.MotionEventWrapper.create({
      motionEvent,
      windowDispatchEvents: await this.getDispatchEvents(motionEvent.eventId),
    });
    return this.makeMotionPropertiesTree(events);
  }

  private async getMotionEventProto(
    index: number,
  ): Promise<perfetto.protos.AndroidMotionEvent> {
    let motionEventProto = await Utils.queryEntry(
      this.traceProcessor,
      this.getTableName(),
      this.entryIndexToRowIdMap,
      index,
    );

    motionEventProto = this.motionEventTransformer.transform(motionEventProto);
    return motionEventProto;
  }

  private async getDispatchEvents(
    eventId: number,
  ): Promise<perfetto.protos.AndroidWindowInputDispatchEvent[]> {
    const sql = `
        SELECT d.id,
               args.key,
               args.value_type,
               args.int_value,
               args.string_value,
               args.real_value
        FROM android_input_event_dispatch AS d
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

  protected override getTableName(): string {
    return 'android_motion_events';
  }

  protected override getStdLibModuleName(): string | undefined {
    return 'android.input';
  }

  private makeMotionPropertiesTree(
    entryProto: perfetto.protos.MotionEventWrapper,
  ): PropertyTreeNode {
    const tree = new PropertyTreeBuilderFromProto()
      .setData(entryProto)
      .setRootId('AndroidMotionEvent')
      .setRootName('entry')
      .build();

    ParserMotionEvent.MOTION_EVENT_OPS.forEach((operation) => {
      operation.apply(assertDefined(tree.getChildByName('motionEvent')));
    });
    ParserMotionEvent.DISPATCH_EVENT_OPS.forEach((operation) => {
      operation.apply(
        assertDefined(tree.getChildByName('windowDispatchEvents')),
      );
    });
    return tree;
  }
}
