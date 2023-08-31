/*
 * Copyright (C) 2023 The Android Open Source Project
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

import {assertDefined} from 'common/assert_utils';
import {UrlUtils} from 'common/url_utils';
import {ParserFactory as PerfettoParserFactory} from 'parsers/perfetto/parser_factory';
import {Parser} from 'trace/parser';
import {TraceFile} from 'trace/trace_file';
import {TraceType} from 'trace/trace_type';

//TODO(b/290183109): unify with UnitTestUtils, once all the Node.js tests have been ported to Karma.
class KarmaTestUtils {
  static async getPerfettoParser(
    traceType: TraceType,
    fixturePath: string
  ): Promise<Parser<object>> {
    const parsers = await KarmaTestUtils.getPerfettoParsers(fixturePath);
    const parser = assertDefined(parsers.find((parser) => parser.getTraceType() === traceType));
    return parser;
  }

  static async getPerfettoParsers(fixturePath: string): Promise<Array<Parser<object>>> {
    const file = await KarmaTestUtils.getFixtureFile(fixturePath);
    const traceFile = new TraceFile(file);
    return await new PerfettoParserFactory().createParsers(traceFile);
  }

  static async getFixtureFile(
    srcFilename: string,
    dstFilename: string = srcFilename
  ): Promise<File> {
    const url = UrlUtils.getRootUrl() + 'base/src/test/fixtures/' + srcFilename;
    const response = await fetch(url);
    expect(response.ok).toBeTrue();
    const blob = await response.blob();
    const file = new File([blob], dstFilename);
    return file;
  }
}

export {KarmaTestUtils};
