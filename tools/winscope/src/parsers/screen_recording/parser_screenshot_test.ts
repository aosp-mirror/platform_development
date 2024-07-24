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
import {TimestampType} from 'common/time';
import {NO_TIMEZONE_OFFSET_FACTORY} from 'common/timestamp_factory';
import {UnitTestUtils} from 'test/unit/utils';
import {ScreenRecordingTraceEntry} from 'trace/screen_recording';
import {TraceFile} from 'trace/trace_file';
import {TraceType} from 'trace/trace_type';
import {ParserScreenshot} from './parser_screenshot';

describe('ParserScreenshot', () => {
  let parser: ParserScreenshot;
  let file: File;

  beforeAll(async () => {
    file = await UnitTestUtils.getFixtureFile('traces/screenshot.png');
    parser = new ParserScreenshot(
      new TraceFile(file),
      NO_TIMEZONE_OFFSET_FACTORY,
    );
    await parser.parse();
  });

  it('has expected trace type', () => {
    expect(parser.getTraceType()).toEqual(TraceType.SCREENSHOT);
  });

  it('provides elapsed timestamps', () => {
    const timestamps = assertDefined(
      parser.getTimestamps(TimestampType.ELAPSED),
    );

    const expected = NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(0n);
    timestamps.forEach((timestamp) => expect(timestamp).toEqual(expected));
  });

  it('provides real timestamps', () => {
    const timestamps = assertDefined(parser.getTimestamps(TimestampType.REAL));

    const expected = NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(0n);
    timestamps.forEach((timestamp) => expect(timestamp).toEqual(expected));
  });

  it('does not apply timezone info', async () => {
    const parserWithTimezoneInfo = new ParserScreenshot(
      new TraceFile(file),
      UnitTestUtils.TIMESTAMP_FACTORY_WITH_TIMEZONE,
    );
    await parserWithTimezoneInfo.parse();

    const expectedElapsed = NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(0n);
    assertDefined(parser.getTimestamps(TimestampType.ELAPSED)).forEach(
      (timestamp) => expect(timestamp).toEqual(expectedElapsed),
    );

    const expectedReal = NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(0n);
    assertDefined(parser.getTimestamps(TimestampType.REAL)).forEach(
      (timestamp) => expect(timestamp).toEqual(expectedReal),
    );
  });

  it('retrieves entry', async () => {
    const entry = await parser.getEntry(0, TimestampType.REAL);
    expect(entry).toBeInstanceOf(ScreenRecordingTraceEntry);
    expect(entry.isImage).toBeTrue();
  });
});
