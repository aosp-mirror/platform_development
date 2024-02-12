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
import {Timestamp, TimestampType} from 'common/time';
import {UnitTestUtils} from 'test/unit/utils';
import {ScreenRecordingTraceEntry} from 'trace/screen_recording';
import {TraceFile} from 'trace/trace_file';
import {TraceType} from 'trace/trace_type';
import {ParserScreenshot} from './parser_screenshot';

describe('ParserScreenshot', () => {
  let parser: ParserScreenshot;

  beforeAll(async () => {
    const file = await UnitTestUtils.getFixtureFile('traces/screenshot.png');
    parser = new ParserScreenshot(new TraceFile(file));
    await parser.parse();
  });

  it('has expected trace type', () => {
    expect(parser.getTraceType()).toEqual(TraceType.SCREENSHOT);
  });

  it('provides elapsed timestamps', () => {
    const timestamps = assertDefined(parser.getTimestamps(TimestampType.ELAPSED));

    const expected = new Timestamp(TimestampType.ELAPSED, 0n);
    timestamps.forEach((timestamp) => expect(timestamp).toEqual(expected));
  });

  it('provides real timestamps', () => {
    const timestamps = assertDefined(parser.getTimestamps(TimestampType.REAL));

    const expected = new Timestamp(TimestampType.REAL, 0n);
    timestamps.forEach((timestamp) => expect(timestamp).toEqual(expected));
  });

  it('retrieves entry', async () => {
    const entry = await parser.getEntry(0, TimestampType.REAL);
    expect(entry).toBeInstanceOf(ScreenRecordingTraceEntry);
    expect(entry.isImage).toBeTrue();
  });
});
