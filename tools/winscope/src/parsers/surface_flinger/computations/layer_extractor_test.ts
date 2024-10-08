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
import {Transform} from 'parsers/surface_flinger/transform_utils';
import {android} from 'protos/surfaceflinger/udc/static';
import {HierarchyTreeBuilder} from 'test/unit/hierarchy_tree_builder';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {LayerExtractor} from './layer_extractor';
import {ZOrderPathsComputation} from './z_order_paths_computation';

describe('LayerExtractor', () => {
  it('sorts according to single-value z-order paths', () => {
    const hierarchyRoot = new HierarchyTreeBuilder()
      .setId('LayerTraceEntry')
      .setName('root')
      .setChildren([
        {
          id: 1,
          name: 'layer1',
          properties: {
            id: 1,
            zOrderPath: [1],
          } as android.surfaceflinger.ILayerProto,
        },
        {
          id: 2,
          name: 'layer2',
          properties: {
            id: 2,
            zOrderPath: [2],
          } as android.surfaceflinger.ILayerProto,
        },
        {
          id: 3,
          name: 'layer3',
          properties: {
            id: 3,
            zOrderPath: [0],
          } as android.surfaceflinger.ILayerProto,
        },
      ])
      .build();

    const layers = LayerExtractor.extractLayersSortedByZ(hierarchyRoot);
    checkLayerOrder(layers, [2, 1, 3]);
  });

  it('sorts according to multiple-value z-order paths', () => {
    const hierarchyRoot = new HierarchyTreeBuilder()
      .setId('LayerTraceEntry')
      .setName('root')
      .setChildren([
        {
          id: 1,
          name: 'layer1',
          properties: {
            id: 1,
            zOrderPath: [1, 1],
          } as android.surfaceflinger.ILayerProto,
        },
        {
          id: 2,
          name: 'layer2',
          properties: {
            id: 2,
            zOrderPath: [2, 1],
          } as android.surfaceflinger.ILayerProto,
        },
        {
          id: 3,
          name: 'layer3',
          properties: {
            id: 3,
            zOrderPath: [1, 2],
          } as android.surfaceflinger.ILayerProto,
        },
      ])
      .build();

    const layers = LayerExtractor.extractLayersSortedByZ(hierarchyRoot);
    checkLayerOrder(layers, [2, 3, 1]);
  });

  it('handles z-order paths with different lengths', () => {
    const hierarchyRoot = new HierarchyTreeBuilder()
      .setId('LayerTraceEntry')
      .setName('root')
      .setChildren([
        {
          id: 1,
          name: 'layer1',
          properties: {
            id: 1,
            zOrderPath: [0, 1],
            transform: Transform.EMPTY,
          } as android.surfaceflinger.ILayerProto,
        },
        {
          id: 2,
          name: 'layer2',
          properties: {
            id: 2,
            zOrderPath: [0, 0, 0],
          } as android.surfaceflinger.ILayerProto,
        },
      ])
      .build();

    const layers = LayerExtractor.extractLayersSortedByZ(hierarchyRoot);
    checkLayerOrder(layers, [1, 2]);
  });

  it('handles relative layers', () => {
    const hierarchyRoot = new HierarchyTreeBuilder()
      .setId('LayerTraceEntry')
      .setName('root')
      .setChildren([
        {
          id: 1,
          name: 'layer1',
          properties: {
            id: 1,
            zOrderPath: [0],
          } as android.surfaceflinger.ILayerProto,
          children: [
            {
              id: 2,
              name: 'layer2',
              properties: {
                id: 2,
                zOrderPath: [0, 1],
              } as android.surfaceflinger.ILayerProto,
            },
          ],
        },
        {
          id: 3,
          name: 'layer3',
          properties: {
            id: 3,
            zOrderPath: [0],
          } as android.surfaceflinger.ILayerProto,
        },
        {
          id: 4,
          name: 'layer4',
          properties: {
            id: 4,
            zOrderPath: [0, 0],
          } as android.surfaceflinger.ILayerProto,
        },
      ])
      .build();

    const layer1 = assertDefined(hierarchyRoot.getChildByName('layer1'));
    const layer4 = assertDefined(hierarchyRoot.getChildByName('layer4'));
    layer1.addRelativeChild(layer4);
    layer4.setZParent(layer1);

    const layers = LayerExtractor.extractLayersSortedByZ(hierarchyRoot);
    checkLayerOrder(layers, [3, 2, 4, 1]);
  });

  it('handles negative z values in z-order path', () => {
    const hierarchyRoot = new HierarchyTreeBuilder()
      .setId('LayerTraceEntry')
      .setName('root')
      .setChildren([
        {
          id: 1,
          name: 'layer1',
          properties: {
            id: 1,
            zOrderPath: [0],
          } as android.surfaceflinger.ILayerProto,
          children: [
            {
              id: 2,
              name: 'layer2',
              properties: {
                id: 2,
                zOrderPath: [0, 1],
              } as android.surfaceflinger.ILayerProto,
            },
          ],
        },
        {
          id: 5,
          name: 'layer5',
          properties: {
            id: 5,
            zOrderPath: [-5],
          } as android.surfaceflinger.ILayerProto,
        },
      ])
      .build();

    const layers = LayerExtractor.extractLayersSortedByZ(hierarchyRoot);
    checkLayerOrder(layers, [2, 1, 5]);
  });

  it('handles z-order paths with equal values (fall back to layer id comparison)', () => {
    const hierarchyRoot = new HierarchyTreeBuilder()
      .setId('LayerTraceEntry')
      .setName('root')
      .setChildren([
        {
          id: 1,
          name: 'layer1',
          properties: {
            id: 1,
            zOrderPath: [0, 1],
          } as android.surfaceflinger.ILayerProto,
        },
        {
          id: 2,
          name: 'layer',
          properties: {
            id: 2,
            zOrderPath: [0, 1, 0],
          } as android.surfaceflinger.ILayerProto,
        },
      ])
      .build();

    const layers = LayerExtractor.extractLayersSortedByZ(hierarchyRoot);
    checkLayerOrder(layers, [2, 1]);
  });

  it('restricts z-order sorting to each level of hierarchy', () => {
    const hierarchyRoot = new HierarchyTreeBuilder()
      .setId('LayerTraceEntry')
      .setName('root')
      .setChildren([
        {
          id: 1,
          name: 'layer1',
          properties: {
            id: 1,
            zOrderPath: [0, 3],
          } as android.surfaceflinger.ILayerProto,
        },
        {
          id: 2,
          name: 'layer2',
          properties: {
            id: 2,
            zOrderPath: [0],
          } as android.surfaceflinger.ILayerProto,
          children: [
            {
              id: 3,
              name: 'layer3',
              properties: {
                id: 3,
                zOrderPath: [0, 2],
              } as android.surfaceflinger.ILayerProto,
            },
            {
              id: 4,
              name: 'layer4',
              properties: {
                id: 4,
                zOrderPath: [0, 1],
              } as android.surfaceflinger.ILayerProto,
            },
          ],
        },
      ])
      .build();

    const layers = LayerExtractor.extractLayersSortedByZ(hierarchyRoot);
    checkLayerOrder(layers, [3, 4, 2, 1]);
  });

  function computeZOrder(root: HierarchyTreeNode) {
    new ZOrderPathsComputation().setRoot(root).executeInPlace();
  }

  function checkLayerOrder(layers: HierarchyTreeNode[], expectedIds: number[]) {
    expect(
      layers.map((layer) => layer.getEagerPropertyByName('id')?.getValue()),
    ).toEqual(expectedIds);
  }
});
