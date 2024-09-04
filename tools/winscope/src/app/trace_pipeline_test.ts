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

import {assertDefined} from 'common/assert_utils';
import {FileUtils} from 'common/file_utils';
import {ProgressListenerStub} from 'messaging/progress_listener_stub';
import {UserWarning} from 'messaging/user_warning';
import {
  CorruptedArchive,
  InvalidPerfettoTrace,
  NoValidFiles,
  TraceOverridden,
  UnsupportedFileFormat,
} from 'messaging/user_warnings';
import {TimestampConverterUtils} from 'test/unit/timestamp_converter_utils';
import {TracesUtils} from 'test/unit/traces_utils';
import {UserNotifierChecker} from 'test/unit/user_notifier_checker';
import {UnitTestUtils} from 'test/unit/utils';
import {TraceType} from 'trace/trace_type';
import {FilesSource} from './files_source';
import {TracePipeline} from './trace_pipeline';

describe('TracePipeline', () => {
  let validSfFile: File;
  let validWmFile: File;
  let shellTransitionFile: File;
  let wmTransitionFile: File;
  let screenshotFile: File;
  let screenRecordingFile: File;
  let brMainEntryFile: File;
  let brCodenameFile: File;
  let brSfFile: File;
  let jpgFile: File;

  let progressListener: ProgressListenerStub;
  let tracePipeline: TracePipeline;
  let userNotifierChecker: UserNotifierChecker;

  beforeAll(async () => {
    userNotifierChecker = new UserNotifierChecker();
    wmTransitionFile = await UnitTestUtils.getFixtureFile(
      'traces/elapsed_and_real_timestamp/wm_transition_trace.pb',
    );
    shellTransitionFile = await UnitTestUtils.getFixtureFile(
      'traces/elapsed_and_real_timestamp/shell_transition_trace.pb',
    );
    validSfFile = await UnitTestUtils.getFixtureFile(
      'traces/elapsed_and_real_timestamp/SurfaceFlinger.pb',
    );
    validWmFile = await UnitTestUtils.getFixtureFile(
      'traces/elapsed_and_real_timestamp/WindowManager.pb',
    );
    screenshotFile = await UnitTestUtils.getFixtureFile(
      'traces/screenshot.png',
    );
    screenRecordingFile = await UnitTestUtils.getFixtureFile(
      'traces/elapsed_and_real_timestamp/screen_recording_metadata_v2.mp4',
    );
    brMainEntryFile = await UnitTestUtils.getFixtureFile(
      'bugreports/main_entry.txt',
      'main_entry.txt',
    );
    brCodenameFile = await UnitTestUtils.getFixtureFile(
      'bugreports/bugreport-codename_beta-UPB2.230407.019-2023-05-30-14-33-48.txt',
      'bugreport-codename_beta-UPB2.230407.019-2023-05-30-14-33-48.txt',
    );
    brSfFile = await UnitTestUtils.getFixtureFile(
      'traces/elapsed_and_real_timestamp/SurfaceFlinger.pb',
      'FS/data/misc/wmtrace/surface_flinger.bp',
    );
    jpgFile = await UnitTestUtils.getFixtureFile('winscope_homepage.jpg');
  });

  beforeEach(async () => {
    jasmine.addCustomEqualityTester(UnitTestUtils.timestampEqualityTester);

    progressListener = new ProgressListenerStub();
    spyOn(progressListener, 'onProgressUpdate');
    spyOn(progressListener, 'onOperationFinished');
    userNotifierChecker.reset();

    tracePipeline = new TracePipeline();
  });

  it('can load valid trace files', async () => {
    expect(tracePipeline.getTraces().getSize()).toEqual(0);

    await loadFiles([validSfFile, validWmFile], FilesSource.TEST);
    await expectLoadResult(2, []);

    expect(tracePipeline.getDownloadArchiveFilename()).toMatch(
      new RegExp(`${FilesSource.TEST}_`),
    );
    expect(tracePipeline.getTraces().getSize()).toEqual(2);

    const traceEntries = await TracesUtils.extractEntries(
      tracePipeline.getTraces(),
    );
    expect(traceEntries.get(TraceType.WINDOW_MANAGER)?.length).toBeGreaterThan(
      0,
    );
    expect(traceEntries.get(TraceType.SURFACE_FLINGER)?.length).toBeGreaterThan(
      0,
    );
  });

  it('can load valid gzipped file and archive', async () => {
    expect(tracePipeline.getTraces().getSize()).toEqual(0);

    const gzippedFile = await UnitTestUtils.getFixtureFile(
      'traces/WindowManager.pb.gz',
    );
    const gzippedArchive = await UnitTestUtils.getFixtureFile(
      'traces/WindowManager.zip.gz',
    );

    await loadFiles([gzippedFile, gzippedArchive], FilesSource.TEST);
    await expectLoadResult(2, []);

    const traces = tracePipeline.getTraces();
    expect(traces.getSize()).toEqual(2);
    expect(traces.getTraces(TraceType.WINDOW_MANAGER).length).toEqual(2);

    const traceEntries = await TracesUtils.extractEntries(traces);
    expect(traceEntries.get(TraceType.WINDOW_MANAGER)?.length).toBeGreaterThan(
      0,
    );
  });

  it('can set download archive filename based on files source', async () => {
    await loadFiles([validSfFile]);
    await expectLoadResult(1, []);
    expect(tracePipeline.getDownloadArchiveFilename()).toMatch(
      new RegExp('SurfaceFlinger_'),
    );

    tracePipeline.clear();

    await loadFiles([validSfFile, validWmFile], FilesSource.COLLECTED);
    await expectLoadResult(2, []);
    expect(tracePipeline.getDownloadArchiveFilename()).toMatch(
      new RegExp(`${FilesSource.COLLECTED}_`),
    );
  });

  it('can convert illegal uploaded archive filename to legal name for download archive', async () => {
    const fileWithIllegalName = await UnitTestUtils.getFixtureFile(
      'traces/SFtrace(with_illegal_characters).pb',
    );
    await loadFiles([fileWithIllegalName]);
    await expectLoadResult(1, []);
    const downloadFilename = tracePipeline.getDownloadArchiveFilename();
    expect(FileUtils.DOWNLOAD_FILENAME_REGEX.test(downloadFilename)).toBeTrue();
  });

  it('detects bugreports and filters out files based on their directory', async () => {
    expect(tracePipeline.getTraces().getSize()).toEqual(0);

    const bugreportFiles = [
      brMainEntryFile,
      brCodenameFile,
      brSfFile,
      await UnitTestUtils.getFixtureFile(
        'traces/elapsed_and_real_timestamp/wm_transition_trace.pb',
        'FS/data/misc/ignored-dir/window_manager.bp',
      ),
    ];

    const bugreportArchive = new File(
      [await FileUtils.createZipArchive(bugreportFiles)],
      'bugreport.zip',
    );

    // Corner case:
    // Another file is loaded along the bugreport -> the file must not be ignored
    //
    // Note:
    // The even weirder corner case where two bugreports are loaded at the same time is
    // currently not properly handled.
    const otherFile = await UnitTestUtils.getFixtureFile(
      'traces/elapsed_and_real_timestamp/InputMethodClients.pb',
      'would-be-ignored-if-was-in-bugreport-archive/input_method_clients.pb',
    );

    await loadFiles([bugreportArchive, otherFile]);
    await expectLoadResult(2, []);

    const traces = tracePipeline.getTraces();
    expect(traces.getTrace(TraceType.SURFACE_FLINGER)).toBeDefined();
    expect(traces.getTrace(TraceType.WINDOW_MANAGER)).toBeUndefined(); // ignored
    expect(traces.getTrace(TraceType.INPUT_METHOD_CLIENTS)).toBeDefined();
  });

  it('detects bugreports and extracts timezone info, then calculates utc offset', async () => {
    const bugreportFiles = [brMainEntryFile, brCodenameFile, brSfFile];
    const bugreportArchive = new File(
      [await FileUtils.createZipArchive(bugreportFiles)],
      'bugreport.zip',
    );

    await loadFiles([bugreportArchive]);
    await expectLoadResult(1, []);

    const timestampConverter = tracePipeline.getTimestampConverter();
    expect(timestampConverter);
    expect(timestampConverter.getUTCOffset()).toEqual('UTC+05:30');

    const expectedTimestamp =
      TimestampConverterUtils.makeRealTimestampWithUTCOffset(
        1659107089102062832n,
      );
    expect(
      timestampConverter.makeTimestampFromMonotonicNs(14500282843n),
    ).toEqual(expectedTimestamp);
  });

  it('is robust to corrupted archive', async () => {
    const corruptedArchive = await UnitTestUtils.getFixtureFile(
      'corrupted_archive.zip',
    );

    await loadFiles([corruptedArchive]);

    await expectLoadResult(0, [
      new CorruptedArchive(corruptedArchive),
      new NoValidFiles(),
    ]);
  });

  it('is robust to invalid trace files', async () => {
    const invalidFiles = [jpgFile];
    await loadFiles(invalidFiles);

    await expectLoadResult(0, [
      new UnsupportedFileFormat('winscope_homepage.jpg'),
    ]);
  });

  it('is robust to invalid perfetto trace files', async () => {
    const invalidFiles = [
      await UnitTestUtils.getFixtureFile(
        'traces/perfetto/invalid_protolog.perfetto-trace',
      ),
    ];

    await loadFiles(invalidFiles);

    await expectLoadResult(0, [
      new InvalidPerfettoTrace('invalid_protolog.perfetto-trace', [
        'Perfetto trace has no IME Clients entries',
        'Perfetto trace has no IME system_server entries',
        'Perfetto trace has no IME Service entries',
        'Perfetto trace has no ProtoLog entries',
        'Perfetto trace has no Surface Flinger entries',
        'Perfetto trace has no Transactions entries',
        'Perfetto trace has no Transitions entries',
        'Perfetto trace has no ViewCapture windows',
        'Perfetto trace has no Window Manager entries',
        'Perfetto trace has no Motion Events entries',
        'Perfetto trace has no Key Events entries',
      ]),
    ]);
  });

  it('is robust to mixed valid and invalid trace files', async () => {
    expect(tracePipeline.getTraces().getSize()).toEqual(0);
    const files = [
      jpgFile,
      await UnitTestUtils.getFixtureFile('traces/dump_WindowManager.pb'),
    ];

    await loadFiles(files);

    await expectLoadResult(1, [
      new UnsupportedFileFormat('winscope_homepage.jpg'),
    ]);
  });

  it('can remove traces', async () => {
    await loadFiles([validSfFile, validWmFile]);
    await expectLoadResult(2, []);

    const sfTrace = assertDefined(
      tracePipeline.getTraces().getTrace(TraceType.SURFACE_FLINGER),
    );
    const wmTrace = assertDefined(
      tracePipeline.getTraces().getTrace(TraceType.WINDOW_MANAGER),
    );

    tracePipeline.removeTrace(sfTrace);
    await expectLoadResult(1, []);

    tracePipeline.removeTrace(wmTrace);
    await expectLoadResult(0, []);
  });

  it('removes constituent traces of transitions trace but keeps for download', async () => {
    const files = [wmTransitionFile, wmTransitionFile, shellTransitionFile];
    await loadFiles(files);
    await expectLoadResult(1, []);

    const transitionTrace = assertDefined(
      tracePipeline.getTraces().getTrace(TraceType.TRANSITION),
    );

    tracePipeline.removeTrace(transitionTrace);
    await expectLoadResult(0, []);

    await loadFiles([wmTransitionFile]);
    await expectLoadResult(1, []);
    expect(
      tracePipeline.getTraces().getTrace(TraceType.WM_TRANSITION),
    ).toBeDefined();
    await expectDownloadResult([
      'transition/shell_transition_trace.pb',
      'transition/wm_transition_trace.pb',
    ]);
  });

  it('removes constituent traces of CUJs trace but keeps for download', async () => {
    const files = [
      await UnitTestUtils.getFixtureFile('traces/eventlog.winscope'),
    ];
    await loadFiles(files);
    await expectLoadResult(1, []);

    const cujTrace = assertDefined(
      tracePipeline.getTraces().getTrace(TraceType.CUJS),
    );

    tracePipeline.removeTrace(cujTrace);
    await expectLoadResult(0, []);
    await expectDownloadResult(['eventlog/eventlog.winscope']);
  });

  it('removes constituent traces of input trace but keeps for download', async () => {
    const files = [
      await UnitTestUtils.getFixtureFile(
        'traces/perfetto/input-events.perfetto-trace',
      ),
    ];
    await loadFiles(files);
    await expectLoadResult(1, []);

    const inputTrace = assertDefined(
      tracePipeline.getTraces().getTrace(TraceType.INPUT_EVENT_MERGED),
    );

    tracePipeline.removeTrace(inputTrace);
    await expectLoadResult(0, []);
    await expectDownloadResult(['input-events.perfetto-trace']);
  });

  it('gets loaded traces', async () => {
    await loadFiles([validSfFile, validWmFile]);
    await expectLoadResult(2, []);

    const traces = tracePipeline.getTraces();

    const actualTraceTypes = new Set(traces.mapTrace((trace) => trace.type));
    const expectedTraceTypes = new Set([
      TraceType.SURFACE_FLINGER,
      TraceType.WINDOW_MANAGER,
    ]);
    expect(actualTraceTypes).toEqual(expectedTraceTypes);

    const sfTrace = assertDefined(traces.getTrace(TraceType.SURFACE_FLINGER));
    expect(sfTrace.getDescriptors().length).toBeGreaterThan(0);
  });

  it('gets screenrecording data', async () => {
    const files = [screenRecordingFile];
    await loadFiles(files);
    await expectLoadResult(1, []);

    const video = await tracePipeline.getScreenRecordingVideo();
    expect(video).toBeDefined();
    expect(video?.size).toBeGreaterThan(0);
  });

  it('gets screenshot data', async () => {
    const files = [screenshotFile];
    await loadFiles(files);
    await expectLoadResult(1, []);

    const video = await tracePipeline.getScreenRecordingVideo();
    expect(video).toBeDefined();
    expect(video?.size).toBeGreaterThan(0);
  });

  it('prioritizes screenrecording over screenshot data', async () => {
    const files = [screenshotFile, screenRecordingFile];
    await loadFiles(files);
    await expectLoadResult(1, [
      new TraceOverridden('screenshot.png', TraceType.SCREEN_RECORDING),
    ]);

    const video = await tracePipeline.getScreenRecordingVideo();
    expect(video).toBeDefined();
    expect(video?.size).toBeGreaterThan(0);
  });

  it('creates traces with correct type', async () => {
    await loadFiles([validSfFile, validWmFile]);
    await expectLoadResult(2, []);

    const traces = tracePipeline.getTraces();
    traces.forEachTrace((trace, type) => {
      expect(trace.type).toEqual(type);
    });
  });

  it('creates zip archive with loaded trace files', async () => {
    const files = [
      screenRecordingFile,
      await UnitTestUtils.getFixtureFile(
        'traces/perfetto/transactions_trace.perfetto-trace',
      ),
    ];
    await loadFiles(files);
    await expectLoadResult(2, []);

    await expectDownloadResult([
      'screen_recording_metadata_v2.mp4',
      'transactions_trace.perfetto-trace',
    ]);
  });

  it('can be cleared', async () => {
    await loadFiles([validSfFile, validWmFile]);
    await expectLoadResult(2, []);

    tracePipeline.clear();
    expect(tracePipeline.getTraces().getSize()).toEqual(0);
  });

  it('can filter traces without visualization', async () => {
    await loadFiles([validSfFile, shellTransitionFile]);
    await expectLoadResult(2, []);

    tracePipeline.filterTracesWithoutVisualization();
    expect(tracePipeline.getTraces().getSize()).toEqual(1);
    expect(
      tracePipeline.getTraces().getTrace(TraceType.SHELL_TRANSITION),
    ).toBeUndefined();
  });

  async function loadFiles(
    files: File[],
    source: FilesSource = FilesSource.TEST,
  ) {
    await tracePipeline.loadFiles(files, source, progressListener);
    expect(progressListener.onOperationFinished).toHaveBeenCalled();
    await tracePipeline.buildTraces();
  }

  async function expectLoadResult(
    numberOfTraces: number,
    expectedWarnings: UserWarning[],
  ) {
    userNotifierChecker.expectAdded(expectedWarnings);
    expect(tracePipeline.getTraces().getSize()).toEqual(numberOfTraces);
  }

  async function expectDownloadResult(expectedArchiveContents: string[]) {
    const zipArchive = await tracePipeline.makeZipArchiveWithLoadedTraceFiles();
    const actualArchiveContents = (await FileUtils.unzipFile(zipArchive))
      .map((file) => file.name)
      .sort();
    expect(actualArchiveContents).toEqual(expectedArchiveContents);
  }
});
