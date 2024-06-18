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

describe('Viewer Input Method Service', () => {
  const viewerSelector = 'viewer-input-method';

  beforeEach(async () => {
    browser.manage().timeouts().implicitlyWait(1000);
    await E2eTestUtils.checkServerIsUp('Winscope', E2eTestUtils.WINSCOPE_URL);
    await browser.get(E2eTestUtils.WINSCOPE_URL);
  });

  it('processes trace from zip and navigates correctly', async () => {
    await E2eTestUtils.loadTraceAndCheckViewer(
      'traces/deployment_full_trace_phone.zip',
      'IME Service',
      viewerSelector,
    );
    await E2eTestUtils.checkTimelineTraceSelector({
      icon: 'keyboard_alt',
      color: 'rgba(242, 153, 0, 1)',
    });
    await E2eTestUtils.checkInitialRealTimestamp('2022-11-21, 18:05:12.497');
    await E2eTestUtils.checkFinalRealTimestamp('2022-11-21, 18:05:18.061');

    await E2eTestUtils.changeRealTimestampInWinscope(
      '2022-11-21, 18:05:14.720',
    );
    await E2eTestUtils.checkWinscopeRealTimestamp('18:05:14.720');

    await E2eTestUtils.applyStateToHierarchyOptions(viewerSelector, true);
    await checkHierarchy();

    await E2eTestUtils.selectItemInHierarchy(
      viewerSelector,
      'com.google.android.apps.messaging/com.google.android.apps.messaging.ui.search.ZeroStateSearchActivity#786',
    );
    await checkProperties();
  });

  async function checkHierarchy() {
    const nodes = await element.all(
      by.css(`${viewerSelector} hierarchy-view .node`),
    );
    expect(nodes.length).toEqual(4);
    expect(await nodes[0].getText()).toContain(
      'InputMethodService - 2022-11-21, 18:05:14.720 - InputMethodService#applyVisibilityInInsetsConsumerIfNecessary',
    );
    expect(await nodes[1].getText()).toContain('253 - SfSubtree - Task=8#253');
    expect(await nodes[2].getText()).toContain(
      '778 - Letterbox - left#778 GPU V',
    );
    expect(await nodes[3].getText()).toContain(
      '786 - com.google.(...).ZeroStateSearchActivity#786 GPU V',
    );
  }

  async function checkProperties() {
    await E2eTestUtils.checkItemInPropertiesTree(
      viewerSelector,
      'damageRegion',
      'damageRegion:\nSkRegion((398, 42, 615, 1596))',
    );

    await E2eTestUtils.checkItemInPropertiesTree(
      viewerSelector,
      'color',
      'color:\n{empty}, alpha: 0.589',
    );

    await E2eTestUtils.checkItemInPropertiesTree(
      viewerSelector,
      'destinationFrame',
      'destinationFrame:\n(0, 0) - (2204, 1080)',
    );

    await E2eTestUtils.checkItemInPropertiesTree(
      viewerSelector,
      'layoutParamsFlags',
      'layoutParamsFlags:\nFLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS | FLAG_HARDWARE_ACCELERATED | FLAG_SPLIT_TOUCH | FLAG_LAYOUT_INSET_DECOR | FLAG_LAYOUT_IN_SCREEN | FLAG_NOT_TOUCH_MODAL',
    );
  }
});
