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

import {Transformer} from 'app/components/timeline/mini-timeline/transformer';
import {Segment} from 'app/components/timeline/segment';
import {TimelineUtils} from 'app/components/timeline/timeline_utils';
import {TimelineData} from 'app/timeline_data';
import {assertDefined} from 'common/assert_utils';
import {TimeRange, Timestamp} from 'common/time';
import {Trace, TraceEntry} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceType} from 'trace/trace_type';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {MiniCanvasDrawerData, TimelineEntries} from './mini_canvas_drawer_data';

export class MiniTimelineDrawerInput {
  constructor(
    public fullRange: TimeRange,
    public selectedPosition: Timestamp,
    public selection: TimeRange,
    public zoomRange: TimeRange,
    public traces: Traces,
    public timelineData: TimelineData,
  ) {}

  transform(mapToRange: Segment): MiniCanvasDrawerData {
    const transformer = new Transformer(this.zoomRange, mapToRange);

    return new MiniCanvasDrawerData(
      transformer.transform(this.selectedPosition),
      {
        from: transformer.transform(this.selection.from),
        to: transformer.transform(this.selection.to),
      },
      () => {
        return this.transformTracesTimestamps(transformer);
      },
      transformer,
    );
  }

  private async transformTracesTimestamps(
    transformer: Transformer,
  ): Promise<TimelineEntries> {
    const transformedTraceSegments = new Map<
      TraceType,
      {
        points: number[];
        segments: Segment[];
        activePoint: number | undefined;
        activeSegment: Segment | undefined;
      }
    >();

    await Promise.all(
      this.traces.mapTrace(async (trace, type) => {
        const activeEntry = this.timelineData.findCurrentEntryFor(
          trace.type,
        ) as TraceEntry<PropertyTreeNode>;

        if (type === TraceType.TRANSITION) {
          // Transition trace is a special case, with entries with time ranges
          const transitionTrace = assertDefined(this.traces.getTrace(type));
          transformedTraceSegments.set(trace.type, {
            points: [],
            activePoint: undefined,
            segments: await this.transformTransitionTraceTimestamps(
              transformer,
              transitionTrace,
            ),
            activeSegment: activeEntry
              ? await this.transformTransitionEntry(transformer, activeEntry)
              : undefined,
          });
        } else {
          transformedTraceSegments.set(trace.type, {
            points: this.transformTraceTimestamps(transformer, trace),
            activePoint: activeEntry
              ? transformer.transform(activeEntry.getTimestamp())
              : undefined,
            segments: [],
            activeSegment: undefined,
          });
        }
      }),
    );

    return transformedTraceSegments;
  }

  private async transformTransitionTraceTimestamps(
    transformer: Transformer,
    trace: Trace<PropertyTreeNode>,
  ): Promise<Segment[]> {
    const promises: Array<Promise<Segment | undefined>> = [];
    trace.forEachEntry((entry) => {
      promises.push(this.transformTransitionEntry(transformer, entry));
    });

    return (await Promise.all(promises)).filter(
      (it) => it !== undefined,
    ) as Segment[];
  }

  private async transformTransitionEntry(
    transformer: Transformer,
    entry: TraceEntry<PropertyTreeNode>,
  ): Promise<Segment | undefined> {
    const transition: PropertyTreeNode = await entry.getValue();

    const timeRange = TimelineUtils.getTimeRangeForTransition(
      transition,
      entry.getTimestamp().getType(),
      this.selection,
    );

    if (!timeRange) {
      return undefined;
    }

    return {
      from: transformer.transform(timeRange.from),
      to: transformer.transform(timeRange.to),
    };
  }

  private transformTraceTimestamps(
    transformer: Transformer,
    trace: Trace<{}>,
  ): number[] {
    const result: number[] = [];

    trace.forEachTimestamp((timestamp) => {
      result.push(transformer.transform(timestamp));
    });

    return result;
  }
}
