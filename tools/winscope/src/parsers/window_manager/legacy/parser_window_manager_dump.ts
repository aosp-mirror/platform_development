/*
 * Copyright (C) 2022 The Android Open Source Project
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
import {Timestamp} from 'common/time';
import {AbstractParser} from 'parsers/legacy/abstract_parser';
import {RectsComputation} from 'parsers/window_manager/computations/rects_computation';
import {WmCustomQueryUtils} from 'parsers/window_manager/custom_query_utils';
import {HierarchyTreeBuilderWm} from 'parsers/window_manager/hierarchy_tree_builder_wm';
import {PropertiesProviderFactory} from 'parsers/window_manager/properties_provider_factory';
import {com} from 'protos/windowmanager/udc/static';
import {
  CustomQueryParserResultTypeMap,
  CustomQueryType,
  VisitableParserCustomQuery,
} from 'trace/custom_query';
import {EntriesRange} from 'trace/index_types';
import {TraceType} from 'trace/trace_type';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {PropertiesProvider} from 'trace/tree_node/properties_provider';
import {TAMPERED_PROTOS_UDC} from './tampered_protos_udc';

class ParserWindowManagerDump extends AbstractParser {
  private readonly factory = new PropertiesProviderFactory(TAMPERED_PROTOS_UDC);

  override getTraceType(): TraceType {
    return TraceType.WINDOW_MANAGER;
  }

  override getMagicNumber(): undefined {
    return undefined;
  }

  override getRealToBootTimeOffsetNs(): bigint | undefined {
    return undefined;
  }

  override getRealToMonotonicTimeOffsetNs(): bigint | undefined {
    return undefined;
  }

  override decodeTrace(
    buffer: Uint8Array,
  ): com.android.server.wm.IWindowManagerServiceDumpProto[] {
    const entryProto = assertDefined(
      TAMPERED_PROTOS_UDC.windowManagerServiceField.tamperedMessageType,
    ).decode(buffer) as com.android.server.wm.IWindowManagerServiceDumpProto;

    // This parser is prone to accepting invalid inputs because it lacks a magic
    // number. Let's reduce the chances of accepting invalid inputs by making
    // sure that a trace entry can actually be created from the decoded proto.
    // If the trace entry creation fails, an exception is thrown and the parser
    // will be considered unsuited for this input data.
    this.processDecodedEntry(0, entryProto);

    return [entryProto];
  }

  protected override getTimestamp(
    entryProto: com.android.server.wm.IWindowManagerServiceDumpProto,
  ): Timestamp {
    return this.timestampConverter.makeZeroTimestamp();
  }

  override processDecodedEntry(
    index: number,
    entryProto: com.android.server.wm.IWindowManagerServiceDumpProto,
  ): HierarchyTreeNode {
    return this.makeHierarchyTree(entryProto);
  }

  private makeHierarchyTree(
    entryProto: com.android.server.wm.IWindowManagerServiceDumpProto,
  ): HierarchyTreeNode {
    const containers: PropertiesProvider[] =
      this.factory.makeContainerProperties(assertDefined(entryProto));

    const entry = this.factory.makeEntryProperties(entryProto);

    return new HierarchyTreeBuilderWm()
      .setRoot(entry)
      .setChildren(containers)
      .setComputations([new RectsComputation()])
      .build();
  }

  override customQuery<Q extends CustomQueryType>(
    type: Q,
    entriesRange: EntriesRange,
  ): Promise<CustomQueryParserResultTypeMap[Q]> {
    return new VisitableParserCustomQuery(type)
      .visit(CustomQueryType.WM_WINDOWS_TOKEN_AND_TITLE, () => {
        const result: CustomQueryParserResultTypeMap[CustomQueryType.WM_WINDOWS_TOKEN_AND_TITLE] =
          [];
        this.decodedEntries
          .slice(entriesRange.start, entriesRange.end)
          .forEach((windowManagerServiceDumpProto) => {
            WmCustomQueryUtils.parseWindowsTokenAndTitle(
              windowManagerServiceDumpProto?.rootWindowContainer,
              result,
            );
          });
        return Promise.resolve(result);
      })
      .getResult();
  }
}

export {ParserWindowManagerDump};
