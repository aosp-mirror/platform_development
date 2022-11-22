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
import {TraceCoordinator} from "./trace_coordinator";
import {UnitTestUtils} from "test/unit/utils";
import {ViewerFactory} from "viewers/viewer_factory";
import {ViewerStub} from "viewers/viewer_stub";
import { TimelineCoordinator } from "./timeline_coordinator";

describe("TraceCoordinator", () => {
  let traceCoordinator: TraceCoordinator;
  let timelineCoordinator: TimelineCoordinator;

  beforeEach(async () => {
    spyOn(TimelineCoordinator.prototype, "setScreenRecordingData").and.callThrough();
    spyOn(TimelineCoordinator.prototype, "removeScreenRecordingData").and.callThrough();
    timelineCoordinator = new TimelineCoordinator();
    traceCoordinator = new TraceCoordinator(timelineCoordinator);
  });

  it("processes trace files", async () => {
    expect(traceCoordinator.getParsers().length).toEqual(0);
    const traces = [
      await UnitTestUtils.getFixtureFile("traces/elapsed_and_real_timestamp/dump_SurfaceFlinger.pb"),
      await UnitTestUtils.getFixtureFile("traces/dump_WindowManager.pb"),
    ];
    const errors = await traceCoordinator.setTraces(traces);
    expect(traceCoordinator.getParsers().length).toEqual(2);
    expect(errors.length).toEqual(0);
  });

  it("it is robust to invalid trace files", async () => {
    expect(traceCoordinator.getParsers().length).toEqual(0);
    const traces = [
      await UnitTestUtils.getFixtureFile("winscope_homepage.png"),
    ];
    const errors = await traceCoordinator.setTraces(traces);
    expect(traceCoordinator.getParsers().length).toEqual(0);
    expect(errors.length).toEqual(1);
  });

  it("is robust to trace files with no entries", async () => {
    const traces = [
      await UnitTestUtils.getFixtureFile(
        "traces/no_entries_InputMethodClients.pb")
    ];
    await traceCoordinator.setTraces(traces);

    let timestamp = new Timestamp(TimestampType.REAL, 0n);
    timelineCoordinator.updateCurrentTimestamp(timestamp);

    timestamp = new Timestamp(TimestampType.ELAPSED, 0n);
    timelineCoordinator.updateCurrentTimestamp(timestamp);
  });

  it("processes mixed valid and invalid trace files", async () => {
    expect(traceCoordinator.getParsers().length).toEqual(0);
    const traces = [
      await UnitTestUtils.getFixtureFile("winscope_homepage.png"),
      await UnitTestUtils.getFixtureFile("traces/dump_WindowManager.pb"),
    ];
    const errors = await traceCoordinator.setTraces(traces);
    expect(traceCoordinator.getParsers().length).toEqual(1);
    expect(errors.length).toEqual(1);
  });

  it("can remove traces", async () => {
    expect(traceCoordinator.getParsers().length).toEqual(0);
    const traces = [
      await UnitTestUtils.getFixtureFile("traces/elapsed_and_real_timestamp/dump_SurfaceFlinger.pb"),
    ];
    await traceCoordinator.setTraces(traces);
    expect(traceCoordinator.getParsers().length).toEqual(1);

    traceCoordinator.removeTrace(TraceType.SURFACE_FLINGER);
    expect(traceCoordinator.getParsers().length).toEqual(0);
  });

  it("can find a parser based on trace type", async () => {
    const traces = [
      await UnitTestUtils.getFixtureFile("traces/elapsed_and_real_timestamp/dump_SurfaceFlinger.pb"),
      await UnitTestUtils.getFixtureFile("traces/dump_WindowManager.pb"),
    ];
    await traceCoordinator.setTraces(traces);

    const parser = traceCoordinator.findParser(TraceType.SURFACE_FLINGER);
    expect(parser).toBeTruthy();
    expect(parser!.getTraceType()).toEqual(TraceType.SURFACE_FLINGER);
  });

  it("cannot find parser that does not exist", async () => {
    expect(traceCoordinator.getParsers().length).toEqual(0);
    const parser = traceCoordinator.findParser(TraceType.SURFACE_FLINGER);
    expect(parser).toBe(null);
  });

  it("can get all timestamps from multiple parsers", async () => {
    const traces = [
      await UnitTestUtils.getFixtureFile("traces/elapsed_and_real_timestamp/SurfaceFlinger.pb"),
      await UnitTestUtils.getFixtureFile("traces/elapsed_and_real_timestamp/WindowManager.pb"),
    ];
    await traceCoordinator.setTraces(traces);
    const timestamps = timelineCoordinator.getAllTimestamps();
    expect(timestamps.length).toEqual(48);
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
    await traceCoordinator.setTraces(traces);

    // create viewers (mocked factory)
    expect(traceCoordinator.getViewers()).toEqual([]);
    traceCoordinator.createViewers();
    expect(traceCoordinator.getViewers()).toEqual([viewerStub]);

    // Gets notified of the current timestamp on creation
    expect(viewerStub.notifyCurrentTraceEntries).toHaveBeenCalledTimes(1);

    // When we update to an undefined timestamp we reset to the default selected
    // timestamp based on the active trace and loaded timelines. Given that
    // we haven't set a timestamp we should still be in the default timestamp
    // and require no update to the current trace entries.
    timelineCoordinator.updateCurrentTimestamp(undefined);
    expect(viewerStub.notifyCurrentTraceEntries).toHaveBeenCalledTimes(1);

    // notify timestamp
    const timestamp = new Timestamp(TimestampType.REAL, 14500282843n);
    expect(timelineCoordinator.getTimestampType()).toBe(TimestampType.REAL);
    timelineCoordinator.updateCurrentTimestamp(timestamp);
    expect(viewerStub.notifyCurrentTraceEntries).toHaveBeenCalledTimes(2);

    // notify timestamp again
    timelineCoordinator.updateCurrentTimestamp(timestamp);
    expect(viewerStub.notifyCurrentTraceEntries).toHaveBeenCalledTimes(2);

    // reset back to the default timestamp should trigger a change
    timelineCoordinator.updateCurrentTimestamp(undefined);
    expect(viewerStub.notifyCurrentTraceEntries).toHaveBeenCalledTimes(3);
  });

  it("trace coordinator sets video data on timelineCoordinator when screenrecording is loaded", async () => {
    expect(traceCoordinator.getParsers().length).toEqual(0);
    const traces = [
      await UnitTestUtils.getFixtureFile("traces/elapsed_and_real_timestamp/SurfaceFlinger.pb"),
      await UnitTestUtils.getFixtureFile("traces/elapsed_and_real_timestamp/WindowManager.pb"),
      await UnitTestUtils.getFixtureFile("traces/elapsed_and_real_timestamp/screen_recording_metadata_v2.mp4"),
    ];
    const errors = await traceCoordinator.setTraces(traces);
    expect(traceCoordinator.getParsers().length).toEqual(3);
    expect(errors.length).toEqual(0);

    expect(timelineCoordinator.setScreenRecordingData).toHaveBeenCalledTimes(1);
  });

  it("video data is removed if video trace is deleted", async () => {
    expect(traceCoordinator.getParsers().length).toEqual(0);
    const traces = [
      await UnitTestUtils.getFixtureFile("traces/elapsed_and_real_timestamp/SurfaceFlinger.pb"),
      await UnitTestUtils.getFixtureFile("traces/elapsed_and_real_timestamp/WindowManager.pb"),
      await UnitTestUtils.getFixtureFile("traces/elapsed_and_real_timestamp/screen_recording_metadata_v2.mp4"),
    ];
    const errors = await traceCoordinator.setTraces(traces);
    expect(traceCoordinator.getParsers().length).toEqual(3);
    expect(errors.length).toEqual(0);
    expect(traceCoordinator.getParserFor(TraceType.SCREEN_RECORDING))
      .withContext("Should have screen recording parser").toBeDefined();
    expect(timelineCoordinator.getTimelines().keys())
      .withContext("Should have screen recording timeline").toContain(TraceType.SCREEN_RECORDING);

    expect(timelineCoordinator.setScreenRecordingData).toHaveBeenCalledTimes(1);
    expect(timelineCoordinator.getVideoData()).withContext("Should have video data").toBeDefined();
    expect(timelineCoordinator.timestampAsElapsedScreenrecordingSeconds(
      new Timestamp(TimestampType.REAL, 1666361049372271045n)))
      .withContext("Should be able to covert timestamp to video seconds").toBeDefined();

    traceCoordinator.removeTrace(TraceType.SCREEN_RECORDING);

    expect(timelineCoordinator.removeScreenRecordingData).toHaveBeenCalledTimes(1);
    expect(timelineCoordinator.getVideoData()).withContext("Should no longer have video data").toBeUndefined();
    expect(() => {
      timelineCoordinator.timestampAsElapsedScreenrecordingSeconds(new Timestamp(TimestampType.REAL, 1666361049372271045n))
    }).toThrow(new Error("No timeline for requested trace type 3"));
  });
});
