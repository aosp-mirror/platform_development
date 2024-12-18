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
import {MediaBasedTraceEntry} from 'trace/media_based_trace_entry';
import {Parser} from 'trace/parser';
import {TraceType} from 'trace/trace_type';

describe('ParserScreenRecording', () => {
  let parser: Parser<MediaBasedTraceEntry>;

  beforeAll(async () => {
    jasmine.addCustomEqualityTester(UnitTestUtils.timestampEqualityTester);
    parser = (await UnitTestUtils.getParser(
      'traces/elapsed_and_real_timestamp/screen_recording_metadata_v2.mp4',
    )) as Parser<MediaBasedTraceEntry>;
  });

  it('has expected trace type', () => {
    expect(parser.getTraceType()).toEqual(TraceType.SCREEN_RECORDING);
  });

  it('has expected coarse version', () => {
    expect(parser.getCoarseVersion()).toEqual(CoarseVersion.LATEST);
  });

  it('provides timestamps', () => {
    const timestamps = assertDefined(parser.getTimestamps());

    expect(timestamps.length).toEqual(123);

    const expected = [
      TimestampConverterUtils.makeRealTimestamp(1666361048792787045n),
      TimestampConverterUtils.makeRealTimestamp(1666361048807348045n),
      TimestampConverterUtils.makeRealTimestamp(1666361048827119045n),
    ];
    expect(timestamps.slice(0, 3)).toEqual(expected);
  });

  it('retrieves trace entry', async () => {
    {
      const entry = await parser.getEntry(0);
      expect(entry).toBeInstanceOf(MediaBasedTraceEntry);
      expect(Number(entry.videoTimeSeconds)).toBeCloseTo(0);
    }
    {
      const entry = await parser.getEntry(parser.getLengthEntries() - 1);
      expect(entry).toBeInstanceOf(MediaBasedTraceEntry);
      expect(Number(entry.videoTimeSeconds)).toBeCloseTo(1.371077, 0.001);
    }
  });
});
