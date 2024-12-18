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

import {Timestamp} from 'common/time';
import {AbstractParser} from 'parsers/legacy/abstract_parser';
import {ParserTransitionsUtils} from 'parsers/transitions/parser_transitions_utils';
import root from 'protos/transitions/udc/json';
import {com} from 'protos/transitions/udc/static';
import {TraceType} from 'trace/trace_type';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';

export class ParserTransitionsShell extends AbstractParser<PropertyTreeNode> {
  private static readonly WmShellTransitionsTraceProto = root.lookupType(
    'com.android.wm.shell.WmShellTransitionTraceProto',
  );

  private realToBootTimeOffsetNs: bigint | undefined;
  private handlerMapping: undefined | {[key: number]: string};

  override getTraceType(): TraceType {
    return TraceType.SHELL_TRANSITION;
  }

  override getRealToBootTimeOffsetNs(): bigint | undefined {
    return this.realToBootTimeOffsetNs;
  }

  override getRealToMonotonicTimeOffsetNs(): bigint | undefined {
    return undefined;
  }

  override decodeTrace(
    traceBuffer: Uint8Array,
  ): com.android.wm.shell.ITransition[] {
    const decodedProto =
      ParserTransitionsShell.WmShellTransitionsTraceProto.decode(
        traceBuffer,
      ) as unknown as com.android.wm.shell.IWmShellTransitionTraceProto;

    const timeOffset = BigInt(
      decodedProto.realToElapsedTimeOffsetNanos?.toString() ?? '0',
    );
    this.realToBootTimeOffsetNs = timeOffset !== 0n ? timeOffset : undefined;

    this.handlerMapping = {};
    for (const mapping of decodedProto.handlerMappings ?? []) {
      this.handlerMapping[mapping.id] = mapping.name;
    }

    return decodedProto.transitions ?? [];
  }

  override processDecodedEntry(
    index: number,
    entryProto: com.android.wm.shell.ITransition,
  ): PropertyTreeNode {
    return this.makePropertiesTree(entryProto);
  }

  protected override getTimestamp(
    entry: com.android.wm.shell.ITransition,
  ): Timestamp {
    return entry.dispatchTimeNs
      ? this.timestampConverter.makeTimestampFromBootTimeNs(
          BigInt(entry.dispatchTimeNs.toString()),
        )
      : this.timestampConverter.makeZeroTimestamp();
  }

  protected getMagicNumber(): number[] | undefined {
    return [0x09, 0x57, 0x4d, 0x53, 0x54, 0x52, 0x41, 0x43, 0x45]; // .WMSTRACE
  }

  private validateShellTransitionEntry(
    entry: com.android.wm.shell.ITransition,
  ) {
    if (entry.id === 0) {
      throw new Error('Shell Transitions entry needs non-null id');
    }
    if (
      !entry.dispatchTimeNs &&
      !entry.mergeRequestTimeNs &&
      !entry.mergeTimeNs &&
      !entry.abortTimeNs
    ) {
      throw new Error(
        'Shell Transitions entry requires at least one non-null timestamp',
      );
    }
    if (this.realToBootTimeOffsetNs === undefined) {
      throw new Error('Shell Transitions trace missing realToBootTimeOffsetNs');
    }
    if (this.handlerMapping === undefined) {
      throw new Error('Shell Transitions trace missing handler mapping');
    }
  }

  private makePropertiesTree(
    entryProto: com.android.wm.shell.ITransition,
  ): PropertyTreeNode {
    this.validateShellTransitionEntry(entryProto);

    const shellEntryTree = ParserTransitionsUtils.makeShellPropertiesTree({
      entry: entryProto,
      realToBootTimeOffsetNs: this.realToBootTimeOffsetNs,
      handlerMapping: this.handlerMapping,
      timestampConverter: this.timestampConverter,
    });
    const wmEntryTree = ParserTransitionsUtils.makeWmPropertiesTree();

    return ParserTransitionsUtils.makeTransitionPropertiesTree(
      shellEntryTree,
      wmEntryTree,
    );
  }
}
