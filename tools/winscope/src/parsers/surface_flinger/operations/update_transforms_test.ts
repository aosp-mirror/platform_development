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
import {TransformTypeFlags} from 'parsers/surface_flinger/transform_utils';
import {PropertyTreeBuilder} from 'test/unit/property_tree_builder';
import {TreeNodeUtils} from 'test/unit/tree_node_utils';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {UpdateTransforms} from './update_transforms';

describe('UpdateTransforms', () => {
  let propertyRoot: PropertyTreeNode;
  let operation: UpdateTransforms;
  let deprecatedProtoTransform: any;
  let protoPosition: any;

  beforeEach(() => {
    operation = new UpdateTransforms();
    propertyRoot = new PropertyTreeBuilder()
      .setRootId('test')
      .setName('node')
      .setIsRoot(true)
      .build();
    deprecatedProtoTransform = {
      dsdx: 1,
      dtdx: 2,
      dtdy: 3,
      dsdy: 4,
      type: TransformTypeFlags.ROT_INVALID_VAL,
    };
    protoPosition = {
      x: 0,
      y: 0,
    };
  });
  it('adds matrix to transform', () => {
    propertyRoot.addOrReplaceChild(
      TreeNodeUtils.makePropertyNode(
        propertyRoot.id,
        'transform',
        deprecatedProtoTransform,
      ),
    );
    propertyRoot.addOrReplaceChild(
      TreeNodeUtils.makePropertyNode(
        propertyRoot.id,
        'position',
        protoPosition,
      ),
    );

    const expectedRoot = makeExpectedMatrixNode(propertyRoot.id, 'transform');

    operation.apply(propertyRoot);
    const transformNode = assertDefined(
      propertyRoot.getChildByName('transform'),
    );
    expect(transformNode.getChildByName('matrix')).toEqual(expectedRoot);
  });

  it('adds matrix to requested transform', () => {
    propertyRoot.addOrReplaceChild(
      TreeNodeUtils.makePropertyNode(
        propertyRoot.id,
        'requestedTransform',
        deprecatedProtoTransform,
      ),
    );
    propertyRoot.addOrReplaceChild(
      TreeNodeUtils.makePropertyNode(
        propertyRoot.id,
        'requestedPosition',
        protoPosition,
      ),
    );

    const expectedRoot = makeExpectedMatrixNode(
      propertyRoot.id,
      'requestedTransform',
    );

    operation.apply(propertyRoot);
    const transformNode = assertDefined(
      propertyRoot.getChildByName('requestedTransform'),
    );
    expect(transformNode.getChildByName('matrix')).toEqual(expectedRoot);
  });

  it('adds matrix to buffer transform', () => {
    propertyRoot.addOrReplaceChild(
      TreeNodeUtils.makePropertyNode(
        propertyRoot.id,
        'bufferTransform',
        deprecatedProtoTransform,
      ),
    );

    const expectedRoot = makeExpectedMatrixNode(
      propertyRoot.id,
      'bufferTransform',
    );

    operation.apply(propertyRoot);
    const transformNode = assertDefined(
      propertyRoot.getChildByName('bufferTransform'),
    );
    expect(transformNode.getChildByName('matrix')).toEqual(expectedRoot);
  });

  it('adds matrix to input window info transform', () => {
    const inputWindowInfo = TreeNodeUtils.makePropertyNode(
      propertyRoot.id,
      'inputWindowInfo',
      {
        transform: deprecatedProtoTransform,
      },
    );

    propertyRoot.addOrReplaceChild(inputWindowInfo);

    const expectedRoot = makeExpectedMatrixNode(
      inputWindowInfo.id,
      'transform',
    );

    operation.apply(propertyRoot);
    const inputWindowNode = assertDefined(
      propertyRoot.getChildByName('inputWindowInfo'),
    );
    const transformNode = assertDefined(
      inputWindowNode.getChildByName('transform'),
    );
    expect(transformNode.getChildByName('matrix')).toEqual(expectedRoot);
  });

  function makeExpectedMatrixNode(
    rootId: string,
    transformName: string,
  ): PropertyTreeNode {
    return TreeNodeUtils.makeCalculatedPropertyNode(
      `${rootId}.${transformName}`,
      'matrix',
      {
        dsdx: 1,
        dtdx: 4,
        tx: 0,
        dtdy: 2,
        dsdy: 3,
        ty: 0,
      },
    );
  }
});
