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
import {UnitTestUtils} from "test/unit/utils";
import {Parser} from "./parser";

describe("ParserScreenRecording", () => {
  let parser: Parser;

  beforeAll(async () => {
    parser = await UnitTestUtils.getParser("traces/elapsed_and_real_timestamp/screen_recording_metadata_v2.mp4");
  });

  it("has expected trace type", () => {
    expect(parser.getTraceType()).toEqual(TraceType.SCREEN_RECORDING);
  });

  it ("provides elapsed timestamps", () => {
    const timestamps = parser.getTimestamps(TimestampType.ELAPSED)!;
    console.log(timestamps[timestamps.length-1]);

    expect(timestamps.length)
      .toEqual(123);

    const expected = [
      new Timestamp(TimestampType.ELAPSED, 211827840430n),
      new Timestamp(TimestampType.ELAPSED, 211842401430n),
      new Timestamp(TimestampType.ELAPSED, 211862172430n),
    ];
    expect(timestamps.slice(0, 3))
      .toEqual(expected);
  });

  it("provides real timestamps", () => {
    const timestamps = parser.getTimestamps(TimestampType.REAL)!;

    expect(timestamps.length)
      .toEqual(123);

    const expected = [
      new Timestamp(TimestampType.REAL, 1666361048792787045n),
      new Timestamp(TimestampType.REAL, 1666361048807348045n),
      new Timestamp(TimestampType.REAL, 1666361048827119045n),
    ];
    expect(timestamps.slice(0, 3))
      .toEqual(expected);
  });

  it("retrieves trace entry from elapsed timestamp", () => {
    {
      const timestamp = new Timestamp(TimestampType.ELAPSED, 211827840430n);
      const entry = parser.getTraceEntry(timestamp)!;
      expect(entry).toBeInstanceOf(ScreenRecordingTraceEntry);
      expect(Number(entry.videoTimeSeconds)).toBeCloseTo(0);
    }

    {
      const timestamp = new Timestamp(TimestampType.ELAPSED, 213198917430n);
      const entry = parser.getTraceEntry(timestamp)!;
      expect(entry).toBeInstanceOf(ScreenRecordingTraceEntry);
      expect(Number(entry.videoTimeSeconds)).toBeCloseTo(1.371077000, 0.001);
    }
  });

  it("retrieves trace entry from real timestamp", () => {
    {
      const timestamp = new Timestamp(TimestampType.REAL, 1666361048792787045n);
      const entry = parser.getTraceEntry(timestamp)!;
      expect(entry).toBeInstanceOf(ScreenRecordingTraceEntry);
      expect(Number(entry.videoTimeSeconds)).toBeCloseTo(0);
    }

    {
      const timestamp = new Timestamp(TimestampType.REAL, 1666361050163864045n);
      const entry = parser.getTraceEntry(timestamp)!;
      expect(entry).toBeInstanceOf(ScreenRecordingTraceEntry);
      expect(Number(entry.videoTimeSeconds)).toBeCloseTo(1.371077000, 0.001);
    }
  });
});
