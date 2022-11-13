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
import {Timeline, TimelineCoordinator, TimestampChangeObserver} from "./timeline_coordinator";

class TimestampChangeObserverStub implements TimestampChangeObserver {
  onCurrentTimestampChanged(timestamp: Timestamp): void {
    // do nothing - function meant to be spied
  }
}

describe("TimelineCoordinator", () => {
  let coordinator: TimelineCoordinator;
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
    coordinator = new TimelineCoordinator();
    observer = new TimestampChangeObserverStub();
    coordinator.registerObserver(observer);
  });

  it("sets timelines", () => {
    expect(coordinator.currentTimestamp).toBeUndefined();

    coordinator.setTimelines(timelines);
    expect(coordinator.currentTimestamp).toEqual(timestamp10);
  });

  it("removes timeline", () => {
    coordinator.setTimelines(timelines);
    expect(coordinator.currentTimestamp).toEqual(timestamp10);

    coordinator.removeTimeline(TraceType.SURFACE_FLINGER);
    expect(coordinator.currentTimestamp).toEqual(timestamp11);

    coordinator.removeTimeline(TraceType.WINDOW_MANAGER);
    expect(coordinator.currentTimestamp).toBeUndefined();
  });

  it("removes timeline even if currently active", () => {
    coordinator.setTimelines(timelines);
    coordinator.setActiveTraceTypes([TraceType.WINDOW_MANAGER]);
    coordinator.removeTimeline(TraceType.WINDOW_MANAGER);
    expect(coordinator.currentTimestamp).toEqual(timestamp10);
  });

  it("sets active trace types and update timestamp", () => {
    coordinator.setTimelines(timelines);

    coordinator.setActiveTraceTypes([]);
    expect(coordinator.currentTimestamp).toEqual(timestamp10);

    coordinator.setActiveTraceTypes([TraceType.WINDOW_MANAGER]);
    expect(coordinator.currentTimestamp).toEqual(timestamp11);

    coordinator.setActiveTraceTypes([TraceType.SURFACE_FLINGER]);
    expect(coordinator.currentTimestamp).toEqual(timestamp10);

    coordinator.setActiveTraceTypes([TraceType.SURFACE_FLINGER, TraceType.WINDOW_MANAGER]);
    expect(coordinator.currentTimestamp).toEqual(timestamp10);
  });

  it("sets/gets active trace types", () => {
    coordinator.setTimelines(timelines);
    expect(coordinator.getActiveTraceTypes()).toEqual([]);

    coordinator.setActiveTraceTypes([]);
    expect(coordinator.getActiveTraceTypes()).toEqual([]);

    coordinator.setActiveTraceTypes([TraceType.WINDOW_MANAGER]);
    expect(coordinator.getActiveTraceTypes()).toEqual([TraceType.WINDOW_MANAGER]);

    coordinator.setActiveTraceTypes([TraceType.SURFACE_FLINGER, TraceType.WINDOW_MANAGER]);
    expect(coordinator.getActiveTraceTypes()).toEqual([
      TraceType.SURFACE_FLINGER,
      TraceType.WINDOW_MANAGER
    ]);

    coordinator.removeTimeline(TraceType.SURFACE_FLINGER);
    expect(coordinator.getActiveTraceTypes()).toEqual([TraceType.WINDOW_MANAGER]);

    coordinator.removeTimeline(TraceType.WINDOW_MANAGER);
    expect(coordinator.getActiveTraceTypes()).toEqual([]);
  });

  it("notifies observers when current timestamp changes", () => {
    spyOn(observer, "onCurrentTimestampChanged");
    expect(observer.onCurrentTimestampChanged).toHaveBeenCalledTimes(0);

    coordinator.setTimelines(timelines);
    expect(observer.onCurrentTimestampChanged).toHaveBeenCalledTimes(1);

    coordinator.setActiveTraceTypes([TraceType.WINDOW_MANAGER]);
    expect(observer.onCurrentTimestampChanged).toHaveBeenCalledTimes(2);

    coordinator.removeTimeline(TraceType.WINDOW_MANAGER);
    expect(observer.onCurrentTimestampChanged).toHaveBeenCalledTimes(3);
  });

  it("doesn't notify observers when current timestamp doesn't change", () => {
    coordinator.setTimelines(timelines);

    spyOn(observer, "onCurrentTimestampChanged");
    expect(observer.onCurrentTimestampChanged).toHaveBeenCalledTimes(0);

    coordinator.setActiveTraceTypes([TraceType.SURFACE_FLINGER]);
    expect(observer.onCurrentTimestampChanged).toHaveBeenCalledTimes(0);

    coordinator.setActiveTraceTypes([TraceType.SURFACE_FLINGER, TraceType.WINDOW_MANAGER]);
    expect(observer.onCurrentTimestampChanged).toHaveBeenCalledTimes(0);

    coordinator.removeTimeline(TraceType.WINDOW_MANAGER);
    expect(observer.onCurrentTimestampChanged).toHaveBeenCalledTimes(0);
  });

  it("uses first timestamp if no active trace or timestamp is specified", () => {
    coordinator.setTimelines(timelines);
    expect(coordinator.currentTimestamp?.getValueNs()).toEqual(10n);
  });

  it("sets first timestamp of active trace if no timestamp is specified", () => {
    coordinator.setTimelines(timelines);
    coordinator.setActiveTraceTypes([TraceType.WINDOW_MANAGER]);
    expect(coordinator.currentTimestamp?.getValueNs()).toEqual(11n);
  });
});
