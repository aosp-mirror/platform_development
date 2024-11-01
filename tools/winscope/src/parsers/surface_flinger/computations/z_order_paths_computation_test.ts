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
import {ZOrderPathsComputation} from './z_order_paths_computation';

describe('ZOrderPathsComputation', () => {
  let computation: ZOrderPathsComputation;

  beforeEach(() => {
    computation = new ZOrderPathsComputation();
  });

  it('throws error if root not set', () => {
    expect(() => computation.executeInPlace()).toThrowError();
  });

  it('does not affect tree without rel z parents set', () => {
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
    const layer1 = assertDefined(hierarchyRoot.getChildByName('layer1'));
    const layer2 = assertDefined(layer1.getChildByName('layer2'));
    const layer3 = assertDefined(layer2.getChildByName('layer3'));
    const layer4 = assertDefined(layer1.getChildByName('layer4'));

    expect(layer1.getRelativeChildren()).toEqual([]);
    expect(layer2.getRelativeChildren()).toEqual([]);
    expect(layer3.getRelativeChildren()).toEqual([]);
    expect(layer4.getRelativeChildren()).toEqual([]);

    expect(layer1.getZParent()).toEqual(layer1.getParent());
    expect(layer2.getZParent()).toEqual(layer2.getParent());
    expect(layer3.getZParent()).toEqual(layer3.getParent());
    expect(layer4.getZParent()).toEqual(layer4.getParent());
  });

  it('updates tree with rel z parent', () => {
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
            {
              id: 5,
              name: 'layer5',
              properties: {
                id: 5,
                name: 'layer5',
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
    const layer1 = assertDefined(hierarchyRoot.getChildByName('layer1'));
    const layer2 = assertDefined(layer1.getChildByName('layer2'));
    const layer4 = assertDefined(layer1.getChildByName('layer4'));
    const layer5 = assertDefined(layer1.getChildByName('layer5'));

    expect(layer1.getRelativeChildren()).toEqual([]);
    expect(layer2.getRelativeChildren()).toEqual([layer4, layer5]);
    expect(layer4.getRelativeChildren()).toEqual([]);
    expect(layer5.getRelativeChildren()).toEqual([]);

    expect(layer1.getZParent()).toEqual(layer1.getParent());
    expect(layer2.getZParent()).toEqual(layer2.getParent());
    expect(layer4.getZParent()).toEqual(layer2);
    expect(layer5.getZParent()).toEqual(layer2);
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
    const layer1 = assertDefined(hierarchyRoot.getChildByName('layer1'));
    const layer2 = assertDefined(layer1.getChildByName('layer2'));
    const layer4 = assertDefined(layer1.getChildByName('layer4'));
    expect(
      layer4.getEagerPropertyByName('isMissingZParent')?.getValue(),
    ).toBeTrue();

    expect(layer1.getRelativeChildren()).toEqual([]);
    expect(layer2.getRelativeChildren()).toEqual([]);
    expect(layer4.getRelativeChildren()).toEqual([]);

    expect(layer1.getZParent()).toEqual(layer1.getParent());
    expect(layer2.getZParent()).toEqual(layer2.getParent());
    expect(layer4.getZParent()).toEqual(layer4.getParent());
  });
});
