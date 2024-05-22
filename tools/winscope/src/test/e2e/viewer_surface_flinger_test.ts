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

describe('Viewer Surface Flinger', () => {
  const viewerSelector = 'viewer-surface-flinger';

  beforeEach(async () => {
    browser.manage().timeouts().implicitlyWait(1000);
    await E2eTestUtils.checkServerIsUp('Winscope', E2eTestUtils.WINSCOPE_URL);
    await browser.get(E2eTestUtils.WINSCOPE_URL);
  });

  it('processes trace from zip and navigates correctly', async () => {
    await loadTraces();
    await E2eTestUtils.checkTimelineTraceSelector({
      icon: 'layers',
      color: 'rgba(78, 205, 230, 1)',
    });
    await E2eTestUtils.checkInitialRealTimestamp('2022-11-21, 18:05:09.780');
    await E2eTestUtils.checkFinalRealTimestamp('2022-11-21, 18:05:18.607');

    await E2eTestUtils.changeRealTimestampInWinscope(
      '2022-11-21, 18:05:11.314',
    );
    await E2eTestUtils.checkWinscopeRealTimestamp('18:05:11.314');
    await E2eTestUtils.filterHierarchy(
      viewerSelector,
      'ConversationListActivity#632',
    );
    await E2eTestUtils.selectItemInHierarchy(
      viewerSelector,
      'com.google.android.apps.messaging/com.google.android.apps.messaging.ui.ConversationListActivity#632',
    );
    await checkLayerProperties();
  });

  async function loadTraces() {
    await E2eTestUtils.loadTraceAndCheckViewer(
      'traces/deployment_full_trace_phone.zip',
      'Surface Flinger',
      viewerSelector,
    );
  }

  async function checkLayerProperties() {
    const curatedProperties = element(
      by.css('surface-flinger-property-groups'),
    );
    const isPresent = await curatedProperties.isPresent();
    expect(isPresent).toBeTruthy();

    const finalBounds = curatedProperties.element(by.css('.final-bounds'));
    const finalBoundsText = await finalBounds.getText();
    expect(finalBoundsText).toEqual(
      'Final Bounds: (162.418, 346.428) - (917.582, 1982.619)',
    );

    const transform = curatedProperties.element(
      by.css('.geometry .left-column transform-matrix'),
    );
    const transformText = await transform.getText();
    expect(transformText).toEqual(
      '0.699\n0\n162.418\n0\n0.699\n346.428\n0\n0\n1',
    );

    const flags = curatedProperties.element(by.css('.flags'));
    const flagsText = await flags.getText();
    expect(flagsText).toEqual('Flags: OPAQUE|ENABLE_BACKPRESSURE (0x102)');

    const destinationFrame = curatedProperties.element(by.css('.dest-frame'));
    const destinationFrameText = await destinationFrame.getText();
    expect(destinationFrameText).toEqual(
      'Destination Frame: (0, 0) - (1080, 2340)',
    );
  }
});
