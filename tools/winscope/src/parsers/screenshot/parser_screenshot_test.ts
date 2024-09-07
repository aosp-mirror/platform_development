/*
 * Copyright (C) 2024 The Android Open Source Project
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
import {TimestampConverter} from 'common/timestamp_converter';
import {TimestampConverterUtils} from 'test/unit/timestamp_converter_utils';
import {UnitTestUtils} from 'test/unit/utils';
import {CoarseVersion} from 'trace/coarse_version';
import {MediaBasedTraceEntry} from 'trace/media_based_trace_entry';
import {TraceFile} from 'trace/trace_file';
import {TraceType} from 'trace/trace_type';
import {ParserScreenshot} from './parser_screenshot';

describe('ParserScreenshot', () => {
  let parser: ParserScreenshot;
  let file: File;

  beforeAll(async () => {
    jasmine.addCustomEqualityTester(UnitTestUtils.timestampEqualityTester);
    file = await UnitTestUtils.getFixtureFile('traces/screenshot.png');
    parser = new ParserScreenshot(
      new TraceFile(file),
      new TimestampConverter(TimestampConverterUtils.UTC_TIMEZONE_INFO, 0n),
    );
    await parser.parse();
    parser.createTimestamps();
  });

  it('has expected trace type', () => {
    expect(parser.getTraceType()).toEqual(TraceType.SCREENSHOT);
  });

  it('has expected coarse version', () => {
    expect(parser.getCoarseVersion()).toEqual(CoarseVersion.LATEST);
  });

  it('provides timestamps', () => {
    const timestamps = assertDefined(parser.getTimestamps());

    const expected = TimestampConverterUtils.makeElapsedTimestamp(0n);
    timestamps.forEach((timestamp) => expect(timestamp).toEqual(expected));
  });

  it('does not apply timezone info', async () => {
    const parserWithTimezoneInfo = new ParserScreenshot(
      new TraceFile(file),
      TimestampConverterUtils.TIMESTAMP_CONVERTER_WITH_UTC_OFFSET,
    );
    await parserWithTimezoneInfo.parse();

    const expectedReal = TimestampConverterUtils.makeElapsedTimestamp(0n);
    assertDefined(parser.getTimestamps()).forEach((timestamp) =>
      expect(timestamp).toEqual(expectedReal),
    );
  });

  it('retrieves entry', async () => {
    const entry = await parser.getEntry(0);
    expect(entry).toBeInstanceOf(MediaBasedTraceEntry);
    expect(entry.isImage).toBeTrue();
  });
});
