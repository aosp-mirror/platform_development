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
import {TimeRange} from 'common/time';
import {ParserBuilder} from 'test/unit/parser_builder';
import {PropertyTreeBuilder} from 'test/unit/property_tree_builder';
import {TimestampConverterUtils} from 'test/unit/timestamp_converter_utils';
import {TracesBuilder} from 'test/unit/traces_builder';
import {TraceBuilder} from 'test/unit/trace_builder';
import {Traces} from 'trace/traces';
import {TracePosition} from 'trace/trace_position';
import {TraceType} from 'trace/trace_type';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {TimelineData} from './timeline_data';

describe('TimelineData', () => {
  let timelineData: TimelineData;

  const timestamp0 = TimestampConverterUtils.makeRealTimestamp(0n);
  const timestamp5 = TimestampConverterUtils.makeRealTimestamp(5n);
  const timestamp9 = TimestampConverterUtils.makeRealTimestamp(9n);
  const timestamp10 = TimestampConverterUtils.makeRealTimestamp(10n);
  const timestamp11 = TimestampConverterUtils.makeRealTimestamp(11n);

  const traces = new TracesBuilder()
    .setTimestamps(TraceType.PROTO_LOG, [timestamp9])
    .setTimestamps(TraceType.EVENT_LOG, [timestamp9])
    .setTimestamps(TraceType.SURFACE_FLINGER, [timestamp10])
    .setTimestamps(TraceType.WINDOW_MANAGER, [timestamp11])
    .setTimestamps(TraceType.TRANSACTIONS, [])
    .build();

  const traceSf = assertDefined(traces.getTrace(TraceType.SURFACE_FLINGER));
  const traceWm = assertDefined(traces.getTrace(TraceType.WINDOW_MANAGER));

  const position10 = TracePosition.fromTraceEntry(
    assertDefined(traces.getTrace(TraceType.SURFACE_FLINGER)).getEntry(0),
  );
  const position11 = TracePosition.fromTraceEntry(
    assertDefined(traces.getTrace(TraceType.WINDOW_MANAGER)).getEntry(0),
  );
  const position1000 = TracePosition.fromTimestamp(
    TimestampConverterUtils.makeRealTimestamp(1000n),
  );

  beforeEach(() => {
    timelineData = new TimelineData();
  });

  it('can be initialized', () => {
    expect(timelineData.getCurrentPosition()).toBeUndefined();

    timelineData.initialize(
      traces,
      undefined,
      TimestampConverterUtils.TIMESTAMP_CONVERTER,
    );
    expect(timelineData.getCurrentPosition()).toBeDefined();
  });

  describe('dumps', () => {
    const traces = new TracesBuilder()
      .setTimestamps(TraceType.SURFACE_FLINGER, [timestamp10, timestamp11])
      .setTimestamps(TraceType.WINDOW_MANAGER, [timestamp0])
      .build();

    const dumpWm = assertDefined(traces.getTrace(TraceType.WINDOW_MANAGER));

    it('drops trace if it is a dump (will not display in timeline UI)', () => {
      timelineData.initialize(
        traces,
        undefined,
        TimestampConverterUtils.TIMESTAMP_CONVERTER,
      );
      expect(
        timelineData.getTraces().getTrace(TraceType.WINDOW_MANAGER),
      ).toBeUndefined();
      expect(timelineData.getFullTimeRange().from).toBe(timestamp10);
      expect(timelineData.getFullTimeRange().to).toBe(timestamp11);
    });

    it('is robust to prev/next entry request of a dump', () => {
      timelineData.initialize(
        traces,
        undefined,
        TimestampConverterUtils.TIMESTAMP_CONVERTER,
      );
      expect(timelineData.getPreviousEntryFor(dumpWm)).toBeUndefined();
      expect(timelineData.getNextEntryFor(dumpWm)).toBeUndefined();
    });
  });

  it('drops empty trace', () => {
    timelineData.initialize(
      traces,
      undefined,
      TimestampConverterUtils.TIMESTAMP_CONVERTER,
    );
    expect(
      timelineData.getTraces().getTrace(TraceType.TRANSACTIONS),
    ).toBeUndefined();
  });

  it('sets first entry as that with valid timestamp', async () => {
    const traces = new TracesBuilder()
      .setTimestamps(TraceType.TRANSITION, [timestamp0, timestamp9])
      .setTimestamps(TraceType.SURFACE_FLINGER, [timestamp9, timestamp10])
      .build();
    await timelineData.initialize(
      traces,
      undefined,
      TimestampConverterUtils.TIMESTAMP_CONVERTER,
    );
    expect(timelineData.getFullTimeRange().from).toEqual(timestamp9);
  });

  it('uses first entry of first active trace by default', () => {
    timelineData.initialize(
      traces,
      undefined,
      TimestampConverterUtils.TIMESTAMP_CONVERTER,
    );
    expect(timelineData.getCurrentPosition()).toEqual(position10);
  });

  it('uses explicit position if set', () => {
    timelineData.initialize(
      traces,
      undefined,
      TimestampConverterUtils.TIMESTAMP_CONVERTER,
    );
    expect(timelineData.getCurrentPosition()).toEqual(position10);

    timelineData.setPosition(position1000);
    expect(timelineData.getCurrentPosition()).toEqual(position1000);

    timelineData.trySetActiveTrace(traceSf);
    expect(timelineData.getCurrentPosition()).toEqual(position1000);

    timelineData.trySetActiveTrace(traceWm);
    expect(timelineData.getCurrentPosition()).toEqual(position1000);
  });

  it('sets active trace and update current position accordingly', () => {
    timelineData.initialize(
      traces,
      undefined,
      TimestampConverterUtils.TIMESTAMP_CONVERTER,
    );

    expect(timelineData.getCurrentPosition()).toEqual(position10);

    timelineData.trySetActiveTrace(traceWm);
    expect(timelineData.getCurrentPosition()).toEqual(position11);

    timelineData.trySetActiveTrace(traceSf);
    expect(timelineData.getCurrentPosition()).toEqual(position10);
  });

  it('does not set active trace if not present in timeline, or already set', () => {
    timelineData.initialize(
      traces,
      undefined,
      TimestampConverterUtils.TIMESTAMP_CONVERTER,
    );

    expect(timelineData.getCurrentPosition()).toEqual(position10);

    let success = timelineData.trySetActiveTrace(traceWm);
    expect(timelineData.getActiveTrace()).toEqual(traceWm);
    expect(success).toBeTrue();

    success = timelineData.trySetActiveTrace(traceWm);
    expect(timelineData.getActiveTrace()).toEqual(traceWm);
    expect(success).toBeFalse();

    success = timelineData.trySetActiveTrace(
      new TraceBuilder<{}>()
        .setType(TraceType.SURFACE_FLINGER)
        .setEntries([])
        .build(),
    );
    expect(timelineData.getActiveTrace()).toEqual(traceWm);
    expect(success).toBeFalse();
  });

  it('hasTimestamps()', () => {
    expect(timelineData.hasTimestamps()).toBeFalse();

    // no trace
    {
      const traces = new TracesBuilder().build();
      timelineData.initialize(
        traces,
        undefined,
        TimestampConverterUtils.TIMESTAMP_CONVERTER,
      );
      expect(timelineData.hasTimestamps()).toBeFalse();
    }
    // trace without timestamps
    {
      const traces = new TracesBuilder()
        .setTimestamps(TraceType.SURFACE_FLINGER, [])
        .build();
      timelineData.initialize(
        traces,
        undefined,
        TimestampConverterUtils.TIMESTAMP_CONVERTER,
      );
      expect(timelineData.hasTimestamps()).toBeFalse();
    }
    // trace with timestamps
    {
      const traces = new TracesBuilder()
        .setTimestamps(TraceType.SURFACE_FLINGER, [timestamp10])
        .build();
      timelineData.initialize(
        traces,
        undefined,
        TimestampConverterUtils.TIMESTAMP_CONVERTER,
      );
      expect(timelineData.hasTimestamps()).toBeTrue();
    }
  });

  it('hasMoreThanOneDistinctTimestamp()', () => {
    expect(timelineData.hasMoreThanOneDistinctTimestamp()).toBeFalse();

    // no trace
    {
      const traces = new TracesBuilder().build();
      timelineData.initialize(
        traces,
        undefined,
        TimestampConverterUtils.TIMESTAMP_CONVERTER,
      );
      expect(timelineData.hasMoreThanOneDistinctTimestamp()).toBeFalse();
    }
    // no distinct timestamps
    {
      const traces = new TracesBuilder()
        .setTimestamps(TraceType.SURFACE_FLINGER, [timestamp10])
        .setTimestamps(TraceType.WINDOW_MANAGER, [timestamp10])
        .build();
      timelineData.initialize(
        traces,
        undefined,
        TimestampConverterUtils.TIMESTAMP_CONVERTER,
      );
      expect(timelineData.hasMoreThanOneDistinctTimestamp()).toBeFalse();
    }
    // distinct timestamps
    {
      const traces = new TracesBuilder()
        .setTimestamps(TraceType.SURFACE_FLINGER, [timestamp10])
        .setTimestamps(TraceType.WINDOW_MANAGER, [timestamp11])
        .build();
      timelineData.initialize(
        traces,
        undefined,
        TimestampConverterUtils.TIMESTAMP_CONVERTER,
      );
      expect(timelineData.hasMoreThanOneDistinctTimestamp()).toBeTrue();
    }
  });

  it('getCurrentPosition() returns same object if no change to range', () => {
    timelineData.initialize(
      traces,
      undefined,
      TimestampConverterUtils.TIMESTAMP_CONVERTER,
    );

    expect(timelineData.getCurrentPosition()).toBe(
      timelineData.getCurrentPosition(),
    );

    timelineData.setPosition(position11);

    expect(timelineData.getCurrentPosition()).toBe(
      timelineData.getCurrentPosition(),
    );
  });

  it('makePositionFromActiveTrace()', () => {
    timelineData.initialize(
      traces,
      undefined,
      TimestampConverterUtils.TIMESTAMP_CONVERTER,
    );
    const time100 = TimestampConverterUtils.makeRealTimestamp(100n);

    {
      timelineData.trySetActiveTrace(traceSf);
      const position = timelineData.makePositionFromActiveTrace(time100);
      expect(position.timestamp).toEqual(time100);
      expect(position.entry).toEqual(traceSf.getEntry(0));
    }

    {
      timelineData.trySetActiveTrace(traceWm);
      const position = timelineData.makePositionFromActiveTrace(time100);
      expect(position.timestamp).toEqual(time100);
      expect(position.entry).toEqual(traceWm.getEntry(0));
    }
  });

  it('getFullTimeRange() returns same object if no change to range', () => {
    timelineData.initialize(
      traces,
      undefined,
      TimestampConverterUtils.TIMESTAMP_CONVERTER,
    );

    expect(timelineData.getFullTimeRange()).toBe(
      timelineData.getFullTimeRange(),
    );
  });

  it('getSelectionTimeRange() returns same object if no change to range', () => {
    timelineData.initialize(
      traces,
      undefined,
      TimestampConverterUtils.TIMESTAMP_CONVERTER,
    );

    expect(timelineData.getSelectionTimeRange()).toBe(
      timelineData.getSelectionTimeRange(),
    );

    timelineData.setSelectionTimeRange(new TimeRange(timestamp0, timestamp5));

    expect(timelineData.getSelectionTimeRange()).toBe(
      timelineData.getSelectionTimeRange(),
    );
  });

  it('getZoomRange() returns same object if no change to range', () => {
    timelineData.initialize(
      traces,
      undefined,
      TimestampConverterUtils.TIMESTAMP_CONVERTER,
    );

    expect(timelineData.getZoomRange()).toBe(timelineData.getZoomRange());

    timelineData.setZoom(new TimeRange(timestamp0, timestamp5));

    expect(timelineData.getZoomRange()).toBe(timelineData.getZoomRange());
  });

  it("getCurrentPosition() prioritizes active trace's first entry", () => {
    timelineData.initialize(
      traces,
      undefined,
      TimestampConverterUtils.TIMESTAMP_CONVERTER,
    );
    timelineData.trySetActiveTrace(traceWm);

    expect(timelineData.getCurrentPosition()?.timestamp).toBe(timestamp11);
  });

  it('updates corrupted state of transition trace', async () => {
    const traces = new Traces();
    const trace = new TraceBuilder<PropertyTreeNode>()
      .setType(TraceType.TRANSITION)
      .setParser(
        new ParserBuilder<PropertyTreeNode>()
          .setIsCorrupted(true)
          .setEntries([
            new PropertyTreeBuilder()
              .setRootId('TransitionsTraceEntry')
              .setName('transition')
              .build(),
          ])
          .setTimestamps([timestamp9])
          .build(),
      )
      .build();
    traces.addTrace(trace);

    expectAsync(
      timelineData.initialize(
        traces,
        undefined,
        TimestampConverterUtils.TIMESTAMP_CONVERTER,
      ),
    ).toBeRejected();

    try {
      await timelineData.initialize(
        traces,
        undefined,
        TimestampConverterUtils.TIMESTAMP_CONVERTER,
      );
    } catch {
      expect(trace.isCorrupted()).toBeTrue();
      expect(trace.getCorruptedReason()).toEqual(
        'Cannot parse all transitions.',
      );
    }
  });
});
