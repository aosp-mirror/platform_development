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

describe('Viewer ProtoLog', () => {
  beforeAll(async () => {
    browser.manage().timeouts().implicitlyWait(1000);
    browser.get('file://' + E2eTestUtils.getProductionIndexHtmlPath());
  }),
    it('processes trace and renders view', async () => {
      await E2eTestUtils.uploadFixture('traces/elapsed_and_real_timestamp/ProtoLog.pb');
      await E2eTestUtils.closeSnackBarIfNeeded();
      await E2eTestUtils.clickViewTracesButton();

      const isViewerRendered = await element(by.css('viewer-protolog')).isPresent();
      expect(isViewerRendered).toBeTruthy();

      const isFirstMessageRendered = await element(
        by.css('viewer-protolog .scroll-messages .message')
      ).isPresent();
      expect(isFirstMessageRendered).toBeTruthy();
    });
});
