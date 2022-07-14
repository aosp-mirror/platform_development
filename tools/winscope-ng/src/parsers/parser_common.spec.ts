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
import {ParserFactory} from "./parser_factory";
import {Parser} from "./parser";
import {TestUtils} from "test/test_utils";

describe("Parser", () => {
  let parser: Parser;

  beforeAll(async () => {
    const buffer = TestUtils.getFixtureBlob("trace_WindowManager.pb");
    const parsers = await new ParserFactory().createParsers([buffer]);
    expect(parsers.length).toEqual(1);
    parser = parsers[0];
  });

  it("provides timestamps", () => {
    expect(parser.getTimestamps())
      .toEqual([850254319343, 850763506110, 850782750048]);
  });

  it("retrieves trace entry (no timestamp matches)", () => {
    expect(parser.getTraceEntry(850254319342))
      .toEqual(undefined);
  });

  it("retrieves trace entry (equal timestamp matches)", () => {
    expect(Number(parser.getTraceEntry(850254319343)!.timestampMs))
      .toEqual(850254319343);
  });

  it("retrieves trace entry (equal timestamp matches)", () => {
    expect(Number(parser.getTraceEntry(850763506110)!.timestampMs))
      .toEqual(850763506110);
  });

  it("retrieves trace entry (lower timestamp matches)", () => {
    expect(Number(parser.getTraceEntry(850254319344)!.timestampMs))
      .toEqual(850254319343);
  });

  it("retrieves trace entry (equal timestamp matches)", () => {
    expect(Number(parser.getTraceEntry(850763506111)!.timestampMs))
      .toEqual(850763506110);
  });
});
