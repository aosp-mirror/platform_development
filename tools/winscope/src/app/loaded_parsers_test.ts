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
import {FileUtils} from 'common/file_utils';
import {TimeRange} from 'common/time';
import {UserWarning} from 'messaging/user_warning';
import {TraceHasOldData, TraceOverridden} from 'messaging/user_warnings';
import {FileAndParser} from 'parsers/file_and_parser';
import {FileAndParsers} from 'parsers/file_and_parsers';
import {ParserBuilder} from 'test/unit/parser_builder';
import {TimestampConverterUtils} from 'test/unit/timestamp_converter_utils';
import {Parser} from 'trace/parser';
import {TraceFile} from 'trace/trace_file';
import {TraceType} from 'trace/trace_type';
import {LoadedParsers} from './loaded_parsers';

describe('LoadedParsers', () => {
  const realZeroTimestamp = TimestampConverterUtils.makeRealTimestamp(0n);
  const elapsedZeroTimestamp = TimestampConverterUtils.makeElapsedTimestamp(0n);
  const oldTimestamps = [
    realZeroTimestamp,
    TimestampConverterUtils.makeRealTimestamp(1n),
    TimestampConverterUtils.makeRealTimestamp(2n),
    TimestampConverterUtils.makeRealTimestamp(3n),
    TimestampConverterUtils.makeRealTimestamp(4n),
  ];

  const elapsedTimestamps = [
    elapsedZeroTimestamp,
    TimestampConverterUtils.makeElapsedTimestamp(1n),
    TimestampConverterUtils.makeElapsedTimestamp(2n),
    TimestampConverterUtils.makeElapsedTimestamp(3n),
    TimestampConverterUtils.makeElapsedTimestamp(4n),
  ];

  const timestamps = [
    TimestampConverterUtils.makeRealTimestamp(5n * 60n * 1000000000n + 10n), // 5m10ns
    TimestampConverterUtils.makeRealTimestamp(5n * 60n * 1000000000n + 11n), // 5m11ns
    TimestampConverterUtils.makeRealTimestamp(5n * 60n * 1000000000n + 12n), // 5m12ns
  ];

  const filename = 'filename';

  const parserSf0 = new ParserBuilder<object>()
    .setType(TraceType.SURFACE_FLINGER)
    .setTimestamps(timestamps)
    .setDescriptors([filename])
    .build();
  const parserSf1 = new ParserBuilder<object>()
    .setType(TraceType.SURFACE_FLINGER)
    .setTimestamps(timestamps)
    .setDescriptors([filename])
    .build();
  const parserSf_longButOldData = new ParserBuilder<object>()
    .setType(TraceType.SURFACE_FLINGER)
    .setTimestamps(oldTimestamps)
    .setDescriptors([filename])
    .build();
  const parserSf_empty = new ParserBuilder<object>()
    .setType(TraceType.SURFACE_FLINGER)
    .setTimestamps([])
    .setDescriptors([filename])
    .build();
  const parserSf_elapsed = new ParserBuilder<object>()
    .setType(TraceType.SURFACE_FLINGER)
    .setTimestamps(elapsedTimestamps)
    .setDescriptors([filename])
    .setNoOffsets(true)
    .build();
  const parserWm0 = new ParserBuilder<object>()
    .setType(TraceType.WINDOW_MANAGER)
    .setTimestamps(timestamps)
    .setDescriptors([filename])
    .build();
  const parserWm1 = new ParserBuilder<object>()
    .setType(TraceType.WINDOW_MANAGER)
    .setTimestamps(timestamps)
    .setDescriptors([filename])
    .build();
  const parserWm_dump = new ParserBuilder<object>()
    .setType(TraceType.WINDOW_MANAGER)
    .setTimestamps([realZeroTimestamp])
    .setDescriptors([filename])
    .build();
  const parserWm_elapsed = new ParserBuilder<object>()
    .setType(TraceType.WINDOW_MANAGER)
    .setTimestamps(elapsedTimestamps)
    .setDescriptors([filename])
    .setNoOffsets(true)
    .build();
  const parserWmTransitions = new ParserBuilder<object>()
    .setType(TraceType.WM_TRANSITION)
    .setTimestamps([
      elapsedZeroTimestamp,
      elapsedZeroTimestamp,
      elapsedZeroTimestamp,
    ])
    .setDescriptors([filename])
    .build();
  const parserEventlog = new ParserBuilder<object>()
    .setType(TraceType.EVENT_LOG)
    .setTimestamps(timestamps)
    .setDescriptors([filename])
    .setNoOffsets(true)
    .build();
  const parserScreenRecording = new ParserBuilder<object>()
    .setType(TraceType.SCREEN_RECORDING)
    .setTimestamps(timestamps)
    .setDescriptors([filename])
    .build();
  const parserViewCapture0 = new ParserBuilder<object>()
    .setType(TraceType.VIEW_CAPTURE)
    .setEntries([])
    .setDescriptors([filename])
    .build();
  const parserViewCapture1 = new ParserBuilder<object>()
    .setType(TraceType.VIEW_CAPTURE)
    .setEntries([])
    .setDescriptors([filename])
    .build();

  let loadedParsers: LoadedParsers;
  let warnings: UserWarning[] = [];

  beforeEach(async () => {
    loadedParsers = new LoadedParsers();
    expect(loadedParsers.getParsers().length).toEqual(0);
  });

  it('can load a single legacy parser', () => {
    loadParsers([parserSf0], []);
    expectLoadResult([parserSf0], []);
  });

  it('can load a single perfetto parser', () => {
    loadParsers([], [parserSf0]);
    expectLoadResult([parserSf0], []);
  });

  it('loads multiple perfetto parsers with same trace type', async () => {
    loadParsers([], [parserSf0, parserSf1]);
    expectLoadResult([parserSf0, parserSf1], []);
  });

  it('loads legacy parser without dropping already-loaded legacy parser (different trace type)', async () => {
    loadParsers([parserSf0], []);
    expectLoadResult([parserSf0], []);

    loadParsers([parserWm0], []);
    expectLoadResult([parserSf0, parserWm0], []);
  });

  it('loads legacy parser without dropping already-loaded legacy parser (same trace type)', async () => {
    loadParsers([parserSf0], []);
    expectLoadResult([parserSf0], []);

    loadParsers([parserSf1], []);
    expectLoadResult([parserSf0, parserSf1], []);
  });

  it('drops elapsed-only parsers if parsers with real timestamps present', () => {
    loadParsers([parserSf_elapsed, parserSf0], []);
    expectLoadResult([parserSf0], [new TraceHasOldData(filename)]);
  });

  it('doesnt drop elapsed-only parsers if no parsers with real timestamps present', () => {
    loadParsers([parserSf_elapsed, parserWm_elapsed], []);
    expectLoadResult([parserSf_elapsed, parserWm_elapsed], []);
  });

  it('keeps real-time parsers without offset', () => {
    loadParsers([parserSf0, parserEventlog], []);
    expectLoadResult([parserSf0, parserEventlog], []);
  });

  describe('drops legacy parser with old data (dangling old trace file)', () => {
    const timeGapFrom = assertDefined(
      parserSf_longButOldData.getTimestamps()?.at(-1),
    );
    const timeGapTo = assertDefined(parserWm0.getTimestamps()?.at(0));
    const timeGap = new TimeRange(timeGapFrom, timeGapTo);

    it('taking into account other legacy parsers', () => {
      loadParsers([parserSf_longButOldData, parserWm0], []);
      expectLoadResult([parserWm0], [new TraceHasOldData(filename, timeGap)]);
    });

    it('taking into account perfetto parsers', () => {
      loadParsers([parserSf_longButOldData], [parserWm0]);
      expectLoadResult([parserWm0], [new TraceHasOldData(filename, timeGap)]);
    });

    it('taking into account already-loaded parsers', () => {
      loadParsers([parserWm0], []);

      // Drop parser with old data, even if it provides
      // a longer trace than the already-loaded parser
      loadParsers([parserSf_longButOldData], []);
      expectLoadResult([parserWm0], [new TraceHasOldData(filename, timeGap)]);
    });

    it('doesnt drop legacy parser with dump (zero timestamp)', () => {
      loadParsers([parserWm_dump, parserSf0], []);
      expectLoadResult([parserWm_dump, parserSf0], []);
    });

    it('doesnt drop legacy parser with wm transitions', () => {
      // Only Shell Transition data used to set timestamps of merged Transition trace,
      // so WM Transition data should not be considered by "old data" policy
      loadParsers([parserWmTransitions, parserSf0], []);
      expectLoadResult([parserWmTransitions, parserSf0], []);
    });

    it('is robust to traces with time range overlap', () => {
      const parser = parserSf0;
      const timestamps = assertDefined(parserSf0.getTimestamps());

      const timestampsOverlappingFront = [
        timestamps[0].add(-1n),
        timestamps[0].add(1n),
      ];
      const parserOverlappingFront = new ParserBuilder<object>()
        .setType(TraceType.TRANSACTIONS)
        .setTimestamps(timestampsOverlappingFront)
        .setDescriptors([filename])
        .build();

      const timestampsOverlappingBack = [
        timestamps[timestamps.length - 1].add(-1n),
        timestamps[timestamps.length - 1].add(1n),
      ];
      const parserOverlappingBack = new ParserBuilder<object>()
        .setType(TraceType.TRANSITION)
        .setTimestamps(timestampsOverlappingBack)
        .setDescriptors([filename])
        .build();

      const timestampsOverlappingEntirely = [
        timestamps[0].add(-1n),
        timestamps[timestamps.length - 1].add(1n),
      ];
      const parserOverlappingEntirely = new ParserBuilder<object>()
        .setType(TraceType.VIEW_CAPTURE)
        .setTimestamps(timestampsOverlappingEntirely)
        .setDescriptors([filename])
        .build();

      const timestampsOverlappingExactly = [
        timestamps[0],
        timestamps[timestamps.length - 1],
      ];
      const parserOverlappingExactly = new ParserBuilder<object>()
        .setType(TraceType.WINDOW_MANAGER)
        .setTimestamps(timestampsOverlappingExactly)
        .setDescriptors([filename])
        .build();

      loadParsers(
        [
          parser,
          parserOverlappingFront,
          parserOverlappingBack,
          parserOverlappingEntirely,
          parserOverlappingExactly,
        ],
        [],
      );
      expectLoadResult(
        [
          parser,
          parserOverlappingFront,
          parserOverlappingBack,
          parserOverlappingEntirely,
          parserOverlappingExactly,
        ],
        [],
      );
    });
  });

  it('loads perfetto parser dropping all already-loaded perfetto parsers', () => {
    loadParsers([], [parserSf0, parserWm0]);
    expectLoadResult([parserSf0, parserWm0], []);

    // We currently run only one Perfetto TP WebWorker at a time,
    // so Perfetto parsers previously loaded are now invalid
    // and must be removed (previous WebWorker is not running anymore).
    loadParsers([], [parserSf1, parserWm1]);
    expectLoadResult([parserSf1, parserWm1], []);
  });

  describe('prioritizes perfetto parsers over legacy parsers', () => {
    // While transitioning to the Perfetto format, devices might still have old legacy trace files
    // dangling in the disk that get automatically included into bugreports. Hence, Perfetto parsers
    // must always override legacy ones so that dangling legacy files are ignored.

    it('when a perfetto parser is already loaded', () => {
      loadParsers([parserSf0], [parserSf1]);
      expectLoadResult([parserSf1], [new TraceOverridden(filename)]);

      loadParsers([parserSf0], []);
      expectLoadResult([parserSf1], [new TraceOverridden(filename)]);
    });

    it('when a perfetto parser is loaded afterwards', () => {
      loadParsers([parserSf0], []);
      expectLoadResult([parserSf0], []);

      loadParsers([], [parserSf1]);
      expectLoadResult([parserSf1], [new TraceOverridden(filename)]);
    });
  });

  describe('is robust to multiple parsers of same type loaded at once', () => {
    it('legacy parsers', () => {
      loadParsers([parserSf0, parserSf1], []);
      expectLoadResult([parserSf0, parserSf1], []);
    });

    it('legacy + perfetto parsers', () => {
      loadParsers([parserSf0, parserSf0], [parserSf1]);
      expectLoadResult(
        [parserSf1],
        [new TraceOverridden(filename), new TraceOverridden(filename)],
      );
    });
  });

  describe('is robust to parser with no entries', () => {
    it('legacy parser', () => {
      loadParsers([parserSf_empty], []);
      expectLoadResult([parserSf_empty], []);
    });

    it('perfetto parser', () => {
      loadParsers([], [parserSf_empty]);
      expectLoadResult([parserSf_empty], []);
    });
  });

  describe('handles screen recordings and screenshots', () => {
    const parserScreenRecording0 = new ParserBuilder<object>()
      .setType(TraceType.SCREEN_RECORDING)
      .setTimestamps(timestamps)
      .setDescriptors(['screen_recording.mp4'])
      .build();
    const parserScreenRecording1 = new ParserBuilder<object>()
      .setType(TraceType.SCREEN_RECORDING)
      .setTimestamps(timestamps)
      .setDescriptors(['screen_recording.mp4'])
      .build();
    const parserScreenshot0 = new ParserBuilder<object>()
      .setType(TraceType.SCREENSHOT)
      .setTimestamps(timestamps)
      .setDescriptors(['screenshot.png'])
      .build();
    const parserScreenshot1 = new ParserBuilder<object>()
      .setType(TraceType.SCREENSHOT)
      .setTimestamps(timestamps)
      .setDescriptors(['screenshot.png'])
      .build();
    const overrideError = new TraceOverridden(
      'screenshot.png',
      TraceType.SCREEN_RECORDING,
    );

    it('loads screenshot parser', () => {
      loadParsers([parserScreenshot0], []);
      expectLoadResult([parserScreenshot0], []);
    });

    it('loads screen recording parser', () => {
      loadParsers([parserScreenRecording0], []);
      expectLoadResult([parserScreenRecording0], []);
    });

    it('discards screenshot parser in favour of screen recording parser', () => {
      loadParsers([parserScreenshot0, parserScreenRecording0], []);
      expectLoadResult([parserScreenRecording0], [overrideError]);
    });

    it('does not load screenshot parser after loading screen recording parser in same call', () => {
      loadParsers([parserScreenRecording0, parserScreenshot0], []);
      expectLoadResult([parserScreenRecording0], [overrideError]);
    });

    it('does not load screenshot parser after loading screen recording parser in previous call', () => {
      loadParsers([parserScreenRecording0], []);
      expectLoadResult([parserScreenRecording0], []);

      loadParsers([parserScreenshot0], []);
      expectLoadResult([parserScreenRecording0], [overrideError]);
    });

    it('overrides previously loaded screenshot parser with screen recording parser', () => {
      loadParsers([parserScreenshot0], []);
      expectLoadResult([parserScreenshot0], []);

      loadParsers([parserScreenRecording0], []);
      expectLoadResult([parserScreenRecording0], [overrideError]);
    });

    it('enforces limit of single screenshot or screenrecord parser', () => {
      loadParsers([parserScreenshot0], []);
      expectLoadResult([parserScreenshot0], []);

      loadParsers([parserScreenshot1], []);
      expectLoadResult(
        [parserScreenshot0],
        [new TraceOverridden('screenshot.png', TraceType.SCREENSHOT)],
      );

      loadParsers([parserScreenRecording0], []);
      expectLoadResult(
        [parserScreenRecording0],
        [new TraceOverridden('screenshot.png', TraceType.SCREEN_RECORDING)],
      );

      loadParsers([parserScreenRecording1], []);
      expectLoadResult(
        [parserScreenRecording0],
        [
          new TraceOverridden(
            'screen_recording.mp4',
            TraceType.SCREEN_RECORDING,
          ),
        ],
      );
    });
  });

  it('can remove parsers', () => {
    loadParsers([parserSf0], [parserWm0]);
    expectLoadResult([parserSf0, parserWm0], []);

    loadedParsers.remove(parserWm0);
    expectLoadResult([parserSf0], []);

    loadedParsers.remove(parserSf0);
    expectLoadResult([], []);
  });

  it('can be cleared', () => {
    loadedParsers.clear();
    loadParsers([parserSf0], [parserWm0]);
    expectLoadResult([parserSf0, parserWm0], []);

    loadedParsers.clear();
    expectLoadResult([], []);

    loadParsers([parserSf0], [parserWm0]);
    expectLoadResult([parserSf0, parserWm0], []);
  });

  it('can make zip archive of traces with appropriate directories and extensions', async () => {
    const fileDuplicated = new File([], filename);

    const legacyFiles = [
      // ScreenRecording
      new File([], filename),

      // ViewCapture
      // Multiple parsers point to the same viewcapture file,
      // but we expect to see only one in the output archive (deduplicated)
      fileDuplicated,
      fileDuplicated,

      // WM
      new File([], filename + '.pb'),

      // WM
      // Same filename as above.
      // Expect this file to be automatically renamed to avoid clashes/overwrites
      new File([], filename + '.pb'),
    ];

    loadParsers(
      [
        parserScreenRecording,
        parserViewCapture0,
        parserViewCapture1,
        parserWm0,
        parserWm1,
      ],
      [parserSf0, parserWmTransitions],
      legacyFiles,
    );
    expectLoadResult(
      [
        parserScreenRecording,
        parserViewCapture0,
        parserViewCapture1,
        parserWm0,
        parserWm1,
        parserSf0,
        parserWmTransitions,
      ],
      [],
    );

    const zipArchive = await loadedParsers.makeZipArchive();
    const zipFile = new File([zipArchive], 'winscope.zip');
    const actualArchiveContents = (await FileUtils.unzipFile(zipFile))
      .map((file) => file.name)
      .sort();

    const expectedArchiveContents = [
      'filename.mp4',
      'filename.perfetto-trace',
      'vc/filename.winscope',
      'wm/filename (1).pb',
      'wm/filename.pb',
    ];
    expect(actualArchiveContents).toEqual(expectedArchiveContents);
  });

  function loadParsers(
    legacy: Array<Parser<object>>,
    perfetto: Array<Parser<object>>,
    legacyFiles?: File[],
  ) {
    const legacyFileAndParsers = legacy.map((parser, i) => {
      const legacyFile = legacyFiles ? legacyFiles[i] : new File([], filename);
      return new FileAndParser(new TraceFile(legacyFile), parser);
    });

    const perfettoTraceFile = new TraceFile(new File([], filename));
    const perfettoFileAndParsers =
      perfetto.length > 0
        ? new FileAndParsers(perfettoTraceFile, perfetto)
        : undefined;

    warnings = [];
    const listener = {
      onNotifications(notifications: UserWarning[]) {
        warnings.push(...notifications);
      },
    };

    loadedParsers.addParsers(
      legacyFileAndParsers,
      perfettoFileAndParsers,
      listener,
    );
  }

  function expectLoadResult(
    expectedParsers: Array<Parser<object>>,
    expectedWarnings: UserWarning[],
  ) {
    const actualParsers = loadedParsers.getParsers();
    expect(actualParsers.length).toEqual(expectedParsers.length);
    expect(new Set([...actualParsers])).toEqual(new Set([...expectedParsers]));

    expect(warnings).toEqual(expectedWarnings);
  }
});
