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
import {TimestampConverterUtils} from 'test/unit/timestamp_converter_utils';
import {UnitTestUtils} from 'test/unit/utils';
import {CoarseVersion} from 'trace/coarse_version';
import {Parser} from 'trace/parser';
import {ScreenRecordingTraceEntry} from 'trace/screen_recording';
import {TraceType} from 'trace/trace_type';

describe('ParserScreenRecordingLegacy', () => {
  let parser: Parser<ScreenRecordingTraceEntry>;

  beforeAll(async () => {
    parser = (await UnitTestUtils.getParser(
      'traces/elapsed_timestamp/screen_recording.mp4',
    )) as Parser<ScreenRecordingTraceEntry>;
  });

  it('has expected trace type', () => {
    expect(parser.getTraceType()).toEqual(TraceType.SCREEN_RECORDING);
  });

  it('has expected coarse version', () => {
    expect(parser.getCoarseVersion()).toEqual(CoarseVersion.LEGACY);
  });

  it('provides timestamps', () => {
    const timestamps = assertDefined(parser.getTimestamps());

    expect(timestamps.length).toEqual(85);

    let expected = [
      TimestampConverterUtils.makeElapsedTimestamp(19446131807000n),
      TimestampConverterUtils.makeElapsedTimestamp(19446158500000n),
      TimestampConverterUtils.makeElapsedTimestamp(19446167117000n),
    ];
    expect(timestamps.slice(0, 3)).toEqual(expected);

    expected = [
      TimestampConverterUtils.makeElapsedTimestamp(19448470076000n),
      TimestampConverterUtils.makeElapsedTimestamp(19448487525000n),
      TimestampConverterUtils.makeElapsedTimestamp(19448501007000n),
    ];
    expect(timestamps.slice(timestamps.length - 3, timestamps.length)).toEqual(
      expected,
    );
  });

  it('retrieves trace entry', async () => {
    {
      const entry = await parser.getEntry(0);
      expect(entry).toBeInstanceOf(ScreenRecordingTraceEntry);
      expect(Number(entry.videoTimeSeconds)).toBeCloseTo(0);
    }
    {
      const entry = await parser.getEntry(parser.getLengthEntries() - 1);
      expect(entry).toBeInstanceOf(ScreenRecordingTraceEntry);
      expect(Number(entry.videoTimeSeconds)).toBeCloseTo(2.37, 0.001);
    }
  });
});
