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
import {UserNotificationListenerStub} from 'interfaces/user_notification_listener_stub';
import {ParserError, ParserErrorType} from 'parsers/parser_factory';
import {TracesUtils} from 'test/unit/traces_utils';
import {UnitTestUtils} from 'test/unit/utils';
import {TraceType} from 'trace/trace_type';
import {FilesSource} from './files_source';
import {TracePipeline} from './trace_pipeline';

describe('TracePipeline', () => {
  let validSfFile: File;
  let validWmFile: File;
  let userNotificationListener: UserNotificationListenerStub;
  let parserErrorsSpy: jasmine.Spy<(errors: ParserError[]) => void>;
  let tracePipeline: TracePipeline;

  beforeEach(async () => {
    validSfFile = await UnitTestUtils.getFixtureFile(
      'traces/elapsed_and_real_timestamp/SurfaceFlinger.pb'
    );
    validWmFile = await UnitTestUtils.getFixtureFile(
      'traces/elapsed_and_real_timestamp/WindowManager.pb'
    );

    userNotificationListener = new UserNotificationListenerStub();
    parserErrorsSpy = spyOn(userNotificationListener, 'onParserErrors');
    tracePipeline = new TracePipeline(userNotificationListener);
  });

  it('can load valid trace files', async () => {
    expect(tracePipeline.getTraces().getSize()).toEqual(0);

    await loadValidSfWmTraces();

    expect(tracePipeline.getDownloadArchiveFilename()).toMatch(
      new RegExp(`${FilesSource.UNKNOWN}_`)
    );
    expect(tracePipeline.getTraces().getSize()).toEqual(2);

    const traceEntries = await TracesUtils.extractEntries(tracePipeline.getTraces());
    expect(traceEntries.get(TraceType.WINDOW_MANAGER)?.length).toBeGreaterThan(0);
    expect(traceEntries.get(TraceType.SURFACE_FLINGER)?.length).toBeGreaterThan(0);
  });

  it('can set download archive filename based on files source', async () => {
    await tracePipeline.loadFiles([validSfFile]);
    expect(tracePipeline.getTraces().getSize()).toEqual(1);
    expect(tracePipeline.getDownloadArchiveFilename()).toMatch(new RegExp('SurfaceFlinger_'));

    tracePipeline.clear();

    await tracePipeline.loadFiles([validSfFile, validWmFile], undefined, FilesSource.COLLECTED);
    expect(tracePipeline.getTraces().getSize()).toEqual(2);
    expect(tracePipeline.getDownloadArchiveFilename()).toMatch(
      new RegExp(`${FilesSource.COLLECTED}_`)
    );
  });

  it('can convert illegal uploaded archive filename to legal name for download archive', async () => {
    const fileWithIllegalName = await UnitTestUtils.getFixtureFile(
      'traces/SF_trace&(*_with:_illegal+_characters.pb'
    );
    await tracePipeline.loadFiles([fileWithIllegalName]);
    expect(tracePipeline.getTraces().getSize()).toEqual(1);
    const downloadFilename = tracePipeline.getDownloadArchiveFilename();
    expect(FileUtils.DOWNLOAD_FILENAME_REGEX.test(downloadFilename)).toBeTrue();
  });

  it('can load a new file without dropping already-loaded traces', async () => {
    expect(tracePipeline.getTraces().getSize()).toEqual(0);

    await tracePipeline.loadFiles([validSfFile]);
    expect(tracePipeline.getTraces().getSize()).toEqual(1);

    await tracePipeline.loadFiles([validWmFile]);
    expect(tracePipeline.getTraces().getSize()).toEqual(2);

    await tracePipeline.loadFiles([validWmFile]); // ignored (duplicated)
    expect(tracePipeline.getTraces().getSize()).toEqual(2);
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

    await tracePipeline.loadFiles([bugreportArchive, otherFile]);
    expect(parserErrorsSpy).not.toHaveBeenCalled();

    await tracePipeline.buildTraces();
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

    await tracePipeline.loadFiles(files);
    await tracePipeline.buildTraces();
    const traces = tracePipeline.getTraces();

    expect(traces.getSize()).toEqual(1);
    expect(assertDefined(traces.getTrace(TraceType.TRANSACTIONS)).getDescriptors()).toEqual([
      'transactions_trace.perfetto-trace',
    ]);
  });

  it('is robust to corrupted archive', async () => {
    const corruptedArchive = await UnitTestUtils.getFixtureFile('corrupted_archive.zip');

    await tracePipeline.loadFiles([corruptedArchive]);
    expect(parserErrorsSpy).toHaveBeenCalledOnceWith([
      new ParserError(ParserErrorType.CORRUPTED_ARCHIVE, 'corrupted_archive.zip'),
      new ParserError(ParserErrorType.NO_INPUT_FILES),
    ]);

    await expectNumberOfBuiltTraces(0);
  });

  it('is robust to invalid trace files', async () => {
    const invalidFiles = [await UnitTestUtils.getFixtureFile('winscope_homepage.png')];

    await tracePipeline.loadFiles(invalidFiles);
    expect(parserErrorsSpy).toHaveBeenCalledOnceWith([
      new ParserError(ParserErrorType.UNSUPPORTED_FORMAT, 'winscope_homepage.png'),
    ]);

    await expectNumberOfBuiltTraces(0);
  });

  it('is robust to mixed valid and invalid trace files', async () => {
    expect(tracePipeline.getTraces().getSize()).toEqual(0);
    const files = [
      await UnitTestUtils.getFixtureFile('winscope_homepage.png'),
      await UnitTestUtils.getFixtureFile('traces/dump_WindowManager.pb'),
    ];

    await tracePipeline.loadFiles(files);
    expect(parserErrorsSpy).toHaveBeenCalledOnceWith([
      new ParserError(ParserErrorType.UNSUPPORTED_FORMAT, 'winscope_homepage.png'),
    ]);

    await expectNumberOfBuiltTraces(1);
  });

  it('is robust to trace files with no entries', async () => {
    const traceFilesWithNoEntries = [
      await UnitTestUtils.getFixtureFile('traces/no_entries_InputMethodClients.pb'),
    ];

    await tracePipeline.loadFiles(traceFilesWithNoEntries);
    expect(parserErrorsSpy).not.toHaveBeenCalled();

    await expectNumberOfBuiltTraces(1);
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

    // Expect one trace to be overridden/discarded
    await tracePipeline.loadFiles(filesOfSameTraceType);
    expect(parserErrorsSpy).toHaveBeenCalledOnceWith([
      new ParserError(ParserErrorType.OVERRIDE, 'file1.pb', TraceType.SURFACE_FLINGER),
    ]);

    await expectNumberOfBuiltTraces(1);
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

    await tracePipeline.loadFiles(sfFileOrig);
    expect(parserErrorsSpy).not.toHaveBeenCalled();

    // Expect original trace to be overridden/discarded
    await tracePipeline.loadFiles(sfFileNew);
    expect(parserErrorsSpy).toHaveBeenCalledOnceWith([
      new ParserError(ParserErrorType.OVERRIDE, 'file_orig.pb', TraceType.SURFACE_FLINGER),
    ]);

    await expectNumberOfBuiltTraces(1);
  });

  it('can remove traces', async () => {
    await loadValidSfWmTraces();
    expect(tracePipeline.getTraces().getSize()).toEqual(2);

    const sfTrace = assertDefined(tracePipeline.getTraces().getTrace(TraceType.SURFACE_FLINGER));
    const wmTrace = assertDefined(tracePipeline.getTraces().getTrace(TraceType.WINDOW_MANAGER));

    tracePipeline.removeTrace(sfTrace);
    await expectNumberOfBuiltTraces(1);

    tracePipeline.removeTrace(wmTrace);
    await expectNumberOfBuiltTraces(0);
  });

  it('gets loaded traces', async () => {
    await loadValidSfWmTraces();

    const traces = tracePipeline.getTraces();
    expect(traces.getSize()).toEqual(2);

    const actualTraceTypes = new Set(traces.mapTrace((trace) => trace.type));
    const expectedTraceTypes = new Set([TraceType.SURFACE_FLINGER, TraceType.WINDOW_MANAGER]);
    expect(actualTraceTypes).toEqual(expectedTraceTypes);

    const sfTrace = assertDefined(traces.getTrace(TraceType.SURFACE_FLINGER));
    expect(sfTrace.getDescriptors().length).toBeGreaterThan(0);
  });

  it('builds traces', async () => {
    await loadValidSfWmTraces();
    const traces = tracePipeline.getTraces();

    expect(traces.getTrace(TraceType.SURFACE_FLINGER)).toBeDefined();
    expect(traces.getTrace(TraceType.WINDOW_MANAGER)).toBeDefined();
  });

  it('gets screenrecording data', async () => {
    const files = [
      await UnitTestUtils.getFixtureFile(
        'traces/elapsed_and_real_timestamp/screen_recording_metadata_v2.mp4'
      ),
    ];
    await tracePipeline.loadFiles(files);
    await tracePipeline.buildTraces();

    const video = await tracePipeline.getScreenRecordingVideo();
    expect(video).toBeDefined();
    expect(video!.size).toBeGreaterThan(0);
  });

  it('creates zip archive with loaded trace files', async () => {
    const files = [
      await UnitTestUtils.getFixtureFile(
        'traces/elapsed_and_real_timestamp/screen_recording_metadata_v2.mp4'
      ),
      await UnitTestUtils.getFixtureFile('traces/perfetto/transactions_trace.perfetto-trace'),
    ];
    await tracePipeline.loadFiles(files);
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
    await loadValidSfWmTraces();
    expect(tracePipeline.getTraces().getSize()).toBeGreaterThan(0);

    tracePipeline.clear();
    expect(tracePipeline.getTraces().getSize()).toEqual(0);
  });

  async function loadValidSfWmTraces() {
    await tracePipeline.loadFiles([validSfFile, validWmFile]);
    expect(parserErrorsSpy).not.toHaveBeenCalled();
    await tracePipeline.buildTraces();
  }

  async function expectNumberOfBuiltTraces(n: number) {
    await tracePipeline.buildTraces();
    expect(tracePipeline.getTraces().getSize()).toEqual(n);
  }
});
