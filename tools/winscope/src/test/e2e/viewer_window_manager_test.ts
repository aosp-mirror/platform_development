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

import {browser} from 'protractor';
import {E2eTestUtils} from './utils';

describe('Viewer Window Manager', () => {
  const viewerSelector = 'viewer-window-manager';

  beforeEach(async () => {
    browser.manage().timeouts().implicitlyWait(1000);
    await E2eTestUtils.checkServerIsUp('Winscope', E2eTestUtils.WINSCOPE_URL);
    await browser.get(E2eTestUtils.WINSCOPE_URL);
  });

  it('processes trace from zip and navigates correctly', async () => {
    await E2eTestUtils.loadTraceAndCheckViewer(
      'traces/deployment_full_trace_phone.zip',
      'Window Manager',
      viewerSelector,
    );
    await E2eTestUtils.checkTimelineTraceSelector({
      icon: 'web',
      color: 'rgba(175, 92, 247, 1)',
    });
    await E2eTestUtils.checkInitialRealTimestamp('2022-11-21, 18:05:09.753');
    await E2eTestUtils.checkFinalRealTimestamp('2022-11-21, 18:05:18.269');

    await E2eTestUtils.changeRealTimestampInWinscope(
      '2022-11-21, 18:05:09.753',
    );
    await E2eTestUtils.checkWinscopeRealTimestamp('18:05:09.753');
    await E2eTestUtils.selectItemInHierarchy(viewerSelector, 'root');
    await checkRootProperties();

    await E2eTestUtils.changeRealTimestampInWinscope(
      '2022-11-21, 18:05:14.544',
    );
    await E2eTestUtils.checkWinscopeRealTimestamp('18:05:14.544');
    await E2eTestUtils.filterHierarchy(viewerSelector, 'InputMethod');
    await E2eTestUtils.selectItemInHierarchy(viewerSelector, 'InputMethod');
    await checkInputMethodWindowProperties();
  });

  async function checkRootProperties() {
    await E2eTestUtils.checkItemInPropertiesTree(
      viewerSelector,
      'focusedApp',
      'focusedApp:\ncom.google.android.apps.messaging/.ui.ConversationListActivity',
    );
    await E2eTestUtils.checkRectLabel(
      viewerSelector,
      'com.google.android.apps.messaging/com.google.android.apps.messaging.ui.ConversationListActivity',
    );
  }

  async function checkInputMethodWindowProperties() {
    await E2eTestUtils.checkItemInPropertiesTree(
      viewerSelector,
      'fitInsetsTypes',
      'fitInsetsTypes:\nNAVIGATION_BARS | STATUS_BARS',
    );

    await E2eTestUtils.checkItemInPropertiesTree(
      viewerSelector,
      'flags',
      'flags:\nFLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS | FLAG_HARDWARE_ACCELERATED | FLAG_SPLIT_TOUCH | FLAG_LAYOUT_IN_SCREEN | FLAG_NOT_FOCUSABLE',
    );

    await E2eTestUtils.checkItemInPropertiesTree(
      viewerSelector,
      'compatFrame',
      'compatFrame:\n(136, 74) - (2340, 1080)',
    );
  }
});
