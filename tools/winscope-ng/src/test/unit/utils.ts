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
import {Parser} from "parsers/parser";
import {ParserFactory} from "parsers/parser_factory";
import {CommonTestUtils} from "test/common/utils";

class UnitTestUtils extends CommonTestUtils {
  static async getParser(filename: string): Promise<Parser> {
    const trace = CommonTestUtils.getFixtureBlob(filename);
    const parsers = await new ParserFactory().createParsers([trace]);
    expect(parsers.length).toEqual(1);
    return parsers[0];
  }
}

export {UnitTestUtils};
