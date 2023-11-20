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
import {PropertyTreeBuilderFromProto} from './property_tree_builder_from_proto';

describe('PropertyTreeBuilderFromProto', () => {
  let proto: any;
  let builder: PropertyTreeBuilderFromProto;

  beforeEach(() => {
    builder = new PropertyTreeBuilderFromProto().setRootId(1).setRootName('rootName');
  });

  it('handle string and integer properties correctly', () => {
    proto = {
      id: 1,
      name: 'rootName',
    };
    const expectedRoot = new PropertyTreeNode(
      '1 rootName',
      'rootName',
      PropertySource.PROTO,
      undefined
    );
    expectedRoot.addChild(new PropertyTreeNode('1 rootName.id', 'id', PropertySource.PROTO, 1));
    expectedRoot.addChild(
      new PropertyTreeNode('1 rootName.name', 'name', PropertySource.PROTO, 'rootName')
    );
    const tree = builder.setData(proto).build();
    expect(tree).toEqual(expectedRoot);
  });

  it('handles boolean properties correctly', () => {
    proto = {
      id: 1,
      name: 'rootName',
      isPresent: true,
    };
    const expectedRoot = new PropertyTreeNode(
      '1 rootName',
      'rootName',
      PropertySource.PROTO,
      undefined
    );
    expectedRoot.addChild(new PropertyTreeNode('1 rootName.id', 'id', PropertySource.PROTO, 1));
    expectedRoot.addChild(
      new PropertyTreeNode('1 rootName.name', 'name', PropertySource.PROTO, 'rootName')
    );
    expectedRoot.addChild(
      new PropertyTreeNode('1 rootName.isPresent', 'isPresent', PropertySource.PROTO, true)
    );
    const tree = builder.setData(proto).build();
    expect(tree).toEqual(expectedRoot);
  });

  it('handles bigint properties correctly', () => {
    proto = {
      id: 1,
      name: 'rootName',
      bigIntProp: BigInt(123),
    };
    const expectedRoot = new PropertyTreeNode(
      '1 rootName',
      'rootName',
      PropertySource.PROTO,
      undefined
    );

    expectedRoot.addChild(new PropertyTreeNode('1 rootName.id', 'id', PropertySource.PROTO, 1));
    expectedRoot.addChild(
      new PropertyTreeNode('1 rootName.name', 'name', PropertySource.PROTO, 'rootName')
    );
    expectedRoot.addChild(
      new PropertyTreeNode('1 rootName.bigIntProp', 'bigIntProp', PropertySource.PROTO, BigInt(123))
    );

    const tree = builder.setData(proto).build();
    expect(tree).toEqual(expectedRoot);
  });

  it('handles nested properties from object correctly', () => {
    proto = {
      id: 1,
      name: 'rootName',
      nestedProperty: {size: 3, isPresent: false},
    };
    const expectedRoot = new PropertyTreeNode(
      '1 rootName',
      'rootName',
      PropertySource.PROTO,
      undefined
    );

    const nestedPropertyNode = new PropertyTreeNode(
      '1 rootName.nestedProperty',
      'nestedProperty',
      PropertySource.PROTO,
      undefined
    );
    nestedPropertyNode.addChild(
      new PropertyTreeNode('1 rootName.nestedProperty.size', 'size', PropertySource.PROTO, 3)
    );
    nestedPropertyNode.addChild(
      new PropertyTreeNode(
        '1 rootName.nestedProperty.isPresent',
        'isPresent',
        PropertySource.PROTO,
        false
      )
    );

    expectedRoot.addChild(new PropertyTreeNode('1 rootName.id', 'id', PropertySource.PROTO, 1));
    expectedRoot.addChild(
      new PropertyTreeNode('1 rootName.name', 'name', PropertySource.PROTO, 'rootName')
    );
    expectedRoot.addChild(nestedPropertyNode);

    const tree = builder.setData(proto).build();
    expect(tree).toEqual(expectedRoot);
  });

  it('handles nested simple properties from array correctly', () => {
    proto = {
      id: 1,
      name: 'rootName',
      nestedProperty: [1, 2, 3],
    };
    const expectedRoot = new PropertyTreeNode(
      '1 rootName',
      'rootName',
      PropertySource.PROTO,
      undefined
    );
    const nestedPropertyNode = new PropertyTreeNode(
      '1 rootName.nestedProperty',
      'nestedProperty',
      PropertySource.PROTO,
      undefined
    );
    nestedPropertyNode.addChild(
      new PropertyTreeNode('1 rootName.nestedProperty.0', '0', PropertySource.PROTO, 1)
    );
    nestedPropertyNode.addChild(
      new PropertyTreeNode('1 rootName.nestedProperty.1', '1', PropertySource.PROTO, 2)
    );
    nestedPropertyNode.addChild(
      new PropertyTreeNode('1 rootName.nestedProperty.2', '2', PropertySource.PROTO, 3)
    );

    expectedRoot.addChild(new PropertyTreeNode('1 rootName.id', 'id', PropertySource.PROTO, 1));
    expectedRoot.addChild(
      new PropertyTreeNode('1 rootName.name', 'name', PropertySource.PROTO, 'rootName')
    );
    expectedRoot.addChild(nestedPropertyNode);

    const tree = builder.setData(proto).build();
    expect(tree).toEqual(expectedRoot);
  });

  it('handles nested object properties from array correctly', () => {
    proto = {
      id: 1,
      name: 'rootName',
      nestedProperty: [{w: 4, h: 8}],
    };
    const expectedRoot = new PropertyTreeNode(
      '1 rootName',
      'rootName',
      PropertySource.PROTO,
      undefined
    );
    const expectedPropertyNode = new PropertyTreeNode(
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
      new PropertyTreeNode('1 rootName.nestedProperty.0.w', 'w', PropertySource.PROTO, 4)
    );
    expectedChild.addChild(
      new PropertyTreeNode('1 rootName.nestedProperty.0.h', 'h', PropertySource.PROTO, 8)
    );
    expectedPropertyNode.addChild(expectedChild);

    expectedRoot.addChild(new PropertyTreeNode('1 rootName.id', 'id', PropertySource.PROTO, 1));
    expectedRoot.addChild(
      new PropertyTreeNode('1 rootName.name', 'name', PropertySource.PROTO, 'rootName')
    );
    expectedRoot.addChild(expectedPropertyNode);

    const tree = builder.setData(proto).build();
    expect(tree).toEqual(expectedRoot);
  });

  it('handles nested array properties from array correctly', () => {
    proto = {
      id: 1,
      name: 'rootName',
      nestedProperty: [[44, 88]],
    };
    const expectedRoot = new PropertyTreeNode(
      '1 rootName',
      'rootName',
      PropertySource.PROTO,
      undefined
    );

    const expectedPropertyNode = new PropertyTreeNode(
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
    expectedPropertyNode.addChild(expectedChild);

    expectedRoot.addChild(new PropertyTreeNode('1 rootName.id', 'id', PropertySource.PROTO, 1));
    expectedRoot.addChild(
      new PropertyTreeNode('1 rootName.name', 'name', PropertySource.PROTO, 'rootName')
    );
    expectedRoot.addChild(expectedPropertyNode);

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

    const tree = builder.setData(proto).setDenyList(['children', 'timestamp']).build();
    expect(tree.getAllChildren().length).toEqual(2);

    expect(tree.getChildById('1 rootName.id')).toBeDefined();
    expect(tree.getChildById('1 rootName.timestamp')).toBeUndefined();
    expect(tree.getChildById('1 rootName.name')).toBeDefined();
    expect(tree.getChildById('1 rootName.children')).toBeUndefined();
  });
});
