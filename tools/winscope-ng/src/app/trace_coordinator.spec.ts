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

describe("TraceCoordinator", () => {
  let traceCoordinator: TraceCoordinator;

  beforeEach(async () => {
    traceCoordinator = new TraceCoordinator();
  });

  it("processes trace files", async () => {
    expect(traceCoordinator.getParsers().length).toEqual(0);
    const traces = [
      await UnitTestUtils.getFixtureFile("traces/elapsed_and_real_timestamp/dump_SurfaceFlinger.pb"),
      await UnitTestUtils.getFixtureFile("traces/dump_WindowManager.pb"),
    ];
    const errors = await traceCoordinator.addTraces(traces);
    expect(traceCoordinator.getParsers().length).toEqual(2);
    expect(errors.length).toEqual(0);
  });

  it("it is robust to invalid trace files", async () => {
    expect(traceCoordinator.getParsers().length).toEqual(0);
    const traces = [
      await UnitTestUtils.getFixtureFile("winscope_homepage.png"),
    ];
    const errors = await traceCoordinator.addTraces(traces);
    expect(traceCoordinator.getParsers().length).toEqual(0);
    expect(errors.length).toEqual(1);
  });

  it("is robust to trace files with no entries", async () => {
    const traces = [
      await UnitTestUtils.getFixtureFile(
        "traces/no_entries_InputMethodClients.pb")
    ];
    await traceCoordinator.addTraces(traces);

    const timestamp = new Timestamp(TimestampType.ELAPSED, 0n);
    traceCoordinator.notifyCurrentTimestamp(timestamp);
  });

  it("processes mixed valid and invalid trace files", async () => {
    expect(traceCoordinator.getParsers().length).toEqual(0);
    const traces = [
      await UnitTestUtils.getFixtureFile("winscope_homepage.png"),
      await UnitTestUtils.getFixtureFile("traces/dump_WindowManager.pb"),
    ];
    const errors = await traceCoordinator.addTraces(traces);
    expect(traceCoordinator.getParsers().length).toEqual(1);
    expect(errors.length).toEqual(1);
  });

  it("can remove traces", async () => {
    expect(traceCoordinator.getParsers().length).toEqual(0);
    const traces = [
      await UnitTestUtils.getFixtureFile("traces/elapsed_and_real_timestamp/dump_SurfaceFlinger.pb"),
    ];
    await traceCoordinator.addTraces(traces);
    expect(traceCoordinator.getParsers().length).toEqual(1);

    traceCoordinator.removeTrace(TraceType.SURFACE_FLINGER);
    expect(traceCoordinator.getParsers().length).toEqual(0);
  });

  it("can find a parser based on trace type", async () => {
    const traces = [
      await UnitTestUtils.getFixtureFile("traces/elapsed_and_real_timestamp/dump_SurfaceFlinger.pb"),
      await UnitTestUtils.getFixtureFile("traces/dump_WindowManager.pb"),
    ];
    await traceCoordinator.addTraces(traces);

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
    await traceCoordinator.addTraces(traces);
    const timestamps = traceCoordinator.getTimestamps();
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
    await traceCoordinator.addTraces(traces);

    // create viewers (mocked factory)
    expect(traceCoordinator.getViewers()).toEqual([]);
    traceCoordinator.createViewers();
    expect(traceCoordinator.getViewers()).toEqual([viewerStub]);

    // notify invalid timestamp
    traceCoordinator.notifyCurrentTimestamp(undefined);
    expect(viewerStub.notifyCurrentTraceEntries).toHaveBeenCalledTimes(0);

    // notify timestamp
    const timestamp = new Timestamp(TimestampType.ELAPSED, 14500282843n);
    traceCoordinator.notifyCurrentTimestamp(timestamp);
    expect(viewerStub.notifyCurrentTraceEntries).toHaveBeenCalledTimes(1);

    // notify timestamp again
    traceCoordinator.notifyCurrentTimestamp(timestamp);
    expect(viewerStub.notifyCurrentTraceEntries).toHaveBeenCalledTimes(2);
  });
});
