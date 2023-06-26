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
import {browser, by, element} from 'protractor';
import {CommonTestUtils} from '../common/utils';

class E2eTestUtils extends CommonTestUtils {
  static readonly WINSCOPE_URL = 'http://localhost:8080';
  static readonly REMOTE_TOOL_MOCK_URL = 'http://localhost:8081';

  static async checkServerIsUp(name: string, url: string) {
    try {
      await browser.get(url);
    } catch (error) {
      fail(`${name} server (${url}) looks down. Did you start it?`);
    }
  }

  static async uploadFixture(...paths: string[]) {
    const inputFile = element(by.css('input[type="file"]'));

    // Uploading multiple files is not properly supported but
    // chrome handles file paths joined with new lines
    await inputFile.sendKeys(paths.map((it) => E2eTestUtils.getFixturePath(it)).join('\n'));
  }

  static async clickViewTracesButton() {
    const button = element(by.css('.load-btn'));
    await button.click();
  }

  static async closeSnackBarIfNeeded() {
    const closeButton = element(by.css('.snack-bar-action'));
    const isPresent = await closeButton.isPresent();
    if (isPresent) {
      await closeButton.click();
    }
  }
}

export {E2eTestUtils};
