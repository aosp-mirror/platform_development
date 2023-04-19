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
import {UnitTestUtils} from 'test/unit/utils';
import {Parser} from 'trace/parser';
import {ElapsedTimestamp, TimestampType} from 'trace/timestamp';
import {TraceType} from 'trace/trace_type';

describe('ParserTransitions', () => {
  describe('trace with elapsed (only) timestamp', () => {
    let parser: Parser<object>;

    beforeAll(async () => {
      parser = await UnitTestUtils.getParser('traces/elapsed_timestamp/Transitions.pb');
    });

    it('has expected trace type', () => {
      expect(parser.getTraceType()).toEqual(TraceType.TRANSITION);
    });

    it('provides elapsed timestamps', () => {
      const timestamps = parser.getTimestamps(TimestampType.ELAPSED)!;

      expect(timestamps.length).toEqual(7);

      const expected = [
        new ElapsedTimestamp(1862299518404n),
        new ElapsedTimestamp(1863412780164n),
        new ElapsedTimestamp(1865439877129n),
      ];
      expect(timestamps.slice(0, 3)).toEqual(expected);
    });

    it("doesn't provide real timestamps", () => {
      expect(parser.getTimestamps(TimestampType.REAL)).toEqual(undefined);
    });
  });
});
