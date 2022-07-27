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
import {TestUtils} from "test/test_utils";

describe("ParserTransactions", () => {
  let parser: Parser;

  beforeAll(async () => {
    const buffer = TestUtils.getFixtureBlob("trace_Transactions.pb");
    const parsers = await new ParserFactory().createParsers([buffer]);
    expect(parsers.length).toEqual(1);
    parser = parsers[0];
  });

  it("has expected trace type", () => {
    expect(parser.getTraceType()).toEqual(TraceType.TRANSACTIONS);
  });

  it("provides timestamps", () => {
    const timestamps = parser.getTimestamps(TimestampType.ELAPSED)!;

    expect(timestamps.length)
      .toEqual(4997);

    const expected = [
      new Timestamp(TimestampType.ELAPSED, 14862317023n),
      new Timestamp(TimestampType.ELAPSED, 14873423549n),
      new Timestamp(TimestampType.ELAPSED, 14884850511n),
    ];
    expect(timestamps.slice(0, 3))
      .toEqual(expected);
  });

  it("retrieves trace entry", () => {
    const timestamp = new Timestamp(TimestampType.ELAPSED, 14862317023n);
    expect(BigInt(parser.getTraceEntry(timestamp)!.elapsedRealtimeNanos))
      .toEqual(14862317023n);
  });
});
