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
import {HierarchyTreeManagerServiceFactory} from 'parsers/input_method/hierarchy_tree_manager_service_factory';
import {AbstractParser} from 'parsers/legacy/abstract_parser';
import {TamperedMessageType} from 'parsers/tampered_message_type';
import root from 'protos/ime/udc/json';
import {android} from 'protos/ime/udc/static';
import {TraceType} from 'trace/trace_type';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';

class ParserInputMethodManagerService extends AbstractParser<HierarchyTreeNode> {
  private static readonly MAGIC_NUMBER = [
    0x09, 0x49, 0x4d, 0x4d, 0x54, 0x52, 0x41, 0x43, 0x45,
  ]; // .IMMTRACE

  private static readonly InputMethodManagerServiceTraceFileProto =
    TamperedMessageType.tamper(
      root.lookupType(
        'android.view.inputmethod.InputMethodManagerServiceTraceFileProto',
      ),
    );
  private static readonly ENTRY_FIELD =
    ParserInputMethodManagerService.InputMethodManagerServiceTraceFileProto
      .fields['entry'];
  private static readonly MANAGER_SERVICE_FIELD = assertDefined(
    ParserInputMethodManagerService.ENTRY_FIELD.tamperedMessageType,
  ).fields['inputMethodManagerService'];
  private static readonly HIERARCHY_TREE_FACTORY =
    new HierarchyTreeManagerServiceFactory(
      ParserInputMethodManagerService.ENTRY_FIELD,
      ParserInputMethodManagerService.MANAGER_SERVICE_FIELD,
    );

  private realToBootTimeOffsetNs: bigint | undefined;

  override getTraceType(): TraceType {
    return TraceType.INPUT_METHOD_MANAGER_SERVICE;
  }

  override getMagicNumber(): number[] {
    return ParserInputMethodManagerService.MAGIC_NUMBER;
  }

  override getRealToBootTimeOffsetNs(): bigint | undefined {
    return this.realToBootTimeOffsetNs;
  }

  override getRealToMonotonicTimeOffsetNs(): bigint | undefined {
    return undefined;
  }

  override decodeTrace(
    buffer: Uint8Array,
  ): android.view.inputmethod.IInputMethodManagerServiceTraceProto[] {
    const decoded =
      ParserInputMethodManagerService.InputMethodManagerServiceTraceFileProto.decode(
        buffer,
      ) as android.view.inputmethod.IInputMethodManagerServiceTraceFileProto;
    const timeOffset = BigInt(
      decoded.realToElapsedTimeOffsetNanos?.toString() ?? '0',
    );
    this.realToBootTimeOffsetNs = timeOffset !== 0n ? timeOffset : undefined;
    return decoded.entry ?? [];
  }

  protected override getTimestamp(
    entry: android.view.inputmethod.IInputMethodManagerServiceTraceProto,
  ): Timestamp {
    return this.timestampConverter.makeTimestampFromBootTimeNs(
      BigInt(assertDefined(entry.elapsedRealtimeNanos).toString()),
    );
  }

  protected override processDecodedEntry(
    index: number,
    entry: android.view.inputmethod.IInputMethodManagerServiceTraceProto,
  ): HierarchyTreeNode {
    if (
      entry.elapsedRealtimeNanos === undefined ||
      entry.elapsedRealtimeNanos === null
    ) {
      throw Error('Missing elapsedRealtimeNanos on entry');
    }
    return ParserInputMethodManagerService.HIERARCHY_TREE_FACTORY.makeHierarchyTree(
      entry,
    );
  }
}

export {ParserInputMethodManagerService};
