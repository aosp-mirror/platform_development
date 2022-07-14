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

describe("ParserInputMethodlClients", () => {
  let parser: Parser;

  beforeAll(async () => {
    const buffer = TestUtils.getFixtureBlob("trace_InputMethodClients.pb");
    const parsers = await new ParserFactory().createParsers([buffer]);
    expect(parsers.length).toEqual(1);
    parser = parsers[0];
  });

  it("has expected trace type", () => {
    expect(parser.getTraceTypeId()).toEqual(TraceTypeId.INPUT_METHOD_CLIENTS);
  });

  it("provides timestamps", () => {
    expect(parser.getTimestamps().length)
      .toEqual(33);
    expect(parser.getTimestamps().slice(0, 3))
      .toEqual([1149083651642, 1149083950633, 1149127567251]);
  });

  it("retrieves trace entry", () => {
    expect(Number(parser.getTraceEntry(1149083651642)!.elapsedRealtimeNanos))
      .toEqual(1149083651642);
  });
});
