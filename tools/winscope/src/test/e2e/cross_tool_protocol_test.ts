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

import {browser, by, element, ElementFinder} from 'protractor';
import {E2eTestUtils} from './utils';

describe('Cross-Tool Protocol', () => {
  const TIMESTAMP_IN_BUGREPORT_MESSAGE = '1670509911000000000';
  const TIMESTAMP_IN_FILES_MESSAGE = '1659107090338700812';
  const TIMESTAMP_FROM_REMOTE_TOOL_TO_WINSCOPE = '1670509912000000000';
  const TIMESTAMP_FROM_WINSCOPE_TO_REMOTE_TOOL = '1670509913000000000';

  beforeEach(async () => {
    await browser.restart();

    jasmine.DEFAULT_TIMEOUT_INTERVAL = 20000;
    await browser.manage().timeouts().implicitlyWait(20000);
    await E2eTestUtils.checkServerIsUp(
      'Remote tool mock',
      E2eTestUtils.REMOTE_TOOL_MOCK_URL,
    );
    await E2eTestUtils.checkServerIsUp('Winscope', E2eTestUtils.WINSCOPE_URL);

    await browser.get(E2eTestUtils.REMOTE_TOOL_MOCK_URL);
  });

  it('allows cross-tool communication (open winscope with MessageBugreport)', async () => {
    await openWinscopeTabFromRemoteTool();
    await waitWinscopeTabIsOpen();

    await sendBugreportToWinscope();
    await checkWinscopeRendersUploadView();
    await closeWinscopeSnackBar();

    await clickWinscopeViewTracesButton();
    await checkWinscopeRenderedSurfaceFlingerView();
    await checkWinscopeRenderedAllViewTabs();
    await checkWinscopeTimestamp(TIMESTAMP_IN_BUGREPORT_MESSAGE);

    await sendTimestampToWinscope(TIMESTAMP_FROM_REMOTE_TOOL_TO_WINSCOPE);
    await checkWinscopeTimestamp(TIMESTAMP_FROM_REMOTE_TOOL_TO_WINSCOPE);

    await sendTimestampToRemoteTool(TIMESTAMP_FROM_WINSCOPE_TO_REMOTE_TOOL);
    await checkRemoteToolTimestamp(TIMESTAMP_FROM_WINSCOPE_TO_REMOTE_TOOL);
  });

  it('allows cross-tool communication (open winscope with MessageFiles)', async () => {
    await openWinscopeTabFromRemoteTool();
    await waitWinscopeTabIsOpen();

    await sendFilesToWinscope();
    await checkWinscopeRendersUploadView();

    await clickWinscopeViewTracesButton();
    await checkWinscopeRenderedSurfaceFlingerView();
    await checkWinscopeTimestamp(TIMESTAMP_IN_FILES_MESSAGE);

    await sendTimestampToWinscope(TIMESTAMP_FROM_REMOTE_TOOL_TO_WINSCOPE);
    await checkWinscopeTimestamp(TIMESTAMP_FROM_REMOTE_TOOL_TO_WINSCOPE);

    await sendTimestampToRemoteTool(TIMESTAMP_FROM_WINSCOPE_TO_REMOTE_TOOL);
    await checkRemoteToolTimestamp(TIMESTAMP_FROM_WINSCOPE_TO_REMOTE_TOOL);
  });

  async function openWinscopeTabFromRemoteTool() {
    await browser.switchTo().window(await getWindowHandleRemoteToolMock());
    const buttonElement = element(by.css('.button-open-winscope'));
    await buttonElement.click();
  }

  async function sendBugreportToWinscope() {
    await browser.switchTo().window(await getWindowHandleRemoteToolMock());
    const inputFileElement = element(by.css('.button-send-bugreport'));
    await inputFileElement.sendKeys(
      E2eTestUtils.getFixturePath('bugreports/bugreport_stripped.zip'),
    );
  }

  async function sendFilesToWinscope() {
    await browser.switchTo().window(await getWindowHandleRemoteToolMock());
    const inputFileElement = element(by.css('.button-send-files'));
    await inputFileElement.sendKeys(
      E2eTestUtils.getFixturePath(
        'traces/perfetto/layers_trace.perfetto-trace',
      ),
    );
  }

  async function checkWinscopeRendersUploadView() {
    await browser.switchTo().window(await getWindowHandleWinscope());
    const isPresent = await element(by.css('.uploaded-files')).isPresent();
    expect(isPresent).toBeTruthy();
  }

  async function clickWinscopeViewTracesButton() {
    await browser.switchTo().window(await getWindowHandleWinscope());
    await E2eTestUtils.clickViewTracesButton();
  }

  async function closeWinscopeSnackBar() {
    await browser.switchTo().window(await getWindowHandleWinscope());
    await E2eTestUtils.closeSnackBar();
  }

  async function waitWinscopeTabIsOpen() {
    await browser.wait(
      async () => {
        const handles = await browser.getAllWindowHandles();
        return handles.length >= 2;
      },
      20000,
      'The Winscope tab did not open',
    );
  }

  async function checkWinscopeRenderedSurfaceFlingerView() {
    await browser.switchTo().window(await getWindowHandleWinscope());
    const viewerPresent = await element(
      by.css('viewer-surface-flinger'),
    ).isPresent();
    expect(viewerPresent).toBeTruthy();
  }

  async function checkWinscopeRenderedAllViewTabs() {
    const tabParagraphs = await element.all(
      by.css('.tabs-navigation-bar a span'),
    );

    const actualTabParagraphs = await Promise.all(
      (tabParagraphs as ElementFinder[]).map(
        async (paragraph) => await paragraph.getText(),
      ),
    );

    const expectedTabParagraphs = [
      'Surface Flinger',
      'Transactions',
      'Transitions',
      'Window Manager',
    ];

    expect(actualTabParagraphs.sort()).toEqual(expectedTabParagraphs.sort());
  }

  async function sendTimestampToWinscope(value: string) {
    await browser.switchTo().window(await getWindowHandleRemoteToolMock());
    const inputElement = element(by.css('.input-timestamp'));
    await inputElement.sendKeys(value);
    const buttonElement = element(by.css('.button-send-timestamp'));
    await buttonElement.click();
  }

  async function sendTimestampToRemoteTool(value: string) {
    browser.switchTo().window(await getWindowHandleWinscope());
    await E2eTestUtils.changeNsTimestampInWinscope(value);
  }

  async function checkWinscopeTimestamp(expectedValue: string) {
    await browser.switchTo().window(await getWindowHandleWinscope());
    await E2eTestUtils.checkWinscopeNsTimestamp(expectedValue);
  }

  async function checkRemoteToolTimestamp(expectedValue: string) {
    await browser.switchTo().window(await getWindowHandleRemoteToolMock());
    const paragraphElement = element(by.css('.paragraph-received-timestamp'));
    const actualValue = await paragraphElement.getText();
    expect(actualValue).toEqual(expectedValue);
  }

  async function getWindowHandleRemoteToolMock(): Promise<string> {
    const handles = await browser.getAllWindowHandles();
    expect(handles.length).toBeGreaterThan(0);
    return handles[0];
  }

  async function getWindowHandleWinscope(): Promise<string> {
    const handles = await browser.getAllWindowHandles();
    expect(handles.length).toEqual(2);
    return handles[1];
  }
});
