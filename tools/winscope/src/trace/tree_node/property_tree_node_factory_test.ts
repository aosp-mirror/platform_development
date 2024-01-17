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

import {PropertySource, PropertyTreeNode} from 'trace/tree_node/property_tree_node';
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
      undefined
    );
    const expectedNode = new PropertyTreeNode(
      '1 rootName',
      'rootName',
      PropertySource.PROTO,
      undefined
    );
    expect(node).toEqual(expectedNode);
  });

  it('makes node with string value', () => {
    const node = factory.makeProtoProperty('1 rootName', 'name', 'rootName');
    const expectedNode = new PropertyTreeNode(
      '1 rootName.name',
      'name',
      PropertySource.PROTO,
      'rootName'
    );
    expect(node).toEqual(expectedNode);
  });

  it('makes node with integer value', () => {
    const node = factory.makeProtoProperty('1 rootName', 'id', 1);
    const expectedNode = new PropertyTreeNode('1 rootName.id', 'id', PropertySource.PROTO, 1);
    expect(node).toEqual(expectedNode);
  });

  it('makes node with boolean value', () => {
    const node = factory.makeProtoProperty('1 rootName', 'isPresent', true);
    const expectedNode = new PropertyTreeNode(
      '1 rootName.isPresent',
      'isPresent',
      PropertySource.PROTO,
      true
    );
    expect(node).toEqual(expectedNode);
  });

  it('makes node with bigint value', () => {
    const node = factory.makeProtoProperty('1 rootName', 'bigIntProp', BigInt(123));
    const expectedNode = new PropertyTreeNode(
      '1 rootName.bigIntProp',
      'bigIntProp',
      PropertySource.PROTO,
      BigInt(123)
    );
    expect(node).toEqual(expectedNode);
  });

  it('makes simple properties nested in object', () => {
    const nestedProperty = {size: 3, isPresent: false};
    const node = factory.makeProtoProperty('1 rootName', 'nestedProperty', nestedProperty);

    const expectedNode = new PropertyTreeNode(
      '1 rootName.nestedProperty',
      'nestedProperty',
      PropertySource.PROTO,
      undefined
    );
    expectedNode.addChild(
      new PropertyTreeNode('1 rootName.nestedProperty.size', 'size', PropertySource.PROTO, 3)
    );
    expectedNode.addChild(
      new PropertyTreeNode(
        '1 rootName.nestedProperty.isPresent',
        'isPresent',
        PropertySource.PROTO,
        false
      )
    );
    expect(node).toEqual(expectedNode);
  });

  it('makes simple properties nested in array', () => {
    const nestedProperty = [1, 2, 3];
    const node = factory.makeProtoProperty('1 rootName', 'nestedProperty', nestedProperty);

    const expectedNode = new PropertyTreeNode(
      '1 rootName.nestedProperty',
      'nestedProperty',
      PropertySource.PROTO,
      undefined
    );

    expectedNode.addChild(
      new PropertyTreeNode('1 rootName.nestedProperty.0', '0', PropertySource.PROTO, 1)
    );
    expectedNode.addChild(
      new PropertyTreeNode('1 rootName.nestedProperty.1', '1', PropertySource.PROTO, 2)
    );
    expectedNode.addChild(
      new PropertyTreeNode('1 rootName.nestedProperty.2', '2', PropertySource.PROTO, 3)
    );
    expect(node).toEqual(expectedNode);
  });

  it('makes object properties nested in array', () => {
    const nestedProperty = [{width: 4, height: 8}];
    const node = factory.makeProtoProperty('1 rootName', 'nestedProperty', nestedProperty);

    const expectedNode = new PropertyTreeNode(
      '1 rootName.nestedProperty',
      'nestedProperty',
      PropertySource.PROTO,
      undefined
    );

    const expectedChild = new PropertyTreeNode(
      '1 rootName.nestedProperty.0',
      '0',
      PropertySource.PROTO,
      undefined
    );
    expectedChild.addChild(
      new PropertyTreeNode('1 rootName.nestedProperty.0.width', 'width', PropertySource.PROTO, 4)
    );
    expectedChild.addChild(
      new PropertyTreeNode('1 rootName.nestedProperty.0.height', 'height', PropertySource.PROTO, 8)
    );
    expectedNode.addChild(expectedChild);

    expect(node).toEqual(expectedNode);
  });

  it('makes array properties nested in array', () => {
    const nestedProperty = [[44, 88]];
    const node = factory.makeProtoProperty('1 rootName', 'nestedProperty', nestedProperty);

    const expectedNode = new PropertyTreeNode(
      '1 rootName.nestedProperty',
      'nestedProperty',
      PropertySource.PROTO,
      undefined
    );

    const expectedChild = new PropertyTreeNode(
      '1 rootName.nestedProperty.0',
      '0',
      PropertySource.PROTO,
      undefined
    );

    expectedChild.addChild(
      new PropertyTreeNode('1 rootName.nestedProperty.0.0', '0', PropertySource.PROTO, 44)
    );
    expectedChild.addChild(
      new PropertyTreeNode('1 rootName.nestedProperty.0.1', '1', PropertySource.PROTO, 88)
    );
    expectedNode.addChild(expectedChild);

    expect(node).toEqual(expectedNode);
  });

  it('makes simple calculated property', () => {
    const node = factory.makeCalculatedProperty('1 rootName', 'isVisible', true);
    const expectedNode = new PropertyTreeNode(
      '1 rootName.isVisible',
      'isVisible',
      PropertySource.CALCULATED,
      true
    );
    expect(node).toEqual(expectedNode);
  });

  it('makes nested calculated property', () => {
    const node = factory.makeCalculatedProperty('1 rootName', 'zOrderPath', [0, 1, 2]);
    const expectedNode = new PropertyTreeNode(
      '1 rootName.zOrderPath',
      'zOrderPath',
      PropertySource.CALCULATED,
      undefined
    );
    expectedNode.addChild(
      new PropertyTreeNode('1 rootName.zOrderPath.0', '0', PropertySource.CALCULATED, 0)
    );
    expectedNode.addChild(
      new PropertyTreeNode('1 rootName.zOrderPath.1', '1', PropertySource.CALCULATED, 1)
    );
    expectedNode.addChild(
      new PropertyTreeNode('1 rootName.zOrderPath.2', '2', PropertySource.CALCULATED, 2)
    );
    expect(node).toEqual(expectedNode);
  });
});
