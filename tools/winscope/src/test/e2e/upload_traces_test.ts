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
    await E2eTestUtils.uploadFixture('bugreports/bugreport_stripped.zip');
    await checkHasLoadedTraces();
    expect(await areMessagesEmitted()).toBeTruthy();
    await checkEmitsUnsupportedFileFormatMessages();
    await checkEmitsOverriddenTracesMessages();
    await E2eTestUtils.closeSnackBarIfNeeded();
    await E2eTestUtils.clickViewTracesButton();
    await checkRendersSurfaceFlingerView();
  });

  it("doesn't emit messages for valid trace file", async () => {
    await E2eTestUtils.uploadFixture('traces/elapsed_and_real_timestamp/SurfaceFlinger.pb');
    expect(await areMessagesEmitted()).toBeFalsy();
  });

  const checkHasLoadedTraces = async () => {
    const text = await element(by.css('.uploaded-files')).getText();
    expect(text).toContain('ProtoLog');
    expect(text).toContain('IME Service');
    expect(text).toContain('IME Manager Service');
    expect(text).toContain('Window Manager');
    expect(text).toContain('Surface Flinger');
    expect(text).toContain('IME Clients');
    expect(text).toContain('Transactions');
    expect(text).toContain('Transitions');

    expect(text).toContain('wm_log.winscope');
    expect(text).toContain('ime_trace_service.winscope');
    expect(text).toContain('ime_trace_managerservice.winscope');
    expect(text).toContain('wm_trace.winscope');
    expect(text).toContain('layers_trace_from_transactions.winscope');
    expect(text).toContain('ime_trace_clients.winscope');
    expect(text).toContain('transactions_trace.winscope');
    expect(text).toContain('wm_transition_trace.winscope');
    expect(text).toContain('shell_transition_trace.winscope');
  };

  const checkEmitsUnsupportedFileFormatMessages = async () => {
    const text = await element(by.css('snack-bar')).getText();
    expect(text).toContain('unsupported file format');
  };

  const checkEmitsOverriddenTracesMessages = async () => {
    const text = await element(by.css('snack-bar')).getText();
    expect(text).toContain('overridden by another trace');
  };

  const areMessagesEmitted = async (): Promise<boolean> => {
    // Messages are emitted quickly. There is no Need to wait for the entire
    // default timeout to understand whether the messages where emitted or not.
    await browser.manage().timeouts().implicitlyWait(1000);
    const emitted = await element(by.css('snack-bar')).isPresent();
    await browser.manage().timeouts().implicitlyWait(DEFAULT_TIMEOUT_MS);
    return emitted;
  };

  const checkRendersSurfaceFlingerView = async () => {
    const viewerPresent = await element(by.css('viewer-surface-flinger')).isPresent();
    expect(viewerPresent).toBeTruthy();
  };
});
