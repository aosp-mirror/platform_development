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
import {WinscopeError, WinscopeErrorType} from 'messaging/winscope_error';
import {TracesUtils} from 'test/unit/traces_utils';
import {UnitTestUtils} from 'test/unit/utils';
import {TraceType} from 'trace/trace_type';
import {FilesSource} from './files_source';
import {TracePipeline} from './trace_pipeline';

describe('TracePipeline', () => {
  let validSfFile: File;
  let validWmFile: File;
  let errors: WinscopeError[];
  let progressListener: ProgressListenerStub;
  let tracePipeline: TracePipeline;

  beforeEach(async () => {
    validSfFile = await UnitTestUtils.getFixtureFile(
      'traces/elapsed_and_real_timestamp/SurfaceFlinger.pb'
    );
    validWmFile = await UnitTestUtils.getFixtureFile(
      'traces/elapsed_and_real_timestamp/WindowManager.pb'
    );

    errors = [];

    progressListener = new ProgressListenerStub();
    spyOn(progressListener, 'onProgressUpdate');
    spyOn(progressListener, 'onOperationFinished');

    tracePipeline = new TracePipeline();
  });

  it('can load valid trace files', async () => {
    expect(tracePipeline.getTraces().getSize()).toEqual(0);

    await loadFiles([validSfFile, validWmFile], FilesSource.TEST);
    await expectLoadResult(2, []);

    expect(tracePipeline.getDownloadArchiveFilename()).toMatch(new RegExp(`${FilesSource.TEST}_`));
    expect(tracePipeline.getTraces().getSize()).toEqual(2);

    const traceEntries = await TracesUtils.extractEntries(tracePipeline.getTraces());
    expect(traceEntries.get(TraceType.WINDOW_MANAGER)?.length).toBeGreaterThan(0);
    expect(traceEntries.get(TraceType.SURFACE_FLINGER)?.length).toBeGreaterThan(0);
  });

  it('can set download archive filename based on files source', async () => {
    await loadFiles([validSfFile]);
    await expectLoadResult(1, []);
    expect(tracePipeline.getDownloadArchiveFilename()).toMatch(new RegExp('SurfaceFlinger_'));

    tracePipeline.clear();

    await loadFiles([validSfFile, validWmFile], FilesSource.COLLECTED);
    await expectLoadResult(2, []);
    expect(tracePipeline.getDownloadArchiveFilename()).toMatch(
      new RegExp(`${FilesSource.COLLECTED}_`)
    );
  });

  it('can convert illegal uploaded archive filename to legal name for download archive', async () => {
    const fileWithIllegalName = await UnitTestUtils.getFixtureFile(
      'traces/SF_trace&(*_with:_illegal+_characters.pb'
    );
    await loadFiles([fileWithIllegalName]);
    await expectLoadResult(1, []);
    const downloadFilename = tracePipeline.getDownloadArchiveFilename();
    expect(FileUtils.DOWNLOAD_FILENAME_REGEX.test(downloadFilename)).toBeTrue();
  });

  it('can load a new file without dropping already-loaded traces', async () => {
    expect(tracePipeline.getTraces().getSize()).toEqual(0);

    await loadFiles([validSfFile]);
    await expectLoadResult(1, []);

    await loadFiles([validWmFile]);
    await expectLoadResult(2, []);

    // ignored (duplicated)
    await loadFiles([validWmFile]);
    await expectLoadResult(2, [
      new WinscopeError(
        WinscopeErrorType.FILE_OVERRIDDEN,
        'WindowManager.pb',
        TraceType.WINDOW_MANAGER
      ),
    ]);
  });

  it('can load bugreport and ignores non-trace dirs', async () => {
    expect(tracePipeline.getTraces().getSize()).toEqual(0);

    const bugreportFiles = [
      await UnitTestUtils.getFixtureFile('bugreports/main_entry.txt', 'main_entry.txt'),
      await UnitTestUtils.getFixtureFile(
        'bugreports/bugreport-codename_beta-UPB2.230407.019-2023-05-30-14-33-48.txt',
        'bugreport-codename_beta-UPB2.230407.019-2023-05-30-14-33-48.txt'
      ),
      await UnitTestUtils.getFixtureFile(
        'traces/elapsed_and_real_timestamp/SurfaceFlinger.pb',
        'FS/data/misc/wmtrace/surface_flinger.bp'
      ),
      await UnitTestUtils.getFixtureFile(
        'traces/elapsed_and_real_timestamp/Transactions.pb',
        'FS/data/misc/wmtrace/transactions.bp'
      ),
      await UnitTestUtils.getFixtureFile(
        'traces/elapsed_and_real_timestamp/WindowManager.pb',
        'proto/window_CRITICAL.proto'
      ),
      await UnitTestUtils.getFixtureFile(
        'traces/elapsed_and_real_timestamp/wm_transition_trace.pb',
        'FS/data/misc/ignored-dir/wm_transition_trace.bp'
      ),
    ];

    const bugreportArchive = new File(
      [await FileUtils.createZipArchive(bugreportFiles)],
      'bugreport.zip'
    );

    // Corner case:
    // Another file is loaded along the bugreport -> the file must not be ignored
    //
    // Note:
    // The even weirder corner case where two bugreports are loaded at the same time is
    // currently not properly handled.
    const otherFile = await UnitTestUtils.getFixtureFile(
      'traces/elapsed_and_real_timestamp/InputMethodClients.pb',
      'would-be-ignored-if-was-in-bugreport-archive/input_method_clients.pb'
    );

    await loadFiles([bugreportArchive, otherFile]);
    await expectLoadResult(4, []);

    const traces = tracePipeline.getTraces();
    expect(traces.getTrace(TraceType.SURFACE_FLINGER)).toBeDefined();
    expect(traces.getTrace(TraceType.TRANSACTIONS)).toBeDefined();
    expect(traces.getTrace(TraceType.WM_TRANSITION)).toBeUndefined(); // ignored
    expect(traces.getTrace(TraceType.INPUT_METHOD_CLIENTS)).toBeDefined();
    expect(traces.getTrace(TraceType.WINDOW_MANAGER)).toBeDefined();
  });

  it('prioritizes perfetto traces over legacy traces', async () => {
    const files = [
      await UnitTestUtils.getFixtureFile('traces/elapsed_and_real_timestamp/Transactions.pb'),
      await UnitTestUtils.getFixtureFile('traces/perfetto/transactions_trace.perfetto-trace'),
    ];

    await loadFiles(files);
    await expectLoadResult(1, []);

    const traces = tracePipeline.getTraces();
    expect(assertDefined(traces.getTrace(TraceType.TRANSACTIONS)).getDescriptors()).toEqual([
      'transactions_trace.perfetto-trace',
    ]);
  });

  it('is robust to corrupted archive', async () => {
    const corruptedArchive = await UnitTestUtils.getFixtureFile('corrupted_archive.zip');

    await loadFiles([corruptedArchive]);

    await expectLoadResult(0, [
      new WinscopeError(WinscopeErrorType.CORRUPTED_ARCHIVE, 'corrupted_archive.zip'),
      new WinscopeError(WinscopeErrorType.NO_INPUT_FILES),
    ]);
  });

  it('is robust to invalid trace files', async () => {
    const invalidFiles = [await UnitTestUtils.getFixtureFile('winscope_homepage.png')];

    await loadFiles(invalidFiles);

    await expectLoadResult(0, [
      new WinscopeError(WinscopeErrorType.UNSUPPORTED_FILE_FORMAT, 'winscope_homepage.png'),
    ]);
  });

  it('is robust to mixed valid and invalid trace files', async () => {
    expect(tracePipeline.getTraces().getSize()).toEqual(0);
    const files = [
      await UnitTestUtils.getFixtureFile('winscope_homepage.png'),
      await UnitTestUtils.getFixtureFile('traces/dump_WindowManager.pb'),
    ];

    await loadFiles(files);

    await expectLoadResult(1, [
      new WinscopeError(WinscopeErrorType.UNSUPPORTED_FILE_FORMAT, 'winscope_homepage.png'),
    ]);
  });

  it('is robust to trace files with no entries', async () => {
    const traceFilesWithNoEntries = [
      await UnitTestUtils.getFixtureFile('traces/no_entries_InputMethodClients.pb'),
    ];

    await loadFiles(traceFilesWithNoEntries);

    await expectLoadResult(1, []);
  });

  it('is robust to multiple files of same trace type in the same archive', async () => {
    const filesOfSameTraceType = [
      await UnitTestUtils.getFixtureFile(
        'traces/elapsed_and_real_timestamp/SurfaceFlinger.pb',
        'file0.pb'
      ),
      await UnitTestUtils.getFixtureFile(
        'traces/elapsed_and_real_timestamp/SurfaceFlinger.pb',
        'file1.pb'
      ),
    ];

    await loadFiles(filesOfSameTraceType);

    // Expect one trace to be overridden/discarded
    await expectLoadResult(1, [
      new WinscopeError(WinscopeErrorType.FILE_OVERRIDDEN, 'file0.pb', TraceType.SURFACE_FLINGER),
    ]);
  });

  it('always overrides trace of same type when new file is uploaded', async () => {
    const sfFileOrig = [
      await UnitTestUtils.getFixtureFile(
        'traces/elapsed_and_real_timestamp/SurfaceFlinger.pb',
        'file_orig.pb'
      ),
    ];
    const sfFileNew = [
      await UnitTestUtils.getFixtureFile(
        'traces/elapsed_and_real_timestamp/SurfaceFlinger.pb',
        'file_new.pb'
      ),
    ];

    await loadFiles(sfFileOrig);
    await expectLoadResult(1, []);

    // Expect original trace to be overridden/discarded
    await loadFiles(sfFileNew);
    await expectLoadResult(1, [
      new WinscopeError(
        WinscopeErrorType.FILE_OVERRIDDEN,
        'file_orig.pb',
        TraceType.SURFACE_FLINGER
      ),
    ]);
  });

  it('can load a single legacy trace file', async () => {
    const file = await UnitTestUtils.getFixtureFile(
      'traces/elapsed_and_real_timestamp/Transactions.pb'
    );
    await loadFiles([file]);
    await expectLoadResult(1, []);
  });

  it('can load a single perfetto trace file', async () => {
    const file = await UnitTestUtils.getFixtureFile(
      'traces/perfetto/transactions_trace.perfetto-trace'
    );
    await loadFiles([file]);
    await expectLoadResult(1, []);
  });

  it('can remove traces', async () => {
    await loadFiles([validSfFile, validWmFile]);
    await expectLoadResult(2, []);

    const sfTrace = assertDefined(tracePipeline.getTraces().getTrace(TraceType.SURFACE_FLINGER));
    const wmTrace = assertDefined(tracePipeline.getTraces().getTrace(TraceType.WINDOW_MANAGER));

    tracePipeline.removeTrace(sfTrace);
    await expectLoadResult(1, []);

    tracePipeline.removeTrace(wmTrace);
    await expectLoadResult(0, []);
  });

  it('gets loaded traces', async () => {
    await loadFiles([validSfFile, validWmFile]);
    await expectLoadResult(2, []);

    const traces = tracePipeline.getTraces();

    const actualTraceTypes = new Set(traces.mapTrace((trace) => trace.type));
    const expectedTraceTypes = new Set([TraceType.SURFACE_FLINGER, TraceType.WINDOW_MANAGER]);
    expect(actualTraceTypes).toEqual(expectedTraceTypes);

    const sfTrace = assertDefined(traces.getTrace(TraceType.SURFACE_FLINGER));
    expect(sfTrace.getDescriptors().length).toBeGreaterThan(0);
  });

  it('gets screenrecording data', async () => {
    const files = [
      await UnitTestUtils.getFixtureFile(
        'traces/elapsed_and_real_timestamp/screen_recording_metadata_v2.mp4'
      ),
    ];
    await loadFiles(files);
    await expectLoadResult(1, []);

    const video = await tracePipeline.getScreenRecordingVideo();
    expect(video).toBeDefined();
    expect(video?.size).toBeGreaterThan(0);
  });

  it('sets traces with correct type', async () => {
    const validTransactionsFile = await UnitTestUtils.getFixtureFile(
      'traces/elapsed_and_real_timestamp/Transactions.pb',
      'FS/data/misc/wmtrace/transactions.bp'
    );
    const validWmTransitionsFile = await UnitTestUtils.getFixtureFile(
      'traces/elapsed_and_real_timestamp/wm_transition_trace.pb'
    );
    const validShellTransitionsFile = await UnitTestUtils.getFixtureFile(
      'traces/elapsed_and_real_timestamp/shell_transition_trace.pb'
    );
    await loadFiles([
      validSfFile,
      validWmFile,
      validTransactionsFile,
      validWmTransitionsFile,
      validShellTransitionsFile,
    ]);

    await expectLoadResult(4, []);
    const traces = tracePipeline.getTraces();
    const loadedTypes = traces.mapTrace((trace) => trace.type);
    for (const loadedType of loadedTypes) {
      expect(assertDefined(traces.getTrace(loadedType)).type).toEqual(loadedType);
    }
  });

  it('creates zip archive with loaded trace files', async () => {
    const files = [
      await UnitTestUtils.getFixtureFile(
        'traces/elapsed_and_real_timestamp/screen_recording_metadata_v2.mp4'
      ),
      await UnitTestUtils.getFixtureFile('traces/perfetto/transactions_trace.perfetto-trace'),
    ];
    await loadFiles(files);
    await expectLoadResult(2, []);

    const archiveBlob = await tracePipeline.makeZipArchiveWithLoadedTraceFiles();
    const actualFiles = await FileUtils.unzipFile(archiveBlob);
    const actualFilenames = actualFiles
      .map((file) => {
        return file.name;
      })
      .sort();

    const expectedFilenames = [
      'screen_recording_metadata_v2.mp4',
      'transactions_trace.perfetto-trace',
    ];

    expect(actualFilenames).toEqual(expectedFilenames);
  });

  it('can be cleared', async () => {
    await loadFiles([validSfFile, validWmFile]);
    await expectLoadResult(2, []);

    tracePipeline.clear();
    expect(tracePipeline.getTraces().getSize()).toEqual(0);
  });

  async function loadFiles(files: File[], source: FilesSource = FilesSource.TEST) {
    const errorListener = {
      onError(error: WinscopeError) {
        errors.push(error);
      },
    };
    await tracePipeline.loadFiles(files, source, errorListener, progressListener);
    expect(progressListener.onOperationFinished).toHaveBeenCalled();
    await tracePipeline.buildTraces();
  }

  async function expectLoadResult(numberOfTraces: number, expectedErrors: WinscopeError[]) {
    expect(errors).toEqual(expectedErrors);
    expect(tracePipeline.getTraces().getSize()).toEqual(numberOfTraces);
  }
});
