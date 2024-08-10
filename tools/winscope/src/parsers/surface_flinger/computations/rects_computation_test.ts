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

import {IDENTITY_MATRIX} from 'common/geometry/transform_matrix';
import {
  Transform,
  TransformTypeFlags,
} from 'parsers/surface_flinger/transform_utils';
import {android} from 'protos/surfaceflinger/udc/static';
import {HierarchyTreeBuilder} from 'test/unit/hierarchy_tree_builder';
import {TraceRect} from 'trace/trace_rect';
import {TraceRectBuilder} from 'trace/trace_rect_builder';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {InputConfig, RectsComputation} from './rects_computation';

describe('SurfaceFlinger RectsComputation', () => {
  const rotationTransform = new Transform(
    TransformTypeFlags.ROT_90_VAL,
    IDENTITY_MATRIX,
  );
  let computation: RectsComputation;

  beforeEach(() => {
    computation = new RectsComputation();
  });

  it('makes layer rects', () => {
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
            cornerRadius: 0,
            layerStack: 0,
            bounds: {left: 0, top: 0, right: 1, bottom: 1},
            screenBounds: {left: 0, top: 0, right: 1, bottom: 1},
            zOrderPath: [0],
            isComputedVisible: true,
            transform: Transform.EMPTY,
          } as android.surfaceflinger.ILayerProto,
          children: [
            {
              id: 2,
              name: 'layer2',
              properties: {
                id: 2,
                name: 'layer2',
                cornerRadius: 2,
                layerStack: 0,
                bounds: {left: 0, top: 0, right: 2, bottom: 2},
                screenBounds: {left: 0, top: 0, right: 2, bottom: 2},
                zOrderPath: [0, 1],
                occludedBy: [1],
                isComputedVisible: false,
                transform: Transform.EMPTY,
              } as android.surfaceflinger.ILayerProto,
            },
          ],
        },
        {
          id: 3,
          name: 'layer3',
          properties: {
            id: 3,
            name: 'layer3',
            cornerRadius: 2,
            layerStack: 0,
            bounds: {left: 0, top: 0, right: 2, bottom: 2},
            screenBounds: {left: 0, top: 0, right: 2, bottom: 2},
            zOrderPath: [0],
            isComputedVisible: false,
            transform: Transform.EMPTY,
          } as android.surfaceflinger.ILayerProto,
        },
        {
          id: 4,
          name: 'layerRelativeZ',
          properties: {
            id: 4,
            name: 'layerRelativeZ',
            cornerRadius: 0,
            layerStack: 0,
            bounds: {left: 0, top: 0, right: 5, bottom: 5},
            screenBounds: {left: 0, top: 0, right: 5, bottom: 5},
            zOrderPath: [0, 2],
            isComputedVisible: true,
            color: {r: 0, g: 0, b: 0, a: 1},
            transform: Transform.EMPTY,
          } as android.surfaceflinger.ILayerProto,
        },
      ])
      .build();

    const expectedRects: TraceRect[] = [
      new TraceRectBuilder()
        .setX(0)
        .setY(0)
        .setWidth(1)
        .setHeight(1)
        .setId('1 layer1')
        .setName('layer1')
        .setCornerRadius(0)
        .setTransform(Transform.EMPTY.matrix)
        .setDepth(0)
        .setGroupId(0)
        .setIsVisible(true)
        .setOpacity(0)
        .setIsDisplay(false)
        .setIsSpy(false)
        .build(),

      new TraceRectBuilder()
        .setX(0)
        .setY(0)
        .setWidth(2)
        .setHeight(2)
        .setId('2 layer2')
        .setName('layer2')
        .setCornerRadius(2)
        .setTransform(Transform.EMPTY.matrix)
        .setDepth(1)
        .setGroupId(0)
        .setIsVisible(false)
        .setIsDisplay(false)
        .setIsSpy(false)
        .build(),

      new TraceRectBuilder()
        .setX(0)
        .setY(0)
        .setWidth(2)
        .setHeight(2)
        .setId('3 layer3')
        .setName('layer3')
        .setCornerRadius(2)
        .setTransform(Transform.EMPTY.matrix)
        .setDepth(2)
        .setGroupId(0)
        .setIsVisible(false)
        .setIsDisplay(false)
        .setIsSpy(false)
        .build(),

      new TraceRectBuilder()
        .setX(0)
        .setY(0)
        .setWidth(5)
        .setHeight(5)
        .setId('4 layerRelativeZ')
        .setName('layerRelativeZ')
        .setCornerRadius(0)
        .setTransform(Transform.EMPTY.matrix)
        .setDepth(3)
        .setGroupId(0)
        .setIsVisible(true)
        .setOpacity(1)
        .setIsDisplay(false)
        .setIsSpy(false)
        .build(),
    ];

    computation.setRoot(hierarchyRoot).executeInPlace();
    checkLayerRects(hierarchyRoot, expectedRects);
  });

  it('handles layer rects with different group ids', () => {
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
            cornerRadius: 0,
            layerStack: 0,
            bounds: {left: 0, top: 0, right: 1, bottom: 1},
            screenBounds: {left: 0, top: 0, right: 1, bottom: 1},
            zOrderPath: [0],
            isComputedVisible: true,
            color: {r: 0, g: 0, b: 0, a: 1},
            transform: Transform.EMPTY,
          } as android.surfaceflinger.ILayerProto,
        },
        {
          id: 2,
          name: 'layer2',
          properties: {
            id: 2,
            name: 'layer2',
            cornerRadius: 0,
            layerStack: 1,
            bounds: {left: 0, top: 0, right: 1, bottom: 1},
            screenBounds: {left: 0, top: 0, right: 1, bottom: 1},
            zOrderPath: [0],
            isComputedVisible: true,
            color: {r: 0, g: 0, b: 0, a: 1},
            transform: Transform.EMPTY,
          } as android.surfaceflinger.ILayerProto,
        },
      ])
      .build();

    const expectedRects: TraceRect[] = [
      new TraceRectBuilder()
        .setX(0)
        .setY(0)
        .setWidth(1)
        .setHeight(1)
        .setId('1 layer1')
        .setName('layer1')
        .setCornerRadius(0)
        .setTransform(Transform.EMPTY.matrix)
        .setDepth(0)
        .setGroupId(0)
        .setIsVisible(true)
        .setOpacity(1)
        .setIsDisplay(false)
        .setIsSpy(false)
        .build(),

      new TraceRectBuilder()
        .setX(0)
        .setY(0)
        .setWidth(1)
        .setHeight(1)
        .setId('2 layer2')
        .setName('layer2')
        .setCornerRadius(0)
        .setTransform(Transform.EMPTY.matrix)
        .setDepth(0)
        .setGroupId(1)
        .setIsVisible(true)
        .setOpacity(1)
        .setIsDisplay(false)
        .setIsSpy(false)
        .build(),
    ];

    computation.setRoot(hierarchyRoot).executeInPlace();
    checkLayerRects(hierarchyRoot, expectedRects);
  });

  it('makes display rects', () => {
    const hierarchyRoot = new HierarchyTreeBuilder()
      .setId('LayerTraceEntry')
      .setName('root')
      .setProperties({
        displays: [
          {
            id: 1,
            layerStack: 0,
            layerStackSpaceRect: {left: 0, top: 0, right: 5, bottom: 5},
            transform: Transform.EMPTY,
            name: 'Test Display',
            size: {w: 5, h: 5},
            isOn: true,
          },
          {
            id: 2,
            layerStack: 0,
            layerStackSpaceRect: null,
            size: {w: 5, h: 10},
            transform: Transform.EMPTY,
            name: 'Test Display 2',
            isOn: true,
            isVirtual: true,
          },
          {
            id: 3,
            layerStack: 0,
            layerStackSpaceRect: null,
            size: {w: 5, h: 10},
            transform: rotationTransform,
            name: 'Test Display 3',
            isOn: false,
            isVirtual: true,
          },
        ],
      })
      .build();

    const expectedDisplayRects = [
      new TraceRectBuilder()
        .setX(0)
        .setY(0)
        .setWidth(5)
        .setHeight(5)
        .setId('Display - 1')
        .setName('Test Display')
        .setCornerRadius(0)
        .setTransform(Transform.EMPTY.matrix)
        .setDepth(0)
        .setGroupId(0)
        .setIsVisible(false)
        .setIsDisplay(true)
        .setIsActiveDisplay(true)
        .setIsSpy(false)
        .build(),
      new TraceRectBuilder()
        .setX(0)
        .setY(0)
        .setWidth(5)
        .setHeight(10)
        .setId('Display - 2')
        .setName('Test Display 2')
        .setCornerRadius(0)
        .setTransform(Transform.EMPTY.matrix)
        .setDepth(1)
        .setGroupId(0)
        .setIsVisible(false)
        .setIsDisplay(true)
        .setIsActiveDisplay(false)
        .setIsSpy(false)
        .build(),
      new TraceRectBuilder()
        .setX(0)
        .setY(0)
        .setWidth(10)
        .setHeight(5)
        .setId('Display - 3')
        .setName('Test Display 3')
        .setCornerRadius(0)
        .setTransform(Transform.EMPTY.matrix)
        .setDepth(2)
        .setGroupId(0)
        .setIsVisible(false)
        .setIsDisplay(true)
        .setIsActiveDisplay(false)
        .setIsSpy(false)
        .build(),
    ];

    computation.setRoot(hierarchyRoot).executeInPlace();
    expect(hierarchyRoot.getRects()).toEqual(expectedDisplayRects);
  });

  it('makes display rects with unknown or empty name', () => {
    const hierarchyRoot = new HierarchyTreeBuilder()
      .setId('LayerTraceEntry')
      .setName('root')
      .setProperties({
        displays: [
          {
            id: 1,
            layerStack: 0,
            layerStackSpaceRect: {left: 0, top: 0, right: 5, bottom: 5},
            transform: Transform.EMPTY,
            size: {w: 5, h: 5},
          },
          {
            id: 1,
            layerStack: 0,
            layerStackSpaceRect: {left: 0, top: 0, right: 5, bottom: 5},
            transform: Transform.EMPTY,
            name: '',
            size: {w: 5, h: 5},
          },
        ],
      })
      .build();

    const expectedDisplayRects = [
      new TraceRectBuilder()
        .setX(0)
        .setY(0)
        .setWidth(5)
        .setHeight(5)
        .setId('Display - 1')
        .setName('Unknown Display')
        .setCornerRadius(0)
        .setTransform(Transform.EMPTY.matrix)
        .setDepth(0)
        .setGroupId(0)
        .setIsVisible(false)
        .setIsDisplay(true)
        .setIsSpy(false)
        .build(),
      new TraceRectBuilder()
        .setX(0)
        .setY(0)
        .setWidth(5)
        .setHeight(5)
        .setId('Display - 1')
        .setName('Unknown Display (2)')
        .setCornerRadius(0)
        .setTransform(Transform.EMPTY.matrix)
        .setDepth(1)
        .setGroupId(0)
        .setIsVisible(false)
        .setIsDisplay(true)
        .setIsSpy(false)
        .build(),
    ];

    computation.setRoot(hierarchyRoot).executeInPlace();
    expect(hierarchyRoot.getRects()).toEqual(expectedDisplayRects);
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
            name: 'layer1',
            cornerRadius: 0,
            layerStack: 0,
            bounds: {left: 0, top: 0, right: 1, bottom: 1},
            screenBounds: {left: 0, top: 0, right: 1, bottom: 1},
            zOrderPath: [0, 1],
            isComputedVisible: true,
            color: {r: 0, g: 0, b: 0, a: 1},
            transform: Transform.EMPTY,
          } as android.surfaceflinger.ILayerProto,
        },
        {
          id: 2,
          name: 'layer2',
          properties: {
            id: 2,
            name: 'layer2',
            cornerRadius: 0,
            layerStack: 0,
            bounds: {left: 0, top: 0, right: 2, bottom: 2},
            screenBounds: {left: 0, top: 0, right: 2, bottom: 2},
            zOrderPath: [0, 0, 0],
            isComputedVisible: true,
            color: {r: 0, g: 0, b: 0, a: 1},
            transform: Transform.EMPTY,
          } as android.surfaceflinger.ILayerProto,
        },
      ])
      .build();

    const expectedRects: TraceRect[] = [
      new TraceRectBuilder()
        .setX(0)
        .setY(0)
        .setWidth(1)
        .setHeight(1)
        .setId('1 layer1')
        .setName('layer1')
        .setCornerRadius(0)
        .setTransform(Transform.EMPTY.matrix)
        .setDepth(1)
        .setGroupId(0)
        .setIsVisible(true)
        .setOpacity(1)
        .setIsDisplay(false)
        .setIsSpy(false)
        .build(),

      new TraceRectBuilder()
        .setX(0)
        .setY(0)
        .setWidth(2)
        .setHeight(2)
        .setId('2 layer2')
        .setName('layer2')
        .setCornerRadius(0)
        .setTransform(Transform.EMPTY.matrix)
        .setDepth(0)
        .setGroupId(0)
        .setIsVisible(true)
        .setOpacity(1)
        .setIsDisplay(false)
        .setIsSpy(false)
        .build(),
    ];

    computation.setRoot(hierarchyRoot).executeInPlace();
    checkLayerRects(hierarchyRoot, expectedRects);
  });

  it('handles z-order paths with equal values (fall back to Layer ID comparison)', () => {
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
            cornerRadius: 0,
            layerStack: 0,
            bounds: {left: 0, top: 0, right: 1, bottom: 1},
            screenBounds: {left: 0, top: 0, right: 1, bottom: 1},
            zOrderPath: [0, 1],
            isComputedVisible: true,
            color: {r: 0, g: 0, b: 0, a: 1},
            transform: Transform.EMPTY,
          } as android.surfaceflinger.ILayerProto,
        },
        {
          id: 2,
          name: 'layer2',
          properties: {
            id: 2,
            name: 'layer2',
            cornerRadius: 0,
            layerStack: 0,
            bounds: {left: 0, top: 0, right: 2, bottom: 2},
            screenBounds: {left: 0, top: 0, right: 2, bottom: 2},
            zOrderPath: [0, 1, 0],
            isComputedVisible: true,
            color: {r: 0, g: 0, b: 0, a: 1},
            transform: Transform.EMPTY,
          } as android.surfaceflinger.ILayerProto,
        },
      ])
      .build();

    const expectedRects: TraceRect[] = [
      new TraceRectBuilder()
        .setX(0)
        .setY(0)
        .setWidth(1)
        .setHeight(1)
        .setId('1 layer1')
        .setName('layer1')
        .setCornerRadius(0)
        .setTransform(Transform.EMPTY.matrix)
        .setDepth(0)
        .setGroupId(0)
        .setIsVisible(true)
        .setOpacity(1)
        .setIsDisplay(false)
        .setIsSpy(false)
        .build(),

      new TraceRectBuilder()
        .setX(0)
        .setY(0)
        .setWidth(2)
        .setHeight(2)
        .setId('2 layer2')
        .setName('layer2')
        .setCornerRadius(0)
        .setTransform(Transform.EMPTY.matrix)
        .setDepth(1)
        .setGroupId(0)
        .setIsVisible(true)
        .setOpacity(1)
        .setIsDisplay(false)
        .setIsSpy(false)
        .build(),
    ];

    computation.setRoot(hierarchyRoot).executeInPlace();
    checkLayerRects(hierarchyRoot, expectedRects);
  });

  it('does not make non-visible rects with invalid screen bounds', () => {
    const hierarchyRoot = new HierarchyTreeBuilder()
      .setId('LayerTraceEntry')
      .setName('root')
      .setProperties({
        displays: [
          {
            id: 1,
            layerStack: 0,
            layerStackSpaceRect: {left: 0, top: 0, right: 5, bottom: 10},
            transform: Transform.EMPTY,
            name: 'Test Display',
            size: {w: 5, h: 10},
          },
        ],
      })
      .setChildren([
        {
          id: 1,
          name: 'layer1',
          properties: {
            id: 1,
            name: 'layer1',
            cornerRadius: 0,
            layerStack: 0,
            bounds: {left: -50, top: -100, right: 50, bottom: 100},
            screenBounds: {left: -50, top: -100, right: 50, bottom: 100},
            zOrderPath: [0],
            isComputedVisible: true,
            transform: Transform.EMPTY,
          } as android.surfaceflinger.ILayerProto,
        },
        {
          id: 2,
          name: 'layer2',
          properties: {
            id: 2,
            name: 'layer2',
            cornerRadius: 0,
            layerStack: 0,
            bounds: {left: -50, top: -100, right: 50, bottom: 100},
            screenBounds: {left: -50, top: -100, right: 50, bottom: 100},
            zOrderPath: [0],
            isComputedVisible: false,
            transform: Transform.EMPTY,
          } as android.surfaceflinger.ILayerProto,
        },
        {
          id: 3,
          name: 'layer3',
          properties: {
            id: 3,
            name: 'layer3',
            cornerRadius: 0,
            layerStack: 0,
            bounds: {left: -50, top: -100, right: 50, bottom: 100},
            screenBounds: {left: -49.991, top: -100, right: 50, bottom: 100},
            zOrderPath: [0],
            isComputedVisible: false,
            transform: Transform.EMPTY,
          } as android.surfaceflinger.ILayerProto,
        },
        {
          id: 4,
          name: 'layer4',
          properties: {
            id: 4,
            name: 'layer4',
            cornerRadius: 0,
            layerStack: 0,
            bounds: {left: -50000, top: -50000, right: 50000, bottom: 50000},
            screenBounds: {
              left: -50000,
              top: -50000,
              right: 50000,
              bottom: 50000,
            },
            zOrderPath: [0],
            isComputedVisible: false,
            transform: Transform.EMPTY,
          } as android.surfaceflinger.ILayerProto,
        },
        {
          id: 5,
          name: 'layer5',
          properties: {
            id: 5,
            name: 'layer5',
            cornerRadius: 0,
            layerStack: 0,
            bounds: {left: -100, top: -50, right: 100, bottom: 50},
            screenBounds: {left: -100, top: -50, right: 100, bottom: 50},
            zOrderPath: [0],
            isComputedVisible: false,
            transform: Transform.EMPTY,
          } as android.surfaceflinger.ILayerProto,
        },
      ])
      .build();

    const expectedRects = [
      new TraceRectBuilder()
        .setX(-50)
        .setY(-100)
        .setWidth(100)
        .setHeight(200)
        .setId('1 layer1')
        .setName('layer1')
        .setCornerRadius(0)
        .setTransform(Transform.EMPTY.matrix)
        .setDepth(1)
        .setGroupId(0)
        .setIsVisible(true)
        .setOpacity(0)
        .setIsDisplay(false)
        .setIsSpy(false)
        .build(),
    ];

    computation.setRoot(hierarchyRoot).executeInPlace();
    checkLayerRects(hierarchyRoot, expectedRects);
  });

  it('calculates invalid screen bounds from all displays present', () => {
    const hierarchyRoot = new HierarchyTreeBuilder()
      .setId('LayerTraceEntry')
      .setName('root')
      .setProperties({
        displays: [
          {
            id: 1,
            layerStack: 0,
            layerStackSpaceRect: {left: 0, top: 0, right: 5, bottom: 10},
            transform: Transform.EMPTY,
            name: 'Test Display',
            size: {w: 5, h: 10},
          },
          {
            id: 2,
            layerStack: 0,
            layerStackSpaceRect: {left: 0, top: 0, right: 5, bottom: 10},
            transform: rotationTransform,
            name: 'Test Display 2',
            size: {w: 5, h: 10},
          },
        ],
      })
      .setChildren([
        {
          id: 1,
          name: 'layer1',
          properties: {
            id: 1,
            name: 'layer1',
            cornerRadius: 0,
            layerStack: 0,
            bounds: {left: -50, top: -100, right: 50, bottom: 100},
            screenBounds: {left: -50, top: -100, right: 50, bottom: 100},
            zOrderPath: [0],
            isComputedVisible: false,
            transform: Transform.EMPTY,
          } as android.surfaceflinger.ILayerProto,
        },
        {
          id: 2,
          name: 'layer2',
          properties: {
            id: 2,
            name: 'layer2',
            cornerRadius: 0,
            layerStack: 0,
            bounds: {left: -100, top: -100, right: 100, bottom: 100},
            screenBounds: {left: -100, top: -100, right: 100, bottom: 100},
            zOrderPath: [0],
            isComputedVisible: false,
            transform: Transform.EMPTY,
          } as android.surfaceflinger.ILayerProto,
        },
      ])
      .build();

    computation.setRoot(hierarchyRoot).executeInPlace();
    checkLayerRects(hierarchyRoot, []);
  });

  it('makes input window rects', () => {
    const hierarchyRoot = new HierarchyTreeBuilder()
      .setId('LayerTraceEntry')
      .setName('root')
      .setProperties({
        displays: [
          {
            id: 1,
            layerStack: 0,
            layerStackSpaceRect: {left: 0, top: 0, right: 5, bottom: 5},
            transform: Transform.EMPTY,
            name: 'Test Display',
            size: {w: 5, h: 5},
          },
        ],
      })
      .setChildren([
        {
          id: 1,
          name: 'layer1',
          properties: {
            id: 1,
            name: 'layer1',
            cornerRadius: 0,
            layerStack: 0,
            bounds: {left: 0, top: 0, right: 1, bottom: 1},
            screenBounds: {left: 0, top: 0, right: 1, bottom: 1},
            zOrderPath: [0],
            isComputedVisible: true,
            transform: Transform.EMPTY,
            inputWindowInfo: {
              inputConfig: InputConfig.SPY,
              visible: true,
            },
          } as android.surfaceflinger.ILayerProto,
          children: [
            {
              id: 2,
              name: 'layer2',
              properties: {
                id: 2,
                name: 'layer2',
                cornerRadius: 2,
                layerStack: 0,
                bounds: {left: 0, top: 0, right: 2, bottom: 2},
                screenBounds: {left: 0, top: 0, right: 2, bottom: 2},
                zOrderPath: [0, 1],
                occludedBy: [1],
                isComputedVisible: false,
                transform: Transform.EMPTY,
                inputWindowInfo: {
                  inputConfig: 0,
                  visible: false,
                },
              } as android.surfaceflinger.ILayerProto,
            },
          ],
        },
        {
          id: 3,
          name: 'wallpaper',
          properties: {
            id: 3,
            name: 'layer3',
            cornerRadius: 2,
            layerStack: 0,
            bounds: {left: -999, top: -999, right: 999, bottom: 999},
            screenBounds: {left: 0, top: 0, right: 2, bottom: 2},
            zOrderPath: [0],
            isComputedVisible: false,
            transform: Transform.EMPTY,
            inputWindowInfo: {
              inputConfig: InputConfig.IS_WALLPAPER,
              visible: true,
            },
          } as android.surfaceflinger.ILayerProto,
        },
        {
          id: 4,
          name: 'layerRelativeZ',
          properties: {
            id: 4,
            name: 'layerRelativeZ',
            cornerRadius: 0,
            layerStack: 0,
            bounds: {left: 0, top: 0, right: 5, bottom: 5},
            screenBounds: {left: 0, top: 0, right: 5, bottom: 5},
            zOrderPath: [0, 2],
            isComputedVisible: true,
            color: {r: 0, g: 0, b: 0, a: 1},
            transform: Transform.EMPTY,
            inputWindowInfo: {
              inputConfig: 0,
            },
          } as android.surfaceflinger.ILayerProto,
        },
        {
          id: 5,
          name: 'noInputLayer',
          properties: {
            id: 5,
            name: 'noInputLayer',
            cornerRadius: 0,
            layerStack: 0,
            bounds: {left: 0, top: 0, right: 5, bottom: 5},
            screenBounds: {left: 0, top: 0, right: 5, bottom: 5},
            zOrderPath: [0],
            isComputedVisible: true,
            color: {r: 0, g: 0, b: 0, a: 1},
            transform: Transform.EMPTY,
            inputWindowInfo: {},
          } as android.surfaceflinger.ILayerProto,
        },
      ])
      .build();

    const expectedInputRects: TraceRect[] = [
      new TraceRectBuilder()
        .setX(0)
        .setY(0)
        .setWidth(1)
        .setHeight(1)
        .setId('1')
        .setName('layer1')
        .setCornerRadius(0)
        .setTransform(Transform.EMPTY.matrix)
        .setDepth(1)
        .setGroupId(0)
        .setIsVisible(true)
        .setIsDisplay(false)
        .setIsSpy(true)
        .build(),

      new TraceRectBuilder()
        .setX(0)
        .setY(0)
        .setWidth(2)
        .setHeight(2)
        .setId('2')
        .setName('layer2')
        .setCornerRadius(0)
        .setTransform(Transform.EMPTY.matrix)
        .setDepth(2)
        .setGroupId(0)
        .setIsVisible(false)
        .setIsDisplay(false)
        .setIsSpy(false)
        .build(),

      // This is a wallpaper window, so it is cropped to display bounds.
      new TraceRectBuilder()
        .setX(0)
        .setY(0)
        .setWidth(5)
        .setHeight(5)
        .setId('3')
        .setName('layer3')
        .setCornerRadius(0)
        .setTransform(Transform.EMPTY.matrix)
        .setDepth(3)
        .setGroupId(0)
        .setIsVisible(true)
        .setIsDisplay(false)
        .setIsSpy(false)
        .build(),

      new TraceRectBuilder()
        .setX(0)
        .setY(0)
        .setWidth(5)
        .setHeight(5)
        .setId('4')
        .setName('layerRelativeZ')
        .setCornerRadius(0)
        .setTransform(Transform.EMPTY.matrix)
        .setDepth(4)
        .setGroupId(0)
        .setIsVisible(true)
        .setIsDisplay(false)
        .setIsSpy(false)
        .build(),
    ];

    const expectedDisplayRect = new TraceRectBuilder()
      .setX(0)
      .setY(0)
      .setWidth(5)
      .setHeight(5)
      .setId('Display - 1')
      .setName('Test Display')
      .setCornerRadius(0)
      .setTransform(Transform.EMPTY.matrix)
      .setDepth(0)
      .setGroupId(0)
      .setIsVisible(false)
      .setIsDisplay(true)
      .setIsSpy(false)
      .build();

    computation.setRoot(hierarchyRoot).executeInPlace();
    checkInputRects(hierarchyRoot, expectedInputRects);
    expect(hierarchyRoot.getRects()).toEqual([expectedDisplayRect]);
  });

  function checkRects(
    hierarchyRoot: HierarchyTreeNode,
    expectedRects: TraceRect[],
    usePrimaryRects: boolean,
  ) {
    const rects: TraceRect[] = [];
    hierarchyRoot.forEachNodeDfs((node) => {
      if (node.id === 'LayerTraceEntry root') {
        return;
      }
      const nodeRects = usePrimaryRects
        ? node.getRects()
        : node.getSecondaryRects();
      if (nodeRects) rects.push(...nodeRects);
    });
    expect(rects).toEqual(expectedRects);
  }

  function checkLayerRects(
    hierarchyRoot: HierarchyTreeNode,
    expectedRects: TraceRect[],
  ) {
    checkRects(hierarchyRoot, expectedRects, true);
  }

  function checkInputRects(
    hierarchyRoot: HierarchyTreeNode,
    expectedRects: TraceRect[],
  ) {
    checkRects(hierarchyRoot, expectedRects, false);
  }
});
