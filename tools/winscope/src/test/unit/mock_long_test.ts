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

import {MockLong} from './mock_long';

describe('MockLong', () => {
  it('converts low/high bits to string correctly', () => {
    expect(new MockLong(0, 0).toString()).toEqual('0');
    expect(new MockLong(10, 0).toString()).toEqual('10');
    expect(new MockLong(0, 10).toString()).toEqual('42949672960');
    expect(new MockLong(10, 10).toString()).toEqual('42949672970');
    expect(new MockLong(-1719594551, 174).toString()).toEqual('749899682249');
  });
});
