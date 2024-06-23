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
import * as path from 'path';
import {browser, by, element, ElementFinder} from 'protractor';

class E2eTestUtils {
  static readonly WINSCOPE_URL = 'http://localhost:8080';
  static readonly REMOTE_TOOL_MOCK_URL = 'http://localhost:8081';

  static async checkServerIsUp(name: string, url: string) {
    try {
      await browser.get(url);
    } catch (error) {
      fail(`${name} server (${url}) looks down. Did you start it?`);
    }
  }

  static async loadTraceAndCheckViewer(
    fixturePath: string,
    viewerTabTitle: string,
    viewerSelector: string,
  ) {
    await E2eTestUtils.uploadFixture(fixturePath);
    await E2eTestUtils.closeSnackBarIfNeeded();
    await E2eTestUtils.clickViewTracesButton();
    await E2eTestUtils.clickViewerTabButton(viewerTabTitle);

    const viewerPresent = await element(by.css(viewerSelector)).isPresent();
    expect(viewerPresent).toBeTruthy();
  }

  static async loadBugReport(defaulttimeMs: number) {
    await E2eTestUtils.uploadFixture('bugreports/bugreport_stripped.zip');
    await E2eTestUtils.checkHasLoadedTracesFromBugReport();
    expect(await E2eTestUtils.areMessagesEmitted(defaulttimeMs)).toBeTruthy();
    await E2eTestUtils.checkEmitsUnsupportedFileFormatMessages();
    await E2eTestUtils.checkEmitsOldDataMessages();
    await E2eTestUtils.closeSnackBarIfNeeded();
  }

  static async areMessagesEmitted(defaultTimeoutMs: number): Promise<boolean> {
    // Messages are emitted quickly. There is no Need to wait for the entire
    // default timeout to understand whether the messages where emitted or not.
    await browser.manage().timeouts().implicitlyWait(1000);
    const emitted = await element(by.css('snack-bar')).isPresent();
    await browser.manage().timeouts().implicitlyWait(defaultTimeoutMs);
    return emitted;
  }

  static async clickViewTracesButton() {
    const button = element(by.css('.load-btn'));
    await button.click();
  }

  static async clickClearAllButton() {
    const button = element(by.css('.clear-all-btn'));
    await button.click();
  }

  static async clickCloseIcon() {
    const button = element.all(by.css('.uploaded-files button')).first();
    await button.click();
  }

  static async clickDownloadTracesButton() {
    const button = element(by.css('.save-button'));
    await button.click();
  }

  static async clickUploadNewButton() {
    const button = element(by.css('.upload-new'));
    await button.click();
  }

  static async closeSnackBarIfNeeded() {
    const closeButton = element(by.css('.snack-bar-action'));
    const isPresent = await closeButton.isPresent();
    if (isPresent) {
      await closeButton.click();
    }
  }

  static async clickViewerTabButton(title: string) {
    const tabs: ElementFinder[] = await element.all(by.css('trace-view .tab'));
    for (const tab of tabs) {
      const tabTitle = await tab.getText();
      if (tabTitle.includes(title)) {
        await tab.click();
        return;
      }
    }
    throw Error(`could not find tab corresponding to ${title}`);
  }

  static async checkTimelineTraceSelector(trace: {
    icon: string;
    color: string;
  }) {
    const traceSelector = element(by.css('#trace-selector'));
    const text = await traceSelector.getText();
    expect(text).toContain(trace.icon);

    const icons: ElementFinder[] = await element.all(
      by.css('.shown-selection .mat-icon'),
    );
    const iconColors: string[] = [];
    for (const icon of icons) {
      iconColors.push(await icon.getCssValue('color'));
    }
    expect(
      iconColors.some((iconColor) => iconColor === trace.color),
    ).toBeTruthy();
  }

  static async checkInitialRealTimestamp(timestamp: string) {
    await E2eTestUtils.changeRealTimestampInWinscope(timestamp);
    await E2eTestUtils.checkWinscopeRealTimestamp(timestamp);
    const prevEntryButton = element(by.css('#prev_entry_button'));
    const isDisabled = await prevEntryButton.getAttribute('disabled');
    expect(isDisabled).toEqual('true');
  }

  static async checkFinalRealTimestamp(timestamp: string) {
    await E2eTestUtils.changeRealTimestampInWinscope(timestamp);
    await E2eTestUtils.checkWinscopeRealTimestamp(timestamp);
    const nextEntryButton = element(by.css('#next_entry_button'));
    const isDisabled = await nextEntryButton.getAttribute('disabled');
    expect(isDisabled).toEqual('true');
  }

  static async checkWinscopeRealTimestamp(timestamp: string) {
    const inputElement = element(by.css('input[name="humanRealTimeInput"]'));
    const value = await inputElement.getAttribute('value');
    expect(value).toEqual(timestamp);
  }

  static async changeRealTimestampInWinscope(newTimestamp: string) {
    await E2eTestUtils.updateInputField('', 'humanRealTimeInput', newTimestamp);
  }

  static async checkWinscopeNsTimestamp(newTimestamp: string) {
    const inputElement = element(by.css('input[name="nsTimeInput"]'));
    const valueWithNsSuffix = await inputElement.getAttribute('value');
    expect(valueWithNsSuffix).toEqual(newTimestamp + ' ns');
  }

