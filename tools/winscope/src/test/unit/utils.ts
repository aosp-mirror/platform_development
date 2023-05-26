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

import {ParserFactory} from 'parsers/parser_factory';
import {CommonTestUtils} from 'test/common/utils';
import {LayerTraceEntry, WindowManagerState} from 'trace/flickerlib/common';
import {Parser} from 'trace/parser';
import {TimestampType} from 'trace/timestamp';
import {Trace} from 'trace/trace';
import {TraceFile} from 'trace/trace_file';
import {TraceType} from 'trace/trace_type';

class UnitTestUtils extends CommonTestUtils {
  static async getTraceFromFile(filename: string): Promise<Trace<object>> {
    const parser = await UnitTestUtils.getParser(filename);

    const trace = Trace.newUninitializedTrace(parser);
    trace.init(
      parser.getTimestamps(TimestampType.REAL) !== undefined
        ? TimestampType.REAL
        : TimestampType.ELAPSED
    );
    return trace;
  }

  static async getParser(filename: string): Promise<Parser<object>> {
    const file = new TraceFile(await CommonTestUtils.getFixtureFile(filename), undefined);
    const [parsers, errors] = await new ParserFactory().createParsers([file]);
    expect(parsers.length).toEqual(1);
    return parsers[0].parser;
  }

  static async getWindowManagerState(): Promise<WindowManagerState> {
    return UnitTestUtils.getTraceEntry('traces/elapsed_timestamp/WindowManager.pb');
  }

  static async getLayerTraceEntry(): Promise<LayerTraceEntry> {
    return await UnitTestUtils.getTraceEntry('traces/elapsed_timestamp/SurfaceFlinger.pb');
  }

  static async getMultiDisplayLayerTraceEntry(): Promise<LayerTraceEntry> {
    return await UnitTestUtils.getTraceEntry(
      'traces/elapsed_and_real_timestamp/SurfaceFlinger_multidisplay.pb'
    );
  }

  static async getImeTraceEntries(): Promise<Map<TraceType, any>> {
    let surfaceFlingerEntry: LayerTraceEntry | undefined;
    {
      const parser = await UnitTestUtils.getParser('traces/ime/SurfaceFlinger_with_IME.pb');
      surfaceFlingerEntry = await parser.getEntry(5, TimestampType.ELAPSED);
    }

    let windowManagerEntry: WindowManagerState | undefined;
    {
      const parser = await UnitTestUtils.getParser('traces/ime/WindowManager_with_IME.pb');
      windowManagerEntry = await parser.getEntry(2, TimestampType.ELAPSED);
    }

    const entries = new Map<TraceType, any>();
    entries.set(
      TraceType.INPUT_METHOD_CLIENTS,
      await UnitTestUtils.getTraceEntry('traces/ime/InputMethodClients.pb')
    );
    entries.set(
      TraceType.INPUT_METHOD_MANAGER_SERVICE,
      await UnitTestUtils.getTraceEntry('traces/ime/InputMethodManagerService.pb')
    );
    entries.set(
      TraceType.INPUT_METHOD_SERVICE,
      await UnitTestUtils.getTraceEntry('traces/ime/InputMethodService.pb')
    );
    entries.set(TraceType.SURFACE_FLINGER, surfaceFlingerEntry);
    entries.set(TraceType.WINDOW_MANAGER, windowManagerEntry);

    return entries;
  }

  private static async getTraceEntry(filename: string) {
    const parser = await UnitTestUtils.getParser(filename);
    return parser.getEntry(0, TimestampType.ELAPSED);
  }
}

export {UnitTestUtils};
