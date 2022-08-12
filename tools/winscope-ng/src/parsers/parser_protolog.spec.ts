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
import {Timestamp, TimestampType} from "common/trace/timestamp";
import {TraceType} from "common/trace/trace_type";
import {Parser} from "./parser";
import {UnitTestUtils} from "test/unit/utils";
import {LogMessage} from "../common/trace/protolog";

describe("ParserProtoLog", () => {
  let parser: Parser;

  const expectedFirstLogMessage = {
    text: "InsetsSource updateVisibility for ITYPE_IME, serverVisible: false clientVisible: false",
    time: "14m10s746ms",
    tag: "WindowManager",
    level: "DEBUG",
    at: "com/android/server/wm/InsetsSourceProvider.java",
    timestamp: Number(850746266486),
  };

  beforeAll(async () => {
    parser = await UnitTestUtils.getParser("traces/elapsed_and_real_timestamp/ProtoLog.pb");
  });

  it("has expected trace type", () => {
    expect(parser.getTraceType()).toEqual(TraceType.PROTO_LOG);
  });

  it("provides elapsed timestamps", () => {
    const timestamps = parser.getTimestamps(TimestampType.ELAPSED)!;
    expect(timestamps.length)
      .toEqual(50);

    const expected = [
      new Timestamp(TimestampType.ELAPSED, 850746266486n),
      new Timestamp(TimestampType.ELAPSED, 850746336718n),
      new Timestamp(TimestampType.ELAPSED, 850746350430n),
    ];
    expect(timestamps.slice(0, 3))
      .toEqual(expected);
  });

  it("provides real timestamps", () => {
    const timestamps = parser.getTimestamps(TimestampType.REAL)!;
    expect(timestamps.length)
      .toEqual(50);

    const expected = [
      new Timestamp(TimestampType.REAL, 1655727125377266486n),
      new Timestamp(TimestampType.REAL, 1655727125377336718n),
      new Timestamp(TimestampType.REAL, 1655727125377350430n),
    ];
    expect(timestamps.slice(0, 3))
      .toEqual(expected);
  });

  it("reconstructs human-readable log message", () => {
    const timestamp = new Timestamp(TimestampType.ELAPSED, 850746266486n);
    const actualMessage = parser.getTraceEntry(timestamp)!;

    expect(actualMessage).toBeInstanceOf(LogMessage);
    expect(Object.assign({}, actualMessage)).toEqual(expectedFirstLogMessage);
  });

  it("allows retrieving all the log messages", () => {
    const actualMessages = parser.getTraceEntries();

    expect(actualMessages.length).toEqual(50);

    actualMessages.forEach(message => {
      expect(message).toBeInstanceOf(LogMessage);
    });

    const actualFirstLogMessage = Object.assign({}, actualMessages[0]);
    expect(actualFirstLogMessage).toEqual(expectedFirstLogMessage);
  });
});
