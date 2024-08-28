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
import {browser, by, element} from 'protractor';
import {E2eTestUtils} from './utils';

describe('Viewer Screenshot', () => {
  const viewerSelector = 'viewer-media-based';

  beforeEach(async () => {
    browser.manage().timeouts().implicitlyWait(1000);
    await E2eTestUtils.checkServerIsUp('Winscope', E2eTestUtils.WINSCOPE_URL);
    await browser.get(E2eTestUtils.WINSCOPE_URL);
  });

  it('processes file and renders view', async () => {
    await E2eTestUtils.uploadFixture('traces/screenshot.png');
    await E2eTestUtils.closeSnackBar();
    await E2eTestUtils.clickViewTracesButton();

    const viewer = element(by.css(viewerSelector));
    expect(await viewer.isPresent()).toBeTruthy();

    const img = element(by.css(`${viewerSelector} img`));
    expect(await img.isPresent()).toBeTruthy();
    expect(await img.getAttribute('src')).toContain('blob:');
  });

  it('processes files and renders view with multiple screenshots', async () => {
    await E2eTestUtils.uploadFixture(
      'traces/screenshot.png',
      'traces/screenshot_2.png',
    );
    await E2eTestUtils.closeSnackBar();
    await E2eTestUtils.clickViewTracesButton();

    const viewer = element(by.css(viewerSelector));
    expect(await viewer.isPresent()).toBeTruthy();

    const img = element(by.css(`${viewerSelector} img`));
    expect(await img.isPresent()).toBeTruthy();
    const src = await img.getAttribute('src');
    expect(src).toContain('blob:');

    const overlayTitle = element(by.css(`${viewerSelector} .overlay-title`));
    expect(await overlayTitle.getText()).toEqual('screenshot');

    const selectTrigger = element(
      by.css(`${viewerSelector} .mat-select-trigger`),
    );
    expect(await selectTrigger.isPresent()).toBeTruthy();
    await selectTrigger.click();
    const option2 = element.all(by.css('.mat-option')).last();
    await option2.click();

    expect(await img.isPresent()).toBeTruthy();
    const newSrc = await img.getAttribute('src');
    expect(newSrc).toContain('blob:');
    expect(newSrc).not.toEqual(src);
    expect(await overlayTitle.getText()).toEqual('screenshot_2');
  });
});
