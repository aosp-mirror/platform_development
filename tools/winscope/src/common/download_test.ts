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

import {Download} from './download';

describe('Download', () => {
  it('fromUrl', () => {
    const testElement = document.createElement('a');
    testElement.className = 'test-download-link';
    const clickSpy = spyOn(testElement, 'click');

    spyOn(document, 'createElement').and.returnValue(testElement);

    Download.fromUrl('test_url', 'test_file_name');

    expect(testElement.href.endsWith('test_url')).toBeTrue();
    expect(testElement.download).toEqual('test_file_name');
    expect(clickSpy).toHaveBeenCalled();

    expect(document.querySelector('.test-download-link')).toBeNull();
  });
});
