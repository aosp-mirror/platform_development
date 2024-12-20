/*
 * Copyright 2023, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {assertDefined} from 'common/assert_utils';
import {Timestamp} from 'common/time/time';
import {ParserTimestampConverter} from 'common/time/timestamp_converter';
import {AbstractTracesParser} from 'parsers/traces/abstract_traces_parser';
import {EntryPropertiesTreeFactory} from 'parsers/transitions/entry_properties_tree_factory';
import {CoarseVersion} from 'trace/coarse_version';
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceType} from 'trace/trace_type';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';

export class TracesParserTransitions extends AbstractTracesParser<PropertyTreeNode> {
  private readonly wmTransitionTrace: Trace<PropertyTreeNode> | undefined;
  private readonly shellTransitionTrace: Trace<PropertyTreeNode> | undefined;
  private readonly descriptors: string[];
  private decodedEntries: PropertyTreeNode[] | undefined;

  constructor(traces: Traces, timestampConverter: ParserTimestampConverter) {
    super(timestampConverter);
    const wmTransitionTrace = traces.getTrace(TraceType.WM_TRANSITION);
    const shellTransitionTrace = traces.getTrace(TraceType.SHELL_TRANSITION);
    if (wmTransitionTrace && shellTransitionTrace) {
      this.wmTransitionTrace = wmTransitionTrace;
      this.shellTransitionTrace = shellTransitionTrace;
      this.descriptors = this.wmTransitionTrace
        .getDescriptors()
        .concat(this.shellTransitionTrace.getDescriptors());
    } else {
      this.descriptors = [];
    }
  }

  override getCoarseVersion(): CoarseVersion {
    return CoarseVersion.LEGACY;
  }

  override async parse() {
    if (this.wmTransitionTrace === undefined) {
      throw new Error('Missing WM Transition trace');
    }

    if (this.shellTransitionTrace === undefined) {
      throw new Error('Missing Shell Transition trace');
    }

    const wmTransitionEntries: PropertyTreeNode[] = await Promise.all(
      this.wmTransitionTrace.mapEntry((entry) => entry.getValue()),
    );

    const shellTransitionEntries: PropertyTreeNode[] = await Promise.all(
      this.shellTransitionTrace.mapEntry((entry) => entry.getValue()),
    );

    const allEntries = wmTransitionEntries.concat(shellTransitionEntries);

    this.decodedEntries = this.compressEntries(allEntries);

    await this.createTimestamps();
  }

  override async createTimestamps() {
    this.timestamps = [];
    const zeroTs = this.timestampConverter.makeZeroTimestamp();
    for (let index = 0; index < this.getLengthEntries(); index++) {
      const entry = await this.getEntry(index);
      const shellData = entry.getChildByName('shellData');

      // for consistency with all transitions, elapsed nanos are defined as
      // shell dispatch time else 0n
      const dispatchTimestamp: Timestamp | undefined = shellData
        ?.getChildByName('dispatchTimeNs')
        ?.getValue();
      this.timestamps.push(dispatchTimestamp ?? zeroTs);
    }
  }

  override getLengthEntries(): number {
    return assertDefined(this.decodedEntries).length;
  }

  override getEntry(index: number): Promise<PropertyTreeNode> {
    const entry = assertDefined(this.decodedEntries)[index];
    return Promise.resolve(entry);
  }

  override getDescriptors(): string[] {
    return this.descriptors;
  }

  override getTraceType(): TraceType {
    return TraceType.TRANSITION;
  }

  override getRealToMonotonicTimeOffsetNs(): bigint | undefined {
    return undefined;
  }

  override getRealToBootTimeOffsetNs(): bigint | undefined {
    return undefined;
  }

  private compressEntries(
    allTransitions: PropertyTreeNode[],
  ): PropertyTreeNode[] {
    const idToTransition = new Map<number, PropertyTreeNode>();

    for (const transition of allTransitions) {
      const id = assertDefined(transition.getChildByName('id')).getValue();

      const accumulatedTransition = idToTransition.get(id);
      if (!accumulatedTransition) {
        idToTransition.set(id, transition);
      } else {
        const mergedTransition = this.mergePartialTransitions(
          accumulatedTransition,
          transition,
        );
        idToTransition.set(id, mergedTransition);
      }
    }

    const compressedTransitions = Array.from(idToTransition.values());
    compressedTransitions.forEach((transition) => {
      EntryPropertiesTreeFactory.TRANSITION_OPERATIONS.forEach((operation) =>
        operation.apply(transition),
      );
    });
    return compressedTransitions.sort(this.compareByTimestamp);
  }

  private compareByTimestamp(a: PropertyTreeNode, b: PropertyTreeNode): number {
    const aNanos =
      assertDefined(a.getChildByName('shellData'))
        .getChildByName('dispatchTimeNs')
        ?.getValue()
        ?.getValueNs() ?? 0n;
    const bNanos =
      assertDefined(b.getChildByName('shellData'))
        .getChildByName('dispatchTimeNs')
        ?.getValue()
        ?.getValueNs() ?? 0n;
    if (aNanos !== bNanos) {
      return aNanos < bNanos ? -1 : 1;
    }
    // if dispatchTimeNs not present for both, fallback to id
    return assertDefined(a.getChildByName('id')).getValue() <
      assertDefined(b.getChildByName('id')).getValue()
      ? -1
      : 1;
  }

  private mergePartialTransitions(
    transition1: PropertyTreeNode,
    transition2: PropertyTreeNode,
  ): PropertyTreeNode {
    if (
      assertDefined(transition1.getChildByName('id')).getValue() !==
      assertDefined(transition2.getChildByName('id')).getValue()
    ) {
      throw new Error("Can't merge transitions with mismatching ids");
    }

    const mergedTransition = this.mergeProperties(
      transition1,
      transition2,
      false,
    );

    const wmData1 = assertDefined(transition1.getChildByName('wmData'));
    const wmData2 = assertDefined(transition2.getChildByName('wmData'));
    const mergedWmData = this.mergeProperties(wmData1, wmData2);
    mergedTransition.addOrReplaceChild(mergedWmData);

    const shellData1 = assertDefined(transition1.getChildByName('shellData'));
    const shellData2 = assertDefined(transition2.getChildByName('shellData'));
    const mergedShellData = this.mergeProperties(shellData1, shellData2);
    mergedTransition.addOrReplaceChild(mergedShellData);

    return mergedTransition;
  }

  private mergeProperties(
    node1: PropertyTreeNode,
    node2: PropertyTreeNode,
    visitNestedChildren = true,
  ): PropertyTreeNode {
    const mergedNode = new PropertyTreeNode(
      node1.id,
      node1.name,
      node1.source,
      undefined,
    );

    node1.getAllChildren().forEach((property1) => {
      if (!visitNestedChildren && property1.getAllChildren().length > 0) {
        return;
      }

      const property2 = node2.getChildByName(property1.name);
      if (
        !property2 ||
        property2.getValue()?.toString() < property1.getValue()?.toString()
      ) {
        mergedNode.addOrReplaceChild(property1);
        return;
      }

      if (visitNestedChildren && property1.getAllChildren().length > 0) {
        const mergedProperty = this.mergeProperties(property1, property2);
        mergedNode.addOrReplaceChild(mergedProperty);
        return;
      }

      mergedNode.addOrReplaceChild(property2);
    });

    node2.getAllChildren().forEach((property2) => {
      const existingProperty = mergedNode.getChildByName(property2.name);
      if (!existingProperty) {
        mergedNode.addOrReplaceChild(property2);
        return;
      }
    });

    return mergedNode;
  }
}
