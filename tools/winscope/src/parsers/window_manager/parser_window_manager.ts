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
import {com} from 'protos/windowmanager/latest/static';
import {
  CustomQueryParserResultTypeMap,
  CustomQueryType,
  VisitableParserCustomQuery,
} from 'trace/custom_query';
import {EntriesRange} from 'trace/trace';
import {TraceType} from 'trace/trace_type';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {PropertiesProvider} from 'trace/tree_node/properties_provider';
import {RectsComputation} from './computations/rects_computation';
import {WmCustomQueryUtils} from './custom_query_utils';
import {HierarchyTreeBuilderWm} from './hierarchy_tree_builder_wm';
import {ParserWmUtils} from './parser_window_manager_utils';
import {WindowManagerTraceFileProto} from './wm_tampered_protos';

export class ParserWindowManager extends AbstractParser<HierarchyTreeNode> {
  private static readonly MAGIC_NUMBER = [
    0x09, 0x57, 0x49, 0x4e, 0x54, 0x52, 0x41, 0x43, 0x45,
  ]; // .WINTRACE

  private realToBootTimeOffsetNs: bigint | undefined;

  override getTraceType(): TraceType {
    return TraceType.WINDOW_MANAGER;
  }

  override getMagicNumber(): number[] {
    return ParserWindowManager.MAGIC_NUMBER;
  }

  override getRealToBootTimeOffsetNs(): bigint | undefined {
    return this.realToBootTimeOffsetNs;
  }

  override getRealToMonotonicTimeOffsetNs(): bigint | undefined {
    return undefined;
  }

  override decodeTrace(
    buffer: Uint8Array,
  ): com.android.server.wm.IWindowManagerTraceProto[] {
    const decoded = WindowManagerTraceFileProto.decode(
      buffer,
    ) as com.android.server.wm.IWindowManagerTraceFileProto;
    const timeOffset = BigInt(
      decoded.realToElapsedTimeOffsetNanos?.toString() ?? '0',
    );
    this.realToBootTimeOffsetNs = timeOffset !== 0n ? timeOffset : undefined;
    return decoded.entry ?? [];
  }

  protected override getTimestamp(
    entry: com.android.server.wm.IWindowManagerTraceProto,
  ): Timestamp {
    return this.timestampConverter.makeTimestampFromBootTimeNs(
      BigInt(assertDefined(entry.elapsedRealtimeNanos).toString()),
    );
  }

  override processDecodedEntry(
    index: number,
    entry: com.android.server.wm.IWindowManagerTraceProto,
  ): HierarchyTreeNode {
    return this.makeHierarchyTree(entry);
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
          .forEach((windowManagerTraceProto) => {
            WmCustomQueryUtils.parseWindowsTokenAndTitle(
              windowManagerTraceProto?.windowManagerService
                ?.rootWindowContainer,
              result,
            );
          });
        return Promise.resolve(result);
      })
      .getResult();
  }

  private makeHierarchyTree(
    entryProto: com.android.server.wm.IWindowManagerTraceProto,
  ): HierarchyTreeNode {
    const containers: PropertiesProvider[] = ParserWmUtils.extractContainers(
      assertDefined(entryProto.windowManagerService),
    );

    const entry = ParserWmUtils.makeEntryProperties(
      assertDefined(entryProto.windowManagerService),
    );

    return new HierarchyTreeBuilderWm()
      .setRoot(entry)
      .setChildren(containers)
      .setComputations([new RectsComputation()])
      .build();
  }
}
