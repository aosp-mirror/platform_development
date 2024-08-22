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
import {E2eTestUtils} from './utils';

describe('Upload traces', () => {
  const DEFAULT_TIMEOUT_MS = 20000;

  beforeAll(async () => {
    jasmine.DEFAULT_TIMEOUT_INTERVAL = DEFAULT_TIMEOUT_MS;
  });

  beforeEach(async () => {
    await E2eTestUtils.beforeEach(DEFAULT_TIMEOUT_MS);
    await browser.get(E2eTestUtils.WINSCOPE_URL);
  });

  it('can clear all files', async () => {
    await E2eTestUtils.loadBugReport(DEFAULT_TIMEOUT_MS);
    await E2eTestUtils.clickClearAllButton();
    await checkNoFilesUploaded();
  });

  it('can remove a file using the close icon', async () => {
    await E2eTestUtils.loadBugReport(DEFAULT_TIMEOUT_MS);
    await E2eTestUtils.clickCloseIcon();
    await checkFileRemoved();
  });

  it('can replace an uploaded file with a new file', async () => {
    await E2eTestUtils.loadBugReport(DEFAULT_TIMEOUT_MS);
    await E2eTestUtils.uploadFixture(
      'traces/perfetto/layers_trace.perfetto-trace',
    );
    await checkFileReplaced();
  });

  it('can process bugreport', async () => {
    await E2eTestUtils.loadBugReport(DEFAULT_TIMEOUT_MS);
    await E2eTestUtils.clickViewTracesButton();
    await checkRendersSurfaceFlingerView();
  });

  async function checkRendersSurfaceFlingerView() {
    const viewerPresent = await element(
      by.css('viewer-surface-flinger'),
    ).isPresent();
    expect(viewerPresent).toBeTruthy();
  }

  it("doesn't emit messages for valid trace file", async () => {
    await E2eTestUtils.uploadFixture(
      'traces/elapsed_and_real_timestamp/SurfaceFlinger.pb',
    );
    expect(
      await E2eTestUtils.areMessagesEmitted(DEFAULT_TIMEOUT_MS),
    ).toBeFalsy();
  });

  async function checkNoFilesUploaded() {
    // default timeout to understand whether the messages where emitted or not.
    await browser.manage().timeouts().implicitlyWait(1000);
    const filesUploaded = await element(by.css('.uploaded-files')).isPresent();
    await browser.manage().timeouts().implicitlyWait(DEFAULT_TIMEOUT_MS);
    expect(filesUploaded).toBeFalsy();
  }

  async function checkFileRemoved() {
    const text = await element(by.css('.uploaded-files')).getText();
    expect(text).toContain('Window Manager');
    expect(text).not.toContain('Surface Flinger');
    expect(text).toContain('Transactions');
    expect(text).toContain('Transitions');
  }

  async function checkFileReplaced() {
    const text = await element(by.css('.uploaded-files')).getText();
    expect(text).toContain('Surface Flinger');

    expect(text).not.toContain('layers_trace_from_transactions.winscope');
    expect(text).toContain('layers_trace.perfetto-trace');
  }
});
