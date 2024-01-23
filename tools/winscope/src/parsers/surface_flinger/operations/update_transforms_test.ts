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

import {assertDefined} from 'common/assert_utils';
import {TreeNodeUtils} from 'test/unit/tree_node_utils';
import {PropertySource, PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {UpdateTransforms} from './update_transforms';

describe('UpdateTransforms', () => {
  let propertyRoot: PropertyTreeNode;
  let operation: UpdateTransforms;
  let protoTransform: any;
  let protoPosition: any;

  beforeEach(() => {
    operation = new UpdateTransforms();
    propertyRoot = new PropertyTreeNode('test node', 'node', PropertySource.PROTO, undefined);
    protoTransform = {
      dsdx: 0,
      dtdx: 0,
      dsdy: 0,
      dtdy: 0,
      type: 0,
    };
    protoPosition = {
      x: 0,
      y: 0,
    };
  });
  it('adds matrix to transform', () => {
    propertyRoot.addChild(
      TreeNodeUtils.makePropertyNode(propertyRoot.id, 'transform', protoTransform)
    );
    propertyRoot.addChild(
      TreeNodeUtils.makePropertyNode(propertyRoot.id, 'position', protoPosition)
    );

    const expectedRoot = makeExpectedMatrixNode('test node', 'transform');

    operation.apply(propertyRoot);
    const transformNode = assertDefined(propertyRoot.getChildByName('transform'));
    expect(transformNode.getChildByName('matrix')).toEqual(expectedRoot);
  });

  it('adds matrix to requested transform', () => {
    propertyRoot.addChild(
      TreeNodeUtils.makePropertyNode(propertyRoot.id, 'requestedTransform', protoTransform)
    );
    propertyRoot.addChild(
      TreeNodeUtils.makePropertyNode(propertyRoot.id, 'requestedPosition', protoPosition)
    );

    const expectedRoot = makeExpectedMatrixNode('test node', 'requestedTransform');

    operation.apply(propertyRoot);
    const transformNode = assertDefined(propertyRoot.getChildByName('requestedTransform'));
    expect(transformNode.getChildByName('matrix')).toEqual(expectedRoot);
  });

  it('adds matrix to buffer transform', () => {
    propertyRoot.addChild(
      TreeNodeUtils.makePropertyNode(propertyRoot.id, 'bufferTransform', protoTransform)
    );

    const expectedRoot = makeExpectedMatrixNode('test node', 'bufferTransform');

    operation.apply(propertyRoot);
    const transformNode = assertDefined(propertyRoot.getChildByName('bufferTransform'));
    expect(transformNode.getChildByName('matrix')).toEqual(expectedRoot);
  });

  it('adds matrix to input window info transform', () => {
    propertyRoot.addChild(
      TreeNodeUtils.makePropertyNode(propertyRoot.id, 'inputWindowInfo', {
        transform: protoTransform,
      })
    );

    const expectedRoot = makeExpectedMatrixNode('test node.inputWindowInfo', 'transform');

    operation.apply(propertyRoot);
    const inputWindowNode = assertDefined(propertyRoot.getChildByName('inputWindowInfo'));
    const transformNode = assertDefined(inputWindowNode.getChildByName('transform'));
    expect(transformNode.getChildByName('matrix')).toEqual(expectedRoot);
  });

  function makeExpectedMatrixNode(rootId: string, transformName: string): PropertyTreeNode {
    return TreeNodeUtils.makeCalculatedPropertyNode(`${rootId}.${transformName}`, 'matrix', {
      dsdx: 1,
      dtdx: 0,
      tx: 0,
      dsdy: 0,
      dtdy: 1,
      ty: 0,
    });
  }
});
