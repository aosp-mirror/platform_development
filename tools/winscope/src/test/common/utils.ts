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
import * as fs from 'fs';
import * as path from 'path';
import {FileImpl} from './file_impl';

class CommonTestUtils {
  static async getFixtureFile(
    srcFilename: string,
    dstFilename: string = srcFilename
  ): Promise<File> {
    const buffer = CommonTestUtils.loadFixture(srcFilename);
    return new FileImpl(buffer, dstFilename) as unknown as File;
  }

  static loadFixture(filename: string): ArrayBuffer {
    return fs.readFileSync(CommonTestUtils.getFixturePath(filename));
  }

  static getFixturePath(filename: string): string {
    if (path.isAbsolute(filename)) {
      return filename;
    }
    return path.join(CommonTestUtils.getProjectRootPath(), 'src/test/fixtures', filename);
  }

  static getProjectRootPath(): string {
    let root = __dirname;
    while (path.basename(root) !== 'winscope') {
      root = path.dirname(root);
    }
    return root;
  }
}

export {CommonTestUtils};
