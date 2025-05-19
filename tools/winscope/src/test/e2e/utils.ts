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
import {browser, by, element, ElementFinder, protractor} from 'protractor';

export const WINSCOPE_URL = 'http://localhost:8080';
export const REMOTE_TOOL_MOCK_URL = 'http://localhost:8081';
const JASMINE_DEFAULT_TIMEOUT_MS = 40000;

export async function setTimeouts(
  defaultTimeoutMs: number,
  jasmineTimeoutMs = JASMINE_DEFAULT_TIMEOUT_MS,
) {
  jasmine.DEFAULT_TIMEOUT_INTERVAL = jasmineTimeoutMs;
  await browser.manage().timeouts().implicitlyWait(defaultTimeoutMs);
  await checkServerIsUp('Winscope', WINSCOPE_URL);
  await browser.driver.manage().window().maximize();
}

export async function checkServerIsUp(name: string, url: string) {
  try {
    await browser.get(url);
  } catch (error) {
    fail(`${name} server (${url}) looks down. Did you start it?`);
  }
}

export async function loadTraceAndCheckViewer(
  fixturePath: string,
  viewerTabTitle: string,
  viewerSelector: string,
) {
  await uploadFixture(fixturePath);
  await closeSnackBar();
  await clickViewTracesButton();
  await clickViewerTabButton(viewerTabTitle);

  const viewerPresent = await element(by.css(viewerSelector)).isPresent();
  expect(viewerPresent).toBeTruthy();
}

export async function loadBugReport(defaulttimeMs: number) {
  await uploadFixture('bugreports/bugreport_stripped.zip');
  await checkHasLoadedTracesFromBugReport();
  expect(await areMessagesEmitted(defaulttimeMs)).toBeTruthy();
  await checkEmitsUnsupportedFileFormatMessages();
  await checkEmitsOldDataMessages();
  await closeSnackBar();
}

export async function areMessagesEmitted(
  defaultTimeoutMs: number,
): Promise<boolean> {
  // Messages are emitted quickly. There is no Need to wait for the entire
  // default timeout to understand whether the messages where emitted or not.
  await browser.manage().timeouts().implicitlyWait(1000);
  const emitted = await element(by.css('snack-bar')).isPresent();
  await browser.manage().timeouts().implicitlyWait(defaultTimeoutMs);
  return emitted;
}

export async function clickViewTracesButton() {
  const button = element(by.css('.load-btn'));
  await button.click();
}

export async function clickClearAllButton() {
  const button = element(by.css('.clear-all-btn'));
  await button.click();
}

export async function clickCloseIcon() {
  const button = element.all(by.css('.uploaded-files button')).first();
  await button.click();
}

export async function clickDownloadTracesButton() {
  const button = element(by.css('.save-button'));
  await button.click();
}

export async function clickUploadNewButton() {
  const button = element(by.css('.upload-new'));
  await button.click();
}

export async function closeSnackBar() {
  const closeButton = element(by.css('.snack-bar-action'));
  const isPresent = await closeButton.isPresent();
  if (isPresent) {
    await closeButton.click();
  }
}

export async function clickViewerTabButton(title: string) {
  await browser.wait(
    async () => {
      return await element(by.css('trace-view')).isPresent();
    },
    20000,
    'Viewers failed to load',
  );
  const tabs: ElementFinder[] = await element.all(by.css('trace-view .tab'));
  for (const tab of tabs) {
    const tabTitle = await tab.getText();
    if (tabTitle.includes(title)) {
      await tab.click();
      return;
    }
  }
  throw new Error(`could not find tab corresponding to ${title}`);
}

export async function checkTimelineTraceSelector(trace: {
  icon: string;
  color: string;
}) {
  const traceSelector = element(by.css('#trace-selector'));
  const text = await traceSelector.getText();
  expect(text).toContain(trace.icon);

  const icons = await element.all(by.css('.shown-selection .mat-icon'));
  const iconColors: string[] = [];
  for (const icon of icons) {
    iconColors.push(await icon.getCssValue('color'));
  }
  expect(
    iconColors.some((iconColor) => iconColor === trace.color),
  ).toBeTruthy();
}

export async function checkInitialRealTimestamp(timestamp: string) {
  await changeRealTimestampInWinscope(timestamp);
  await checkWinscopeRealTimestamp(timestamp.slice(12));
  const prevEntryButton = element(by.css('#prev_entry_button'));
  const isDisabled = await prevEntryButton.getAttribute('disabled');
  expect(isDisabled).toEqual('true');
}

export async function checkFinalRealTimestamp(timestamp: string) {
  await changeRealTimestampInWinscope(timestamp);
  await checkWinscopeRealTimestamp(timestamp.slice(12));
  const nextEntryButton = element(by.css('#next_entry_button'));
  const isDisabled = await nextEntryButton.getAttribute('disabled');
  expect(isDisabled).toEqual('true');
}

export async function checkWinscopeRealTimestamp(timestamp: string) {
  const inputElement = element(by.css('input[name="humanTimeInput"]'));
  const value = await inputElement.getAttribute('value');
  expect(value).toEqual(timestamp);
}

export async function changeRealTimestampInWinscope(newTimestamp: string) {
  await updateInputField('', 'humanTimeInput', newTimestamp);
}

export async function checkWinscopeNsTimestamp(newTimestamp: string) {
  const inputElement = element(by.css('input[name="nsTimeInput"]'));
  const valueWithNsSuffix = await inputElement.getAttribute('value');
  expect(valueWithNsSuffix).toEqual(newTimestamp + ' ns');
}

export async function changeNsTimestampInWinscope(newTimestamp: string) {
  await updateInputField('', 'nsTimeInput', newTimestamp);
}

