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
import {ScreenRecordingTraceEntry} from "common/trace/screen_recording";
import {Timestamp, TimestampType} from "common/trace/timestamp";
import {TraceType} from "common/trace/trace_type";
import {TestUtils} from "test/test_utils";
import {Parser} from "./parser";
import {ParserFactory} from "./parser_factory";

describe("ParserScreenRecording", () => {
  let parser: Parser;

  beforeAll(async () => {
    const trace = TestUtils.getFixtureBlob("screen_recording.mp4");
    const parsers = await new ParserFactory().createParsers([trace]);
    expect(parsers.length).toEqual(1);
    parser = parsers[0];
  });

  it("has expected trace type", () => {
    expect(parser.getTraceType()).toEqual(TraceType.SCREEN_RECORDING);
  });

  it ("provides elapsed timestamps", () => {
    const timestamps = parser.getTimestamps(TimestampType.ELAPSED)!;

    expect(timestamps.length)
      .toEqual(88);

    const expected = [
      new Timestamp(TimestampType.ELAPSED, 732949304000n),
      new Timestamp(TimestampType.ELAPSED, 733272129000n),
      new Timestamp(TimestampType.ELAPSED, 733283916000n),
    ];
    expect(timestamps.slice(0, 3))
      .toEqual(expected);
  });

  it("provides real timestamps", () => {
    const timestamps = parser.getTimestamps(TimestampType.REAL)!;

    expect(timestamps.length)
      .toEqual(88);

    const expected = [
      new Timestamp(TimestampType.REAL, 1658843852566916386n),
      new Timestamp(TimestampType.REAL, 1658843852889741386n),
      new Timestamp(TimestampType.REAL, 1658843852901528386n),
    ];
    expect(timestamps.slice(0, 3))
      .toEqual(expected);
  });

  it("retrieves trace entry from elapsed timestamp", () => {
    {
      const timestamp = new Timestamp(TimestampType.ELAPSED, 732949304000n);
      const entry = parser.getTraceEntry(timestamp)!;
      expect(entry).toBeInstanceOf(ScreenRecordingTraceEntry);
      expect(Number(entry.videoTimeSeconds)).toBeCloseTo(0);
    }

    {
      const timestamp = new Timestamp(TimestampType.ELAPSED, 733272129000n);
      const entry = parser.getTraceEntry(timestamp)!;
      expect(entry).toBeInstanceOf(ScreenRecordingTraceEntry);
      expect(Number(entry.videoTimeSeconds)).toBeCloseTo(0.322, 0.001);
    }
  });

  it("retrieves trace entry from real timestamp", () => {
    {
      const timestamp = new Timestamp(TimestampType.REAL, 1658843852566916386n);
      const entry = parser.getTraceEntry(timestamp)!;
      expect(entry).toBeInstanceOf(ScreenRecordingTraceEntry);
      expect(Number(entry.videoTimeSeconds)).toBeCloseTo(0);
    }

    {
      const timestamp = new Timestamp(TimestampType.REAL, 1658843852889741386n);
      const entry = parser.getTraceEntry(timestamp)!;
      expect(entry).toBeInstanceOf(ScreenRecordingTraceEntry);
      expect(Number(entry.videoTimeSeconds)).toBeCloseTo(0.322, 0.001);
    }
  });
});
