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
import { TraceCoordinator } from "./trace_coordinator";
import { UnitTestUtils } from "test/unit/utils";
import { TraceType } from "common/trace/trace_type";

describe("TraceCoordinator", () => {
  let traceCoordinator: TraceCoordinator;

  beforeEach(async () => {
    traceCoordinator = new TraceCoordinator();
  });

  it("adds parsers from recognised traces", async () => {
    expect(traceCoordinator.getParsers().length).toEqual(0);
    const traces = [
      await UnitTestUtils.getFixtureFile("traces/elapsed_and_real_timestamp/dump_SurfaceFlinger.pb"),
      await UnitTestUtils.getFixtureFile("traces/dump_WindowManager.pb"),
    ];
    const errors = await traceCoordinator.addTraces(traces);
    expect(traceCoordinator.getParsers().length).toEqual(2);
    expect(errors.length).toEqual(0);
  });

  it("handles unrecognised file types added", async () => {
    expect(traceCoordinator.getParsers().length).toEqual(0);
    const traces = [
      await UnitTestUtils.getFixtureFile("winscope_homepage.png"),
    ];
    const errors = await traceCoordinator.addTraces(traces);
    expect(traceCoordinator.getParsers().length).toEqual(0);
    expect(errors.length).toEqual(1);
  });

  it("handles both recognised and unrecognised file types added", async () => {
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
});
