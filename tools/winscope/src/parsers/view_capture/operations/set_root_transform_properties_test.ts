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

import {PropertyTreeBuilder} from 'test/unit/property_tree_builder';
import {PropertySource} from 'trace/tree_node/property_tree_node';
import {SetRootTransformProperties} from './set_root_transform_properties';

describe('SetRootTransformProperties', () => {
  let operation: SetRootTransformProperties;

  beforeEach(() => {
    operation = new SetRootTransformProperties();
  });

  it('sets scale and translation properties', () => {
    const propertyRoot = new PropertyTreeBuilder()
      .setRootId('test')
      .setName('node')
      .setChildren([
        {name: 'scaleX', value: 2},
        {name: 'scaleY', value: 2},
        {name: 'translationX', value: 2},
        {name: 'translationY', value: 2},
      ])
      .build();

    const expectedRoot = new PropertyTreeBuilder()
      .setRootId('test')
      .setName('node')
      .setChildren([
        {name: 'scaleX', value: 1, source: PropertySource.CALCULATED},
        {name: 'scaleY', value: 1, source: PropertySource.CALCULATED},
        {name: 'translationX', value: 0, source: PropertySource.CALCULATED},
        {name: 'translationY', value: 0, source: PropertySource.CALCULATED},
      ])
      .build();

    operation.apply(propertyRoot);
    expect(propertyRoot).toEqual(expectedRoot);
  });
});
