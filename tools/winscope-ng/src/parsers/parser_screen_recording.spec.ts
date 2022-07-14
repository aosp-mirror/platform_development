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
import {TraceTypeId} from "common/trace/type_id";
import {TestUtils} from "test/test_utils";
import {Parser} from "./parser";
import {ParserFactory} from "./parser_factory";

describe("ParserScreenRecording", () => {
  let parser: Parser;

  beforeAll(async () => {
    const buffer = TestUtils.getFixtureBlob("screen_recording.mp4");
    const parsers = await new ParserFactory().createParsers([buffer]);
    expect(parsers.length).toEqual(1);
    parser = parsers[0];
  });

  it("has expected trace type", () => {
    expect(parser.getTraceTypeId()).toEqual(TraceTypeId.SCREEN_RECORDING);
  });

  it("provides timestamps", () => {
    const timestamps = parser.getTimestamps();

    expect(timestamps.length)
      .toEqual(85);

    expect(timestamps.slice(0, 3))
      .toEqual([19446131807000, 19446158500000, 19446167117000]);

    expect(timestamps.slice(timestamps.length-3, timestamps.length))
      .toEqual([19448470076000, 19448487525000, 19448501007000]);
  });

  it("retrieves trace entry", () => {
    {
      const entry = parser.getTraceEntry(19446131807000)!;
      expect(entry).toBeInstanceOf(ScreenRecordingTraceEntry);
      expect(Number(entry.videoTimeSeconds)).toBeCloseTo(0);
    }

    {
      const entry = parser.getTraceEntry(19448501007000)!;
      expect(entry).toBeInstanceOf(ScreenRecordingTraceEntry);
      expect(Number(entry.videoTimeSeconds)).toBeCloseTo(2.37, 0.001);
    }
  });
});
