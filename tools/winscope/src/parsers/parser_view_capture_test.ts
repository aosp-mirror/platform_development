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
import {UnitTestUtils} from 'test/unit/utils';
import {Parser} from 'trace/parser';
import {Timestamp, TimestampType} from 'trace/timestamp';
import {TraceType} from 'trace/trace_type';

describe('ParserViewCapture', () => {
  let parser: Parser<object>;

  beforeAll(async () => {
    parser = await UnitTestUtils.getParser(
      'traces/elapsed_and_real_timestamp/com.google.android.apps.nexuslauncher_0.vc'
    );
  });

  it('has expected trace type', () => {
    expect(parser.getTraceType()).toEqual(TraceType.VIEW_CAPTURE);
  });

  it('provides elapsed timestamps', () => {
    const expected = [
      new Timestamp(TimestampType.ELAPSED, 26231798759n),
      new Timestamp(TimestampType.ELAPSED, 26242905367n),
      new Timestamp(TimestampType.ELAPSED, 26255550549n),
    ];
    expect(parser.getTimestamps(TimestampType.ELAPSED)!.slice(0, 3)).toEqual(expected);
  });

  it('provides real timestamps', () => {
    const expected = [
      new Timestamp(TimestampType.REAL, 1686674380113072216n),
      new Timestamp(TimestampType.REAL, 1686674380124178824n),
      new Timestamp(TimestampType.REAL, 1686674380136824006n),
    ];
    expect(parser.getTimestamps(TimestampType.REAL)!.slice(0, 3)).toEqual(expected);
  });

  it('retrieves trace entry', async () => {
    const entry = (await parser.getEntry(1, TimestampType.REAL)) as any;
    expect(entry.timestamp).toBeTruthy();
    expect(entry.node).toBeTruthy();
  });
});
