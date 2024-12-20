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

import {tryMergeConfigArrays} from './trace_configuration';

describe('tryMergeConfigArrays', () => {
  it('replaces entire array if key not enabledConfigs or selectionConfigs', () => {
    const target = {
      otherConfigs: [{name: 'test1', key: 'test1'}],
    };
    const source = {
      otherConfigs: [{name: 'test2', key: 'test2'}],
    };
    tryMergeConfigArrays('otherConfigs', target, source);
    expect(target.otherConfigs).toEqual([{name: 'test2', key: 'test2'}]);
  });

  it('replaces only configs that are present in destination already', () => {
    const target = {
      enableConfigs: [{name: 'test1', key: 'test1', enabled: false}],
      selectionConfigs: [{name: 'test1', key: 'test1', value: '1'}],
    };
    const source = {
      enableConfigs: [
        {name: 'test1', key: 'test1', enabled: true},
        {name: 'test2', key: 'test2', enabled: false},
      ],
      selectionConfigs: [],
    };
    tryMergeConfigArrays('enableConfigs', target, source);
    expect(target.enableConfigs).toEqual([
      {name: 'test1', key: 'test1', enabled: true},
    ]);
    expect(target.selectionConfigs).toEqual([
      {name: 'test1', key: 'test1', value: '1'},
    ]);
  });
});
