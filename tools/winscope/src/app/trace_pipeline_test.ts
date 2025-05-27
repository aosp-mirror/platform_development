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
import {
  TimestampConverterUtils,
  timestampEqualityTester,
} from 'common/time/test_utils';
import {ProgressListenerStub} from 'messaging/progress_listener_stub';
import {UserWarning} from 'messaging/user_warning';
import {
  CorruptedArchive,
  InvalidPerfettoTrace,
  NoValidFiles,
  TraceOverridden,
  UnsupportedFileFormat,
} from 'messaging/user_warnings';
import {BugreportFileSelected} from 'messaging/winscope_event';
import {LegacyToPerfettoConverter} from 'parsers/legacy_to_perfetto_converter';
import {getFixtureFile} from 'test/unit/fixture_utils';
import {extractEntries} from 'test/unit/traces_utils';
import {UserNotifierChecker} from 'test/unit/user_notifier_checker';
import {Parser} from 'trace/parser';
import {TraceFile} from 'trace/trace_file';
import {TraceType} from 'trace/trace_type';
import {QueryResult} from 'trace_processor/query_result';
import {TraceProcessor} from 'trace_processor/trace_processor';
import {FilesSource} from './files_source';
import {TraceFileFilter} from './trace_file_filter';
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
  let perfettoFile: File;
  let elapsedFile: File;

  let progressListener: ProgressListenerStub;
  let tracePipeline: TracePipeline;
  let userNotifierChecker: UserNotifierChecker;

  beforeAll(async () => {
    userNotifierChecker = new UserNotifierChecker();
    wmTransitionFile = await getFixtureFile(
      'traces/elapsed_and_real_timestamp/wm_transition_trace.pb',
    );
    shellTransitionFile = await getFixtureFile(
      'traces/elapsed_and_real_timestamp/shell_transition_trace.pb',
    );
    validSfFile = await getFixtureFile(
      'traces/elapsed_and_real_timestamp/SurfaceFlinger.pb',
    );
    validWmFile = await getFixtureFile(
      'traces/elapsed_and_real_timestamp/WindowManager.pb',
    );
    screenshotFile = await getFixtureFile('traces/screenshot/screenshot.png');
    screenRecordingFile = await getFixtureFile(
      'traces/elapsed_and_real_timestamp/screen_recording_metadata_v2.mp4',
    );
    brMainEntryFile = await getFixtureFile(
      'bugreports/main_entry.txt',
      'main_entry.txt',
    );
    brCodenameFile = await getFixtureFile(
      'bugreports/bugreport-codename_beta-UPB2.230407.019-2023-05-30-14-33-48.txt',
      'bugreport-codename_beta-UPB2.230407.019-2023-05-30-14-33-48.txt',
    );
    brSfFile = await getFixtureFile(
      'traces/elapsed_and_real_timestamp/SurfaceFlinger.pb',
      'FS/data/misc/wmtrace/surface_flinger.bp',
    );
    jpgFile = await getFixtureFile('invalid_files/winscope_homepage.jpg');
    perfettoFile = await getFixtureFile(
      'traces/perfetto/protolog.perfetto-trace',
      'traces/perfetto/protolog',
    );
    elapsedFile = await getFixtureFile(
      'traces/elapsed_timestamp/SurfaceFlinger.pb',
    );
  });

  beforeEach(async () => {
    jasmine.addCustomEqualityTester(timestampEqualityTester);

    progressListener = new ProgressListenerStub();
    spyOn(progressListener, 'onProgressUpdate');
    spyOn(progressListener, 'onOperationFinished');

    tracePipeline = new TracePipeline();
  });

  afterEach(() => {
    userNotifierChecker.expectNone();
    userNotifierChecker.reset();
  });

  it('can load valid trace files', async () => {
    expect(tracePipeline.getTraces().getSize()).toEqual(0);

    await loadFiles([validSfFile, validWmFile], FilesSource.TEST);
    await expectLoadResult(2, []);

    expect(tracePipeline.getDownloadArchiveFilename()).toMatch(
      new RegExp(`${FilesSource.TEST}_`),
    );
    expect(tracePipeline.getTraces().getSize()).toEqual(2);

    const traces = tracePipeline.getTraces();
    expect(
      traces.getTrace(TraceType.WINDOW_MANAGER)?.lengthEntries,
    ).toBeGreaterThan(0);
    expect(
      traces.getTrace(TraceType.SURFACE_FLINGER)?.lengthEntries,
    ).toBeGreaterThan(0);
  });

  it('can load valid gzipped file and archive', async () => {
    expect(tracePipeline.getTraces().getSize()).toEqual(0);

    const gzippedFile = await getFixtureFile('archives/WindowManager.pb.gz');
    const gzippedArchive = await getFixtureFile(
      'archives/WindowManager.zip.gz',
    );

    await loadFiles([gzippedFile, gzippedArchive], FilesSource.TEST);
    await expectLoadResult(2, []);

    const traces = tracePipeline.getTraces();
    expect(traces.getSize()).toEqual(2);
    expect(traces.getTraces(TraceType.WINDOW_MANAGER).length).toEqual(2);

    const traceEntries = await extractEntries(traces);
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
    const fileWithIllegalName = await getFixtureFile(
      'traces/elapsed_and_real_timestamp/SFtrace(with_illegal_characters).pb',
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
      await getFixtureFile(
        'traces/elapsed_and_real_timestamp/WindowManager.pb',
        'FS/data/misc/ignored-dir/window_manager.pb',
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
    const otherFile = await getFixtureFile(
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

  it('forwards winscope events to file filter', async () => {
    const setEmitEventSpy = spyOn(TraceFileFilter.prototype, 'setEmitEvent');
    const emitEventSpy = jasmine.createSpy();
    tracePipeline.setEmitEvent(emitEventSpy);
    expect(setEmitEventSpy).toHaveBeenCalledOnceWith(emitEventSpy);

    const onEventSpy = spyOn(TraceFileFilter.prototype, 'onWinscopeEvent');
    const testEvent = new BugreportFileSelected('f1');
    tracePipeline.onWinscopeEvent(testEvent);
    expect(onEventSpy).toHaveBeenCalledOnceWith(testEvent);
  });

  it('is robust to corrupted archive', async () => {
    const corruptedArchive = await getFixtureFile(
      'invalid_files/corrupted_archive.zip',
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

  it('notifies for unsupported file uploaded before valid file', async () => {
    const invalidFiles = [jpgFile, perfettoFile];
    await loadFiles(invalidFiles);
    await expectLoadResult(1, [
      new UnsupportedFileFormat('winscope_homepage.jpg'),
    ]);
  });

  it('notifies for unsupported file uploaded after valid file', async () => {
    const invalidFiles = [perfettoFile, jpgFile];
    await loadFiles(invalidFiles);
    await expectLoadResult(1, [
      new UnsupportedFileFormat('winscope_homepage.jpg'),
    ]);
  });

  it('is robust to invalid perfetto trace files', async () => {
    const invalidFiles = [
      await getFixtureFile('invalid_files/invalid_protolog.perfetto-trace'),
    ];
    await loadFiles(invalidFiles);
    await expectLoadResult(0, [
      new InvalidPerfettoTrace('invalid_protolog.perfetto-trace', [
        'Perfetto trace has no Winscope trace entries',
      ]),
    ]);
  });

  it('surfaces information about packet loss', async () => {
    await loadFiles([perfettoFile]);
    expect(tracePipeline.lostPackets()).toEqual(0);

    const queryResultObj = jasmine.createSpyObj<QueryResult>('result', [
      'numRows',
      'firstRow',
    ]);
    queryResultObj.numRows.and.returnValue(1);
    queryResultObj.firstRow.and.returnValue({value: 2n});

    const spy = spyOn(TraceProcessor.prototype, 'query').and.callThrough();
    spy
      .withArgs(
        'SELECT name, value FROM stats ' +
          "WHERE name = 'traced_buf_trace_writer_packet_loss'",
      )
      .and.returnValue(Promise.resolve(queryResultObj));
    await loadFiles([perfettoFile]);
    expect(tracePipeline.lostPackets()).toEqual(2);

    queryResultObj.numRows.and.returnValue(0);
    await loadFiles([perfettoFile]); // clears lost packets from previous load on overwrite
    expect(tracePipeline.lostPackets()).toEqual(0);

    queryResultObj.numRows.and.returnValue(1);
    await loadFiles([perfettoFile]);
    expect(tracePipeline.lostPackets()).toEqual(2);
    tracePipeline.clear(); // resets lost packets on explicit clear call
    expect(tracePipeline.lostPackets()).toEqual(0);
  });

  it('is robust to mixed valid and invalid trace files', async () => {
    expect(tracePipeline.getTraces().getSize()).toEqual(0);
    const files = [jpgFile, elapsedFile];

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
      await getFixtureFile(
        'traces/elapsed_and_real_timestamp/eventlog.winscope',
      ),
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
      await getFixtureFile('traces/perfetto/input-events.perfetto-trace'),
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
      await getFixtureFile('traces/perfetto/transactions_trace.perfetto-trace'),
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
    await loadFiles([shellTransitionFile]);
    await expectLoadResult(1, []);

    tracePipeline.filterTracesWithoutVisualization();
    expect(tracePipeline.getTraces().getSize()).toEqual(0);
    expect(
      tracePipeline.getTraces().getTrace(TraceType.SHELL_TRANSITION),
    ).toBeUndefined();
  });

  it('tries to create search trace', async () => {
    await loadFiles([perfettoFile]);
    const validQuery = 'select ts from protolog';
    expect(await tracePipeline.tryCreateSearchTrace(validQuery)).toBeDefined();
    expect(await tracePipeline.tryCreateSearchTrace('fail')).toBeUndefined();
    userNotifierChecker.reset();
  });

  it('creates screen recording using metadata', async () => {
    const screenRecording = await getFixtureFile(
      'traces/elapsed_and_real_timestamp/screen_recording_no_metadata.mp4',
    );
    const metadata = await getFixtureFile(
      'traces/elapsed_and_real_timestamp/screen_recording_metadata.json',
    );
    await loadFiles([screenRecording, metadata]);
    await expectLoadResult(1, []);
  });

  it('discards legacy traces without conversion', async () => {
    await loadFiles([validSfFile, screenshotFile]);
    expectLoadResult(2, []);
    tracePipeline.discardLegacyTraces();
    const traces = tracePipeline.getTraces();
    expect(traces.getSize()).toEqual(1);
    expect(traces.getTrace(TraceType.SCREENSHOT)).toBeDefined();
  });

  describe('legacy to perfetto conversion', () => {
    let parserSf: Parser<object>;
    let converterSpy: jasmine.Spy;

    beforeEach(async () => {
      converterSpy = spyOn(
        LegacyToPerfettoConverter,
        'convertToSinglePerfettoFile',
      ).and.callThrough();
      await loadFiles([validSfFile]);
      parserSf = assertDefined(
        tracePipeline
          .getTraces()
          .getTrace(TraceType.SURFACE_FLINGER)
          ?.getParser(),
      );
    });

    it('robust to no available legacy-to-perfetto conversions', async () => {
      tracePipeline.clear();
      await loadFiles([screenshotFile]);
      await tracePipeline.convertLegacyTracesToPerfetto();
      expect(converterSpy).not.toHaveBeenCalled();
    });

    it('robust to failed legacy-to-perfetto conversion', async () => {
      converterSpy.and.returnValue(Promise.resolve(undefined));
      await expectAsync(
        tracePipeline.convertLegacyTracesToPerfetto(),
      ).not.toBeRejected();
      expect(converterSpy).toHaveBeenCalledTimes(1);
    });

    it('robust to no perfetto data in converted file', async () => {
      converterSpy.and.returnValue(Promise.resolve(new TraceFile(validSfFile)));
      await tracePipeline.convertLegacyTracesToPerfetto();
      userNotifierChecker.expectAdded([
        new InvalidPerfettoTrace('SurfaceFlinger.pb', [
          'failed to convert legacy parsers into perfetto trace',
        ]),
      ]);
      userNotifierChecker.reset();
    });

    it('with single legacy trace', async () => {
      await tracePipeline.convertLegacyTracesToPerfetto();
      expect(converterSpy).toHaveBeenCalledOnceWith(
        [parserSf],
        [parserSf],
        undefined,
      );
      expect(tracePipeline.getTraces().getSize()).toEqual(1);
      checkSfTraceIsPerfetto();
    });

    it('with perfetto parser loaded', async () => {
      await loadFiles([perfettoFile]);
      const parserPerfetto = getParser(TraceType.PROTO_LOG);
      await tracePipeline.convertLegacyTracesToPerfetto();
      expect(converterSpy).toHaveBeenCalledOnceWith(
        [parserSf],
        [parserSf, parserPerfetto],
        new TraceFile(perfettoFile),
      );
      expect(tracePipeline.getTraces().getSize()).toEqual(2);
      checkSfTraceIsPerfetto();
    });

    it('with multiple legacy traces', async () => {
      await loadFiles([validWmFile]);
      const parserWm = getParser(TraceType.WINDOW_MANAGER);
      await tracePipeline.convertLegacyTracesToPerfetto();
      expect(converterSpy).toHaveBeenCalledOnceWith(
        [parserSf, parserWm],
        [parserSf, parserWm],
        undefined,
      );
      expect(tracePipeline.getTraces().getSize()).toEqual(2);
      checkSfTraceIsPerfetto();
    });

    function checkSfTraceIsPerfetto() {
      const traces = tracePipeline.getTraces();
      const trace = traces.getTrace(TraceType.SURFACE_FLINGER);
      expect(trace?.isPerfetto()).toBeTrue();
    }

    function getParser(type: TraceType): Parser<{}> {
      return assertDefined(
        tracePipeline.getTraces().getTrace(type)?.getParser(),
      );
    }
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
    userNotifierChecker.reset();
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
