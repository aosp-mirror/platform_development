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
import {ParserFactory} from "./parser_factory";
import {Parser} from "./parser";
import {UnitTestUtils} from "test/unit/utils";

describe("Parser", () => {
  let parser: Parser;

  beforeAll(async () => {
    const buffer = UnitTestUtils.getFixtureBlob("trace_WindowManager.pb");
    const parsers = await new ParserFactory().createParsers([buffer]);
    expect(parsers.length).toEqual(1);
    parser = parsers[0];
  });

  it("provides elapsed timestamps", () => {
    const expected = [
      new Timestamp(TimestampType.ELAPSED, 850254319343n),
      new Timestamp(TimestampType.ELAPSED, 850763506110n),
      new Timestamp(TimestampType.ELAPSED, 850782750048n)
    ];
    expect(parser.getTimestamps(TimestampType.ELAPSED))
      .toEqual(expected);
  });

  it("retrieves trace entry (no timestamp matches)", () => {
    const timestamp = new Timestamp(TimestampType.ELAPSED, 850254319342n);
    expect(parser.getTraceEntry(timestamp))
      .toEqual(undefined);
  });

  it("retrieves trace entry (equal timestamp matches)", () => {
    const timestamp = new Timestamp(TimestampType.ELAPSED, 850254319343n);
    expect(BigInt(parser.getTraceEntry(timestamp)!.timestampMs))
      .toEqual(850254319343n);
  });

  it("retrieves trace entry (equal timestamp matches)", () => {
    const timestamp = new Timestamp(TimestampType.ELAPSED, 850763506110n);
    expect(BigInt(parser.getTraceEntry(timestamp)!.timestampMs))
      .toEqual(850763506110n);
  });

  it("retrieves trace entry (lower timestamp matches)", () => {
    const timestamp = new Timestamp(TimestampType.ELAPSED, 850254319344n);
    expect(BigInt(parser.getTraceEntry(timestamp)!.timestampMs))
      .toEqual(850254319343n);
  });

  it("retrieves trace entry (equal timestamp matches)", () => {
    const timestamp = new Timestamp(TimestampType.ELAPSED, 850763506111n);
    expect(BigInt(parser.getTraceEntry(timestamp)!.timestampMs))
      .toEqual(850763506110n);
  });
});
