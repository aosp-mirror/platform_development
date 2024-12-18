/*
 * Copyright 2024, The Android Open Source Project
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

import {assertDefined, assertTrue} from 'common/assert_utils';
import {ParserTimestampConverter} from 'common/time/timestamp_converter';
import {AbstractTracesParser} from 'parsers/traces/abstract_traces_parser';
import {CoarseVersion} from 'trace/coarse_version';
import {
  CustomQueryParserResultTypeMap,
  CustomQueryType,
  VisitableParserCustomQuery,
} from 'trace/custom_query';
import {EntriesRange, Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceType} from 'trace/trace_type';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';

type OriginalTraceIndex = number;

export class TracesParserInput extends AbstractTracesParser<PropertyTreeNode> {
  private readonly keyEventTrace: Trace<PropertyTreeNode> | undefined;
  private readonly motionEventTrace: Trace<PropertyTreeNode> | undefined;
  private readonly descriptors: string[];
  private mergedEntryIndexMap:
    | Array<[OriginalTraceIndex, TraceType]>
    | undefined;

  constructor(traces: Traces, timestampConverter: ParserTimestampConverter) {
    super(timestampConverter);
    this.keyEventTrace = traces.getTrace(TraceType.INPUT_KEY_EVENT);
    this.motionEventTrace = traces.getTrace(TraceType.INPUT_MOTION_EVENT);
    this.descriptors = this.keyEventTrace?.getDescriptors() ?? [];
    this.motionEventTrace?.getDescriptors().forEach((d) => {
      if (!this.descriptors.includes(d)) {
        this.descriptors.push(d);
      }
    });
  }

  override async parse() {
    if (
      this.keyEventTrace === undefined &&
      this.motionEventTrace === undefined
    ) {
      throw new Error('Missing input traces');
    }
    this.mergedEntryIndexMap = TracesParserInput.createMergedEntryIndexMap(
      this.keyEventTrace,
      this.motionEventTrace,
    );
    await this.createTimestamps();
  }

  override getCoarseVersion(): CoarseVersion {
    return CoarseVersion.LATEST;
  }

  override getLengthEntries(): number {
    return assertDefined(this.mergedEntryIndexMap).length;
  }

  override getEntry(index: number): Promise<PropertyTreeNode> {
    const [subIndex, type] = assertDefined(this.mergedEntryIndexMap)[index];
    const trace = assertDefined(
      type === TraceType.INPUT_KEY_EVENT
        ? this.keyEventTrace
        : this.motionEventTrace,
    );
    return trace.getEntry(subIndex).getValue();
  }

  override getDescriptors(): string[] {
    return this.descriptors;
  }

  override getTraceType(): TraceType {
    return TraceType.INPUT_EVENT_MERGED;
  }

  override getRealToMonotonicTimeOffsetNs(): bigint | undefined {
    return undefined;
  }

  override getRealToBootTimeOffsetNs(): bigint | undefined {
    return undefined;
  }

  override async createTimestamps() {
    this.timestamps = [];
    assertDefined(this.mergedEntryIndexMap).forEach(([index, traceType]) => {
      const trace = assertDefined(
        traceType === TraceType.INPUT_KEY_EVENT
          ? this.keyEventTrace
          : this.motionEventTrace,
      );
      this.timestamps?.push(
        assertDefined(trace.getParser().getTimestamps())[index],
      );
    });
  }

  // Given two traces, merge the two traces into one based on their timestamps.
  // Returns the mapping from the index of the merged trace to the index in the
  // sub-trace.
  private static createMergedEntryIndexMap(
    trace1: Trace<PropertyTreeNode> | undefined,
    trace2: Trace<PropertyTreeNode> | undefined,
  ): Array<[OriginalTraceIndex, TraceType]> {
    // We are assuming the parsers entries are sorted by timestamps.
    const timestamps1 = trace1?.getParser().getTimestamps() ?? [];
    const timestamps2 = trace2?.getParser().getTimestamps() ?? [];
    const type1 = trace1?.type ?? TraceType.INPUT_EVENT_MERGED;
    const type2 = trace2?.type ?? TraceType.INPUT_EVENT_MERGED;
    const mergedIndices: Array<[OriginalTraceIndex, TraceType]> = [];

    let curIndex1 = 0;
    let curIndex2 = 0;
    while (curIndex1 < timestamps1.length && curIndex2 < timestamps2.length) {
      if (
        timestamps1[curIndex1].getValueNs() <=
        timestamps2[curIndex2].getValueNs()
      ) {
        mergedIndices.push([curIndex1++, type1]);
        continue;
      }
      mergedIndices.push([curIndex2++, type2]);
    }
    while (curIndex1 < timestamps1.length) {
      mergedIndices.push([curIndex1++, type1]);
    }
    while (curIndex2 < timestamps2.length) {
      mergedIndices.push([curIndex2++, type2]);
    }

    return mergedIndices;
  }

  override async customQuery<Q extends CustomQueryType>(
    type: Q,
    entriesRange: EntriesRange,
  ): Promise<CustomQueryParserResultTypeMap[Q]> {
    return new VisitableParserCustomQuery(type)
      .visit(CustomQueryType.VSYNCID, async () => {
        assertTrue(entriesRange.start < entriesRange.end);

        const {keyRange, motionRange} = this.getSubTraceRanges(entriesRange);

        let keyResult: Array<bigint> = [];
        if (keyRange !== undefined) {
          keyResult =
            (await this.keyEventTrace
              ?.getParser()
              .customQuery(CustomQueryType.VSYNCID, keyRange)) ?? [];
        }

        let motionResult: Array<bigint> = [];
        if (motionRange !== undefined) {
          motionResult =
            (await this.motionEventTrace
              ?.getParser()
              .customQuery(CustomQueryType.VSYNCID, motionRange)) ?? [];
        }

        const mergedResult: Array<bigint> = [];
        let curKeyIndex = 0;
        let curMotionIndex = 0;
        for (let i = entriesRange.start; i < entriesRange.end; i++) {
          if (
            assertDefined(this.mergedEntryIndexMap)[i][1] ===
            TraceType.INPUT_KEY_EVENT
          ) {
            mergedResult.push(keyResult[curKeyIndex++]);
          } else {
            mergedResult.push(motionResult[curMotionIndex++]);
          }
        }
        return mergedResult;
      })
      .getResult();
  }

  // Given the entries range for the merged trace, get the entries ranges for
  // the individual sub-traces that make up this merged trace.
  private getSubTraceRanges(entriesRange: EntriesRange): {
    keyRange?: EntriesRange;
    motionRange?: EntriesRange;
  } {
    const ranges: {keyRange?: EntriesRange; motionRange?: EntriesRange} = {};

    for (let i = entriesRange.start; i < entriesRange.end; i++) {
      const [subEventIndex, type] = assertDefined(this.mergedEntryIndexMap)[i];
      if (type === TraceType.INPUT_KEY_EVENT) {
        if (ranges.keyRange === undefined) {
          ranges.keyRange = {start: subEventIndex, end: subEventIndex + 1};
        } else {
          ranges.keyRange.end = subEventIndex + 1;
        }
      } else {
        if (ranges.motionRange === undefined) {
          ranges.motionRange = {start: subEventIndex, end: subEventIndex + 1};
        } else {
          ranges.motionRange.end = subEventIndex + 1;
        }
      }
    }
    return ranges;
  }
}
