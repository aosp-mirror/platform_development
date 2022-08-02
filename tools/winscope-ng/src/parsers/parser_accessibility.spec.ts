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
import {ParserFactory} from "./parser_factory";
import {UnitTestUtils} from "test/unit/utils";

describe("ParserAccessibility", () => {
  let parser: Parser;

  beforeAll(async () => {
    parser = await UnitTestUtils.getParser("traces/Accessibility.pb");
  });

  it("has expected trace type", () => {
    expect(parser.getTraceType()).toEqual(TraceType.ACCESSIBILITY);
  });

  it("provides elapsed timestamps", () => {
    const expected = [
      new Timestamp(TimestampType.ELAPSED, 850297444302n),
      new Timestamp(TimestampType.ELAPSED, 850297882046n),
      new Timestamp(TimestampType.ELAPSED, 850756176154n),
      new Timestamp(TimestampType.ELAPSED, 850773581835n),
    ];
    expect(parser.getTimestamps(TimestampType.ELAPSED))
      .toEqual(expected);
  });

  it("retrieves trace entry from elapsed timestamp", () => {
    const timestamp = new Timestamp(TimestampType.ELAPSED, 850297444302n);
    expect(BigInt(parser.getTraceEntry(timestamp)!.elapsedRealtimeNanos))
      .toEqual(850297444302n);
  });
});
