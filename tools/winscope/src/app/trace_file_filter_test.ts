/*
 * Copyright (C) 2023 The Android Open Source Project
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
import {MissingPersistentTrace, TraceOverridden} from 'messaging/user_warnings';
import {
  BugreportFileSelected,
  WinscopeEventType,
} from 'messaging/winscope_event';
import {getFixtureFile} from 'test/unit/fixture_utils';
import {UserNotifierChecker} from 'test/unit/user_notifier_checker';
import {TraceFile} from 'trace/trace_file';
import {BuildType, TraceFileFilter} from './trace_file_filter';

describe('TraceFileFilter', () => {
  const filter = new TraceFileFilter();

  // Could be any file, we just need an instance of File to be used as a fake bugreport archive
  const bugreportArchive = new File(
    [new ArrayBuffer(0)],
    'test_bugreport.zip',
  ) as unknown as File;

  let userNotifierChecker: UserNotifierChecker;

  beforeAll(() => {
    userNotifierChecker = new UserNotifierChecker();
  });

  beforeEach(() => {
    userNotifierChecker.reset();
  });

  describe('bugreport (detects it is a bugreport)', () => {
    it('ignores non-trace dirs', async () => {
      const pickedBugreportFiles = [
        makeTraceFile(
          'FS/data/misc/wmtrace/surface_flinger.bp',
          bugreportArchive,
        ),
        makeTraceFile('FS/data/misc/wmtrace/transactions.bp', bugreportArchive),
        makeTraceFile('proto/window_CRITICAL.proto', bugreportArchive),
        makeTraceFile('proto/input_method_CRITICAL.proto', bugreportArchive),
        makeTraceFile('proto/SurfaceFlinger_CRITICAL.proto', bugreportArchive),
      ];

      const ignoredBugreportFile = makeTraceFile(
        'FS/data/misc/ignored-dir/wm_transition_trace.bp',
        bugreportArchive,
      );

      const bugreportFiles = [
        await makeBugreportMainEntryTraceFile(),
        await makeBugreportCodenameTraceFile(),
        ...pickedBugreportFiles,
        ignoredBugreportFile,
      ];

      // Corner case:
      // A plain trace file is loaded along the bugreport
      //    -> trace file must not be ignored
      //
      // Note:
      // The even weirder corner case where two bugreports are loaded at the same time is
      // currently not properly handled.
      const plainTraceFile = makeTraceFile(
        'would-be-ignored-if-was-part-of-bugreport/input_method_clients.pb',
      );

      const result = await filter.filter([...bugreportFiles, plainTraceFile]);
      expect(result.perfetto).toBeUndefined();

      const expectedLegacy = new Set([...pickedBugreportFiles, plainTraceFile]);
      const actualLegacy = new Set(result.legacy);
      expect(actualLegacy).toEqual(expectedLegacy);
      userNotifierChecker.expectNone();
    });

    it('picks perfetto sysui.pftrace (persistent session)', async () => {
      const perfettoSysUi = makeTraceFile(
        'FS/data/misc/perfetto-traces/bugreport/sysui.pftrace',
        bugreportArchive,
      );
      const otherFiles = [
        makeTraceFile(
          'FS/data/misc/perfetto-traces/other.perfetto-trace',
          bugreportArchive,
        ),
        makeTraceFile(
          'FS/data/misc/perfetto-traces/other.pftrace',
          bugreportArchive,
        ),
        makeTraceFile(
          'FS/data/misc/perfetto-traces/bugreport/other.pftrace',
          bugreportArchive,
          10,
        ),
      ];
      await checkPerfettoPicked(perfettoSysUi, otherFiles);
    });

    it('picks perfetto systrace.pftrace (traceur or aot session) over sysui.pftrace', async () => {
      const perfettoSysTrace = makeTraceFile(
        'FS/data/misc/perfetto-traces/bugreport/systrace.pftrace',
        bugreportArchive,
      );
      await checkPerfettoPicked(perfettoSysTrace, [
        makeTraceFile(
          'FS/data/misc/perfetto-traces/bugreport/sysui.pftrace',
          bugreportArchive,
          10,
        ),
      ]);
    });

    it('picks single file in perfetto directory', async () => {
      const perfettoTest = makeTraceFile(
        'FS/data/misc/perfetto-traces/bugreport/test.pftrace',
        bugreportArchive,
      );
      await checkPerfettoPicked(perfettoTest, []);
    });

    it('sends request for file selection if multiple files in perfetto directory', async () => {
      let requested: string[] | undefined;
      filter.setEmitEvent(async (event) => {
        await event.visit(
          WinscopeEventType.BUGREPORT_FILE_SELECTION_REQUEST,
          async (event) => {
            requested = event.filenames;
            await filter.onWinscopeEvent(
              new BugreportFileSelected(event.filenames[1]),
            );
          },
        );
      });

      const perfettoTest = makeTraceFile(
        'FS/data/misc/perfetto-traces/bugreport/test.pftrace',
        bugreportArchive,
      );
      const perfettoOther = makeTraceFile(
        'FS/data/misc/perfetto-traces/bugreport/other.pftrace',
        bugreportArchive,
      );
      await checkPerfettoPicked(perfettoOther, [perfettoTest]);
    });

    it('ignores perfetto traces not in bugreport directory', async () => {
      const bugreportFiles = [
        await makeBugreportMainEntryTraceFile(),
        await makeBugreportCodenameTraceFile(),
        makeTraceFile(
          'FS/data/misc/perfetto-traces/other.perfetto-trace',
          bugreportArchive,
        ),
        makeTraceFile(
          'FS/data/misc/perfetto-traces/other.pftrace',
          bugreportArchive,
        ),
      ];
      const result = await filter.filter(bugreportFiles);
      expect(result.perfetto).toBeUndefined();
      expect(result.legacy).toEqual([]);
      userNotifierChecker.expectNone();
    });

    it('identifies timezone information from bugreport codename file', async () => {
      const legacyFile = makeTraceFile(
        'proto/window_CRITICAL.proto',
        bugreportArchive,
      );
      const bugreportFiles = [
        await makeBugreportMainEntryTraceFile(),
        await makeBugreportCodenameTraceFile(),
        legacyFile,
      ];
      const result = await filter.filter(bugreportFiles);
      expect(result.legacy).toEqual([legacyFile]);
      expect(result.perfetto).toBeUndefined();
      expect(result.timezoneInfo).toEqual({
        timezone: 'Asia/Kolkata',
        locale: 'en-US',
      });
      userNotifierChecker.expectNone();
    });

    it('unzips trace files within bugreport zip', async () => {
      const zippedTraceFile = await makeZippedTraceFile();

      const bugreportFiles = [
        await makeBugreportMainEntryTraceFile(),
        await makeBugreportCodenameTraceFile(),
        zippedTraceFile,
      ];

      const result = await filter.filter(bugreportFiles);
      expect(result.perfetto).toBeUndefined();
      expect(result.legacy.map((file) => file.file.name)).toEqual([
        'Surface Flinger/SurfaceFlinger.pb',
        'Window Manager/WindowManager.pb',
      ]);
      userNotifierChecker.expectNone();
    });

    it('warns about missing trace on user build', async () => {
      await checkMissingPerfettoTraceWarning(BuildType.USER, undefined, [
        "'user' builds",
        'expected',
      ]);
    });

    it('warns about missing trace on userdebug build with persistent flag disabled', async () => {
      await checkMissingPerfettoTraceWarning(BuildType.USERDEBUG, '0', [
        'seems to be disabled',
        'adb shell setprop',
      ]);
    });

    it('warns about missing trace on userdebug build with persistent flag enabled', async () => {
      await checkMissingPerfettoTraceWarning(BuildType.USERDEBUG, '1', [
        'No Winscope Perfetto trace found in bug report. Ensure the bugreport comes from a device where persistent tracing is enabled',
      ]);
    });

    it('warns about missing trace on userdebug build with persistent flag unknown', async () => {
      await checkMissingPerfettoTraceWarning(BuildType.USERDEBUG, undefined, [
        'No Winscope Perfetto trace found in bug report.',
        "The persistent tracing property ('persist.debug.perfetto.persistent') seems to be disabled",
      ]);
    });

    it('warns about missing trace on eng build with persistent flag disabled', async () => {
      await checkMissingPerfettoTraceWarning(BuildType.ENG, '0', [
        'No Winscope Perfetto trace found in bug report.',
        "The persistent tracing property ('persist.debug.perfetto.persistent') seems to be disabled",
      ]);
    });

    it('warns about missing trace on eng build with persistent flag enabled', async () => {
      await checkMissingPerfettoTraceWarning(BuildType.ENG, '1', [
        'No Winscope Perfetto trace found in bug report. Ensure the bugreport comes from a device where persistent tracing is enabled',
      ]);
    });

    it('does not warn if a valid perfetto trace is found', async () => {
      const perfettoSysTrace = makeTraceFile(
        'FS/data/misc/perfetto-traces/bugreport/systrace.pftrace',
        bugreportArchive,
      );
      const mainBugreportFilename = 'bugreport-user-build.txt';
      const bugreportFiles = [
        await makeCustomBugreportMainEntryTraceFile(mainBugreportFilename),
        makeMainBugreportFile(mainBugreportFilename, {
          'persist.sys.timezone': 'America/Los_Angeles',
        }),
        perfettoSysTrace, // Include the trace file
      ];

      const result = await filter.filter(bugreportFiles);
      expect(result.perfetto).toEqual(perfettoSysTrace);
      expect(result.criticalWarnings?.length).toEqual(0); // No warnings expected
      userNotifierChecker.expectNone();
    });

    async function checkPerfettoPicked(
      perfetto: TraceFile,
      other: TraceFile[],
    ) {
      const bugreportFiles = [
        await makeBugreportMainEntryTraceFile(),
        await makeBugreportCodenameTraceFile(),
        ...other,
        perfetto,
      ];
      const result = await filter.filter(bugreportFiles);
      expect(result.perfetto).toEqual(perfetto);
      expect(result.legacy).toEqual([]);
      userNotifierChecker.expectNone();
    }
  });

  describe('plain input (no bugreport)', () => {
    it('picks perfetto trace with .perfetto-trace extension', async () => {
      const perfettoTrace = makeTraceFile('file.perfetto-trace');
      await checkPerfettoFilePickedWithoutErrors(perfettoTrace);
    });

    it('picks perfetto trace with .pftrace extension', async () => {
      const pftrace = makeTraceFile('file.pftrace');
      await checkPerfettoFilePickedWithoutErrors(pftrace);
    });

    it('picks perfetto trace with .perfetto extension', async () => {
      const perfetto = makeTraceFile('file.perfetto');
      await checkPerfettoFilePickedWithoutErrors(perfetto);
    });

    it('picks perfetto trace with .perfetto-trace.gz extension', async () => {
      const perfettoTraceGz = makeTraceFile('file.perfetto-trace.gz');
      await checkPerfettoFilePickedWithoutErrors(perfettoTraceGz);
    });

    it('picks perfetto trace with .pftrace.gz extension', async () => {
      const pftraceGz = makeTraceFile('file.pftrace.gz');
      await checkPerfettoFilePickedWithoutErrors(pftraceGz);
    });

    it('picks perfetto trace with .perfetto.gz extension', async () => {
      const perfettoGz = makeTraceFile('file.perfetto.gz');
      await checkPerfettoFilePickedWithoutErrors(perfettoGz);
    });

    it('picks largest perfetto trace', async () => {
      const small = makeTraceFile('small.perfetto-trace', undefined, 10);
      const medium = makeTraceFile('medium.perfetto-trace', undefined, 20);
      const large = makeTraceFile('large.perfetto-trace', undefined, 30);
      const result = await filter.filter([small, large, medium]);
      expect(result.perfetto).toEqual(large);
      expect(result.legacy).toEqual([]);
      userNotifierChecker.expectAdded([
        new TraceOverridden(small.getDescriptor()),
        new TraceOverridden(medium.getDescriptor()),
      ]);
    });

    it('extracts screen recording metadata', async () => {
      const metadataJson = await makeMetadataJsonFile();
      const screenRecording = makeTraceFile('screen_recording.mp4');
      const result = await filter.filter([screenRecording, metadataJson]);
      expect(result.legacy).toEqual([screenRecording]);
      expect(result.metadata.screenRecordingOffsets).toEqual({
        elapsedRealTimeNanos: 0n,
        realToElapsedTimeOffsetNanos: 1732721670187419904n,
      });
      userNotifierChecker.expectNone();
    });

    async function checkPerfettoFilePickedWithoutErrors(
      perfettoFile: TraceFile,
    ) {
      const result = await filter.filter([perfettoFile]);
      expect(result.perfetto).toEqual(perfettoFile);
      expect(result.legacy).toEqual([]);
      userNotifierChecker.expectNone();
    }
  });

  async function checkMissingPerfettoTraceWarning(
    buildType: BuildType,
    persistentFlag: string | undefined,
    expectedMessageSubstrings: string[],
  ) {
    const mainBugreportFilename = `bugreport-${buildType}-build${
      persistentFlag ? '-flag' + persistentFlag : ''
    }.txt`;
    const properties: {[key: string]: string} = {
      'ro.build.type': buildType,
      'persist.sys.timezone': 'America/Los_Angeles', // Example timezone
    };
    if (persistentFlag !== undefined) {
      properties[
        'persist.debug.perfetto.persistent_sysui_tracing_for_bugreport'
      ] = persistentFlag;
    }

    const bugreportFiles = [
      await makeCustomBugreportMainEntryTraceFile(mainBugreportFilename),
      makeMainBugreportFile(mainBugreportFilename, properties),
    ];

    const result = await filter.filter(bugreportFiles);

    expect(result.perfetto).toBeUndefined();
    expect(result.criticalWarnings).toBeDefined();
    expect(result.criticalWarnings?.length).toEqual(1);
    const warning = assertDefined(
      result.criticalWarnings,
    )[0] as MissingPersistentTrace;
    expect(warning).toBeInstanceOf(MissingPersistentTrace);

    const actualMessage = warning.getMessage();
    expectedMessageSubstrings.forEach((substring) => {
      expect(actualMessage).toContain(substring);
    });

    userNotifierChecker.expectNone();
  }

  function makeTraceFile(
    filename: string,
    parentArchive?: File,
    size?: number,
  ): TraceFile {
    size = size ?? 0;
    const file = new File([new ArrayBuffer(size)], filename);
    return new TraceFile(file as unknown as File, parentArchive);
  }

  function makeMainBugreportFile(
    filename: string, // Should match the content of main_entry.txt
    properties: {[key: string]: string},
    parentArchive?: File,
  ): TraceFile {
    let content = 'some initial bugreport content...\n';
    for (const [key, value] of Object.entries(properties)) {
      // Add other properties if needed for testing timezone etc.
      content += `[${key}]: [${value}]\n`;
    }
    content += '...some trailing bugreport content\n';

    const file = new File([content], filename);
    return new TraceFile(file, parentArchive ?? bugreportArchive);
  }

  async function makeCustomBugreportMainEntryTraceFile(
    mainBugreportFilename = 'bugreport-codename_beta-UPB2.230407.019-2023-05-30-14-33-48.txt',
  ): Promise<TraceFile> {
    // Ensure the content matches the filename used in makeMainBugreportFile
    const file = new File([mainBugreportFilename], 'main_entry.txt');
    return new TraceFile(file, bugreportArchive);
  }

  async function makeBugreportMainEntryTraceFile(): Promise<TraceFile> {
    const file = await getFixtureFile(
      'bugreports/main_entry.txt',
      'main_entry.txt',
    );
    return new TraceFile(file, bugreportArchive);
  }

  async function makeBugreportCodenameTraceFile(): Promise<TraceFile> {
    const file = await getFixtureFile(
      'bugreports/bugreport-codename_beta-UPB2.230407.019-2023-05-30-14-33-48.txt',
      'bugreport-codename_beta-UPB2.230407.019-2023-05-30-14-33-48.txt',
    );
    return new TraceFile(file, bugreportArchive);
  }

  async function makeZippedTraceFile(): Promise<TraceFile> {
    const file = await getFixtureFile(
      'archives/winscope.zip',
      'FS/data/misc/wmtrace/winscope.zip',
    );
    return new TraceFile(file, bugreportArchive);
  }

  async function makeMetadataJsonFile(): Promise<TraceFile> {
    const file = await getFixtureFile(
      'traces/elapsed_and_real_timestamp/screen_recording_metadata.json',
    );
    return new TraceFile(file, bugreportArchive);
  }
});
