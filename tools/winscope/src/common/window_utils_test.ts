/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {WindowUtils} from './window_utils';

describe('WindowUtils', () => {
  let windowSpy: jasmine.Spy;

  beforeEach(() => {
    windowSpy = spyOn(window, 'open');
  });

  it('opens new window', () => {
    windowSpy.and.returnValue(jasmine.any(Window));
    expect(WindowUtils.showPopupWindow('test')).toEqual(true);
    expect(windowSpy).toHaveBeenCalledTimes(1);
    expect(windowSpy.calls.allArgs()[0][0]).toEqual('test');
  });

  it('returns false if window fails to open', () => {
    windowSpy.and.returnValue(null);
    expect(WindowUtils.showPopupWindow('test')).toEqual(false);
  });
});
