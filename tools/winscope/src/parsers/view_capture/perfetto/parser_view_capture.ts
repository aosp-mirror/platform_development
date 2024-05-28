/*
 * Copyright (C) 2024 The Android Open Source Project
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

import {assertDefined, assertTrue} from 'common/assert_utils';
import {ParserTimestampConverter} from 'common/timestamp_converter';
import {TraceFile} from 'trace/trace_file';
import {WasmEngineProxy} from 'trace_processor/wasm_engine_proxy';
import {ParserViewCaptureWindow} from './parser_view_capture_window';

interface WindowAndPackage {
  window: string | undefined;
  package: string | undefined;
}

export class ParserViewCapture {
  private readonly traceFile: TraceFile;
  private readonly traceProcessor: WasmEngineProxy;
  private readonly timestampConverter: ParserTimestampConverter;

  private windowParsers: ParserViewCaptureWindow[] = [];

  private static readonly STDLIB_MODULE_NAME = 'android.winscope.viewcapture';

  constructor(
    traceFile: TraceFile,
    traceProcessor: WasmEngineProxy,
    timestampConverter: ParserTimestampConverter,
  ) {
    this.traceFile = traceFile;
    this.traceProcessor = traceProcessor;
    this.timestampConverter = timestampConverter;
  }

  async parse() {
    await this.traceProcessor.query(
      `INCLUDE PERFETTO MODULE ${ParserViewCapture.STDLIB_MODULE_NAME};`,
    );

    const windowAndPackageNames = await this.queryWindowAndPackageNames();
    assertTrue(
      windowAndPackageNames.length > 0,
      () => 'Perfetto trace has no ViewCapture windows',
    );

    this.windowParsers = windowAndPackageNames.map(
      (windowAndPackage) =>
        new ParserViewCaptureWindow(
          this.traceFile,
          this.traceProcessor,
          this.timestampConverter,
          assertDefined(windowAndPackage.package),
          assertDefined(windowAndPackage.window),
        ),
    );

    const parsePromises = this.windowParsers.map((parser) => parser.parse());
    await Promise.all(parsePromises);
  }

  getWindowParsers(): ParserViewCaptureWindow[] {
    return this.windowParsers;
  }

  private async queryWindowAndPackageNames(): Promise<WindowAndPackage[]> {
    const sql = `
      SELECT vc.id as id, args.key as key, args.string_value as value
      FROM android_viewcapture AS vc
      JOIN args ON vc.arg_set_id = args.arg_set_id
      WHERE
        args.key = 'window_name' OR
        args.key = 'package_name';
    `;
    const result = await this.traceProcessor.query(sql).waitAllRows();

    const idToNames = new Map<bigint, WindowAndPackage>();
    for (const it = result.iter({}); it.valid(); it.next()) {
      const id = it.get('id') as bigint;
      const key = it.get('key') as string;
      const value = it.get('value') as string;

      if (idToNames.get(id) === undefined) {
        idToNames.set(id, {window: undefined, package: undefined});
      }

      if (key === 'window_name') {
        assertDefined(idToNames.get(id)).window = value;
      } else {
        assertDefined(idToNames.get(id)).package = value;
      }
    }

    const namesSorted = [...idToNames.values()].sort((a, b) => {
      const aWindow = assertDefined(a.window);
      const bWindow = assertDefined(b.window);
      if (aWindow < bWindow) {
        return -1;
      } else if (bWindow < aWindow) {
        return +1;
      } else {
        return 0;
      }
    });

    return namesSorted;
  }
}
