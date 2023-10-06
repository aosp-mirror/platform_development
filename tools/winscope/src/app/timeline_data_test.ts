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

import {TracesBuilder} from 'test/unit/traces_builder';
import {RealTimestamp, Timestamp, TimestampType} from 'trace/timestamp';
import {TracePosition} from 'trace/trace_position';
import {TraceType} from 'trace/trace_type';
import {TimelineData} from './timeline_data';

class TracePositionUpdateListener {
  onTracePositionUpdate(position: TracePosition) {
    // do nothing
  }
}

describe('TimelineData', () => {
  let timelineData: TimelineData;
  const positionUpdateListener = new TracePositionUpdateListener();

  const timestamp10 = new Timestamp(TimestampType.REAL, 10n);
  const timestamp11 = new Timestamp(TimestampType.REAL, 11n);

  const traces = new TracesBuilder()
    .setTimestamps(TraceType.SURFACE_FLINGER, [timestamp10])
    .setTimestamps(TraceType.WINDOW_MANAGER, [timestamp11])
    .build();

  const position10 = TracePosition.fromTraceEntry(
    traces.getTrace(TraceType.SURFACE_FLINGER)!.getEntry(0)
  );
  const position11 = TracePosition.fromTraceEntry(
    traces.getTrace(TraceType.WINDOW_MANAGER)!.getEntry(0)
  );

  beforeEach(() => {
    timelineData = new TimelineData();
    timelineData.setOnTracePositionUpdate((position) => {
      positionUpdateListener.onTracePositionUpdate(position);
    });
  });

  it('can be initialized', () => {
    expect(timelineData.getCurrentPosition()).toBeUndefined();

    timelineData.initialize(traces, undefined);
    expect(timelineData.getCurrentPosition()).toBeDefined();
  });

  it('uses first entry by default', () => {
    timelineData.initialize(traces, undefined);
    expect(timelineData.getCurrentPosition()).toEqual(position10);
  });

  it('uses explicit position if set', () => {
    timelineData.initialize(traces, undefined);
    expect(timelineData.getCurrentPosition()).toEqual(position10);

    const explicitPosition = TracePosition.fromTimestamp(new RealTimestamp(1000n));
    timelineData.setPosition(explicitPosition);
    expect(timelineData.getCurrentPosition()).toEqual(explicitPosition);

    timelineData.setActiveViewTraceTypes([TraceType.SURFACE_FLINGER]);
    expect(timelineData.getCurrentPosition()).toEqual(explicitPosition);

    timelineData.setActiveViewTraceTypes([TraceType.WINDOW_MANAGER]);
    expect(timelineData.getCurrentPosition()).toEqual(explicitPosition);
  });

  it('sets active trace types and update current position accordingly', () => {
    timelineData.initialize(traces, undefined);

    timelineData.setActiveViewTraceTypes([]);
    expect(timelineData.getCurrentPosition()).toEqual(position10);

    timelineData.setActiveViewTraceTypes([TraceType.WINDOW_MANAGER]);
    expect(timelineData.getCurrentPosition()).toEqual(position11);

    timelineData.setActiveViewTraceTypes([TraceType.SURFACE_FLINGER]);
    expect(timelineData.getCurrentPosition()).toEqual(position10);

    timelineData.setActiveViewTraceTypes([TraceType.SURFACE_FLINGER, TraceType.WINDOW_MANAGER]);
    expect(timelineData.getCurrentPosition()).toEqual(position10);
  });

  it('executes callback on position update', () => {
    spyOn(positionUpdateListener, 'onTracePositionUpdate');
    expect(positionUpdateListener.onTracePositionUpdate).toHaveBeenCalledTimes(0);

    timelineData.initialize(traces, undefined);
    expect(positionUpdateListener.onTracePositionUpdate).toHaveBeenCalledTimes(1);

    timelineData.setActiveViewTraceTypes([TraceType.WINDOW_MANAGER]);
    expect(positionUpdateListener.onTracePositionUpdate).toHaveBeenCalledTimes(2);
  });

  it("doesn't execute callback when position doesn't change", () => {
    timelineData.initialize(traces, undefined);

    spyOn(positionUpdateListener, 'onTracePositionUpdate');
    expect(positionUpdateListener.onTracePositionUpdate).toHaveBeenCalledTimes(0);

    timelineData.setActiveViewTraceTypes([TraceType.SURFACE_FLINGER]);
    expect(positionUpdateListener.onTracePositionUpdate).toHaveBeenCalledTimes(0);

    timelineData.setActiveViewTraceTypes([TraceType.SURFACE_FLINGER, TraceType.WINDOW_MANAGER]);
    expect(positionUpdateListener.onTracePositionUpdate).toHaveBeenCalledTimes(0);
  });

  it('hasTimestamps()', () => {
    expect(timelineData.hasTimestamps()).toBeFalse();

    // no trace
    {
      const traces = new TracesBuilder().build();
      timelineData.initialize(traces, undefined);
      expect(timelineData.hasTimestamps()).toBeFalse();
    }
    // trace without timestamps
    {
      const traces = new TracesBuilder().setTimestamps(TraceType.SURFACE_FLINGER, []).build();
      timelineData.initialize(traces, undefined);
      expect(timelineData.hasTimestamps()).toBeFalse();
    }
    // trace with timestamps
    {
      const traces = new TracesBuilder()
        .setTimestamps(TraceType.SURFACE_FLINGER, [timestamp10])
        .build();
      timelineData.initialize(traces, undefined);
      expect(timelineData.hasTimestamps()).toBeTrue();
    }
  });

  it('hasMoreThanOneDistinctTimestamp()', () => {
    expect(timelineData.hasMoreThanOneDistinctTimestamp()).toBeFalse();

    // no trace
    {
      const traces = new TracesBuilder().build();
      timelineData.initialize(traces, undefined);
      expect(timelineData.hasMoreThanOneDistinctTimestamp()).toBeFalse();
    }
    // no distinct timestamps
    {
      const traces = new TracesBuilder()
        .setTimestamps(TraceType.SURFACE_FLINGER, [timestamp10])
        .setTimestamps(TraceType.WINDOW_MANAGER, [timestamp10])
        .build();
      timelineData.initialize(traces, undefined);
      expect(timelineData.hasMoreThanOneDistinctTimestamp()).toBeFalse();
    }
    // distinct timestamps
    {
      const traces = new TracesBuilder()
        .setTimestamps(TraceType.SURFACE_FLINGER, [timestamp10])
        .setTimestamps(TraceType.WINDOW_MANAGER, [timestamp11])
        .build();
      timelineData.initialize(traces, undefined);
      expect(timelineData.hasMoreThanOneDistinctTimestamp()).toBeTrue();
    }
  });
});
