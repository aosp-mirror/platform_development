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

import {InMemoryStorage} from './in_memory_storage';

describe('InMemoryStorage', () => {
  it('can store and retrieve values', () => {
    const mockStorage = new InMemoryStorage();
    expect(mockStorage.get('key')).toBeUndefined();
    mockStorage.add('key', 'value');
    expect(mockStorage.get('key')).toEqual('value');
  });

  it('can clear values', () => {
    const mockStorage = new InMemoryStorage();
    // robust to value not found in store
    mockStorage.clear('key');
    mockStorage.add('key', 'value');
    mockStorage.clear('key');
    expect(mockStorage.get('key')).toBeUndefined();
  });
});
