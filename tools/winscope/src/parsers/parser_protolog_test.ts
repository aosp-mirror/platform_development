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
import {LogMessage} from 'trace/protolog';
import {Timestamp, TimestampType} from 'trace/timestamp';
import {TraceType} from 'trace/trace_type';

describe('ParserProtoLog', () => {
  let parser: Parser<LogMessage>;

  const expectedFirstLogMessageElapsed = {
    text: 'InsetsSource updateVisibility for ITYPE_IME, serverVisible: false clientVisible: false',
    time: '14m10s746ms266486ns',
    tag: 'WindowManager',
    level: 'DEBUG',
    at: 'com/android/server/wm/InsetsSourceProvider.java',
    timestamp: 850746266486n,
  };

  const expectedFirstLogMessageReal = {
    text: 'InsetsSource updateVisibility for ITYPE_IME, serverVisible: false clientVisible: false',
    time: '2022-06-20T12:12:05.377266486',
    tag: 'WindowManager',
    level: 'DEBUG',
    at: 'com/android/server/wm/InsetsSourceProvider.java',
    timestamp: 1655727125377266486n,
  };

  beforeAll(async () => {
    parser = (await UnitTestUtils.getParser(
      'traces/elapsed_and_real_timestamp/ProtoLog.pb'
    )) as Parser<LogMessage>;
  });

  it('has expected trace type', () => {
    expect(parser.getTraceType()).toEqual(TraceType.PROTO_LOG);
  });

  it('has expected length', () => {
    expect(parser.getLengthEntries()).toEqual(50);
  });

  it('provides elapsed timestamps', () => {
    const timestamps = parser.getTimestamps(TimestampType.ELAPSED)!;
    expect(timestamps.length).toEqual(50);

    const expected = [
      new Timestamp(TimestampType.ELAPSED, 850746266486n),
      new Timestamp(TimestampType.ELAPSED, 850746336718n),
      new Timestamp(TimestampType.ELAPSED, 850746350430n),
    ];
    expect(timestamps.slice(0, 3)).toEqual(expected);
  });

  it('provides real timestamps', () => {
    const timestamps = parser.getTimestamps(TimestampType.REAL)!;
    expect(timestamps.length).toEqual(50);

    const expected = [
      new Timestamp(TimestampType.REAL, 1655727125377266486n),
      new Timestamp(TimestampType.REAL, 1655727125377336718n),
      new Timestamp(TimestampType.REAL, 1655727125377350430n),
    ];
    expect(timestamps.slice(0, 3)).toEqual(expected);
  });

  it('reconstructs human-readable log message (ELAPSED time)', async () => {
    const message = await parser.getEntry(0, TimestampType.ELAPSED);

    expect(Object.assign({}, message)).toEqual(expectedFirstLogMessageElapsed);
    expect(message).toBeInstanceOf(LogMessage);
  });

  it('reconstructs human-readable log message (REAL time)', async () => {
    const message = await parser.getEntry(0, TimestampType.REAL);

    expect(Object.assign({}, message)).toEqual(expectedFirstLogMessageReal);
  });
});
