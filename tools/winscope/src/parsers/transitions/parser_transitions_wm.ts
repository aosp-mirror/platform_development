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

import {Timestamp, TimestampType} from 'common/time';
import {AbstractParser} from 'parsers/abstract_parser';
import root from 'protos/transitions/udc/json';
import {com} from 'protos/transitions/udc/static';
import {TraceType} from 'trace/trace_type';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {ParserTransitionsUtils} from './parser_transitions_utils';

export class ParserTransitionsWm extends AbstractParser<PropertyTreeNode> {
  private static readonly TransitionTraceProto = root.lookupType(
    'com.android.server.wm.shell.TransitionTraceProto',
  );

  private realToElapsedTimeOffsetNs: undefined | bigint;

  override getTraceType(): TraceType {
    return TraceType.WM_TRANSITION;
  }

  override processDecodedEntry(
    index: number,
    timestampType: TimestampType,
    entryProto: com.android.server.wm.shell.ITransition,
  ): PropertyTreeNode {
    return this.makePropertiesTree(timestampType, entryProto);
  }

  override decodeTrace(
    buffer: Uint8Array,
  ): com.android.server.wm.shell.ITransition[] {
    const decodedProto = ParserTransitionsWm.TransitionTraceProto.decode(
      buffer,
    ) as unknown as com.android.server.wm.shell.ITransitionTraceProto;

    const timeOffset = BigInt(
      decodedProto.realToElapsedTimeOffsetNanos?.toString() ?? '0',
    );
    this.realToElapsedTimeOffsetNs = timeOffset !== 0n ? timeOffset : undefined;

    return decodedProto.transitions ?? [];
  }

  override getMagicNumber(): number[] | undefined {
    return [0x09, 0x54, 0x52, 0x4e, 0x54, 0x52, 0x41, 0x43, 0x45]; // .TRNTRACE
  }

  override getTimestamp(
    type: TimestampType,
    entry: com.android.server.wm.shell.ITransition,
  ): undefined | Timestamp {
    // for consistency with all transitions, elapsed nanos are defined as shell dispatch time else 0n
    return this.timestampFactory.makeTimestampFromType(
      type,
      0n,
      this.realToElapsedTimeOffsetNs,
    );
  }

  private validateWmTransitionEntry(
    entry: com.android.server.wm.shell.ITransition,
  ) {
    if (entry.id === 0) {
      throw new Error('Entry need a non null id');
    }
    if (
      !entry.createTimeNs &&
      !entry.sendTimeNs &&
      !entry.abortTimeNs &&
      !entry.finishTimeNs
    ) {
      throw new Error('Requires at least one non-null timestamp');
    }
    if (this.realToElapsedTimeOffsetNs === undefined) {
      throw new Error('missing realToElapsedTimeOffsetNs');
    }
  }

  private makePropertiesTree(
    timestampType: TimestampType,
    entryProto: com.android.server.wm.shell.ITransition,
  ): PropertyTreeNode {
    this.validateWmTransitionEntry(entryProto);

    const shellEntryTree = ParserTransitionsUtils.makeShellPropertiesTree();
    const wmEntryTree = ParserTransitionsUtils.makeWmPropertiesTree({
      entry: entryProto,
      realToElapsedTimeOffsetNs: this.realToElapsedTimeOffsetNs,
      timestampType,
      timestampFactory: this.timestampFactory,
    });

    return ParserTransitionsUtils.makeTransitionPropertiesTree(
      shellEntryTree,
      wmEntryTree,
    );
  }
}
