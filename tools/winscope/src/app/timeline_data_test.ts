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

import {Timestamp, TimestampType} from 'trace/timestamp';
import {TraceType} from 'trace/trace_type';
import {TimelineData} from './timeline_data';
import {Timeline} from './trace_data';

class TimestampChangedObserver {
  onCurrentTimestampChanged(timestamp: Timestamp | undefined) {
    // do nothing
  }
}

describe('TimelineData', () => {
  let timelineData: TimelineData;
  const timestampChangedObserver = new TimestampChangedObserver();

  const timestamp10 = new Timestamp(TimestampType.REAL, 10n);
  const timestamp11 = new Timestamp(TimestampType.REAL, 11n);

  const timelines: Timeline[] = [
    {
      traceType: TraceType.SURFACE_FLINGER,
      timestamps: [timestamp10],
    },
    {
      traceType: TraceType.WINDOW_MANAGER,
      timestamps: [timestamp11],
    },
  ];

  beforeEach(() => {
    timelineData = new TimelineData();
    timelineData.setOnCurrentTimestampChanged((timestamp) => {
      timestampChangedObserver.onCurrentTimestampChanged(timestamp);
    });
  });

  it('sets timelines', () => {
    expect(timelineData.getCurrentTimestamp()).toBeUndefined();

    timelineData.initialize(timelines, undefined);
    expect(timelineData.getCurrentTimestamp()).toEqual(timestamp10);
  });

  it('uses first timestamp by default', () => {
    timelineData.initialize(timelines, undefined);
    expect(timelineData.getCurrentTimestamp()?.getValueNs()).toEqual(10n);
  });

  it('uses explicit timestamp if set', () => {
    timelineData.initialize(timelines, undefined);
    expect(timelineData.getCurrentTimestamp()?.getValueNs()).toEqual(10n);

    const explicitTimestamp = new Timestamp(TimestampType.REAL, 1000n);
    timelineData.setCurrentTimestamp(explicitTimestamp);
    expect(timelineData.getCurrentTimestamp()).toEqual(explicitTimestamp);

    timelineData.setActiveViewTraceTypes([TraceType.WINDOW_MANAGER]);
    expect(timelineData.getCurrentTimestamp()).toEqual(explicitTimestamp);
  });

  it('sets active trace types and update current timestamp accordingly', () => {
    timelineData.initialize(timelines, undefined);

    timelineData.setActiveViewTraceTypes([]);
    expect(timelineData.getCurrentTimestamp()).toEqual(timestamp10);

    timelineData.setActiveViewTraceTypes([TraceType.WINDOW_MANAGER]);
    expect(timelineData.getCurrentTimestamp()).toEqual(timestamp11);

    timelineData.setActiveViewTraceTypes([TraceType.SURFACE_FLINGER]);
    expect(timelineData.getCurrentTimestamp()).toEqual(timestamp10);

    timelineData.setActiveViewTraceTypes([TraceType.SURFACE_FLINGER, TraceType.WINDOW_MANAGER]);
    expect(timelineData.getCurrentTimestamp()).toEqual(timestamp10);
  });

  it('notifies callback when current timestamp changes', () => {
    spyOn(timestampChangedObserver, 'onCurrentTimestampChanged');
    expect(timestampChangedObserver.onCurrentTimestampChanged).toHaveBeenCalledTimes(0);

    timelineData.initialize(timelines, undefined);
    expect(timestampChangedObserver.onCurrentTimestampChanged).toHaveBeenCalledTimes(1);

    timelineData.setActiveViewTraceTypes([TraceType.WINDOW_MANAGER]);
    expect(timestampChangedObserver.onCurrentTimestampChanged).toHaveBeenCalledTimes(2);
  });

  it("doesn't notify observers when current timestamp doesn't change", () => {
    timelineData.initialize(timelines, undefined);

    spyOn(timestampChangedObserver, 'onCurrentTimestampChanged');
    expect(timestampChangedObserver.onCurrentTimestampChanged).toHaveBeenCalledTimes(0);

    timelineData.setActiveViewTraceTypes([TraceType.SURFACE_FLINGER]);
    expect(timestampChangedObserver.onCurrentTimestampChanged).toHaveBeenCalledTimes(0);

    timelineData.setActiveViewTraceTypes([TraceType.SURFACE_FLINGER, TraceType.WINDOW_MANAGER]);
    expect(timestampChangedObserver.onCurrentTimestampChanged).toHaveBeenCalledTimes(0);
  });

  it('hasTimestamps()', () => {
    expect(timelineData.hasTimestamps()).toBeFalse();

    timelineData.initialize([], undefined);
    expect(timelineData.hasTimestamps()).toBeFalse();

    timelineData.initialize(
      [
        {
          traceType: TraceType.SURFACE_FLINGER,
          timestamps: [],
        },
      ],
      undefined
    );
    expect(timelineData.hasTimestamps()).toBeFalse();

    timelineData.initialize(
      [
        {
          traceType: TraceType.SURFACE_FLINGER,
          timestamps: [new Timestamp(TimestampType.REAL, 10n)],
        },
      ],
      undefined
    );
    expect(timelineData.hasTimestamps()).toBeTrue();
  });

  it('hasMoreThanOneDistinctTimestamp()', () => {
    expect(timelineData.hasMoreThanOneDistinctTimestamp()).toBeFalse();

    timelineData.initialize([], undefined);
    expect(timelineData.hasMoreThanOneDistinctTimestamp()).toBeFalse();

    timelineData.initialize(
      [
        {
          traceType: TraceType.SURFACE_FLINGER,
          timestamps: [new Timestamp(TimestampType.REAL, 10n)],
        },
        {
          traceType: TraceType.WINDOW_MANAGER,
          timestamps: [new Timestamp(TimestampType.REAL, 10n)],
        },
      ],
      undefined
    );
    expect(timelineData.hasMoreThanOneDistinctTimestamp()).toBeFalse();

    timelineData.initialize(
      [
        {
          traceType: TraceType.SURFACE_FLINGER,
          timestamps: [new Timestamp(TimestampType.REAL, 10n)],
        },
        {
          traceType: TraceType.WINDOW_MANAGER,
          timestamps: [new Timestamp(TimestampType.REAL, 11n)],
        },
      ],
      undefined
    );
    expect(timelineData.hasMoreThanOneDistinctTimestamp()).toBeTrue();
  });
});
