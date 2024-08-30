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

describe('Viewer ScreenRecording', () => {
  const viewerSelector = 'viewer-media-based';

  beforeEach(async () => {
    await E2eTestUtils.beforeEach(1000);
    await browser.get(E2eTestUtils.WINSCOPE_URL);
  });

  it('processes trace and renders view', async () => {
    await E2eTestUtils.uploadFixture(
      'traces/elapsed_and_real_timestamp/screen_recording_metadata_v2.mp4',
    );
    await E2eTestUtils.closeSnackBar();
    await E2eTestUtils.clickViewTracesButton();

    const viewer = element(by.css(viewerSelector));
    expect(await viewer.isPresent()).toBeTruthy();

    const video = element(by.css(`${viewerSelector} video`));
    expect(await video.isPresent()).toBeTruthy();
    expect(await video.getAttribute('src')).toContain('blob:');
    expect(await video.getAttribute('currentTime')).toBeCloseTo(0, 0.001);
  });

  it('processes files and renders view with multiple recordings', async () => {
    await E2eTestUtils.uploadFixture(
      'traces/elapsed_and_real_timestamp/screen_recording_metadata_v2.mp4',
      'traces/elapsed_and_real_timestamp/screen_recording_metadata_v2.mp4',
    );
    await E2eTestUtils.closeSnackBar();
    await E2eTestUtils.clickViewTracesButton();

    const viewer = element(by.css(viewerSelector));
    expect(await viewer.isPresent()).toBeTruthy();

    const video = element(by.css(`${viewerSelector} video`));
    expect(await video.isPresent()).toBeTruthy();
    const src = await video.getAttribute('src');
    expect(src).toContain('blob:');

    const overlayTitle = element(by.css(`${viewerSelector} .overlay-title`));
    expect(await overlayTitle.getText()).toEqual(
      'screen_recording_metadata_v2',
    );

    const selectTrigger = element(
      by.css(`${viewerSelector} .mat-select-trigger`),
    );
    expect(await selectTrigger.isPresent()).toBeTruthy();
    await selectTrigger.click();
    const option2 = element.all(by.css('.mat-option')).last();
    await option2.click();

    expect(await video.isPresent()).toBeTruthy();
    const newSrc = await video.getAttribute('src');
    expect(newSrc).toContain('blob:');
    expect(newSrc).not.toEqual(src);
  });
});
