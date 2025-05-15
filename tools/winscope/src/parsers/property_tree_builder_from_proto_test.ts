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
import {PropertyTreeBuilderFromProto} from './property_tree_builder_from_proto';

describe('PropertyTreeBuilderFromProto', () => {
  let proto: object;
  let builder: PropertyTreeBuilderFromProto;

  beforeEach(() => {
    builder = new PropertyTreeBuilderFromProto()
      .setRootId(1)
      .setRootName('rootName');
  });

  it('handle string and integer properties correctly', () => {
    proto = {
      id: 1,
      name: 'rootName',
    };

    const expectedRoot = new PropertyTreeBuilder()
      .setRootId('1')
      .setName('rootName')
      .setIsRoot(true)
      .setChildren([
        {name: 'id', value: 1},
        {name: 'name', value: 'rootName'},
      ])
      .build();

    const tree = builder.setData(proto).build();
    expect(tree).toEqual(expectedRoot);
  });

  it('handles boolean properties correctly', () => {
    proto = {
      id: 1,
      name: 'rootName',
      isPresent: true,
    };

    const expectedRoot = new PropertyTreeBuilder()
      .setRootId('1')
      .setName('rootName')
      .setIsRoot(true)
      .setChildren([
        {name: 'id', value: 1},
        {name: 'name', value: 'rootName'},
        {name: 'isPresent', value: true},
      ])
      .build();

    const tree = builder.setData(proto).build();
    expect(tree).toEqual(expectedRoot);
  });

  it('handles bigint properties correctly', () => {
    proto = {
      id: 1,
      name: 'rootName',
      bigIntProp: BigInt(123),
    };

    const expectedRoot = new PropertyTreeBuilder()
      .setRootId('1')
      .setName('rootName')
      .setIsRoot(true)
      .setChildren([
        {name: 'id', value: 1},
        {name: 'name', value: 'rootName'},
        {name: 'bigIntProp', value: BigInt(123)},
      ])
      .build();

    const tree = builder.setData(proto).build();
    expect(tree).toEqual(expectedRoot);
  });

  it('handles nested properties from object correctly', () => {
    proto = {
      id: 1,
      name: 'rootName',
      nestedProperty: {size: 3, isPresent: false},
    };

    const expectedRoot = new PropertyTreeBuilder()
      .setRootId('1')
      .setName('rootName')
      .setIsRoot(true)
      .setChildren([
        {name: 'id', value: 1},
        {name: 'name', value: 'rootName'},
        {
          name: 'nestedProperty',
          value: undefined,
          children: [
            {name: 'size', value: 3},
            {name: 'isPresent', value: false},
          ],
        },
      ])
      .build();

    const tree = builder.setData(proto).build();
    expect(tree).toEqual(expectedRoot);
  });

  it('handles nested simple properties from array correctly', () => {
    proto = {
      id: 1,
      name: 'rootName',
      nestedProperty: [1, 2, 3],
    };

    const expectedRoot = new PropertyTreeBuilder()
      .setRootId('1')
      .setName('rootName')
      .setIsRoot(true)
      .setChildren([
        {name: 'id', value: 1},
        {name: 'name', value: 'rootName'},
        {
          name: 'nestedProperty',
          value: undefined,
          children: [
            {name: '0', value: 1},
            {name: '1', value: 2},
            {name: '2', value: 3},
          ],
        },
      ])
      .build();

    const tree = builder.setData(proto).build();
    expect(tree).toEqual(expectedRoot);
  });

  it('handles nested object properties from array correctly', () => {
    proto = {
      id: 1,
      name: 'rootName',
      nestedProperty: [{w: 4, h: 8}],
    };

    const expectedRoot = new PropertyTreeBuilder()
      .setRootId('1')
      .setName('rootName')
      .setIsRoot(true)
      .setChildren([
        {name: 'id', value: 1},
        {name: 'name', value: 'rootName'},
        {
          name: 'nestedProperty',
          value: undefined,
          children: [
            {
              name: '0',
              value: undefined,
              children: [
                {name: 'w', value: 4},
                {name: 'h', value: 8},
              ],
            },
          ],
        },
      ])
      .build();

    const tree = builder.setData(proto).build();
    expect(tree).toEqual(expectedRoot);
  });

  it('handles nested array properties from array correctly', () => {
    proto = {
      id: 1,
      name: 'rootName',
      nestedProperty: [[44, 88]],
    };

    const expectedRoot = new PropertyTreeBuilder()
      .setRootId('1')
      .setName('rootName')
      .setIsRoot(true)
      .setChildren([
        {name: 'id', value: 1},
        {name: 'name', value: 'rootName'},
        {
          name: 'nestedProperty',
          value: undefined,
          children: [
            {
              name: '0',
              value: undefined,
              children: [
                {name: '0', value: 44},
                {name: '1', value: 88},
              ],
            },
          ],
        },
      ])
      .build();

    const tree = builder.setData(proto).build();
    expect(tree).toEqual(expectedRoot);
  });

  it('ignores deny properties', () => {
    proto = {
      id: 1,
      children: [],
      name: 'rootName',
      timestamp: BigInt(123),
    };

    const tree = builder
      .setData(proto)
      .setDenyList(['children', 'timestamp'])
      .build();
    expect(tree.getAllChildren().length).toEqual(2);

    expect(tree.getChildByName('id')).toBeDefined();
    expect(tree.getChildByName('timestamp')).toBeUndefined();
    expect(tree.getChildByName('name')).toBeDefined();
    expect(tree.getChildByName('children')).toBeUndefined();
  });
});
