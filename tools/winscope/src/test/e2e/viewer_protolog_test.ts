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
import {
  checkFinalRealTimestamp,
  checkInitialRealTimestamp,
  checkScrollPresent,
  checkSelectFilter,
  checkTimelineTraceSelector,
  checkTotalScrollEntries,
  loadTraceAndCheckViewer,
  setTimeouts,
  updateInputField,
  WINSCOPE_URL,
} from './utils';

describe('Viewer Protolog', () => {
  const viewerSelector = 'viewer-protolog';
  const totalEntries = 7295;

  beforeEach(async () => {
    await setTimeouts(1000);
    await browser.get(WINSCOPE_URL);
  });

  it('processes trace from zip and navigates correctly', async () => {
    await loadTraceAndCheckViewer(
      'archives/deployment_full_trace_phone.zip',
      'ProtoLog',
      viewerSelector,
    );
    await checkScrollPresent(viewerSelector);
    await checkTotalScrollEntries(viewerSelector, totalEntries, true);
    await checkTimelineTraceSelector({
      icon: 'notes',
      color: 'rgba(52, 168, 83, 1)',
    });
    await checkFinalRealTimestamp('2022-11-21, 18:05:18.259');
    await checkInitialRealTimestamp('2022-11-21, 18:05:09.777');

    await checkSelectFilter(
      viewerSelector,
      '.source-file',
      ['com/android/server/wm/ActivityStarter.java'],
      1,
      totalEntries,
    );

    await checkSelectFilter(
      viewerSelector,
      '.source-file',
      [
        'com/android/server/wm/ActivityStarter.java',
        'com/android/server/wm/ActivityClientController.java',
      ],
      4,
      totalEntries,
    );

    await filterByText('FREEZE');
    await checkTotalScrollEntries(viewerSelector, 4);
  });

  async function filterByText(filterString: string) {
    await updateInputField(
      `${viewerSelector} .headers .text`,
      'Search text',
      filterString,
    );
  }
});
