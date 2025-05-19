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
import {
  applyStateToHierarchyOptions,
  changeRealTimestampInWinscope,
  checkFinalRealTimestamp,
  checkInitialRealTimestamp,
  checkItemInPropertiesTree,
  checkTimelineTraceSelector,
  checkWinscopeRealTimestamp,
  loadTraceAndCheckViewer,
  selectItemInHierarchy,
  setTimeouts,
  WINSCOPE_URL,
} from './utils';

describe('Viewer Input Method Service', () => {
  const viewerSelector = 'viewer-input-method';

  beforeEach(async () => {
    await setTimeouts(1000);
    await browser.get(WINSCOPE_URL);
  });

  it('processes trace from zip and navigates correctly', async () => {
    await loadTraceAndCheckViewer(
      'archives/deployment_full_trace_phone.zip',
      'IME Service',
      viewerSelector,
    );
    await checkTimelineTraceSelector({
      icon: 'keyboard_alt',
      color: 'rgba(255, 194, 75, 1)',
    });
    await checkInitialRealTimestamp('2022-11-21, 18:05:12.497');
    await checkFinalRealTimestamp('2022-11-21, 18:05:18.061');

    await changeRealTimestampInWinscope('2022-11-21, 18:05:14.720');
    await checkWinscopeRealTimestamp('18:05:14.720');

    await applyStateToHierarchyOptions(viewerSelector, true);
    await checkHierarchy();

    await selectItemInHierarchy(
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
    await checkItemInPropertiesTree(
      viewerSelector,
      'damageRegion',
      'damageRegion:\nSkRegion((398, 42, 615, 1596))',
    );

    await checkItemInPropertiesTree(
      viewerSelector,
      'color',
      'color:\n{empty}, alpha: 0.589',
    );

    await checkItemInPropertiesTree(
      viewerSelector,
      'destinationFrame',
      'destinationFrame:\n(0, 0) - (2204, 1080)',
    );

    await checkItemInPropertiesTree(
      viewerSelector,
      'layoutParamsFlags',
      'layoutParamsFlags:\nFLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS | FLAG_HARDWARE_ACCELERATED | FLAG_SPLIT_TOUCH | FLAG_LAYOUT_INSET_DECOR | FLAG_LAYOUT_IN_SCREEN | FLAG_NOT_TOUCH_MODAL',
    );
  }
});
