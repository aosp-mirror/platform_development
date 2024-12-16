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
import {TimeRange} from 'common/time/time';
import {Trace} from 'trace/trace';
import {MiniTimelineDrawerOutput} from './mini_timeline_drawer_output';

export type TimelineTraces = Map<Trace<object>, TimelineTrace>;

export interface TimelineTrace {
  points: number[];
  segments: Segment[];
  activePoint: number | undefined;
  activeSegment: Segment | undefined;
}

export class MiniCanvasDrawerData {
  constructor(
    public selectedPosition: number,
    public selection: Segment,
    private timelineTracesGetter: () => Promise<TimelineTraces>,
    public transformer: Transformer,
    public bookmarks: number[],
  ) {}

  private traces: TimelineTraces | undefined = undefined;

  async getTimelineTraces(): Promise<TimelineTraces> {
    if (this.traces === undefined) {
      this.traces = await this.timelineTracesGetter();
    }
    return this.traces;
  }

  toOutput(): MiniTimelineDrawerOutput {
    return new MiniTimelineDrawerOutput(
      this.transformer.untransform(this.selectedPosition),
      new TimeRange(
        this.transformer.untransform(this.selection.from),
        this.transformer.untransform(this.selection.to),
      ),
    );
  }
}
