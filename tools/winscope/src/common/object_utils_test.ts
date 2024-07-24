/*
 * Copyright (C) 2023 The Android Open Source Project
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

import {ObjectUtils} from './object_utils';

describe('ObjectUtils', () => {
  it('getField()', () => {
    const obj = {
      child0: {
        key0: 'value0',
      },
      child1: [{key1: 'value1'}, 10],
    };

    expect(ObjectUtils.getProperty(obj, 'child0')).toEqual({key0: 'value0'});
    expect(ObjectUtils.getProperty(obj, 'child0.key0')).toEqual('value0');
    expect(ObjectUtils.getProperty(obj, 'child1')).toEqual([
      {key1: 'value1'},
      10,
    ]);
    expect(ObjectUtils.getProperty(obj, 'child1[0]')).toEqual({key1: 'value1'});
    expect(ObjectUtils.getProperty(obj, 'child1[0].key1')).toEqual('value1');
    expect(ObjectUtils.getProperty(obj, 'child1[1]')).toEqual(10);
  });

  it('setField()', () => {
    const obj = {};

    ObjectUtils.setProperty(obj, 'child0.key0', 'value0');
    expect(obj).toEqual({
      child0: {
        key0: 'value0',
      },
    });

    ObjectUtils.setProperty(obj, 'child1[0].key1', 'value1');
    ObjectUtils.setProperty(obj, 'child1[1]', 10);
    expect(obj).toEqual({
      child0: {
        key0: 'value0',
      },
      child1: [{key1: 'value1'}, 10],
    });
  });
});
