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
import {AbstractInputEventParser} from 'parsers/input/perfetto/abstract_input_event_parser';
import {SetFormatters} from 'parsers/operations/set_formatters';
import {TranslateIntDef} from 'parsers/operations/translate_intdef';
import {FakeProtoTransformer} from 'parsers/perfetto/fake_proto_transformer';
import {Utils} from 'parsers/perfetto/utils';
import {perfetto} from 'protos/input/latest/static';
import {TraceFile} from 'trace/trace_file';
import {TraceType} from 'trace/trace_type';
import {PropertyTreeBuilderFromProto} from 'trace/tree_node/property_tree_builder_from_proto';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {WasmEngineProxy} from 'trace_processor/wasm_engine_proxy';

export class ParserMotionEvent extends AbstractInputEventParser {
  private static readonly MotionEventField =
    AbstractInputEventParser.WrapperProto.fields['motionEvent'];

  private static readonly MOTION_EVENT_OPS = [
    new SetFormatters(ParserMotionEvent.MotionEventField),
    new TranslateIntDef(ParserMotionEvent.MotionEventField),
  ];

  private motionEventTransformer: FakeProtoTransformer;

  constructor(
    traceFile: TraceFile,
    traceProcessor: WasmEngineProxy,
    timestampConverter: ParserTimestampConverter,
  ) {
    super(traceFile, traceProcessor, timestampConverter);

    this.motionEventTransformer = new FakeProtoTransformer(
      assertDefined(ParserMotionEvent.MotionEventField.tamperedMessageType),
    );
  }

  override getTraceType(): TraceType {
    return TraceType.INPUT_MOTION_EVENT;
  }

  override async getEntry(index: number): Promise<PropertyTreeNode> {
    const motionEvent = await this.getMotionEventProto(index);
    const events = perfetto.protos.InputEventWrapper.create({
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

  protected override getTableName(): string {
    return 'android_motion_events';
  }

  protected override getStdLibModuleName(): string | undefined {
    return 'android.input';
  }

  private makeMotionPropertiesTree(
    entryProto: perfetto.protos.InputEventWrapper,
  ): PropertyTreeNode {
    const tree = new PropertyTreeBuilderFromProto()
      .setData(entryProto)
      .setRootId('AndroidMotionEvent')
      .setRootName('entry')
      .build();

    ParserMotionEvent.MOTION_EVENT_OPS.forEach((operation) => {
      operation.apply(assertDefined(tree.getChildByName('motionEvent')));
    });

    this.processDispatchEventsTree(
      assertDefined(tree.getChildByName('windowDispatchEvents')),
    );

    return tree;
  }
}
