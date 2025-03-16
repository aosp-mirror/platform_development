/*
 * Copyright (C) 2025 The Android Open Source Project
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

import {TimeUtils} from './time_utils';

describe('TimeUtils', () => {
  it('waits for condition', async () => {
    let success = false;
    setTimeout(() => {
      success = true;
    }, 200);
    await expectAsync(TimeUtils.wait(() => success, 1000)).toBeResolved();
  });

  it('times out waiting for condition', async () => {
    let success = false;
    const promise = TimeUtils.sleepMs(200).then(() => {
      success = true;
    });
    await expectAsync(TimeUtils.wait(() => success, 100, 50)).toBeRejected();
    await promise;
  });

  it('checks condition based on interval', async () => {
    let success = false;
    const promise = TimeUtils.sleepMs(250).then(() => {
      success = true;
    });
    await expectAsync(TimeUtils.wait(() => success, 500, 500)).toBeRejected();
    await promise;
  });
});