  static async changeNsTimestampInWinscope(newTimestamp: string) {
    await E2eTestUtils.updateInputField('', 'nsTimeInput', newTimestamp);
  }

  static async filterHierarchy(viewer: string, filterString: string) {
    await E2eTestUtils.updateInputField(
      `${viewer} hierarchy-view .title-filter`,
      'filter',
      filterString,
    );
  }

  static async updateInputField(
    inputFieldSelector: string,
    inputFieldName: string,
    newInput: string,
  ) {
    const inputElement = element(
      by.css(`${inputFieldSelector} input[name="${inputFieldName}"]`),
    );
    const inputStringStep1 = newInput.slice(0, -1);
    const inputStringStep2 = newInput.slice(-1) + '\r\n';
    const script = `document.querySelector("${inputFieldSelector} input[name=\\"${inputFieldName}\\"]").value = "${inputStringStep1}"`;
    await browser.executeScript(script);
    await inputElement.sendKeys(inputStringStep2);
  }

  static async selectItemInHierarchy(viewer: string, itemName: string) {
    const nodes: ElementFinder[] = await element.all(
      by.css(`${viewer} hierarchy-view .node`),
    );
    for (const node of nodes) {
      const id = await node.getAttribute('id');
      if (id.includes(itemName)) {
        await node.click();
        return;
      }
    }
    throw Error(`could not find item matching ${itemName} in hierarchy`);
  }

  static async applyStateToHierarchyCheckboxes(
    viewerSelector: string,
    shouldEnable: boolean,
  ) {
    const checkboxes: ElementFinder[] = await element.all(
      by.css(`${viewerSelector} hierarchy-view .view-controls .mat-checkbox`),
    );
    for (const box of checkboxes) {
      const input = box.element(by.css('input'));
      const isEnabled = await input.isSelected();
      if (shouldEnable && !isEnabled) {
        await box.click();
      } else if (!shouldEnable && isEnabled) {
        await box.click();
      }
    }
  }

  static async checkItemInPropertiesTree(
    viewer: string,
    itemName: string,
    expectedText: string,
  ) {
    const nodes = await element.all(by.css(`${viewer} .properties-view .node`));
    for (const node of nodes) {
      const id: string = await node.getAttribute('id');
      if (id === 'node' + itemName) {
        const text = await node.getText();
        expect(text).toEqual(expectedText);
        return;
      }
    }
    throw Error(`could not find item ${itemName} in properties tree`);
  }

  static async checkRectLabel(viewer: string, expectedLabel: string) {
    const labels = await element.all(
      by.css(`${viewer} rects-view .rect-label`),
    );

    let foundLabel: ElementFinder | undefined;

    for (const label of labels) {
      const text = await label.getText();
      if (text.includes(expectedLabel)) {
        foundLabel = label;
        break;
      }
    }

    expect(foundLabel).toBeTruthy();
  }

  static async uploadFixture(...paths: string[]) {
    const inputFile = element(by.css('input[type="file"]'));

    // Uploading multiple files is not properly supported but
    // chrome handles file paths joined with new lines
    await inputFile.sendKeys(
      paths.map((it) => E2eTestUtils.getFixturePath(it)).join('\n'),
    );
  }

  static getFixturePath(filename: string): string {
    if (path.isAbsolute(filename)) {
      return filename;
    }
    return path.join(
      E2eTestUtils.getProjectRootPath(),
      'src/test/fixtures',
      filename,
    );
  }

  private static getProjectRootPath(): string {
    let root = __dirname;
    while (path.basename(root) !== 'winscope') {
      root = path.dirname(root);
    }
    return root;
  }

  private static async checkHasLoadedTracesFromBugReport() {
    const text = await element(by.css('.uploaded-files')).getText();
    expect(text).toContain('Window Manager');
    expect(text).toContain('Surface Flinger');
    expect(text).toContain('Transactions');
    expect(text).toContain('Transitions');

    // Should be merged into a single Transitions trace
    expect(text).not.toContain('WM Transitions');
    expect(text).not.toContain('Shell Transitions');

    expect(text).toContain('layers_trace_from_transactions.winscope');
    expect(text).toContain('transactions_trace.winscope');
    expect(text).toContain('wm_transition_trace.winscope');
    expect(text).toContain('shell_transition_trace.winscope');
    expect(text).toContain('window_CRITICAL.proto');

    // discards some traces due to old data
    expect(text).not.toContain('ProtoLog');
    expect(text).not.toContain('IME Service');
    expect(text).not.toContain('IME Manager Service');
    expect(text).not.toContain('IME Clients');
    expect(text).not.toContain('wm_log.winscope');
    expect(text).not.toContain('ime_trace_service.winscope');
    expect(text).not.toContain('ime_trace_managerservice.winscope');
    expect(text).not.toContain('wm_trace.winscope');
    expect(text).not.toContain('ime_trace_clients.winscope');
  }

  private static async checkEmitsUnsupportedFileFormatMessages() {
    const text = await element(by.css('snack-bar')).getText();
    expect(text).toContain('unsupported format');
  }

  private static async checkEmitsOldDataMessages() {
    const text = await element(by.css('snack-bar')).getText();
    expect(text).toContain('discarded because data is older than');
  }
}

export {E2eTestUtils};
