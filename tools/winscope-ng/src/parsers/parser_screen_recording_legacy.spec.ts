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

describe("ParserScreenRecordingLegacy", () => {
  let parser: Parser;

  beforeAll(async () => {
    parser = await UnitTestUtils.getParser("traces/elapsed_timestamp/screen_recording.mp4");
  });

  it("has expected trace type", () => {
    expect(parser.getTraceType()).toEqual(TraceType.SCREEN_RECORDING);
  });

  it("provides elapsed timestamps", () => {
    const timestamps = parser.getTimestamps(TimestampType.ELAPSED)!;

    expect(timestamps.length)
      .toEqual(85);

    let expected = [
      new Timestamp(TimestampType.ELAPSED, 19446131807000n),
      new Timestamp(TimestampType.ELAPSED, 19446158500000n),
      new Timestamp(TimestampType.ELAPSED, 19446167117000n),
    ];
    expect(timestamps.slice(0, 3))
      .toEqual(expected);

    expected = [
      new Timestamp(TimestampType.ELAPSED, 19448470076000n),
      new Timestamp(TimestampType.ELAPSED, 19448487525000n),
      new Timestamp(TimestampType.ELAPSED, 19448501007000n),
    ];
    expect(timestamps.slice(timestamps.length-3, timestamps.length))
      .toEqual(expected);
  });

  it("doesn't provide real timestamps", () => {
    expect(parser.getTimestamps(TimestampType.REAL))
      .toEqual(undefined);
  });

  it("retrieves trace entry", () => {
    {
      const timestamp = new Timestamp(TimestampType.ELAPSED, 19446131807000n);
      const entry = parser.getTraceEntry(timestamp)!;
      expect(entry).toBeInstanceOf(ScreenRecordingTraceEntry);
      expect(Number(entry.videoTimeSeconds)).toBeCloseTo(0);
    }

    {
      const timestamp = new Timestamp(TimestampType.ELAPSED, 19448501007000n);
      const entry = parser.getTraceEntry(timestamp)!;
      expect(entry).toBeInstanceOf(ScreenRecordingTraceEntry);
      expect(Number(entry.videoTimeSeconds)).toBeCloseTo(2.37, 0.001);
    }
  });
});
