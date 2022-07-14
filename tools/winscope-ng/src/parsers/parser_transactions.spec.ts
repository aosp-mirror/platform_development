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
import {TraceTypeId} from "common/trace/type_id";
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
    expect(parser.getTraceTypeId()).toEqual(TraceTypeId.TRANSACTIONS);
  });

  it("provides timestamps", () => {
    const timestamps = parser.getTimestamps();
    expect(timestamps.length)
      .toEqual(4997);
    expect(timestamps.slice(0, 3))
      .toEqual([14862317023, 14873423549, 14884850511]);
  });

  it("retrieves trace entry", () => {
    expect(Number(parser.getTraceEntry(14862317023)!.elapsedRealtimeNanos))
      .toEqual(14862317023);
  });
});
