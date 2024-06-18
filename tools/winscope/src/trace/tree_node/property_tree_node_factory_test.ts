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
import {
  PropertySource,
  PropertyTreeNode,
} from 'trace/tree_node/property_tree_node';
import {PropertyTreeNodeFactory} from './property_tree_node_factory';

describe('PropertyTreeNodeFactory', () => {
  let factory: PropertyTreeNodeFactory;

  beforeEach(() => {
    factory = new PropertyTreeNodeFactory();
  });

  it('makes property node root', () => {
    const node = factory.makePropertyRoot(
      '1 rootName',
      'rootName',
      PropertySource.PROTO,
      undefined,
    );
    const expectedNode = new PropertyTreeNode(
      '1 rootName',
      'rootName',
      PropertySource.PROTO,
      undefined,
    );
    expect(node).toEqual(expectedNode);
  });

  it('makes node with string value', () => {
    const node = factory.makeProtoProperty('1 rootName', 'name', 'rootName');

    const expectedNode = new PropertyTreeBuilder()
      .setRootId('1 rootName')
      .setName('name')
      .setSource(PropertySource.PROTO)
      .setValue('rootName')
      .build();

    expect(node).toEqual(expectedNode);
  });

  it('makes node with integer value', () => {
    const node = factory.makeProtoProperty('1 rootName', 'id', 1);

    const expectedNode = new PropertyTreeBuilder()
      .setRootId('1 rootName')
      .setName('id')
      .setSource(PropertySource.PROTO)
      .setValue(1)
      .build();

    expect(node).toEqual(expectedNode);
  });

  it('makes node with boolean value', () => {
    const node = factory.makeProtoProperty('1 rootName', 'isPresent', true);

    const expectedNode = new PropertyTreeBuilder()
      .setRootId('1 rootName')
      .setName('isPresent')
      .setSource(PropertySource.PROTO)
      .setValue(true)
      .build();

    expect(node).toEqual(expectedNode);
  });

  it('makes node with bigint value', () => {
    const node = factory.makeProtoProperty(
      '1 rootName',
      'bigIntProp',
      BigInt(123),
    );

    const expectedNode = new PropertyTreeBuilder()
      .setRootId('1 rootName')
      .setName('bigIntProp')
      .setSource(PropertySource.PROTO)
      .setValue(BigInt(123))
      .build();

    expect(node).toEqual(expectedNode);
  });

  it('makes simple properties nested in object', () => {
    const nestedProperty = {size: 3, isPresent: false};
    const node = factory.makeProtoProperty(
      '1 rootName',
      'nestedProperty',
      nestedProperty,
    );

    const expectedNode = new PropertyTreeBuilder()
      .setRootId('1 rootName')
      .setName('nestedProperty')
      .setSource(PropertySource.PROTO)
      .setChildren([
        {name: 'size', value: 3},
        {name: 'isPresent', value: false},
      ])
      .build();

    expect(node).toEqual(expectedNode);
  });

  it('makes simple properties nested in array', () => {
    const nestedProperty = [1, 2, 3];
    const node = factory.makeProtoProperty(
      '1 rootName',
      'nestedProperty',
      nestedProperty,
    );

    const expectedNode = new PropertyTreeBuilder()
      .setRootId('1 rootName')
      .setName('nestedProperty')
      .setSource(PropertySource.PROTO)
      .setChildren([
        {name: '0', value: 1},
        {name: '1', value: 2},
        {name: '2', value: 3},
      ])
      .build();

    expect(node).toEqual(expectedNode);
  });

  it('makes object properties nested in array', () => {
    const nestedProperty = [{width: 4, height: 8}];
    const node = factory.makeProtoProperty(
      '1 rootName',
      'nestedProperty',
      nestedProperty,
    );

    const expectedNode = new PropertyTreeBuilder()
      .setRootId('1 rootName')
      .setName('nestedProperty')
      .setSource(PropertySource.PROTO)
      .setChildren([
        {
          name: '0',
          value: undefined,
          children: [
            {name: 'width', value: 4},
            {name: 'height', value: 8},
          ],
        },
      ])
      .build();

    expect(node).toEqual(expectedNode);
  });

  it('makes array properties nested in array', () => {
    const nestedProperty = [[44, 88]];
    const node = factory.makeProtoProperty(
      '1 rootName',
      'nestedProperty',
      nestedProperty,
    );

    const expectedNode = new PropertyTreeBuilder()
      .setRootId('1 rootName')
      .setName('nestedProperty')
      .setSource(PropertySource.PROTO)
      .setChildren([
        {
          name: '0',
          value: undefined,
          children: [
            {name: '0', value: 44},
            {name: '1', value: 88},
          ],
        },
      ])
      .build();

    expect(node).toEqual(expectedNode);
  });

  it('makes simple calculated property', () => {
    const node = factory.makeCalculatedProperty(
      '1 rootName',
      'isVisible',
      true,
    );

    const expectedNode = new PropertyTreeBuilder()
      .setRootId('1 rootName')
      .setName('isVisible')
      .setSource(PropertySource.CALCULATED)
      .setValue(true)
      .build();

    expect(node).toEqual(expectedNode);
  });

  it('makes nested calculated property', () => {
    const node = factory.makeCalculatedProperty(
      '1 rootName',
      'zOrderPath',
      [0, 1, 2],
    );

    const expectedNode = new PropertyTreeBuilder()
      .setRootId('1 rootName')
      .setName('zOrderPath')
      .setSource(PropertySource.CALCULATED)
      .setChildren([
        {name: '0', value: 0},
        {name: '1', value: 1},
        {name: '2', value: 2},
      ])
      .build();

    expect(node).toEqual(expectedNode);
  });
});
