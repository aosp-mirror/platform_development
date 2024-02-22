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
  const DEFAULT_TIMEOUT_MS = 15000;

  beforeAll(async () => {
    jasmine.DEFAULT_TIMEOUT_INTERVAL = DEFAULT_TIMEOUT_MS;
    await browser.manage().timeouts().implicitlyWait(DEFAULT_TIMEOUT_MS);
    await E2eTestUtils.checkServerIsUp('Winscope', E2eTestUtils.WINSCOPE_URL);
  });

  beforeEach(async () => {
    await browser.get(E2eTestUtils.WINSCOPE_URL);
  });

  it('can process bugreport', async () => {
    await E2eTestUtils.loadBugReport(DEFAULT_TIMEOUT_MS);
    await E2eTestUtils.clickViewTracesButton();
    await checkRendersSurfaceFlingerView();
  });

  it("doesn't emit messages for valid trace file", async () => {
    await E2eTestUtils.uploadFixture(
      'traces/elapsed_and_real_timestamp/SurfaceFlinger.pb',
    );
    expect(
      await E2eTestUtils.areMessagesEmitted(DEFAULT_TIMEOUT_MS),
    ).toBeFalsy();
  });

  async function checkRendersSurfaceFlingerView() {
    const viewerPresent = await element(
      by.css('viewer-surface-flinger'),
    ).isPresent();
    expect(viewerPresent).toBeTruthy();
  }
});
