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

describe("ParserWindowManagerDump", () => {
  let parser: Parser;

  beforeAll(async () => {
    parser = await UnitTestUtils.getParser("traces/dump_WindowManager.pb");
  });

  it("has expected trace type", () => {
    expect(parser.getTraceType()).toEqual(TraceType.WINDOW_MANAGER);
  });

  it("provides elapsed timestamp (always zero)", () => {
    const expected = [
      new Timestamp(TimestampType.ELAPSED, 0n),
    ];
    expect(parser.getTimestamps(TimestampType.ELAPSED))
      .toEqual(expected);
  });

  it("doesn't provide real timestamp (never)", () => {
    expect(parser.getTimestamps(TimestampType.REAL))
      .toEqual(undefined);
  });

  it("retrieves trace entry from elapsed timestamp", () => {
    const timestamp = new Timestamp(TimestampType.ELAPSED, 0n);
    const entry = parser.getTraceEntry(timestamp)!;
    expect(entry).toBeInstanceOf(WindowManagerState);
    expect(BigInt(entry.timestampMs)).toEqual(0n);
  });
});
