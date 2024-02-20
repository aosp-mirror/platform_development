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
import {TimeRange, TimestampType} from 'common/time';
import {NO_TIMEZONE_OFFSET_FACTORY} from 'common/timestamp_factory';
import {
  TraceHasOldData,
  TraceOverridden,
  WinscopeError,
} from 'messaging/winscope_error';
import {FileAndParser} from 'parsers/file_and_parser';
import {FileAndParsers} from 'parsers/file_and_parsers';
import {ParserBuilder} from 'test/unit/parser_builder';
import {Parser} from 'trace/parser';
import {TraceFile} from 'trace/trace_file';
import {TraceType} from 'trace/trace_type';
import {LoadedParsers} from './loaded_parsers';

describe('LoadedParsers', () => {
  const oldTimestamps = [
    NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(0n),
    NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1n),
    NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(2n),
    NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(3n),
    NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(4n),
  ];

  const timestamps = [
    NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(5n * 60n * 1000000000n + 10n), // 5m10ns
    NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(5n * 60n * 1000000000n + 11n), // 5m11ns
    NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(5n * 60n * 1000000000n + 12n), // 5m12ns
  ];

  const filename = 'filename';
  const file = new TraceFile(new File([], filename));

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
    .setTimestamps([NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(0n)])
    .setDescriptors([filename])
    .build();

  let loadedParsers: LoadedParsers;
  let errors: WinscopeError[] = [];

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

  it('loads legacy parser without dropping already-loaded legacy parser with different type', async () => {
    loadParsers([parserSf0], []);
    expectLoadResult([parserSf0], []);

    loadParsers([parserWm0], []);
    expectLoadResult([parserSf0, parserWm0], []);
  });

  it('loads legacy parser overriding already-loaded legacy parser with same type (newly loaded file/archive always wins)', () => {
    loadParsers([parserSf0], []);
    expectLoadResult([parserSf0], []);

    loadParsers([parserSf1], []);
    expectLoadResult([parserSf1], [new TraceOverridden(filename)]);
  });

  it('gives priority to parsers with longer data', () => {
    loadParsers([parserWm0, parserWm_dump], []);
    expectLoadResult([parserWm0], [new TraceOverridden(filename)]);
  });

  describe('drops legacy parser with old data (dangling old trace file)', () => {
    const timeGapFrom = assertDefined(
      parserSf_longButOldData.getTimestamps(TimestampType.REAL)?.at(-1),
    );
    const timeGapTo = assertDefined(
      parserWm0.getTimestamps(TimestampType.REAL)?.at(0),
    );
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

    it('is robust to traces with time range overlap', () => {
      const parser = parserSf0;
      const timestamps = assertDefined(
        parserSf0.getTimestamps(TimestampType.REAL),
      );

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
      expectLoadResult([parserSf0], [new TraceOverridden(filename)]);
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
    const parserScreenRecording = new ParserBuilder<object>()
      .setType(TraceType.SCREEN_RECORDING)
      .setTimestamps(timestamps)
      .setDescriptors(['screen_recording.mp4'])
      .build();
    const parserScreenshot = new ParserBuilder<object>()
      .setType(TraceType.SCREENSHOT)
      .setTimestamps(timestamps)
      .setDescriptors(['screenshot.png'])
      .build();
    const overrideError = new TraceOverridden(
      'screenshot.png',
      TraceType.SCREEN_RECORDING,
    );

    it('loads screenshot parser', () => {
      loadParsers([parserScreenshot], []);
      expectLoadResult([parserScreenshot], []);
    });

    it('loads screen recording parser', () => {
      loadParsers([parserScreenRecording], []);
      expectLoadResult([parserScreenRecording], []);
    });

    it('discards screenshot parser in favour of screen recording parser', () => {
      loadParsers([parserScreenshot, parserScreenRecording], []);
      expectLoadResult([parserScreenRecording], [overrideError]);
    });

    it('does not load screenshot parser after loading screen recording parser in same call', () => {
      loadParsers([parserScreenRecording, parserScreenshot], []);
      expectLoadResult([parserScreenRecording], [overrideError]);
    });

    it('does not load screenshot parser after loading screen recording parser in previous call', () => {
      loadParsers([parserScreenRecording], []);
      expectLoadResult([parserScreenRecording], []);

      loadParsers([parserScreenshot], []);
      expectLoadResult([parserScreenRecording], [overrideError]);
    });

    it('overrides previously loaded screenshot parser with screen recording parser', () => {
      loadParsers([parserScreenshot], []);
      expectLoadResult([parserScreenshot], []);

      loadParsers([parserScreenRecording], []);
      expectLoadResult([parserScreenRecording], [overrideError]);
    });
  });

  it('can remove parsers', () => {
    loadParsers([parserSf0], [parserWm0]);
    expectLoadResult([parserSf0, parserWm0], []);

    loadedParsers.remove(TraceType.WINDOW_MANAGER);
    expectLoadResult([parserSf0], []);

    loadedParsers.remove(TraceType.SURFACE_FLINGER);
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

  function loadParsers(
    legacy: Array<Parser<object>>,
    perfetto: Array<Parser<object>>,
  ) {
    const legacyFileAndParsers = legacy.map(
      (parser) => new FileAndParser(file, parser),
    );
    const perfettoFileAndParsers =
      perfetto.length > 0 ? new FileAndParsers(file, perfetto) : undefined;

    errors = [];
    const errorListener = {
      onError(error: WinscopeError) {
        errors.push(error);
      },
    };

    loadedParsers.addParsers(
      legacyFileAndParsers,
      perfettoFileAndParsers,
      errorListener,
    );
  }

  function expectLoadResult(
    expectedParsers: Array<Parser<object>>,
    expectedErrors: WinscopeError[],
  ) {
    expectedParsers.sort((a, b) => a.getTraceType() - b.getTraceType());
    const actualParsers = loadedParsers
      .getParsers()
      .sort((a, b) => a.getTraceType() - b.getTraceType());

    for (
      let i = 0;
      i < Math.max(expectedParsers.length, actualParsers.length);
      ++i
    ) {
      expect(actualParsers[i]).toBe(expectedParsers[i]);
    }

    expect(errors).toEqual(expectedErrors);
  }
});
