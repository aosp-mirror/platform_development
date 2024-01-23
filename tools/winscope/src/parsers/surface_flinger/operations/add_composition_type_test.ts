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

import {perfetto} from 'protos/surfaceflinger/latest/static';
import {TreeNodeUtils} from 'test/unit/tree_node_utils';
import {LayerCompositionType} from 'trace/layer_composition_type';
import {PropertySource, PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {AddCompositionType} from './add_composition_type';

describe('AddCompositionType', () => {
  let propertyRoot: PropertyTreeNode;
  let expectedRoot: PropertyTreeNode;
  let operation: AddCompositionType;

  beforeEach(() => {
    propertyRoot = new PropertyTreeNode('test node', 'node', PropertySource.PROTO, undefined);
    expectedRoot = new PropertyTreeNode('test node', 'node', PropertySource.PROTO, undefined);
    operation = new AddCompositionType();
  });

  it('creates compositionType node with GPU value', () => {
    const hwcCompositionType = TreeNodeUtils.makePropertyNode(
      propertyRoot.id,
      'hwcCompositionType',
      perfetto.protos.HwcCompositionType.HWC_TYPE_CLIENT
    );
    propertyRoot.addChild(hwcCompositionType);

    const expectedCompositionType = TreeNodeUtils.makeCalculatedPropertyNode(
      'test node',
      'compositionType',
      LayerCompositionType.GPU
    );
    expectedRoot.addChild(hwcCompositionType);
    expectedRoot.addChild(expectedCompositionType);

    operation.apply(propertyRoot);
    expect(propertyRoot).toEqual(expectedRoot);
  });

  it('creates compositionType node with HWC value for HwcCompositionType.HWC_TYPE_DEVICE', () => {
    const hwcCompositionType = TreeNodeUtils.makePropertyNode(
      propertyRoot.id,
      'hwcCompositionType',
      perfetto.protos.HwcCompositionType.HWC_TYPE_DEVICE
    );
    propertyRoot.addChild(hwcCompositionType);

    const expectedCompositionType = TreeNodeUtils.makeCalculatedPropertyNode(
      'test node',
      'compositionType',
      LayerCompositionType.HWC
    );
    expectedRoot.addChild(hwcCompositionType);
    expectedRoot.addChild(expectedCompositionType);

    operation.apply(propertyRoot);
    expect(propertyRoot).toEqual(expectedRoot);
  });

  it('creates compositionType node with HWC value for HwcCompositionType.HWC_TYPE_SOLID_COLOR', () => {
    const hwcCompositionType = TreeNodeUtils.makePropertyNode(
      propertyRoot.id,
      'hwcCompositionType',
      perfetto.protos.HwcCompositionType.HWC_TYPE_SOLID_COLOR
    );
    propertyRoot.addChild(hwcCompositionType);

    const expectedCompositionType = TreeNodeUtils.makeCalculatedPropertyNode(
      'test node',
      'compositionType',
      LayerCompositionType.HWC
    );
    expectedRoot.addChild(hwcCompositionType);
    expectedRoot.addChild(expectedCompositionType);

    operation.apply(propertyRoot);
    expect(propertyRoot).toEqual(expectedRoot);
  });

  it('does not create compositionType node if hwcCompositionType not in mapping', () => {
    const hwcCompositionType = TreeNodeUtils.makePropertyNode(
      propertyRoot.id,
      'hwcCompositionType',
      0
    );
    propertyRoot.addChild(hwcCompositionType);
    expectedRoot.addChild(hwcCompositionType);

    operation.apply(propertyRoot);
    expect(propertyRoot).toEqual(expectedRoot);
  });
});
