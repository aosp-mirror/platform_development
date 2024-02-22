/*
 * Copyright (C) 2024 The Android Open Source Project
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
import {E2eTestUtils} from '../utils';

describe('Deployment - Viewer Protolog', () => {
  const viewerSelector = 'viewer-protolog';

  beforeEach(async () => {
    browser.manage().timeouts().implicitlyWait(1000);
    await E2eTestUtils.checkServerIsUp('Winscope', E2eTestUtils.WINSCOPE_URL);
    await browser.get(E2eTestUtils.WINSCOPE_URL);
  });

  it('processes trace and navigates correctly', async () => {
    await loadTraces();
    await E2eTestUtils.checkTimelineTraceSelector({
      icon: 'notes',
      color: 'rgba(64, 165, 138, 1)',
    });
    await E2eTestUtils.checkInitialRealTimestamp(
      '2022-11-21T18:05:09.777144978',
    );
    await E2eTestUtils.checkFinalRealTimestamp('2022-11-21T18:05:18.259191031');

    await checkNumberOfEntries(40);
    await filterByText('FREEZE');
    await checkNumberOfEntries(4);
  });

  async function loadTraces() {
    await E2eTestUtils.loadTrace(
      'traces/deployment_full_trace_phone.zip',
      'ProtoLog',
      viewerSelector,
    );
  }

  async function filterByText(filterString: string) {
    await E2eTestUtils.updateInputField(
      `${viewerSelector} .filters .text`,
      'protologTextInput',
      filterString,
    );
  }

  async function checkNumberOfEntries(numberOfEntries: number) {
    const entries: ElementFinder[] = await element.all(
      by.css(`${viewerSelector} .scroll-messages .message`),
    );
    expect(entries.length).toEqual(numberOfEntries);
  }
});
