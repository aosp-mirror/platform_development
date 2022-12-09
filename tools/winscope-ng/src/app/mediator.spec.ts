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
import {Mediator} from "./mediator";
import {UnitTestUtils} from "test/unit/utils";
import {ViewerFactory} from "viewers/viewer_factory";
import {ViewerStub} from "viewers/viewer_stub";
import {TimelineData} from "./timeline_data";
import {TraceData} from "./trace_data";
import {MockStorage} from "test/unit/mock_storage";

class TimelineComponentStub {
  onCurrentTimestampChanged(timestamp: Timestamp|undefined) {
    // do nothing
  }
}

describe("Mediator", () => {
  const viewerStub = new ViewerStub("Title");
  let timelineComponent: TimelineComponentStub;
  let traceData: TraceData;
  let timelineData: TimelineData;
  let mediator: Mediator;

  beforeEach(async () => {
    timelineComponent = new TimelineComponentStub();
    traceData = new TraceData();
    timelineData = new TimelineData();
    mediator = new Mediator(traceData, timelineData);
    mediator.setNotifyCurrentTimestampChangedToTimelineComponentCallback(timestamp => {
      timelineComponent.onCurrentTimestampChanged(timestamp);
    });

    spyOn(ViewerFactory.prototype, "createViewers").and.returnValue([viewerStub]);
  });

  it("initializes TimelineData on data load event", async () => {
    spyOn(timelineData, "initialize").and.callThrough();

    await loadTraces();
    expect(timelineData.initialize).toHaveBeenCalledTimes(0);

    mediator.onTraceDataLoaded(new MockStorage());
    expect(timelineData.initialize).toHaveBeenCalledTimes(1);
  });


  it("it creates viewers on data load event", async () => {
    spyOn(viewerStub, "notifyCurrentTraceEntries");

    await loadTraces();
    expect(mediator.getViewers()).toEqual([]);

    mediator.onTraceDataLoaded(new MockStorage());
    expect(mediator.getViewers()).toEqual([viewerStub]);

    // notifies viewer about current timestamp on creation
    expect(viewerStub.notifyCurrentTraceEntries).toHaveBeenCalledTimes(1);
  });

  it("forwards timestamp changed events/notifications", async () => {
    const timestamp10 = new Timestamp(TimestampType.REAL, 10n);
    const timestamp11 = new Timestamp(TimestampType.REAL, 11n);

    await loadTraces();
    mediator.onTraceDataLoaded(new MockStorage());
    expect(mediator.getViewers()).toEqual([viewerStub]);

    spyOn(viewerStub, "notifyCurrentTraceEntries");
    spyOn(timelineComponent, "onCurrentTimestampChanged");
    expect(viewerStub.notifyCurrentTraceEntries).toHaveBeenCalledTimes(0);
    expect(timelineComponent.onCurrentTimestampChanged).toHaveBeenCalledTimes(0);

    // notify timestamp
    timelineData.setCurrentTimestamp(timestamp10);
    expect(viewerStub.notifyCurrentTraceEntries).toHaveBeenCalledTimes(1);
    expect(timelineComponent.onCurrentTimestampChanged).toHaveBeenCalledTimes(1);

    // notify timestamp again (no timestamp change)
    timelineData.setCurrentTimestamp(timestamp10);
    expect(viewerStub.notifyCurrentTraceEntries).toHaveBeenCalledTimes(1);
    expect(timelineComponent.onCurrentTimestampChanged).toHaveBeenCalledTimes(1);

    // reset back to the default timestamp should trigger a change
    timelineData.setCurrentTimestamp(timestamp11);
    expect(viewerStub.notifyCurrentTraceEntries).toHaveBeenCalledTimes(2);
    expect(timelineComponent.onCurrentTimestampChanged).toHaveBeenCalledTimes(2);
  });

  const loadTraces = async () => {
    const traces = [
      await UnitTestUtils.getFixtureFile("traces/elapsed_and_real_timestamp/SurfaceFlinger.pb"),
      await UnitTestUtils.getFixtureFile("traces/elapsed_and_real_timestamp/WindowManager.pb"),
      await UnitTestUtils.getFixtureFile("traces/elapsed_and_real_timestamp/screen_recording_metadata_v2.mp4"),
    ];
    const errors = await traceData.loadTraces(traces);
  };
});
