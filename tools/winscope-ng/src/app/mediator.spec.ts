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
import {Mediator} from "./mediator";
import {UnitTestUtils} from "test/unit/utils";
import {ViewerFactory} from "viewers/viewer_factory";
import {ViewerStub} from "viewers/viewer_stub";
import { TimelineData } from "./timeline_data";
import { MockStorage } from "test/unit/mock_storage";

//TODO: uncomment/fix tests
/*
describe("Mediator", () => {
  let mediator: Mediator;
  let timelineData: TimelineData;

  beforeEach(async () => {
    spyOn(TimelineData.prototype, "setScreenRecordingData").and.callThrough();
    spyOn(TimelineData.prototype, "removeScreenRecordingData").and.callThrough();
    timelineData = new TimelineData();
    mediator = new Mediator(timelineData);
  });

  it("can create viewers and notify current trace entries", async () => {
    const viewerStub = new ViewerStub("Title");

    spyOn(ViewerFactory.prototype, "createViewers").and.returnValue([viewerStub]);
    spyOn(viewerStub, "notifyCurrentTraceEntries");

    const traces = [
      await UnitTestUtils.getFixtureFile(
        "traces/elapsed_and_real_timestamp/SurfaceFlinger.pb"),
      await UnitTestUtils.getFixtureFile(
        "traces/elapsed_and_real_timestamp/WindowManager.pb"),
      // trace file with no entries for some more robustness checks
      await UnitTestUtils.getFixtureFile(
        "traces/no_entries_InputMethodClients.pb")
    ];
    await mediator.setTraces(traces);

    // create viewers (mocked factory)
    expect(mediator.getViewers()).toEqual([]);
    mediator.createViewers(new MockStorage());
    expect(mediator.getViewers()).toEqual([viewerStub]);

    // Gets notified of the current timestamp on creation
    expect(viewerStub.notifyCurrentTraceEntries).toHaveBeenCalledTimes(1);

    // When we update to an undefined timestamp we reset to the default selected
    // timestamp based on the active trace and loaded timelines. Given that
    // we haven't set a timestamp we should still be in the default timestamp
    // and require no update to the current trace entries.
    timelineData.updateCurrentTimestamp(undefined);
    expect(viewerStub.notifyCurrentTraceEntries).toHaveBeenCalledTimes(1);

    // notify timestamp
    const timestamp = new Timestamp(TimestampType.REAL, 14500282843n);
    expect(timelineData.getTimestampType()).toBe(TimestampType.REAL);
    timelineData.updateCurrentTimestamp(timestamp);
    expect(viewerStub.notifyCurrentTraceEntries).toHaveBeenCalledTimes(2);

    // notify timestamp again
    timelineData.updateCurrentTimestamp(timestamp);
    expect(viewerStub.notifyCurrentTraceEntries).toHaveBeenCalledTimes(2);

    // reset back to the default timestamp should trigger a change
    timelineData.updateCurrentTimestamp(undefined);
    expect(viewerStub.notifyCurrentTraceEntries).toHaveBeenCalledTimes(3);
  });

  it("sets video data on timelineData when screenrecording is loaded", async () => {
    expect(mediator.getParsers().length).toEqual(0);
    const traces = [
      await UnitTestUtils.getFixtureFile("traces/elapsed_and_real_timestamp/SurfaceFlinger.pb"),
      await UnitTestUtils.getFixtureFile("traces/elapsed_and_real_timestamp/WindowManager.pb"),
      await UnitTestUtils.getFixtureFile("traces/elapsed_and_real_timestamp/screen_recording_metadata_v2.mp4"),
    ];
    const errors = await mediator.setTraces(traces);
    expect(mediator.getParsers().length).toEqual(3);
    expect(errors.length).toEqual(0);

    expect(timelineData.setScreenRecordingData).toHaveBeenCalledTimes(1);
  });

  it("video data is removed if video trace is deleted", async () => {
    expect(mediator.getParsers().length).toEqual(0);
    const traces = [
      await UnitTestUtils.getFixtureFile("traces/elapsed_and_real_timestamp/SurfaceFlinger.pb"),
      await UnitTestUtils.getFixtureFile("traces/elapsed_and_real_timestamp/WindowManager.pb"),
      await UnitTestUtils.getFixtureFile("traces/elapsed_and_real_timestamp/screen_recording_metadata_v2.mp4"),
    ];
    const errors = await mediator.setTraces(traces);
    expect(mediator.getParsers().length).toEqual(3);
    expect(errors.length).toEqual(0);
    expect(mediator.getParserFor(TraceType.SCREEN_RECORDING))
      .withContext("Should have screen recording parser").toBeDefined();
    expect(timelineData.getTimelines().keys())
      .withContext("Should have screen recording timeline").toContain(TraceType.SCREEN_RECORDING);

    expect(timelineData.setScreenRecordingData).toHaveBeenCalledTimes(1);
    expect(timelineData.getVideoData()).withContext("Should have video data").toBeDefined();
    expect(timelineData.timestampAsElapsedScreenrecordingSeconds(
      new Timestamp(TimestampType.REAL, 1666361049372271045n)))
      .withContext("Should be able to covert timestamp to video seconds").toBeDefined();

    mediator.removeTrace(TraceType.SCREEN_RECORDING);

    expect(timelineData.removeScreenRecordingData).toHaveBeenCalledTimes(1);
    expect(timelineData.getVideoData()).withContext("Should no longer have video data").toBeUndefined();
    expect(() => {
      timelineData.timestampAsElapsedScreenrecordingSeconds(new Timestamp(TimestampType.REAL, 1666361049372271045n))
    }).toThrow(new Error("No timeline for requested trace type 3"));
  });
});
*/
