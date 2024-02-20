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
import {TraceType} from 'trace/trace_type';
import {MiniTimelineDrawerOutput} from './mini_timeline_drawer_output';

export type TimelineEntries = Map<
  TraceType,
  {
    points: number[];
    segments: Segment[];
    activePoint: number | undefined;
    activeSegment: Segment | undefined;
  }
>;

export class MiniCanvasDrawerData {
  constructor(
    public selectedPosition: number,
    public selection: Segment,
    private timelineEntriesGetter: () => Promise<TimelineEntries>,
    public transformer: Transformer,
  ) {}

  private entries: TimelineEntries | undefined = undefined;

  async getTimelineEntries(): Promise<TimelineEntries> {
    if (this.entries === undefined) {
      this.entries = await this.timelineEntriesGetter();
    }
    return this.entries;
  }

  toOutput(): MiniTimelineDrawerOutput {
    return new MiniTimelineDrawerOutput(
      this.transformer.untransform(this.selectedPosition),
      {
        from: this.transformer.untransform(this.selection.from),
        to: this.transformer.untransform(this.selection.to),
      },
    );
  }
}
