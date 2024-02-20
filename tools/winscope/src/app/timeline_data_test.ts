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
import {NO_TIMEZONE_OFFSET_FACTORY} from 'common/timestamp_factory';
import {TracesBuilder} from 'test/unit/traces_builder';
import {TracePosition} from 'trace/trace_position';
import {TraceType} from 'trace/trace_type';
import {TimelineData} from './timeline_data';

describe('TimelineData', () => {
  let timelineData: TimelineData;

  const timestamp0 = NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(0n);
  const timestamp5 = NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(5n);
  const timestamp9 = NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(9n);
  const timestamp10 = NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(10n);
  const timestamp11 = NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(11n);

  const traces = new TracesBuilder()
    .setTimestamps(TraceType.PROTO_LOG, [timestamp9])
    .setTimestamps(TraceType.EVENT_LOG, [timestamp9])
    .setTimestamps(TraceType.SURFACE_FLINGER, [timestamp10])
    .setTimestamps(TraceType.WINDOW_MANAGER, [timestamp11])
    .build();

  const position9 = TracePosition.fromTraceEntry(
    assertDefined(traces.getTrace(TraceType.PROTO_LOG)).getEntry(0),
  );
  const position10 = TracePosition.fromTraceEntry(
    assertDefined(traces.getTrace(TraceType.SURFACE_FLINGER)).getEntry(0),
  );
  const position11 = TracePosition.fromTraceEntry(
    assertDefined(traces.getTrace(TraceType.WINDOW_MANAGER)).getEntry(0),
  );
  const position1000 = TracePosition.fromTimestamp(
    NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1000n),
  );

  beforeEach(() => {
    timelineData = new TimelineData();
  });

  it('can be initialized', () => {
    expect(timelineData.getCurrentPosition()).toBeUndefined();

    timelineData.initialize(traces, undefined);
    expect(timelineData.getCurrentPosition()).toBeDefined();
  });

  describe('dumps', () => {
    const traces = new TracesBuilder()
      .setTimestamps(TraceType.SURFACE_FLINGER, [timestamp10, timestamp11])
      .setTimestamps(TraceType.WINDOW_MANAGER, [timestamp0])
      .build();

    it('drops trace if it is a dump (will not display in timeline UI)', () => {
      timelineData.initialize(traces, undefined);
      expect(
        timelineData.getTraces().getTrace(TraceType.WINDOW_MANAGER),
      ).toBeUndefined();
      expect(timelineData.getFullTimeRange().from).toBe(timestamp10);
      expect(timelineData.getFullTimeRange().to).toBe(timestamp11);
    });

    it('is robust to prev/next entry request of a dump', () => {
      timelineData.initialize(traces, undefined);
      expect(
        timelineData.getPreviousEntryFor(TraceType.WINDOW_MANAGER),
      ).toBeUndefined();
      expect(
        timelineData.getNextEntryFor(TraceType.WINDOW_MANAGER),
      ).toBeUndefined();
    });
  });

  it('uses first entry of first active trace by default', () => {
    timelineData.initialize(traces, undefined);
    expect(timelineData.getCurrentPosition()).toEqual(position10);
  });

  it('uses explicit position if set', () => {
    timelineData.initialize(traces, undefined);
    expect(timelineData.getCurrentPosition()).toEqual(position10);

    timelineData.setPosition(position1000);
    expect(timelineData.getCurrentPosition()).toEqual(position1000);

    timelineData.setActiveViewTraceTypes([TraceType.SURFACE_FLINGER]);
    expect(timelineData.getCurrentPosition()).toEqual(position1000);

    timelineData.setActiveViewTraceTypes([TraceType.WINDOW_MANAGER]);
    expect(timelineData.getCurrentPosition()).toEqual(position1000);
  });

  it('sets active trace types and update current position accordingly', () => {
    timelineData.initialize(traces, undefined);

    timelineData.setActiveViewTraceTypes([]);
    expect(timelineData.getCurrentPosition()).toEqual(position9);

    timelineData.setActiveViewTraceTypes([TraceType.WINDOW_MANAGER]);
    expect(timelineData.getCurrentPosition()).toEqual(position11);

    timelineData.setActiveViewTraceTypes([TraceType.SURFACE_FLINGER]);
    expect(timelineData.getCurrentPosition()).toEqual(position10);

    timelineData.setActiveViewTraceTypes([
      TraceType.SURFACE_FLINGER,
      TraceType.WINDOW_MANAGER,
    ]);
    expect(timelineData.getCurrentPosition()).toEqual(position10);
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
      const traces = new TracesBuilder()
        .setTimestamps(TraceType.SURFACE_FLINGER, [])
        .build();
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

  it('getCurrentPosition() returns same object if no change to range', () => {
    timelineData.initialize(traces, undefined);

    expect(timelineData.getCurrentPosition()).toBe(
      timelineData.getCurrentPosition(),
    );

    timelineData.setPosition(position11);

    expect(timelineData.getCurrentPosition()).toBe(
      timelineData.getCurrentPosition(),
    );
  });

  it('makePositionFromActiveTrace()', () => {
    timelineData.initialize(traces, undefined);
    const time100 = NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(100n);

    {
      timelineData.setActiveViewTraceTypes([TraceType.SURFACE_FLINGER]);
      const position = timelineData.makePositionFromActiveTrace(time100);
      expect(position.timestamp).toEqual(time100);
      expect(position.entry).toEqual(
        traces.getTrace(TraceType.SURFACE_FLINGER)?.getEntry(0),
      );
    }

    {
      timelineData.setActiveViewTraceTypes([TraceType.WINDOW_MANAGER]);
      const position = timelineData.makePositionFromActiveTrace(time100);
      expect(position.timestamp).toEqual(time100);
      expect(position.entry).toEqual(
        traces.getTrace(TraceType.WINDOW_MANAGER)?.getEntry(0),
      );
    }
  });

  it('getFullTimeRange() returns same object if no change to range', () => {
    timelineData.initialize(traces, undefined);

    expect(timelineData.getFullTimeRange()).toBe(
      timelineData.getFullTimeRange(),
    );
  });

  it('getSelectionTimeRange() returns same object if no change to range', () => {
    timelineData.initialize(traces, undefined);

    expect(timelineData.getSelectionTimeRange()).toBe(
      timelineData.getSelectionTimeRange(),
    );

    timelineData.setSelectionTimeRange({
      from: timestamp0,
      to: timestamp5,
    });

    expect(timelineData.getSelectionTimeRange()).toBe(
      timelineData.getSelectionTimeRange(),
    );
  });

  it('getZoomRange() returns same object if no change to range', () => {
    timelineData.initialize(traces, undefined);

    expect(timelineData.getZoomRange()).toBe(timelineData.getZoomRange());

    timelineData.setZoom({
      from: timestamp0,
      to: timestamp5,
    });

    expect(timelineData.getZoomRange()).toBe(timelineData.getZoomRange());
  });

  it("getCurrentPosition() prioritizes active trace's first entry", () => {
    timelineData.initialize(traces, undefined);
    timelineData.setActiveViewTraceTypes([TraceType.WINDOW_MANAGER]);

    expect(timelineData.getCurrentPosition()?.timestamp).toBe(timestamp11);
  });
});
