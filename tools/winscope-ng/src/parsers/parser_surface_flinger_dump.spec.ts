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
import {LayerTraceEntry} from "common/trace/flickerlib/layers/LayerTraceEntry";
import {UnitTestUtils} from "test/unit/utils";
import {Parser} from "./parser";
import {ParserFactory} from "./parser_factory";

describe("ParserSurfaceFlingerDump", () => {
  let parser: Parser;

  beforeAll(async () => {
    const buffer = UnitTestUtils.getFixtureBlob("dump_SurfaceFlinger.pb");
    const parsers = await new ParserFactory().createParsers([buffer]);
    expect(parsers.length).toEqual(1);
    parser = parsers[0];
  });

  it("has expected trace type", () => {
    expect(parser.getTraceType()).toEqual(TraceType.SURFACE_FLINGER);
  });

  it("provides timestamp", () => {
    const expected = [
      new Timestamp(TimestampType.ELAPSED, 0n),
    ];
    expect(parser.getTimestamps(TimestampType.ELAPSED)).toEqual(expected);
  });

  it("retrieves trace entry", () => {
    const timestamp = new Timestamp(TimestampType.ELAPSED, 0n);
    const entry = parser.getTraceEntry(timestamp)!;
    expect(entry).toBeInstanceOf(LayerTraceEntry);
    expect(BigInt(entry.timestampMs)).toEqual(0n);
  });

  //TODO: add real timestamp
});
