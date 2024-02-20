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
import {Timestamp, TimestampType} from 'common/time';
import {NO_TIMEZONE_OFFSET_FACTORY} from 'common/timestamp_factory';
import {AbstractParser} from 'parsers/abstract_parser';
import {com} from 'protos/windowmanager/latest/static';
import {
  CustomQueryParserResultTypeMap,
  CustomQueryType,
  VisitableParserCustomQuery,
} from 'trace/custom_query';
import {EntriesRange} from 'trace/index_types';
import {TraceType} from 'trace/trace_type';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {PropertiesProvider} from 'trace/tree_node/properties_provider';
import {RectsComputation} from './computations/rects_computation';
import {WmCustomQueryUtils} from './custom_query_utils';
import {HierarchyTreeBuilderWm} from './hierarchy_tree_builder_wm';
import {ParserWmUtils} from './parser_window_manager_utils';
import {WindowManagerServiceField} from './wm_tampered_protos';

class ParserWindowManagerDump extends AbstractParser {
  override getTraceType(): TraceType {
    return TraceType.WINDOW_MANAGER;
  }

  override getMagicNumber(): undefined {
    return undefined;
  }

  override decodeTrace(
    buffer: Uint8Array,
  ): com.android.server.wm.IWindowManagerServiceDumpProto[] {
    const entryProto = assertDefined(
      WindowManagerServiceField.tamperedMessageType,
    ).decode(buffer) as com.android.server.wm.IWindowManagerServiceDumpProto;

    // This parser is prone to accepting invalid inputs because it lacks a magic
    // number. Let's reduce the chances of accepting invalid inputs by making
    // sure that a trace entry can actually be created from the decoded proto.
    // If the trace entry creation fails, an exception is thrown and the parser
    // will be considered unsuited for this input data.
    this.processDecodedEntry(
      0,
      TimestampType.ELAPSED /*irrelevant for dump*/,
      entryProto,
    );

    return [entryProto];
  }

  override getTimestamp(
    type: TimestampType,
    entryProto: any,
  ): undefined | Timestamp {
    if (NO_TIMEZONE_OFFSET_FACTORY.canMakeTimestampFromType(type, 0n)) {
      return NO_TIMEZONE_OFFSET_FACTORY.makeTimestampFromType(type, 0n, 0n);
    }
    return undefined;
  }

  override processDecodedEntry(
    index: number,
    timestampType: TimestampType,
    entryProto: com.android.server.wm.IWindowManagerServiceDumpProto,
  ): HierarchyTreeNode {
    return this.makeHierarchyTree(entryProto);
  }

  private makeHierarchyTree(
    entryProto: com.android.server.wm.IWindowManagerServiceDumpProto,
  ): HierarchyTreeNode {
    const containers: PropertiesProvider[] = ParserWmUtils.extractContainers(
      assertDefined(entryProto),
    );

    const entry = ParserWmUtils.makeEntryProperties(entryProto);

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
