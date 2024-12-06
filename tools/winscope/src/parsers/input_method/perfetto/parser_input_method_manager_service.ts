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
import {HierarchyTreeManagerServiceFactory} from 'parsers/input_method/hierarchy_tree_manager_service_factory';
import {AbstractParser} from 'parsers/perfetto/abstract_parser';
import {FakeProtoTransformer} from 'parsers/perfetto/fake_proto_transformer';
import {Utils} from 'parsers/perfetto/utils';
import {TamperedMessageType} from 'parsers/tampered_message_type';
import root from 'protos/ime/latest/json';
import {TraceFile} from 'trace/trace_file';
import {TraceType} from 'trace/trace_type';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {WasmEngineProxy} from 'trace_processor/wasm_engine_proxy';

export class ParserInputMethodManagerService extends AbstractParser<HierarchyTreeNode> {
  private static readonly Wrapper = TamperedMessageType.tamper(
    root.lookupType('perfetto.protos.Wrapper'),
  );
  private static readonly ENTRY_FIELD =
    ParserInputMethodManagerService.Wrapper.fields['inputmethodManagerService'];
  private static readonly MANAGER_SERVICE_FIELD = assertDefined(
    ParserInputMethodManagerService.ENTRY_FIELD.tamperedMessageType,
  ).fields['inputMethodManagerService'];
  private static readonly HIERARCHY_TREE_FACTORY =
    new HierarchyTreeManagerServiceFactory(
      ParserInputMethodManagerService.ENTRY_FIELD,
      ParserInputMethodManagerService.MANAGER_SERVICE_FIELD,
    );

  private protoTransformer: FakeProtoTransformer;

  constructor(
    traceFile: TraceFile,
    traceProcessor: WasmEngineProxy,
    timestampConverter: ParserTimestampConverter,
  ) {
    super(traceFile, traceProcessor, timestampConverter);

    this.protoTransformer = new FakeProtoTransformer(
      assertDefined(
        ParserInputMethodManagerService.ENTRY_FIELD.tamperedMessageType,
      ),
    );
  }

  override getTraceType(): TraceType {
    return TraceType.INPUT_METHOD_MANAGER_SERVICE;
  }

  override async getEntry(index: number): Promise<HierarchyTreeNode> {
    let entryProto = await Utils.queryEntry(
      this.traceProcessor,
      this.getTableName(),
      this.entryIndexToRowIdMap,
      index,
    );
    entryProto = this.protoTransformer.transform(entryProto);
    return ParserInputMethodManagerService.HIERARCHY_TREE_FACTORY.makeHierarchyTree(
      entryProto,
    );
  }

  protected override getStdLibModuleName(): string | undefined {
    return 'android.winscope.inputmethod';
  }

  protected override getTableName(): string {
    return 'android_inputmethod_manager_service';
  }
}