export async function filterHierarchy(viewer: string, filterString: string) {
  await updateInputField(
    `${viewer} hierarchy-view .title-section`,
    'filter',
    filterString,
  );
}

export async function updateInputField(
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

export async function selectItemInHierarchy(viewer: string, itemName: string) {
  const nodes: ElementFinder[] = await element.all(
    by.css(`${viewer} hierarchy-view .node`),
  );
  for (const node of nodes) {
    const id = await node.getAttribute('id');
    if (id.includes(itemName)) {
      const desc = node.element(by.css('.description'));
      await desc.click();
      return;
    }
  }
  throw new Error(`could not find item matching ${itemName} in hierarchy`);
}

export async function applyStateToHierarchyOptions(
  viewerSelector: string,
  shouldEnable: boolean,
) {
  const options: ElementFinder[] = await element.all(
    by.css(`${viewerSelector} hierarchy-view .view-controls .user-option`),
  );
  for (const option of options) {
    const isEnabled = !(await option.getAttribute('class')).includes(
      'not-enabled',
    );
    if (shouldEnable && !isEnabled) {
      await option.click();
    } else if (!shouldEnable && isEnabled) {
      await option.click();
    }
  }
}

export async function checkItemInPropertiesTree(
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
  throw new Error(`could not find item ${itemName} in properties tree`);
}

export async function checkRectLabel(viewer: string, expectedLabel: string) {
  const labels = await element.all(by.css(`${viewer} rects-view .rect-label`));

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

export async function checkScrollPresent(viewerSelector: string) {
  await browser.wait(
    async () => {
      const scrollIsPresent = await element(
        by.css(`${viewerSelector} .scroll`),
      ).isPresent();
      const placeholderPresent = await element(
        by.css(`${viewerSelector} .fetching-data`),
      ).isPresent();
      return scrollIsPresent && !placeholderPresent;
    },
    5000,
    'Fetching data timeout',
  );
}

export async function checkTotalScrollEntries(
  viewerSelector: string,
  numberOfEntries: number,
  scrollToBottom = false,
) {
  if (scrollToBottom) {
    const viewport = element(by.css(`${viewerSelector} .scroll`));
    let lastId: string | undefined;
    let lastScrollEntryItemId = await getLastScrollEntryItemId(viewerSelector);
    while (lastId !== lastScrollEntryItemId) {
      lastId = lastScrollEntryItemId;
      await viewport.sendKeys(protractor.Key.END);
      await new Promise<void>((resolve) => setTimeout(resolve, 500));
      lastScrollEntryItemId = await getLastScrollEntryItemId(viewerSelector);
    }
  }
  const lastId = await getLastScrollEntryItemId(viewerSelector);
  expect(lastId).toEqual(`${numberOfEntries - 1}`);
}

export async function getLastScrollEntryItemId(
  viewerSelector: string,
): Promise<string> {
  const entries = await element.all(by.css(`${viewerSelector} .scroll .entry`));
  return await entries[entries.length - 1].getAttribute('item-id');
}

export async function checkSelectFilter(
  viewerSelector: string,
  filterSelector: string,
  options: string[],
  expectedFilteredEntries: number,
  totalEntries: number,
) {
  await toggleSelectFilterOptions(viewerSelector, filterSelector, options);
  await checkTotalScrollEntries(viewerSelector, expectedFilteredEntries);

  await toggleSelectFilterOptions(viewerSelector, filterSelector, options);
  await checkTotalScrollEntries(viewerSelector, totalEntries, true);
}

export async function uploadFixture(...paths: string[]) {
  const inputFile = element(by.css('input[type="file"]'));

  // Uploading multiple files is not properly supported but
  // chrome handles file paths joined with new lines
  await inputFile.sendKeys(paths.map((it) => getFixturePath(it)).join('\n'));
}

export function getFixturePath(filename: string): string {
  if (path.isAbsolute(filename)) {
    return filename;
  }
  return path.join(getProjectRootPath(), 'src/test/fixtures', filename);
}

export function getProjectRootPath(): string {
  let root = __dirname;
  while (path.basename(root) !== 'winscope') {
    root = path.dirname(root);
  }
  return root;
}

async function checkHasLoadedTracesFromBugReport() {
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
  expect(text).not.toContain('IME system_server');
  expect(text).not.toContain('IME Clients');
  expect(text).not.toContain('wm_log.winscope');
  expect(text).not.toContain('ime_trace_service.winscope');
  expect(text).not.toContain('ime_trace_managerservice.winscope');
  expect(text).not.toContain('wm_trace.winscope');
  expect(text).not.toContain('ime_trace_clients.winscope');
}

async function checkEmitsUnsupportedFileFormatMessages() {
  const text = await element(by.css('snack-bar')).getText();
  expect(text).toContain('unsupported format');
}

async function checkEmitsOldDataMessages() {
  const text = await element(by.css('snack-bar')).getText();
  expect(text).toContain('discarded because data is old');
}

async function toggleSelectFilterOptions(
  viewerSelector: string,
  filterSelector: string,
  options: string[],
) {
  await element(
    by.css(`${viewerSelector} .headers ${filterSelector} .mat-select-trigger`),
  ).click();
  const optionElements: ElementFinder[] = await element.all(
    by.css('.mat-select-panel .option'),
  );
  for (const optionEl of optionElements) {
    const optionText = (await optionEl.getText()).trim();
    if (options.some((option) => optionText === option)) {
      await optionEl.click();
      options = options.filter((option) => option !== optionText);
      if (options.length === 0) {
        break;
      }
    }
  }
  const backdrop = await element(
    by.css('.cdk-overlay-backdrop'),
  ).getWebElement();
  await browser.actions().mouseMove(backdrop, {x: 0, y: 0}).click().perform();
}
