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

import {TracesUtils} from 'test/unit/traces_utils';
import {UnitTestUtils} from 'test/unit/utils';
import {TraceFile} from 'trace/trace_file';
import {TraceType} from 'trace/trace_type';
import {TracePipeline} from './trace_pipeline';

describe('TracePipeline', () => {
  let tracePipeline: TracePipeline;

  beforeEach(async () => {
    tracePipeline = new TracePipeline();
  });

  it('can load valid trace files', async () => {
    expect(tracePipeline.getLoadedTraces().length).toEqual(0);

    await loadValidSfWmTraces();

    expect(tracePipeline.getLoadedTraces().length).toEqual(2);

    const traceEntries = TracesUtils.extractEntries(tracePipeline.getTraces());
    expect(traceEntries.get(TraceType.WINDOW_MANAGER)?.length).toBeGreaterThan(0);
    expect(traceEntries.get(TraceType.SURFACE_FLINGER)?.length).toBeGreaterThan(0);
  });

  it('can load bugreport and ignores non-trace dirs', async () => {
    expect(tracePipeline.getLoadedTraces().length).toEqual(0);

    // Could be any file, we just need an instance of File to be used as a fake bugreport archive
    const bugreportArchive = await UnitTestUtils.getFixtureFile(
      'bugreports/bugreport_stripped.zip'
    );

    const bugreportFiles = [
      new TraceFile(
        await UnitTestUtils.getFixtureFile('bugreports/main_entry.txt', 'main_entry.txt'),
        bugreportArchive
      ),
      new TraceFile(
        await UnitTestUtils.getFixtureFile(
          'bugreports/bugreport-codename_beta-UPB2.230407.019-2023-05-30-14-33-48.txt',
          'bugreport-codename_beta-UPB2.230407.019-2023-05-30-14-33-48.txt'
        ),
        bugreportArchive
      ),
      new TraceFile(
        await UnitTestUtils.getFixtureFile(
          'traces/elapsed_and_real_timestamp/SurfaceFlinger.pb',
          'FS/data/misc/wmtrace/surface_flinger.bp'
        ),
        bugreportArchive
      ),
      new TraceFile(
        await UnitTestUtils.getFixtureFile(
          'traces/elapsed_and_real_timestamp/Transactions.pb',
          'FS/data/misc/wmtrace/transactions.bp'
        ),
        bugreportArchive
      ),
      new TraceFile(
        await UnitTestUtils.getFixtureFile(
          'traces/elapsed_and_real_timestamp/WindowManager.pb',
          'FS/data/misc/ignored-dir/window_manager.bp'
        ),
        bugreportArchive
      ),
    ];

    // Corner case:
    // A plain trace file is loaded along the bugreport -> trace file must not be ignored
    //
    // Note:
    // The even weirder corner case where two bugreports are loaded at the same time is
    // currently not properly handled.
    const plainTraceFile = new TraceFile(
      await UnitTestUtils.getFixtureFile(
        'traces/elapsed_and_real_timestamp/InputMethodClients.pb',
        'would-be-ignored-if-was-part-of-bugreport/input_method_clients.pb'
      )
    );

    const mergedFiles = bugreportFiles.concat([plainTraceFile]);
    const errors = await tracePipeline.loadTraceFiles(mergedFiles);
    expect(errors.length).toEqual(0);
    tracePipeline.buildTraces();
    const traces = tracePipeline.getTraces();

    expect(traces.getTrace(TraceType.SURFACE_FLINGER)).toBeDefined();
    expect(traces.getTrace(TraceType.TRANSACTIONS)).toBeDefined();
    expect(traces.getTrace(TraceType.WINDOW_MANAGER)).toBeUndefined(); // ignored
    expect(traces.getTrace(TraceType.INPUT_METHOD_CLIENTS)).toBeDefined();
  });

  it('is robust to invalid trace files', async () => {
    const invalidTraceFiles = [
      new TraceFile(await UnitTestUtils.getFixtureFile('winscope_homepage.png')),
    ];

    const errors = await tracePipeline.loadTraceFiles(invalidTraceFiles);
    tracePipeline.buildTraces();
    expect(errors.length).toEqual(1);
    expect(tracePipeline.getLoadedTraces().length).toEqual(0);
  });

  it('is robust to mixed valid and invalid trace files', async () => {
    expect(tracePipeline.getLoadedTraces().length).toEqual(0);
    const files = [
      new TraceFile(await UnitTestUtils.getFixtureFile('winscope_homepage.png')),
      new TraceFile(await UnitTestUtils.getFixtureFile('traces/dump_WindowManager.pb')),
    ];
    const errors = await tracePipeline.loadTraceFiles(files);
    tracePipeline.buildTraces();
    expect(tracePipeline.getLoadedTraces().length).toEqual(1);
    expect(errors.length).toEqual(1);
  });

  it('is robust to trace files with no entries', async () => {
    const traceFilesWithNoEntries = [
      new TraceFile(await UnitTestUtils.getFixtureFile('traces/no_entries_InputMethodClients.pb')),
    ];

    const errors = await tracePipeline.loadTraceFiles(traceFilesWithNoEntries);
    tracePipeline.buildTraces();

    expect(errors.length).toEqual(0);

    expect(tracePipeline.getLoadedTraces().length).toEqual(1);
  });

  it('can remove traces', async () => {
    await loadValidSfWmTraces();
    expect(tracePipeline.getLoadedTraces().length).toEqual(2);

    tracePipeline.removeTraceFile(TraceType.SURFACE_FLINGER);
    tracePipeline.buildTraces();
    expect(tracePipeline.getLoadedTraces().length).toEqual(1);

    tracePipeline.removeTraceFile(TraceType.WINDOW_MANAGER);
    tracePipeline.buildTraces();
    expect(tracePipeline.getLoadedTraces().length).toEqual(0);
  });

  it('gets loaded trace files', async () => {
    await loadValidSfWmTraces();

    const files = tracePipeline.getLoadedTraces();
    expect(files.length).toEqual(2);
    expect(files[0].descriptors).toBeTruthy();
    expect(files[0].descriptors.length).toBeGreaterThan(0);

    const actualTraceTypes = new Set(files.map((file) => file.type));
    const expectedTraceTypes = new Set([TraceType.SURFACE_FLINGER, TraceType.WINDOW_MANAGER]);
    expect(actualTraceTypes).toEqual(expectedTraceTypes);
  });

  it('builds traces', async () => {
    await loadValidSfWmTraces();
    const traces = tracePipeline.getTraces();

    expect(traces.getTrace(TraceType.SURFACE_FLINGER)).toBeDefined();
    expect(traces.getTrace(TraceType.WINDOW_MANAGER)).toBeDefined();
  });

  it('gets screenrecording data', async () => {
    const traceFiles = [
      new TraceFile(
        await UnitTestUtils.getFixtureFile(
          'traces/elapsed_and_real_timestamp/screen_recording_metadata_v2.mp4'
        )
      ),
    ];
    await tracePipeline.loadTraceFiles(traceFiles);
    tracePipeline.buildTraces();

    const video = tracePipeline.getScreenRecordingVideo();
    expect(video).toBeDefined();
    expect(video!.size).toBeGreaterThan(0);
  });

  it('can be cleared', async () => {
    await loadValidSfWmTraces();
    expect(tracePipeline.getLoadedTraces().length).toBeGreaterThan(0);

    tracePipeline.clear();
    expect(tracePipeline.getLoadedTraces().length).toEqual(0);
    expect(() => {
      tracePipeline.getTraces();
    }).toThrow();
    expect(() => {
      tracePipeline.getScreenRecordingVideo();
    }).toThrow();
  });

  it('throws if accessed before traces are built', async () => {
    expect(() => {
      tracePipeline.getTraces();
    }).toThrow();
    expect(() => {
      tracePipeline.getScreenRecordingVideo();
    }).toThrow();
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

    const errors = await tracePipeline.loadTraceFiles(traceFiles);
    expect(errors.length).toEqual(0);

    tracePipeline.buildTraces();
  };
});
