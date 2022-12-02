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

import {Timestamp, TimestampType} from "common/trace/timestamp";
import {TraceType} from "common/trace/trace_type";
import {TimelineData} from "./timeline_data";

//TODO
/*
describe("TimelineData", () => {
  let timelineData: TimelineData;
  let observer: TimestampChangeObserver;

  const timestamp10 = new Timestamp(TimestampType.REAL, 10n);
  const timestamp11 = new Timestamp(TimestampType.REAL, 11n);

  const timelines: Timeline[] = [{
    traceType: TraceType.SURFACE_FLINGER,
    timestamps: [timestamp10]
  },
  {
    traceType: TraceType.WINDOW_MANAGER,
    timestamps: [timestamp11]
  }];

  beforeEach(() => {
    timelineData = new TimelineData();
    observer = new TimestampChangeObserverStub();
    timelineData.registerObserver(observer);
  });

  it("sets timelines", () => {
    expect(timelineData.currentTimestamp).toBeUndefined();

    timelineData.setTimelines(timelines);
    expect(timelineData.currentTimestamp).toEqual(timestamp10);
  });

  it("removes timeline", () => {
    timelineData.setTimelines(timelines);
    expect(timelineData.currentTimestamp).toEqual(timestamp10);

    timelineData.removeTimeline(TraceType.SURFACE_FLINGER);
    expect(timelineData.currentTimestamp).toEqual(timestamp11);

    timelineData.removeTimeline(TraceType.WINDOW_MANAGER);
    expect(timelineData.currentTimestamp).toBeUndefined();
  });

  it("removes timeline even if currently active", () => {
    timelineData.setTimelines(timelines);
    timelineData.setActiveTraceTypes([TraceType.WINDOW_MANAGER]);
    timelineData.removeTimeline(TraceType.WINDOW_MANAGER);
    expect(timelineData.currentTimestamp).toEqual(timestamp10);
  });

  it("sets active trace types and update timestamp", () => {
    timelineData.setTimelines(timelines);

    timelineData.setActiveTraceTypes([]);
    expect(timelineData.currentTimestamp).toEqual(timestamp10);

    timelineData.setActiveTraceTypes([TraceType.WINDOW_MANAGER]);
    expect(timelineData.currentTimestamp).toEqual(timestamp11);

    timelineData.setActiveTraceTypes([TraceType.SURFACE_FLINGER]);
    expect(timelineData.currentTimestamp).toEqual(timestamp10);

    timelineData.setActiveTraceTypes([TraceType.SURFACE_FLINGER, TraceType.WINDOW_MANAGER]);
    expect(timelineData.currentTimestamp).toEqual(timestamp10);
  });

  it("sets/gets active trace types", () => {
    timelineData.setTimelines(timelines);
    expect(timelineData.getActiveTraceTypes()).toEqual([]);

    timelineData.setActiveTraceTypes([]);
    expect(timelineData.getActiveTraceTypes()).toEqual([]);

    timelineData.setActiveTraceTypes([TraceType.WINDOW_MANAGER]);
    expect(timelineData.getActiveTraceTypes()).toEqual([TraceType.WINDOW_MANAGER]);

    timelineData.setActiveTraceTypes([TraceType.SURFACE_FLINGER, TraceType.WINDOW_MANAGER]);
    expect(timelineData.getActiveTraceTypes()).toEqual([
      TraceType.SURFACE_FLINGER,
      TraceType.WINDOW_MANAGER
    ]);

    timelineData.removeTimeline(TraceType.SURFACE_FLINGER);
    expect(timelineData.getActiveTraceTypes()).toEqual([TraceType.WINDOW_MANAGER]);

    timelineData.removeTimeline(TraceType.WINDOW_MANAGER);
    expect(timelineData.getActiveTraceTypes()).toEqual([]);
  });

  it("notifies observers when current timestamp changes", () => {
    spyOn(observer, "onCurrentTimestampChanged");
    expect(observer.onCurrentTimestampChanged).toHaveBeenCalledTimes(0);

    timelineData.setTimelines(timelines);
    expect(observer.onCurrentTimestampChanged).toHaveBeenCalledTimes(1);

    timelineData.setActiveTraceTypes([TraceType.WINDOW_MANAGER]);
    expect(observer.onCurrentTimestampChanged).toHaveBeenCalledTimes(2);

    timelineData.removeTimeline(TraceType.WINDOW_MANAGER);
    expect(observer.onCurrentTimestampChanged).toHaveBeenCalledTimes(3);
  });

  it("doesn't notify observers when current timestamp doesn't change", () => {
    timelineData.setTimelines(timelines);

    spyOn(observer, "onCurrentTimestampChanged");
    expect(observer.onCurrentTimestampChanged).toHaveBeenCalledTimes(0);

    timelineData.setActiveTraceTypes([TraceType.SURFACE_FLINGER]);
    expect(observer.onCurrentTimestampChanged).toHaveBeenCalledTimes(0);

    timelineData.setActiveTraceTypes([TraceType.SURFACE_FLINGER, TraceType.WINDOW_MANAGER]);
    expect(observer.onCurrentTimestampChanged).toHaveBeenCalledTimes(0);

    timelineData.removeTimeline(TraceType.WINDOW_MANAGER);
    expect(observer.onCurrentTimestampChanged).toHaveBeenCalledTimes(0);
  });

  it("uses first timestamp if no active trace or timestamp is specified", () => {
    timelineData.setTimelines(timelines);
    expect(timelineData.currentTimestamp?.getValueNs()).toEqual(10n);
  });

  it("sets first timestamp of active trace if no timestamp is specified", () => {
    timelineData.setTimelines(timelines);
    timelineData.setActiveTraceTypes([TraceType.WINDOW_MANAGER]);
    expect(timelineData.currentTimestamp?.getValueNs()).toEqual(11n);
  });
});
*/
