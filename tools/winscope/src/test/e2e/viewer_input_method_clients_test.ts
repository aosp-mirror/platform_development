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

describe('Viewer Input Method Clients', () => {
  const viewerSelector = 'viewer-input-method';

  beforeEach(async () => {
    await E2eTestUtils.beforeEach(1000);
    await browser.get(E2eTestUtils.WINSCOPE_URL);
  });

  it('processes trace from zip and navigates correctly', async () => {
    await E2eTestUtils.loadTraceAndCheckViewer(
      'traces/deployment_full_trace_phone.zip',
      'IME Clients',
      viewerSelector,
    );
    await E2eTestUtils.checkTimelineTraceSelector({
      icon: 'keyboard_alt',
      color: 'rgba(250, 144, 62, 1)',
    });
    await E2eTestUtils.checkInitialRealTimestamp('2022-11-21, 18:05:11.145');
    await E2eTestUtils.checkFinalRealTimestamp('2022-11-21, 18:05:18.245');

    await E2eTestUtils.changeRealTimestampInWinscope(
      '2022-11-21, 18:05:14.969',
    );
    await E2eTestUtils.checkWinscopeRealTimestamp('18:05:14.969');

    await checkAdditionalProperties();
    await clickWmState();
    await checkWmStateProperties();
    await clickImeContainer();
    await checkImeContainerProperties();
    await clickInputMethodSurface();
    await checkInputMethodSurfaceProperties();

    await E2eTestUtils.applyStateToHierarchyOptions(viewerSelector, true);
    await checkHierarchy();
    await E2eTestUtils.selectItemInHierarchy(viewerSelector, 'InputMethod#765');
    await checkInputMethodLayerProperties();
  });

  async function checkHierarchy() {
    const nodes = await element.all(
      by.css(`${viewerSelector} hierarchy-view .node`),
    );
    expect(nodes.length).toEqual(5);
    expect(await nodes[0].getText()).toContain(
      'InputMethodClients - 2022-11-21, 18:05:14.969 - InsetsSourceConsumer#notifyAnimationFinished',
    );
    expect(await nodes[1].getText()).toContain('253 - SfSubtree - Task=8#253');
    expect(await nodes[2].getText()).toContain(
      '778 - Letterbox - left#778 HWC V',
    );
    expect(await nodes[3].getText()).toContain(
      '786 - com.google.(...).ZeroStateSearchActivity#786 HWC V',
    );
    expect(await nodes[4].getText()).toContain('765 - InputMethod#765 HWC V');
  }

  async function checkInputMethodLayerProperties() {
    await E2eTestUtils.checkItemInPropertiesTree(
      viewerSelector,
      'activeBuffer',
      'activeBuffer:\nw: 1006, h: 2204, stride: 268437760, format: 1',
    );

    await E2eTestUtils.checkItemInPropertiesTree(
      viewerSelector,
      'bufferTransform',
      'bufferTransform:\nROT_270',
    );

    await E2eTestUtils.checkItemInPropertiesTree(
      viewerSelector,
      'hwcCompositionType',
      'hwcCompositionType:\nDEVICE',
    );

    await E2eTestUtils.checkItemInPropertiesTree(
      viewerSelector,
      'bounds',
      'bounds:\n(0, 0) - (2204, 1006)',
    );
  }

  async function checkAdditionalProperties() {
    const additionalProperties = element(by.css('ime-additional-properties'));
    expect(await additionalProperties.isPresent()).toBeTruthy();

    const sfState = additionalProperties.element(by.css('.sf-state'));
    const sfStateTimestamp = await sfState.getText();
    expect(sfStateTimestamp).toEqual('2022-11-21, 18:05:14.902');

    const wmState = additionalProperties.element(by.css('.wm-state'));
    const wmStateTimestamp = await wmState.getText();
    expect(wmStateTimestamp).toEqual('2022-11-21, 18:05:14.896');

    const focusSection = additionalProperties.element(by.css('.focus'));
    const focusSectionText = await focusSection.getText();
    expect(focusSectionText).toContain(
      'Focused App: com.google.android.apps.messaging/.ui.search.ZeroStateSearchActivity',
    );
    expect(focusSectionText).toContain(
      'Focused Activity: {8170434 com.google.android.apps.messaging/.ui.search.ZeroStateSearchActivity} state=RESUMED visible=true',
    );
    expect(focusSectionText).toContain(
      'Focused Window: {25d7778 com.google.android.apps.messaging/com.google.android.apps.messaging.ui.search.ZeroStateSearchActivity EXITING} type=TYPE_BASE_APPLICATION cf={empty} pf=(136, 0) - (2340, 1080)',
    );
    expect(focusSectionText).toContain(
      'Focused Window Color: {empty}, alpha: 1',
    );
    expect(focusSectionText).toContain(
      'Input Control Target Frame:\nLeft\nTop\nRight\nBottom\n136\n0\n2340\n1080',
    );

    const visibilitySection = additionalProperties.element(
      by.css('.visibility'),
    );
    const visibilitySectionText = await visibilitySection.getText();
    expect(visibilitySectionText).toEqual(
      'Visibility\nInputMethod Window: true\nInputMethod Surface: false',
    );

    const imeContainer = additionalProperties.element(by.css('.ime-container'));
    const imeContainerText = await imeContainer.getText();
    expect(imeContainerText).toEqual(
      'Ime Container\nZOrderRelativeOfId: 780\nZ: 1',
    );

    const inputMethodSurface = additionalProperties.element(
      by.css('.input-method-surface'),
    );
    const inputMethodSurfaceText = await inputMethodSurface.getText();
    expect(inputMethodSurfaceText).toContain(
      'Screen Bounds:\nLeft\nTop\nRight\nBottom\n-10800\n-23400\n10800\n23400',
    );
    expect(inputMethodSurfaceText).toContain(
      'Rect:\nLeft\nTop\nRight\nBottom\n-10936\n-23548\n10664\n23252',
    );
  }

  async function clickWmState() {
    const wmStateButton = element(
      by.css('ime-additional-properties .wm-state-button'),
    );
    await wmStateButton.click();
  }

  async function checkWmStateProperties() {
    await E2eTestUtils.checkItemInPropertiesTree(
      viewerSelector,
      'screenState',
      'screenState:\nSCREEN_STATE_ON',
    );

    await E2eTestUtils.checkItemInPropertiesTree(
      viewerSelector,
      'windowFramesValid',
      'windowFramesValid:\ntrue',
    );
  }

  async function clickImeContainer() {
    const imeStateButton = element(
      by.css('ime-additional-properties .ime-container-button'),
    );
    await imeStateButton.click();
  }

  async function checkImeContainerProperties() {
    await E2eTestUtils.checkItemInPropertiesTree(
      viewerSelector,
      'id',
      'id:\n12',
    );

    await E2eTestUtils.checkItemInPropertiesTree(
      viewerSelector,
      'bounds',
      'bounds:\n(-10800, -23400) - (10800, 23400)',
    );
  }

  async function clickInputMethodSurface() {
    const imeStateButton = element(
      by.css('ime-additional-properties .input-method-surface-button'),
    );
    await imeStateButton.click();
  }

  async function checkInputMethodSurfaceProperties() {
    await E2eTestUtils.checkItemInPropertiesTree(
      viewerSelector,
      'id',
      'id:\n795',
    );

    await E2eTestUtils.checkItemInPropertiesTree(
      viewerSelector,
      'position',
      'position:\nx: 136, y: 148',
    );

    await E2eTestUtils.checkItemInPropertiesTree(
      viewerSelector,
      'transform',
      'transform:\nTRANSLATE',
    );
  }
});
