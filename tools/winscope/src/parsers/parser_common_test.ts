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
import {CommonTestUtils} from 'test/common/utils';
import {UnitTestUtils} from 'test/unit/utils';
import {WindowManagerState} from 'trace/flickerlib/windows/WindowManagerState';
import {Parser} from 'trace/parser';
import {Timestamp, TimestampType} from 'trace/timestamp';
import {TraceFile} from 'trace/trace_file';
import {TraceType} from 'trace/trace_type';
import {ParserFactory} from './parser_factory';

describe('Parser', () => {
  it('is robust to empty trace file', async () => {
    const trace = new TraceFile(await CommonTestUtils.getFixtureFile('traces/empty.pb'), undefined);
    const [parsers, errors] = await new ParserFactory().createParsers([trace]);
    expect(parsers.length).toEqual(0);
  });

  it('is robust to trace with no entries', async () => {
    const parser = await UnitTestUtils.getParser('traces/no_entries_InputMethodClients.pb');

    expect(parser.getTraceType()).toEqual(TraceType.INPUT_METHOD_CLIENTS);
    expect(parser.getTimestamps(TimestampType.ELAPSED)).toEqual([]);
    expect(parser.getTimestamps(TimestampType.REAL)).toEqual([]);
  });

  describe('real timestamp', () => {
    let parser: Parser<WindowManagerState>;

    beforeAll(async () => {
      parser = await UnitTestUtils.getParser('traces/elapsed_and_real_timestamp/WindowManager.pb');
    });

    it('provides timestamps', () => {
      const expected = [
        new Timestamp(TimestampType.REAL, 1659107089075566202n),
        new Timestamp(TimestampType.REAL, 1659107089999048990n),
        new Timestamp(TimestampType.REAL, 1659107090010194213n),
      ];
      expect(parser.getTimestamps(TimestampType.REAL)!.slice(0, 3)).toEqual(expected);
    });

    it('retrieves trace entries', async () => {
      let entry = await parser.getEntry(0, TimestampType.REAL);
      expect(BigInt(entry.timestamp.unixNanos.toString())).toEqual(1659107089075566202n);

      entry = await parser.getEntry(parser.getLengthEntries() - 1, TimestampType.REAL);
      expect(BigInt(entry.timestamp.unixNanos.toString())).toEqual(1659107091700249187n);
    });
  });

  describe('elapsed timestamp', () => {
    let parser: Parser<WindowManagerState>;

    beforeAll(async () => {
      parser = await UnitTestUtils.getParser('traces/elapsed_timestamp/WindowManager.pb');
    });

    it('provides timestamps', () => {
      const expected = [
        new Timestamp(TimestampType.ELAPSED, 850254319343n),
        new Timestamp(TimestampType.ELAPSED, 850763506110n),
        new Timestamp(TimestampType.ELAPSED, 850782750048n),
      ];
      expect(parser.getTimestamps(TimestampType.ELAPSED)).toEqual(expected);
    });

    it('retrieves trace entries', async () => {
      let entry = await parser.getEntry(0, TimestampType.ELAPSED);
      expect(BigInt(entry.timestamp.elapsedNanos.toString())).toEqual(850254319343n);

      entry = await parser.getEntry(parser.getLengthEntries() - 1, TimestampType.ELAPSED);
      expect(BigInt(entry.timestamp.elapsedNanos.toString())).toEqual(850782750048n);
    });
  });
});
