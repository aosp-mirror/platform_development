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
import {android} from 'protos/surfaceflinger/udc/static';
import {HierarchyTreeBuilder} from 'test/unit/hierarchy_tree_builder';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {ZOrderPathsComputation} from './z_order_paths_computation';

describe('ZOrderPathsComputation', () => {
  let computation: ZOrderPathsComputation;

  beforeEach(() => {
    computation = new ZOrderPathsComputation();
  });

  it('calculates zOrderPath for tree without rel z parent', () => {
    const hierarchyRoot = new HierarchyTreeBuilder()
      .setId('LayerTraceEntry')
      .setName('root')
      .setChildren([
        {
          id: 1,
          name: 'layer1',
          properties: {
            id: 1,
            name: 'layer1',
            parent: -1,
            children: [2, 4],
            z: 0,
            zOrderRelativeOf: -1,
          } as android.surfaceflinger.ILayerProto,
          children: [
            {
              id: 2,
              name: 'layer2',
              properties: {
                id: 2,
                name: 'layer2',
                parent: 1,
                children: [3],
                z: 1,
                zOrderRelativeOf: -1,
              } as android.surfaceflinger.ILayerProto,
              children: [
                {
                  id: 3,
                  name: 'layer3',
                  properties: {
                    id: 3,
                    name: 'layer3',
                    parent: 2,
                    children: [],
                    z: 1,
                    zOrderRelativeOf: -1,
                  } as android.surfaceflinger.ILayerProto,
                },
              ],
            },
            {
              id: 4,
              name: 'layer4',
              properties: {
                id: 4,
                name: 'layer4',
                parent: 1,
                children: [],
                z: 2,
                zOrderRelativeOf: -1,
              } as android.surfaceflinger.ILayerProto,
            },
          ],
        },
      ])
      .build();

    computation.setRoot(hierarchyRoot).executeInPlace();
    const layer1WithPath = assertDefined(
      hierarchyRoot.getChildByName('layer1'),
    );
    const layer2WithPath = assertDefined(
      layer1WithPath.getChildByName('layer2'),
    );
    const layer3WithPath = assertDefined(
      layer2WithPath.getChildByName('layer3'),
    );
    const layer4WithPath = assertDefined(
      layer1WithPath.getChildByName('layer4'),
    );

    expect(
      getZOrderPathArray(layer1WithPath.getEagerPropertyByName('zOrderPath')),
    ).toEqual([0]);
    expect(
      getZOrderPathArray(layer2WithPath.getEagerPropertyByName('zOrderPath')),
    ).toEqual([0, 1]);
    expect(
      getZOrderPathArray(layer3WithPath.getEagerPropertyByName('zOrderPath')),
    ).toEqual([0, 1, 1]);
    expect(
      getZOrderPathArray(layer4WithPath.getEagerPropertyByName('zOrderPath')),
    ).toEqual([0, 2]);
  });

  it('calculates zOrderPath for tree with rel z parent', () => {
    const hierarchyRoot = new HierarchyTreeBuilder()
      .setId('LayerTraceEntry')
      .setName('root')
      .setChildren([
        {
          id: 1,
          name: 'layer1',
          properties: {
            id: 1,
            name: 'layer1',
            parent: -1,
            children: [2, 4],
            z: 0,
            zOrderRelativeOf: -1,
          } as android.surfaceflinger.ILayerProto,
          children: [
            {
              id: 2,
              name: 'layer2',
              properties: {
                id: 2,
                name: 'layer2',
                parent: 1,
                children: [3],
                z: 1,
                zOrderRelativeOf: -1,
              } as android.surfaceflinger.ILayerProto,
            },
            {
              id: 4,
              name: 'layer4',
              properties: {
                id: 4,
                name: 'layer4',
                parent: 1,
                children: [],
                z: 2,
                zOrderRelativeOf: 2,
              } as android.surfaceflinger.ILayerProto,
            },
          ],
        },
      ])
      .build();

    computation.setRoot(hierarchyRoot).executeInPlace();
    const layer1WithPath = assertDefined(
      hierarchyRoot.getChildByName('layer1'),
    );
    const layer2WithPath = assertDefined(
      layer1WithPath.getChildByName('layer2'),
    );
    const layer4WithPath = assertDefined(
      layer1WithPath.getChildByName('layer4'),
    );

    expect(
      getZOrderPathArray(layer1WithPath.getEagerPropertyByName('zOrderPath')),
    ).toEqual([0]);
    expect(
      getZOrderPathArray(layer2WithPath.getEagerPropertyByName('zOrderPath')),
    ).toEqual([0, 1]);
    expect(
      getZOrderPathArray(layer4WithPath.getEagerPropertyByName('zOrderPath')),
    ).toEqual([0, 1, 2]);
  });

  it('adds isMissingZParent chip', () => {
    const hierarchyRoot = new HierarchyTreeBuilder()
      .setId('LayerTraceEntry')
      .setName('root')
      .setChildren([
        {
          id: 1,
          name: 'layer1',
          properties: {
            id: 1,
            name: 'layer1',
            parent: -1,
            children: [2, 4],
            z: 0,
            zOrderRelativeOf: -1,
          } as android.surfaceflinger.ILayerProto,
          children: [
            {
              id: 2,
              name: 'layer2',
              properties: {
                id: 2,
                name: 'layer2',
                parent: 1,
                children: [3],
                z: 1,
                zOrderRelativeOf: -1,
              } as android.surfaceflinger.ILayerProto,
            },
            {
              id: 4,
              name: 'layer4',
              properties: {
                id: 4,
                name: 'layer4',
                parent: 1,
                children: [],
                z: 2,
                zOrderRelativeOf: 5,
              } as android.surfaceflinger.ILayerProto,
            },
          ],
        },
      ])
      .build();

    computation.setRoot(hierarchyRoot).executeInPlace();
    const layer1WithPath = assertDefined(
      hierarchyRoot.getChildByName('layer1'),
    );
    const layer2WithPath = assertDefined(
      layer1WithPath.getChildByName('layer2'),
    );
    const layer4WithPath = assertDefined(
      layer1WithPath.getChildByName('layer4'),
    );

    expect(
      getZOrderPathArray(layer1WithPath.getEagerPropertyByName('zOrderPath')),
    ).toEqual([0]);
    expect(
      getZOrderPathArray(layer2WithPath.getEagerPropertyByName('zOrderPath')),
    ).toEqual([0, 1]);
    expect(
      getZOrderPathArray(layer4WithPath.getEagerPropertyByName('zOrderPath')),
    ).toEqual([0, 2]);
    expect(
      layer4WithPath.getEagerPropertyByName('isMissingZParent')?.getValue(),
    ).toBeTrue();
  });

  function getZOrderPathArray(
    property: PropertyTreeNode | undefined,
  ): number[] {
    if (!property) return [];
    return property.getAllChildren().map((child) => Number(child.getValue()));
  }
});
