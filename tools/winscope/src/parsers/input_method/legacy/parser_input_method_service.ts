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
import {Timestamp} from 'common/time/time';
import {HierarchyTreeServiceFactory} from 'parsers/input_method/hierarchy_tree_service_factory';
import {AbstractParser} from 'parsers/legacy/abstract_parser';
import {TamperedMessageType} from 'parsers/tampered_message_type';
import root from 'protos/ime/udc/json';
import {android} from 'protos/ime/udc/static';
import {TraceType} from 'trace/trace_type';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';

class ParserInputMethodService extends AbstractParser<HierarchyTreeNode> {
  private static readonly MAGIC_NUMBER = [
    0x09, 0x49, 0x4d, 0x53, 0x54, 0x52, 0x41, 0x43, 0x45,
  ]; // .IMSTRACE

  private static readonly InputMethodServiceTraceFileProto =
    TamperedMessageType.tamper(
      root.lookupType(
        'android.view.inputmethod.InputMethodServiceTraceFileProto',
      ),
    );
  private static readonly ENTRY_FIELD =
    ParserInputMethodService.InputMethodServiceTraceFileProto.fields['entry'];
  private static readonly SERVICE_FIELD = assertDefined(
    ParserInputMethodService.ENTRY_FIELD.tamperedMessageType,
  ).fields['inputMethodService'];

  private static readonly HIERARCHY_TREE_FACTORY =
    new HierarchyTreeServiceFactory(
      ParserInputMethodService.ENTRY_FIELD,
      ParserInputMethodService.SERVICE_FIELD,
    );

  private realToBootTimeOffsetNs: bigint | undefined;

  override getTraceType(): TraceType {
    return TraceType.INPUT_METHOD_SERVICE;
  }

  override getMagicNumber(): number[] {
    return ParserInputMethodService.MAGIC_NUMBER;
  }

  override getRealToBootTimeOffsetNs(): bigint | undefined {
    return this.realToBootTimeOffsetNs;
  }

  override getRealToMonotonicTimeOffsetNs(): bigint | undefined {
    return undefined;
  }

  override decodeTrace(
    buffer: Uint8Array,
  ): android.view.inputmethod.IInputMethodServiceTraceProto[] {
    const decoded =
      ParserInputMethodService.InputMethodServiceTraceFileProto.decode(
        buffer,
      ) as android.view.inputmethod.IInputMethodServiceTraceFileProto;
    const timeOffset = BigInt(
      decoded.realToElapsedTimeOffsetNanos?.toString() ?? '0',
    );
    this.realToBootTimeOffsetNs = timeOffset !== 0n ? timeOffset : undefined;
    return decoded.entry ?? [];
  }

  protected override getTimestamp(
    entry: android.view.inputmethod.IInputMethodServiceTraceProto,
  ): Timestamp {
    return this.timestampConverter.makeTimestampFromBootTimeNs(
      BigInt(assertDefined(entry.elapsedRealtimeNanos).toString()),
    );
  }

  override processDecodedEntry(
    index: number,
    entry: android.view.inputmethod.IInputMethodServiceTraceProto,
  ): HierarchyTreeNode {
    if (
      entry.elapsedRealtimeNanos === undefined ||
      entry.elapsedRealtimeNanos === null
    ) {
      throw new Error('Missing elapsedRealtimeNanos on IME Service entry');
    }
    return ParserInputMethodService.HIERARCHY_TREE_FACTORY.makeHierarchyTree(
      entry,
    );
  }
}

export {ParserInputMethodService};
