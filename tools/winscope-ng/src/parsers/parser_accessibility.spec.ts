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

describe("ParserAccessibility", () => {
  let parser: Parser;

  beforeAll(async () => {
    const trace = TestUtils.getFixtureBlob("trace_Accessibility.pb");
    const parsers = await new ParserFactory().createParsers([trace]);
    expect(parsers.length).toEqual(1);
    parser = parsers[0];
  });

  it("has expected trace type", () => {
    expect(parser.getTraceTypeId()).toEqual(TraceTypeId.ACCESSIBILITY);
  });

  it("provides timestamps", () => {
    expect(parser.getTimestamps())
      .toEqual([850297444302, 850297882046, 850756176154, 850773581835]);
  });

  it("retrieves trace entry", () => {
    expect(Number(parser.getTraceEntry(850297444302)!.elapsedRealtimeNanos))
      .toEqual(850297444302);
  });
});
