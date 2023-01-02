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
import {Timestamp, TimestampType} from 'common/trace/timestamp';
import {TraceFile} from 'common/trace/trace';
import {TraceType} from 'common/trace/trace_type';
import {UnitTestUtils} from 'test/unit/utils';
import {TraceData} from './trace_data';

describe('TraceData', () => {
  let traceData: TraceData;

  beforeEach(async () => {
    traceData = new TraceData();
  });

  it('can load valid trace files', async () => {
    expect(traceData.getLoadedTraces().length).toEqual(0);
    await loadValidSfWmTraces();
    expect(traceData.getLoadedTraces().length).toEqual(2);
  });

  it('is robust to invalid trace files', async () => {
    const invalidTraceFiles = [
      new TraceFile(await UnitTestUtils.getFixtureFile('winscope_homepage.png')),
    ];

    const errors = await traceData.loadTraces(invalidTraceFiles);
    expect(errors.length).toEqual(1);
    expect(traceData.getLoadedTraces().length).toEqual(0);
  });

  it('is robust to mixed valid and invalid trace files', async () => {
    expect(traceData.getLoadedTraces().length).toEqual(0);
    const traces = [
      new TraceFile(await UnitTestUtils.getFixtureFile('winscope_homepage.png')),
      new TraceFile(await UnitTestUtils.getFixtureFile('traces/dump_WindowManager.pb')),
    ];
    const errors = await traceData.loadTraces(traces);
    expect(traceData.getLoadedTraces().length).toEqual(1);
    expect(errors.length).toEqual(1);
  });

  it('is robust to trace files with no entries', async () => {
    const traceFilesWithNoEntries = [
      new TraceFile(await UnitTestUtils.getFixtureFile('traces/no_entries_InputMethodClients.pb')),
    ];

    const errors = await traceData.loadTraces(traceFilesWithNoEntries);

    expect(errors.length).toEqual(0);

    expect(traceData.getLoadedTraces().length).toEqual(1);

    const timelines = traceData.getTimelines();
    expect(timelines.length).toEqual(1);
    expect(timelines[0].timestamps).toEqual([]);
  });

  it('can remove traces', async () => {
    await loadValidSfWmTraces();
    expect(traceData.getLoadedTraces().length).toEqual(2);

    traceData.removeTrace(TraceType.SURFACE_FLINGER);
    expect(traceData.getLoadedTraces().length).toEqual(1);

    traceData.removeTrace(TraceType.WINDOW_MANAGER);
    expect(traceData.getLoadedTraces().length).toEqual(0);
  });

  it('gets loaded traces', async () => {
    await loadValidSfWmTraces();

    const traces = traceData.getLoadedTraces();
    expect(traces.length).toEqual(2);
    expect(traces[0].traceFile.file).toBeTruthy();

    const actualTraceTypes = new Set(traces.map((trace) => trace.type));
    const expectedTraceTypes = new Set([TraceType.SURFACE_FLINGER, TraceType.WINDOW_MANAGER]);
    expect(actualTraceTypes).toEqual(expectedTraceTypes);
  });

  it('gets trace entries for a given timestamp', async () => {
    const traceFiles = [
      new TraceFile(
        await UnitTestUtils.getFixtureFile('traces/elapsed_and_real_timestamp/SurfaceFlinger.pb')
      ),
      new TraceFile(
        await UnitTestUtils.getFixtureFile('traces/elapsed_and_real_timestamp/WindowManager.pb')
      ),
    ];

    const errors = await traceData.loadTraces(traceFiles);
    expect(errors.length).toEqual(0);

    {
      const entries = traceData.getTraceEntries(undefined);
      expect(entries.size).toEqual(0);
    }
    {
      const timestamp = new Timestamp(TimestampType.REAL, 0n);
      const entries = traceData.getTraceEntries(timestamp);
      expect(entries.size).toEqual(0);
    }
    {
      const twoHundredYearsTimestamp = new Timestamp(
        TimestampType.REAL,
        200n * 365n * 24n * 60n * 3600n * 1000000000n
      );
      const entries = traceData.getTraceEntries(twoHundredYearsTimestamp);
      expect(entries.size).toEqual(2);
    }
  });

  it('gets timelines', async () => {
    await loadValidSfWmTraces();

    const timelines = traceData.getTimelines();

    const actualTraceTypes = new Set(timelines.map((timeline) => timeline.traceType));
    const expectedTraceTypes = new Set([TraceType.SURFACE_FLINGER, TraceType.WINDOW_MANAGER]);
    expect(actualTraceTypes).toEqual(expectedTraceTypes);

    timelines.forEach((timeline) => {
      expect(timeline.timestamps.length).toBeGreaterThan(0);
    });
  });

  it('gets screenrecording data', async () => {
    expect(traceData.getScreenRecordingVideo()).toBeUndefined();

    const traceFiles = [
      new TraceFile(
        await UnitTestUtils.getFixtureFile(
          'traces/elapsed_and_real_timestamp/screen_recording_metadata_v2.mp4'
        )
      ),
    ];
    await traceData.loadTraces(traceFiles);

    const video = traceData.getScreenRecordingVideo();
    expect(video).toBeDefined();
    expect(video!.size).toBeGreaterThan(0);
  });

  it('can be cleared', async () => {
    await loadValidSfWmTraces();
    expect(traceData.getLoadedTraces().length).toBeGreaterThan(0);
    expect(traceData.getTimelines().length).toBeGreaterThan(0);

    traceData.clear();
    expect(traceData.getLoadedTraces().length).toEqual(0);
    expect(traceData.getTimelines().length).toEqual(0);
  });

  const loadValidSfWmTraces = async () => {
    const traceFiles = [
      new TraceFile(
        await UnitTestUtils.getFixtureFile('traces/elapsed_and_real_timestamp/SurfaceFlinger.pb')
      ),
      new TraceFile(
        await UnitTestUtils.getFixtureFile('traces/elapsed_and_real_timestamp/WindowManager.pb')
      ),
    ];

    const errors = await traceData.loadTraces(traceFiles);
    expect(errors.length).toEqual(0);
  };
});
