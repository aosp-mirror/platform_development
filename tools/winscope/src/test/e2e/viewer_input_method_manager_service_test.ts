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

describe('Viewer Input Method Manager Service', () => {
  const viewerSelector = 'viewer-input-method';

  beforeEach(async () => {
    browser.manage().timeouts().implicitlyWait(1000);
    await E2eTestUtils.checkServerIsUp('Winscope', E2eTestUtils.WINSCOPE_URL);
    await browser.get(E2eTestUtils.WINSCOPE_URL);
  });

  it('processes trace from zip and navigates correctly', async () => {
    await E2eTestUtils.loadTraceAndCheckViewer(
      'traces/deployment_full_trace_phone.zip',
      'Input Method Manager Service',
      viewerSelector,
    );
    await E2eTestUtils.checkTimelineTraceSelector({
      icon: 'keyboard_alt',
      color: 'rgba(217, 48, 37, 1)',
    });
    await E2eTestUtils.checkInitialRealTimestamp(
      '2022-11-21T18:05:11.145417632',
    );
    await E2eTestUtils.checkFinalRealTimestamp('2022-11-21T18:05:18.081981604');

    await E2eTestUtils.changeRealTimestampInWinscope(
      '2022-11-21T18:05:14.713780435',
    );
    await E2eTestUtils.checkWinscopeRealTimestamp(
      '2022-11-21T18:05:14.713780435',
    );

    await checkAdditionalProperties();
    await clickWmState();
    await checkWmStateProperties();

    await E2eTestUtils.applyStateToHierarchyCheckboxes(viewerSelector, false);
    await E2eTestUtils.selectItemInHierarchy(
      viewerSelector,
      'inputMethodManagerService',
    );
    await checkManagerServiceProperties();
  });

  async function checkAdditionalProperties() {
    const additionalProperties = element(by.css('ime-additional-properties'));
    expect(await additionalProperties.isPresent()).toBeTruthy();

    const wmState = additionalProperties.element(
      by.css('.ime-manager-service .wm-state'),
    );
    const wmStateTimestamp = await wmState.getText();
    expect(wmStateTimestamp).toEqual('2022-11-21T18:05:14.713503732');

    const insetsSourceProvider = additionalProperties.element(
      by.css('.insets-source-provider'),
    );
    const insetsSourceProviderText = await insetsSourceProvider.getText();
    expect(insetsSourceProviderText).toEqual(
      'IME Insets Source Provider\nSource Frame:\nnull\nSource Visible: null\nSource Visible Frame:\nnull\nPosition: x: 136, y: 74\nIsLeashReadyForDispatching: true\nControllable: true',
    );

    const target =
      'com.google.android.apps.messaging/com.google.android.apps.messaging.ui.search.ZeroStateSearchActivity';
    const controlTargetText = await additionalProperties
      .element(by.css('.ime-control-target'))
      .getText();
    expect(controlTargetText).toContain(target);

    const inputTargetText = await additionalProperties
      .element(by.css('.ime-input-target'))
      .getText();
    expect(inputTargetText).toContain(target);

    const layeringTargetText = await additionalProperties
      .element(by.css('.ime-layering-target'))
      .getText();
    expect(layeringTargetText).toContain(target);
  }

  async function clickWmState() {
    const wmStateButton = element(
      by.css('ime-additional-properties .ime-manager-service button'),
    );
    await wmStateButton.click();
  }

  async function checkWmStateProperties() {
    await E2eTestUtils.checkItemInPropertiesTree(
      viewerSelector,
      'interactiveState',
      'interactiveState:\nINTERACTIVE_STATE_AWAKE',
    );

    await E2eTestUtils.checkItemInPropertiesTree(
      viewerSelector,
      'windowFramesValid',
      'windowFramesValid:\ntrue',
    );
  }

  async function checkManagerServiceProperties() {
    await E2eTestUtils.checkItemInPropertiesTree(
      viewerSelector,
      'fieldId',
      'fieldId:\n2131430027',
    );

    await E2eTestUtils.checkItemInPropertiesTree(
      viewerSelector,
      'curToken',
      'curToken:\nandroid.os.Binder@a75e797',
    );

    await E2eTestUtils.checkItemInPropertiesTree(
      viewerSelector,
      'curFocusedWindowSoftInputMode',
      'curFocusedWindowSoftInputMode:\nSTATE_UNSPECIFIED|ADJUST_RESIZE',
    );

    await E2eTestUtils.checkItemInPropertiesTree(
      viewerSelector,
      'inputShown',
      'inputShown:\ntrue',
    );
  }
});
