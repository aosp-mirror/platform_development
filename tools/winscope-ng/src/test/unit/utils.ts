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
import { Parser } from "parsers/parser";
import { ParserFactory } from "parsers/parser_factory";
import { CommonTestUtils } from "test/common/utils";
import {
  LayerTraceEntry,
  WindowManagerState,
} from "common/trace/flickerlib/common";
import { TimestampType } from "common/trace/timestamp";

class UnitTestUtils extends CommonTestUtils {
  static async getParser(filename: string): Promise<Parser> {
    const trace = await CommonTestUtils.getFixtureFile(filename);
    const [parsers, errors] = await new ParserFactory().createParsers([trace]);
    expect(parsers.length).toEqual(1);
    return parsers[0];
  }

  static async getWindowManagerState(): Promise<WindowManagerState> {
    return this.getTraceEntry("traces/elapsed_timestamp/WindowManager.pb");
  }

  static async getLayerTraceEntry(): Promise<LayerTraceEntry> {
    return await this.getTraceEntry("traces/elapsed_timestamp/SurfaceFlinger.pb");
  }

  private static async getTraceEntry(filename: string) {
    const parser = await this.getParser(filename);
    const timestamp = parser.getTimestamps(TimestampType.ELAPSED)![0];
    return parser.getTraceEntry(timestamp);
  }
}

export {UnitTestUtils};
