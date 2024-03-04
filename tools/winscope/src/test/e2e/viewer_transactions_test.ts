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

describe('Viewer Transactions', () => {
  const viewerSelector = 'viewer-transactions';

  beforeEach(async () => {
    browser.manage().timeouts().implicitlyWait(1000);
    await E2eTestUtils.checkServerIsUp('Winscope', E2eTestUtils.WINSCOPE_URL);
    await browser.get(E2eTestUtils.WINSCOPE_URL);
  });

  it('processes trace from zip and navigates correctly', async () => {
    await E2eTestUtils.loadTraceAndCheckViewer(
      'traces/deployment_full_trace_phone.zip',
      'Transactions',
      viewerSelector,
    );
    await checkNumberOfEntries(44);
    await E2eTestUtils.checkTimelineTraceSelector({
      icon: 'show_chart',
      color: 'rgba(91, 185, 116, 1)',
    });
    await E2eTestUtils.checkFinalRealTimestamp('2022-11-21T18:05:19.592049232');
    await E2eTestUtils.checkInitialRealTimestamp(
      '2022-11-21T11:36:19.513353722',
    );

    await E2eTestUtils.changeRealTimestampInWinscope(
      '2022-11-21T18:05:17.505508034',
    );
    await E2eTestUtils.checkWinscopeRealTimestamp(
      '2022-11-21T18:05:17.505508034',
    );
    await checkNumberOfEntries(49);
    await checkSelectedEntry();
  });

  async function checkSelectedEntry() {
    const selectedEntry = element(by.css(`${viewerSelector} .scroll .current`));
    expect(await selectedEntry.isPresent()).toBeTruthy();

    const transactionId = selectedEntry.element(by.css('.transaction-id'));
    expect(await transactionId.getText()).toEqual('7975754272149');

    const vsyncId = selectedEntry.element(by.css('.vsyncid'));
    expect(await vsyncId.getText()).toEqual('93389');

    const pid = selectedEntry.element(by.css('.pid'));
    expect(await pid.getText()).toEqual('1857');

    const uid = selectedEntry.element(by.css('.uid'));
    expect(await uid.getText()).toEqual('1000');

    const type = selectedEntry.element(by.css('.type'));
    expect(await type.getText()).toEqual('LAYER_CHANGED');

    const layerOrDisplayId = selectedEntry.element(
      by.css('.layer-or-display-id'),
    );
    expect(await layerOrDisplayId.getText()).toEqual('798');

    const whatString =
      'eLayerChanged | eAlphaChanged | eFlagsChanged | eReparent | eColorChanged | eHasListenerCallbacksChanged';
    const what = selectedEntry.element(by.css('.what'));
    expect(await what.getText()).toEqual(whatString);

    await E2eTestUtils.checkItemInPropertiesTree(
      viewerSelector,
      'what',
      'what:\n' + whatString,
    );
    await E2eTestUtils.checkItemInPropertiesTree(
      viewerSelector,
      'color',
      'color:\n(0.106, 0.106, 0.106)',
    );
  }

  async function checkNumberOfEntries(numberOfEntries: number) {
    const entries: ElementFinder[] = await element.all(
      by.css(`${viewerSelector} .scroll .entry`),
    );
    expect(entries.length).toEqual(numberOfEntries);
  }
});
