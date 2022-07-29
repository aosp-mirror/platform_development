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
import {WindowManagerState} from "common/trace/flickerlib/windows/WindowManagerState";
import {Timestamp, TimestampType} from "common/trace/timestamp";
import {TraceType} from "common/trace/trace_type";
import {Parser} from "./parser";
import {UnitTestUtils} from "test/unit/utils";

describe("ParserWindowManager", () => {
  describe("trace with elapsed + real timestamp", () => {
    let parser: Parser;

    beforeAll(async () => {
      parser = await UnitTestUtils.getParser("traces/elapsed_and_real_timestamp/WindowManager.pb");
    });

    it("has expected trace type", () => {
      expect(parser.getTraceType()).toEqual(TraceType.WINDOW_MANAGER);
    });

    it("provides elapsed timestamps", () => {
      const expected = [
        new Timestamp(TimestampType.ELAPSED, 14474594000n),
        new Timestamp(TimestampType.ELAPSED, 15398076788n),
        new Timestamp(TimestampType.ELAPSED, 15409222011n),
      ];
      expect(parser.getTimestamps(TimestampType.ELAPSED)!.slice(0, 3))
        .toEqual(expected);
    });

    it("provides real timestamps", () => {
      const expected = [
        new Timestamp(TimestampType.REAL, 1659107089075566202n),
        new Timestamp(TimestampType.REAL, 1659107089999048990n),
        new Timestamp(TimestampType.REAL, 1659107090010194213n),
      ];
      expect(parser.getTimestamps(TimestampType.REAL)!.slice(0, 3))
        .toEqual(expected);
    });

    it("retrieves trace entry from elapsed timestamp", () => {
      const timestamp = new Timestamp(TimestampType.ELAPSED, 15398076788n);
      const entry = parser.getTraceEntry(timestamp)!;
      expect(entry).toBeInstanceOf(WindowManagerState);
      expect(BigInt(entry.timestampMs)).toEqual(15398076788n);
    });

    it("retrieves trace entry from real timestamp", () => {
      const timestamp = new Timestamp(TimestampType.REAL, 1659107089999048990n);
      const entry = parser.getTraceEntry(timestamp)!;
      expect(entry).toBeInstanceOf(WindowManagerState);
      expect(BigInt(entry.timestampMs)).toEqual(15398076788n);
    });
  });

  describe("trace elapsed timestamp", () => {
    let parser: Parser;

    beforeAll(async () => {
      parser = await UnitTestUtils.getParser("traces/elapsed_timestamp/WindowManager.pb");
    });

    it("has expected trace type", () => {
      expect(parser.getTraceType()).toEqual(TraceType.WINDOW_MANAGER);
    });

    it("provides timestamps", () => {
      const expected = [
        new Timestamp(TimestampType.ELAPSED, 850254319343n),
        new Timestamp(TimestampType.ELAPSED, 850763506110n),
        new Timestamp(TimestampType.ELAPSED, 850782750048n),
      ];
      expect(parser.getTimestamps(TimestampType.ELAPSED))
        .toEqual(expected);
    });

    it("retrieves trace entry", () => {
      const timestamp = new Timestamp(TimestampType.ELAPSED, 850254319343n);
      const entry = parser.getTraceEntry(timestamp)!;
      expect(entry).toBeInstanceOf(WindowManagerState);
      expect(BigInt(entry.timestampMs)).toEqual(850254319343n);
    });
  });
});
