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
  const TIMESTAMP_FROM_REMOTE_TOOL_TO_WINSCOPE = '1670509912000000000';
  const TIMESTAMP_FROM_WINSCOPE_TO_REMOTE_TOOL = '1670509913000000000';

  beforeAll(async () => {
    jasmine.DEFAULT_TIMEOUT_INTERVAL = 15000;
    await browser.manage().timeouts().implicitlyWait(15000);
    await E2eTestUtils.checkServerIsUp('Remote tool mock', E2eTestUtils.REMOTE_TOOL_MOCK_URL);
    await E2eTestUtils.checkServerIsUp('Winscope', E2eTestUtils.WINSCOPE_URL);
  });

  beforeEach(async () => {
    await browser.get(E2eTestUtils.REMOTE_TOOL_MOCK_URL);
  });

  it('allows communication between remote tool and Winscope', async () => {
    await openWinscopeTabFromRemoteTool();
    await waitWinscopeTabIsOpen();

    await sendBugreportToWinscope();
    await checkWinscopeRendersUploadView();
    await closeWinscopeSnackBarIfNeeded();

    await clickWinscopeViewTracesButton();
    await checkWinscopeRenderedSurfaceFlingerView();
    await checkWinscopeRenderedAllViewTabs();
    await checkWinscopeAppliedTimestampInBugreportMessage();

    await sendTimestampToWinscope();
    await checkWinscopeReceivedTimestamp();

    await changeTimestampInWinscope();
    await checkRemoteToolReceivedTimestamp();
  });

  const openWinscopeTabFromRemoteTool = async () => {
    await browser.switchTo().window(await getWindowHandleRemoteToolMock());
    const buttonElement = element(by.css('.button-open-winscope'));
    await buttonElement.click();
  };

  const sendBugreportToWinscope = async () => {
    await browser.switchTo().window(await getWindowHandleRemoteToolMock());
    const inputFileElement = element(by.css('.button-upload-bugreport'));
    await inputFileElement.sendKeys(
      E2eTestUtils.getFixturePath('bugreports/bugreport_stripped.zip')
    );
  };

  const checkWinscopeRendersUploadView = async () => {
    await browser.switchTo().window(await getWindowHandleWinscope());
    const isPresent = await element(by.css('.uploaded-files')).isPresent();
    expect(isPresent).toBeTruthy();
  };

  const clickWinscopeViewTracesButton = async () => {
    await browser.switchTo().window(await getWindowHandleWinscope());
    await E2eTestUtils.clickViewTracesButton();
  };

  const closeWinscopeSnackBarIfNeeded = async () => {
    await browser.switchTo().window(await getWindowHandleWinscope());
    await E2eTestUtils.closeSnackBarIfNeeded();
  };

  const waitWinscopeTabIsOpen = async () => {
    await browser.wait(
      async () => {
        const handles = await browser.getAllWindowHandles();
        return handles.length >= 2;
      },
      20000,
      'The Winscope tab did not open'
    );
  };

  const checkWinscopeRenderedSurfaceFlingerView = async () => {
    await browser.switchTo().window(await getWindowHandleWinscope());
    const viewerPresent = await element(by.css('viewer-surface-flinger')).isPresent();
    expect(viewerPresent).toBeTruthy();
  };

  const checkWinscopeRenderedAllViewTabs = async () => {
    const tabParagraphs = await element.all(by.css('.tabs-navigation-bar a p'));

    const actualTabParagraphs = await Promise.all(
      (tabParagraphs as ElementFinder[]).map(async (paragraph) => await paragraph.getText())
    );

    const expectedTabParagraphs = [
      'Input Method Clients',
      'Input Method Manager Service',
      'Input Method Service',
      'ProtoLog',
      'Surface Flinger',
      'Transactions',
      'Transitions',
      'Window Manager',
    ];

    expect(actualTabParagraphs.sort()).toEqual(expectedTabParagraphs.sort());
  };

  const checkWinscopeAppliedTimestampInBugreportMessage = async () => {
    await browser.switchTo().window(await getWindowHandleWinscope());
    const inputElement = element(by.css('input[name="nsTimeInput"]'));
    const valueWithNsSuffix = await inputElement.getAttribute('value');
    expect(valueWithNsSuffix).toEqual(TIMESTAMP_IN_BUGREPORT_MESSAGE + ' ns');
  };

  const sendTimestampToWinscope = async () => {
    await browser.switchTo().window(await getWindowHandleRemoteToolMock());
    const inputElement = element(by.css('.input-timestamp'));
    await inputElement.sendKeys(TIMESTAMP_FROM_REMOTE_TOOL_TO_WINSCOPE);
    const buttonElement = element(by.css('.button-send-timestamp'));
    await buttonElement.click();
  };

  const checkWinscopeReceivedTimestamp = async () => {
    await browser.switchTo().window(await getWindowHandleWinscope());
    const inputElement = element(by.css('input[name="nsTimeInput"]'));
    const valueWithNsSuffix = await inputElement.getAttribute('value');
    expect(valueWithNsSuffix).toEqual(TIMESTAMP_FROM_REMOTE_TOOL_TO_WINSCOPE + ' ns');
  };

  const changeTimestampInWinscope = async () => {
    await browser.switchTo().window(await getWindowHandleWinscope());
    const inputElement = element(by.css('input[name="nsTimeInput"]'));
    const inputStringStep1 = TIMESTAMP_FROM_WINSCOPE_TO_REMOTE_TOOL.slice(0, -1);
    const inputStringStep2 = TIMESTAMP_FROM_WINSCOPE_TO_REMOTE_TOOL.slice(-1) + '\r\n';
    const script = `document.querySelector("input[name=\\"nsTimeInput\\"]").value = "${inputStringStep1}"`;
    await browser.executeScript(script);
    await inputElement.sendKeys(inputStringStep2);
  };

  const checkRemoteToolReceivedTimestamp = async () => {
    await browser.switchTo().window(await getWindowHandleRemoteToolMock());
    const paragraphElement = element(by.css('.paragraph-received-timestamp'));
    const value = await paragraphElement.getText();
    expect(value).toEqual(TIMESTAMP_FROM_WINSCOPE_TO_REMOTE_TOOL);
  };

  const getWindowHandleRemoteToolMock = async (): Promise<string> => {
    const handles = await browser.getAllWindowHandles();
    expect(handles.length).toBeGreaterThan(0);
    return handles[0];
  };

  const getWindowHandleWinscope = async (): Promise<string> => {
    const handles = await browser.getAllWindowHandles();
    expect(handles.length).toEqual(2);
    return handles[1];
  };
});
