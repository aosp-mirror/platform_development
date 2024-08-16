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

import {browser, ElementFinder} from 'protractor';
import {E2eTestUtils} from './utils';

describe('Viewer Protolog', () => {
  const viewerSelector = 'viewer-protolog';
  const totalEntries = 7295;
  const scrollToTotalBottomOffset = 700000;

  beforeEach(async () => {
    browser.manage().timeouts().implicitlyWait(1000);
    await E2eTestUtils.checkServerIsUp('Winscope', E2eTestUtils.WINSCOPE_URL);
    await browser.get(E2eTestUtils.WINSCOPE_URL);
  });

  it('processes trace from zip and navigates correctly', async () => {
    await E2eTestUtils.loadTraceAndCheckViewer(
      'traces/deployment_full_trace_phone.zip',
      'ProtoLog',
      viewerSelector,
    );
    await E2eTestUtils.checkTotalScrollEntries(
      viewerSelector,
      scrollViewport,
      totalEntries,
      scrollToTotalBottomOffset,
    );
    await E2eTestUtils.checkTimelineTraceSelector({
      icon: 'notes',
      color: 'rgba(64, 165, 138, 1)',
    });
    await E2eTestUtils.checkFinalRealTimestamp('2022-11-21, 18:05:18.259');
    await E2eTestUtils.checkInitialRealTimestamp('2022-11-21, 18:05:09.777');

    await checkSelectFilter(
      '.source-file',
      ['com/android/server/wm/ActivityStarter.java'],
      1,
    );
    await checkSelectFilter(
      '.source-file',
      [
        'com/android/server/wm/ActivityStarter.java',
        'com/android/server/wm/ActivityClientController.java',
      ],
      4,
    );

    await E2eTestUtils.checkTotalScrollEntries(
      viewerSelector,
      scrollViewport,
      totalEntries,
      scrollToTotalBottomOffset,
    );
    await filterByText('FREEZE');
    await E2eTestUtils.checkTotalScrollEntries(
      viewerSelector,
      scrollViewport,
      4,
    );
  });

  async function checkSelectFilter(
    filterSelector: string,
    options: string[],
    expectedFilteredEntries: number,
  ) {
    await E2eTestUtils.toggleSelectFilterOptions(
      viewerSelector,
      filterSelector,
      options,
    );
    await E2eTestUtils.checkTotalScrollEntries(
      viewerSelector,
      scrollViewport,
      expectedFilteredEntries,
    );

    await E2eTestUtils.toggleSelectFilterOptions(
      viewerSelector,
      filterSelector,
      options,
    );
    await E2eTestUtils.checkTotalScrollEntries(
      viewerSelector,
      scrollViewport,
      totalEntries,
      scrollToTotalBottomOffset,
    );
  }

  async function filterByText(filterString: string) {
    await E2eTestUtils.updateInputField(
      `${viewerSelector} .filters .text`,
      'Search text',
      filterString,
    );
  }

  function scrollViewport(
    viewportEl: ElementFinder,
    offset: number,
    done: () => void,
  ) {
    viewportEl['scrollTop'] = offset;
    window.requestAnimationFrame(() => done());
  }
});
