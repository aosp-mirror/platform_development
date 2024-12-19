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
import {ParserTimestampConverter} from 'common/time/timestamp_converter';
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

export class ParserKeyEvent extends AbstractInputEventParser {
  private static readonly KeyEventField =
    AbstractInputEventParser.WrapperProto.fields['keyEvent'];

  private static readonly KEY_EVENT_OPS = [
    new SetFormatters(ParserKeyEvent.KeyEventField),
    new TranslateIntDef(ParserKeyEvent.KeyEventField),
  ];

  private keyEventTransformer: FakeProtoTransformer;

  constructor(
    traceFile: TraceFile,
    traceProcessor: WasmEngineProxy,
    timestampConverter: ParserTimestampConverter,
  ) {
    super(traceFile, traceProcessor, timestampConverter);

    this.keyEventTransformer = new FakeProtoTransformer(
      assertDefined(ParserKeyEvent.KeyEventField.tamperedMessageType),
    );
  }

  override getTraceType(): TraceType {
    return TraceType.INPUT_KEY_EVENT;
  }

  override async getEntry(index: number): Promise<PropertyTreeNode> {
    const keyEvent = await this.getKeyEventProto(index);
    const events = perfetto.protos.InputEventWrapper.create({
      keyEvent,
      windowDispatchEvents: await this.getDispatchEvents(keyEvent.eventId),
    });
    return this.makeKeyPropertiesTree(events);
  }

  private async getKeyEventProto(
    index: number,
  ): Promise<perfetto.protos.AndroidKeyEvent> {
    let keyEventProto = await Utils.queryEntry(
      this.traceProcessor,
      this.getTableName(),
      this.entryIndexToRowIdMap,
      index,
    );

    keyEventProto = this.keyEventTransformer.transform(keyEventProto);
    return keyEventProto;
  }

  protected override getTableName(): string {
    return 'android_key_events';
  }

  protected override getStdLibModuleName(): string | undefined {
    return 'android.input';
  }

  private makeKeyPropertiesTree(
    entryProto: perfetto.protos.InputEventWrapper,
  ): PropertyTreeNode {
    const tree = new PropertyTreeBuilderFromProto()
      .setData(entryProto)
      .setRootId('AndroidKeyEvent')
      .setRootName('entry')
      .build();

    ParserKeyEvent.KEY_EVENT_OPS.forEach((operation) => {
      operation.apply(assertDefined(tree.getChildByName('keyEvent')));
    });
    this.processDispatchEventsTree(
      assertDefined(tree.getChildByName('windowDispatchEvents')),
    );
    return tree;
  }
}
