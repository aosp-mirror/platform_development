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

import {browser, by, element, ElementFinder} from 'protractor';
import {E2eTestUtils} from './utils';

describe('Trace navigation', () => {
  const DEFAULT_TIMEOUT_MS = 1000;

  beforeAll(async () => {
    await browser.manage().timeouts().implicitlyWait(DEFAULT_TIMEOUT_MS);
    await E2eTestUtils.checkServerIsUp('Winscope', E2eTestUtils.WINSCOPE_URL);
  });

  beforeEach(async () => {
    await browser.get(E2eTestUtils.WINSCOPE_URL);
  });

  it('can go between home and trace view pages correctly', async () => {
    await E2eTestUtils.uploadFixture(
      'traces/perfetto/layers_trace.perfetto-trace',
    );
    await checkHomepage();
    await E2eTestUtils.closeSnackBarIfNeeded();
    await E2eTestUtils.clickViewTracesButton();
    await checkTraceViewPage();

    await E2eTestUtils.clickUploadNewButton();
    await checkHomepage();
  });

  async function checkHomepage() {
    const toolbar = element(by.css('.toolbar'));
    const elements = [
      toolbar.element(by.css('.app-title')),
      toolbar.element(by.css('.documentation')),
      toolbar.element(by.css('.report-bug')),
      toolbar.element(by.css('.dark-mode')),
      element(by.css('.welcome-info')),
      element(by.css('collect-traces')),
      element(by.css('upload-traces')),
    ];
    await checkElementsPresent(elements);
  }

  async function checkTraceViewPage() {
    const toolbar = element(by.css('.toolbar'));
    const elements = [
      toolbar.element(by.css('.app-title')),
      toolbar.element(by.css('.file-descriptor')),
      toolbar.element(by.css('.upload-new')),
      toolbar.element(by.css('.save-button')),
      toolbar.element(by.css('.trace-file-info')),
      toolbar.element(by.css('.documentation')),
      toolbar.element(by.css('.report-bug')),
      toolbar.element(by.css('.dark-mode')),
      element(by.css('viewer-surface-flinger')),
      element(by.css('timeline')),
    ];
    await checkElementsPresent(elements);
  }

  async function checkElementsPresent(elements: ElementFinder[]) {
    for (const element of elements) {
      expect(await element.isPresent()).toBeTruthy();
    }
  }
});
