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
import {LayerFlag} from 'parsers/surface_flinger/layer_flag';
import {android} from 'protos/surfaceflinger/udc/static';
import {HierarchyTreeBuilder} from 'test/unit/hierarchy_tree_builder';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {VisibilityPropertiesComputation} from './visibility_properties_computation';
import {ZOrderPathsComputation} from './z_order_paths_computation';

describe('VisibilityPropertiesComputation', () => {
  let computation: VisibilityPropertiesComputation;

  beforeEach(() => {
    computation = new VisibilityPropertiesComputation();
  });

  it('detects visible layer due to non-empty visible region', () => {
    const hierarchyRoot = new HierarchyTreeBuilder()
      .setId('LayerTraceEntry')
      .setName('root')
      .setChildren([
        {
          id: 1,
          name: 'layerVisibleRegionNonEmpty',
          properties: {
            id: 1,
            name: 'layerVisibleRegionNonEmpty',
            parent: -1,
            children: [],
            visibleRegion: {rect: [{left: 0, right: 1, top: 0, bottom: 1}]},
            activeBuffer: {width: 1, height: 1, stride: 1, format: 1},
            flags: 0,
            color: {r: 0, g: 0, b: 0, a: 1},
            cornerRadius: 0,
            shadowRadius: 0,
            backgroundBlurRadius: 0,
            layerStack: 0,
            z: 0,
            transform: {
              type: 0,
              dsdx: 1,
              dtdx: 0,
              dsdy: 0,
              dtdy: 1,
            },
            screenBounds: null,
            isOpaque: false,
          } as android.surfaceflinger.ILayerProto,
        },
      ])
      .build();

    computation.setRoot(hierarchyRoot).executeInPlace();
    const visibleLayer = assertDefined(
      hierarchyRoot.getChildByName('layerVisibleRegionNonEmpty'),
    );
    expect(
      assertDefined(
        visibleLayer.getEagerPropertyByName('isComputedVisible'),
      ).getValue(),
    ).toBeTrue();
    expect(
      visibleLayer.getEagerPropertyByName('visibilityReason'),
    ).toBeUndefined();
  });

  it('detects non-visible layer that is hidden by policy', () => {
    const hierarchyRoot = new HierarchyTreeBuilder()
      .setId('LayerTraceEntry')
      .setName('root')
      .setChildren([
        {
          id: 1,
          name: 'layerHiddenByPolicy',
          properties: {
            id: 1,
            name: 'layerHiddenByPolicy',
            parent: -1,
            children: [],
            flags: LayerFlag.HIDDEN,
            visibleRegion: {rect: [{left: 0, right: 1, top: 0, bottom: 1}]},
            activeBuffer: {width: 1, height: 1, stride: 1, format: 1},
            color: {r: 0, g: 0, b: 0, a: 1},
            cornerRadius: 0,
            shadowRadius: 0,
            backgroundBlurRadius: 0,
            layerStack: 0,
            z: 0,
            transform: {
              type: 0,
              dsdx: 1,
              dtdx: 0,
              dsdy: 0,
              dtdy: 1,
            },
            screenBounds: null,
            isOpaque: false,
          } as android.surfaceflinger.ILayerProto,
        },
      ])
      .build();

    computation.setRoot(hierarchyRoot).executeInPlace();
    const invisibleLayer = assertDefined(
      hierarchyRoot.getChildByName('layerHiddenByPolicy'),
    );
    expect(
      assertDefined(
        invisibleLayer.getEagerPropertyByName('isComputedVisible'),
      ).getValue(),
    ).toBeFalse();
    expect(getVisibilityReasons(invisibleLayer)).toEqual(['flag is hidden']);
  });

  it('detects non-visible layer that is hidden by parent, even if rel-z parent is not hidden', () => {
    const hierarchyRoot = new HierarchyTreeBuilder()
      .setId('LayerTraceEntry')
      .setName('root')
      .setChildren([
        {
          id: 1,
          name: 'parent',
          properties: {
            id: 1,
            name: 'parent',
            parent: -1,
            children: [2],
            visibleRegion: {rect: [{left: 0, right: 1, top: 0, bottom: 1}]},
            activeBuffer: {width: 1, height: 1, stride: 1, format: 1},
            flags: LayerFlag.HIDDEN,
            color: {r: 0, g: 0, b: 0, a: 1},
            cornerRadius: 0,
            shadowRadius: 0,
            backgroundBlurRadius: 0,
            layerStack: 0,
            z: 0,
            transform: {
              type: 0,
              dsdx: 1,
              dtdx: 0,
              dsdy: 0,
              dtdy: 1,
            },
            screenBounds: null,
            isOpaque: false,
          } as android.surfaceflinger.ILayerProto,
          children: [
            {
              id: 2,
              name: 'childHiddenByParent',
              properties: {
                id: 2,
                name: 'childHiddenByParent',
                parent: 1,
                children: [],
                flags: 0,
                visibleRegion: {rect: [{left: 0, right: 1, top: 0, bottom: 1}]},
                activeBuffer: {width: 1, height: 1, stride: 1, format: 1},
                color: {r: 0, g: 0, b: 0, a: 1},
                cornerRadius: 0,
                shadowRadius: 0,
                backgroundBlurRadius: 0,
                layerStack: 0,
                z: 0,
                zOrderRelativeOf: 3,
                transform: {
                  type: 0,
                  dsdx: 1,
                  dtdx: 0,
                  dsdy: 0,
                  dtdy: 1,
                },
                screenBounds: null,
                isOpaque: false,
              } as android.surfaceflinger.ILayerProto,
            },
          ],
        },
        {
          id: 3,
          name: 'relZParent',
          properties: {
            id: 3,
            name: 'relZParent',
            parent: -1,
            children: [],
            visibleRegion: {rect: [{left: 0, right: 1, top: 0, bottom: 1}]},
            activeBuffer: {width: 1, height: 1, stride: 1, format: 1},
            flags: 0,
            color: {r: 0, g: 0, b: 0, a: 1},
            cornerRadius: 0,
            shadowRadius: 0,
            backgroundBlurRadius: 0,
            layerStack: 0,
            z: 0,
            transform: {
              type: 0,
              dsdx: 1,
              dtdx: 0,
              dsdy: 0,
              dtdy: 1,
            },
            screenBounds: null,
            isOpaque: false,
          } as android.surfaceflinger.ILayerProto,
        },
      ])
      .build();
    new ZOrderPathsComputation().setRoot(hierarchyRoot).executeInPlace();

    computation.setRoot(hierarchyRoot).executeInPlace();
    const visibleLayer = assertDefined(hierarchyRoot.getChildByName('parent'));
    expect(
      assertDefined(
        visibleLayer.getEagerPropertyByName('isComputedVisible'),
      ).getValue(),
    ).toBeFalse();

    const hiddenLayer = assertDefined(
      visibleLayer.getChildByName('childHiddenByParent'),
    );
    expect(
      assertDefined(
        hiddenLayer.getEagerPropertyByName('isComputedVisible'),
      ).getValue(),
    ).toBeFalse();
    expect(getVisibilityReasons(hiddenLayer)).toEqual(['hidden by parent 1']);
  });

  it('detects non-visible layer due to zero alpha', () => {
    const hierarchyRoot = new HierarchyTreeBuilder()
      .setId('LayerTraceEntry')
      .setName('root')
      .setChildren([
        {
          id: 1,
          name: 'layerZeroAlpha',
          properties: {
            id: 1,
            name: 'layerZeroAlpha',
            parent: -1,
            children: [],
            visibleRegion: {rect: [{left: 0, right: 1, top: 0, bottom: 1}]},
            activeBuffer: {width: 1, height: 1, stride: 1, format: 1},
            flags: 0,
            color: {r: 0, g: 0, b: 0, a: 0},
            cornerRadius: 0,
            shadowRadius: 0,
            backgroundBlurRadius: 0,
            layerStack: 0,
            z: 0,
            transform: {
              type: 0,
              dsdx: 1,
              dtdx: 0,
              dsdy: 0,
              dtdy: 1,
            },
            screenBounds: null,
            isOpaque: false,
          } as android.surfaceflinger.ILayerProto,
        },
      ])
      .build();

    computation.setRoot(hierarchyRoot).executeInPlace();

    const layerZeroAlpha = assertDefined(
      hierarchyRoot.getChildByName('layerZeroAlpha'),
    );
    expect(
      assertDefined(
        layerZeroAlpha.getEagerPropertyByName('isComputedVisible'),
      ).getValue(),
    ).toBeFalse();
    expect(getVisibilityReasons(layerZeroAlpha)).toEqual(['alpha is 0']);
  });

  it('detects non-visible layer due to null active buffer and no effects', () => {
    const hierarchyRoot = new HierarchyTreeBuilder()
      .setId('LayerTraceEntry')
      .setName('root')
      .setChildren([
        {
          id: 1,
          name: 'layerNoActiveBuffer',
          properties: {
            id: 1,
            name: 'layerNoActiveBuffer',
            parent: -1,
            children: [],
            activeBuffer: null,
            visibleRegion: {rect: [{left: 0, right: 1, top: 0, bottom: 1}]},
            flags: 0,
            color: {r: 0, g: 0, b: 0, a: 0},
            cornerRadius: 0,
            shadowRadius: 0,
            backgroundBlurRadius: 0,
            layerStack: 0,
            z: 0,
            transform: {
              type: 0,
              dsdx: 1,
              dtdx: 0,
              dsdy: 0,
              dtdy: 1,
            },
            screenBounds: null,
            isOpaque: false,
          } as android.surfaceflinger.ILayerProto,
        },
      ])
      .build();

    computation.setRoot(hierarchyRoot).executeInPlace();
    const invisibleLayer = assertDefined(
      hierarchyRoot.getChildByName('layerNoActiveBuffer'),
    );
    expect(
      assertDefined(
        invisibleLayer.getEagerPropertyByName('isComputedVisible'),
      ).getValue(),
    ).toBeFalse();
    expect(getVisibilityReasons(invisibleLayer)).toEqual([
      'buffer is empty',
      'alpha is 0',
      'does not have color fill, shadow or blur',
    ]);
  });

  it('detects non-visible layer due to empty bounds', () => {
    const hierarchyRoot = new HierarchyTreeBuilder()
      .setId('LayerTraceEntry')
      .setName('root')
      .setChildren([
        {
          id: 1,
          name: 'layerEmptyBounds',
          properties: {
            id: 1,
            name: 'layerEmptyBounds',
            parent: -1,
            children: [],
            activeBuffer: {width: 1, height: 1, stride: 1, format: 1},
            visibleRegion: {rect: [{left: 0, right: 1, top: 0, bottom: 1}]},
            bounds: {left: 0, right: 0, top: 0, bottom: 0},
            excludesCompositionState: true,
            flags: 0,
            color: {r: 0, g: 0, b: 0, a: 1},
            cornerRadius: 0,
            shadowRadius: 0,
            backgroundBlurRadius: 0,
            layerStack: 0,
            z: 0,
            transform: {
              type: 0,
              dsdx: 1,
              dtdx: 0,
              dsdy: 0,
              dtdy: 1,
            },
            screenBounds: null,
            isOpaque: false,
          } as android.surfaceflinger.ILayerProto,
        },
      ])
      .build();

    computation.setRoot(hierarchyRoot).executeInPlace();
    const invisibleLayer = assertDefined(
      hierarchyRoot.getChildByName('layerEmptyBounds'),
    );
    expect(
      assertDefined(
        invisibleLayer.getEagerPropertyByName('isComputedVisible'),
      ).getValue(),
    ).toBeFalse();
    expect(getVisibilityReasons(invisibleLayer)).toEqual(['bounds is 0x0']);
  });

  it('detects non-visible layer due to null visible region', () => {
    const hierarchyRoot = new HierarchyTreeBuilder()
      .setId('LayerTraceEntry')
      .setName('root')
      .setChildren([
        {
          id: 1,
          name: 'layerNoVisibleRegion',
          properties: {
            id: 1,
            name: 'layerNoVisibleRegion',
            parent: -1,
            children: [],
            activeBuffer: {width: 1, height: 1, stride: 1, format: 1},
            visibleRegion: null,
            flags: 0,
            color: {r: 0, g: 0, b: 0, a: 1},
            cornerRadius: 0,
            shadowRadius: 0,
            backgroundBlurRadius: 0,
            layerStack: 0,
            z: 0,
            transform: {
              type: 0,
              dsdx: 1,
              dtdx: 0,
              dsdy: 0,
              dtdy: 1,
            },
            screenBounds: null,
            isOpaque: false,
          } as android.surfaceflinger.ILayerProto,
        },
      ])
      .build();

    computation.setRoot(hierarchyRoot).executeInPlace();
    const invisibleLayer = assertDefined(
      hierarchyRoot.getChildByName('layerNoVisibleRegion'),
    );
    expect(
      assertDefined(
        invisibleLayer.getEagerPropertyByName('isComputedVisible'),
      ).getValue(),
    ).toBeFalse();
    expect(getVisibilityReasons(invisibleLayer)).toEqual([
      'null visible region',
    ]);
  });

  it('detects non-visible layer due to empty visible region', () => {
    const hierarchyRoot = new HierarchyTreeBuilder()
      .setId('LayerTraceEntry')
      .setName('root')
      .setChildren([
        {
          id: 1,
          name: 'layerEmptyVisibleRegion',
          properties: {
            id: 1,
            name: 'layerEmptyVisibleRegion',
            parent: -1,
            children: [],
            visibleRegion: {rect: [{left: 0, right: 1, top: 0, bottom: 0}]},
            activeBuffer: {width: 1, height: 1, stride: 1, format: 1},
            flags: 0,
            color: {r: 0, g: 0, b: 0, a: 1},
            cornerRadius: 0,
            shadowRadius: 0,
            backgroundBlurRadius: 0,
            layerStack: 0,
            z: 0,
            transform: {
              type: 0,
              dsdx: 1,
              dtdx: 0,
              dsdy: 0,
              dtdy: 1,
            },
            screenBounds: null,
            isOpaque: false,
          } as android.surfaceflinger.ILayerProto,
        },
      ])
      .build();

    computation.setRoot(hierarchyRoot).executeInPlace();
    const invisibleLayer = assertDefined(
      hierarchyRoot.getChildByName('layerEmptyVisibleRegion'),
    );
    expect(
      assertDefined(
        invisibleLayer.getEagerPropertyByName('isComputedVisible'),
      ).getValue(),
    ).toBeFalse();
    expect(getVisibilityReasons(invisibleLayer)).toEqual([
      'visible region calculated by Composition Engine is empty',
    ]);
  });

  it('adds occludedBy layers and updates isVisible', () => {
    const hierarchyRoot = new HierarchyTreeBuilder()
      .setId('LayerTraceEntry')
      .setName('root')
      .setProperties({
        displays: [
          {
            layerStack: 0,
            layerStackSpaceRect: {left: 0, right: 5, top: 0, bottom: 5},
          },
        ],
      })
      .setChildren([
        {
          id: 1,
          name: 'occludedLayer',
          properties: {
            id: 1,
            name: 'occludedLayer',
            parent: -1,
            children: [],
            layerStack: 0,
            isOpaque: true,
            z: 0,
            screenBounds: {left: 0, top: 0, bottom: 1, right: 1},
            visibleRegion: {rect: [{left: 0, top: 0, bottom: 1, right: 1}]},
            activeBuffer: {width: 1, height: 1, stride: 1, format: 1},
            cornerRadius: 0,
            flags: 0,
            color: {r: 0, g: 0, b: 0, a: 1},
            shadowRadius: 0,
            backgroundBlurRadius: 0,
            transform: {
              type: 0,
              dsdx: 1,
              dtdx: 0,
              dsdy: 0,
              dtdy: 1,
            },
          } as android.surfaceflinger.ILayerProto,
        },
        {
          id: 2,
          name: 'occludingLayer',
          properties: {
            id: 2,
            name: 'occludingLayer',
            parent: -1,
            children: [],
            layerStack: 0,
            isOpaque: true,
            z: 1,
            screenBounds: {left: 0, top: 0, bottom: 2, right: 2},
            visibleRegion: {rect: [{left: 0, top: 0, bottom: 2, right: 2}]},
            activeBuffer: {width: 1, height: 1, stride: 1, format: 1},
            cornerRadius: 0,
            flags: 0,
            color: {r: 0, g: 0, b: 0, a: 1},
            shadowRadius: 0,
            backgroundBlurRadius: 0,
            transform: {
              type: 0,
              dsdx: 1,
              dtdx: 0,
              dsdy: 0,
              dtdy: 1,
            },
          } as android.surfaceflinger.ILayerProto,
        },
      ])
      .build();

    computation.setRoot(hierarchyRoot).executeInPlace();

    const invisibleLayer = assertDefined(
      hierarchyRoot.getChildByName('occludedLayer'),
    );
    const visibleLayer = assertDefined(
      hierarchyRoot.getChildByName('occludingLayer'),
    );

    expect(
      assertDefined(
        invisibleLayer.getEagerPropertyByName('isComputedVisible'),
      ).getValue(),
    ).toBeFalse();
    expect(
      invisibleLayer
        .getEagerPropertyByName('occludedBy')
        ?.getChildByName('0')
        ?.getValue(),
    ).toEqual('2 occludingLayer');
    expect(
      invisibleLayer.getEagerPropertyByName('partiallyOccludedBy')?.getValue(),
    ).toEqual([]);
    expect(
      invisibleLayer.getEagerPropertyByName('visibilityReason'),
    ).toBeUndefined();

    expect(
      assertDefined(
        visibleLayer.getEagerPropertyByName('isComputedVisible'),
      ).getValue(),
    ).toBeTrue();
    expect(
      visibleLayer.getEagerPropertyByName('occludedBy')?.getValue(),
    ).toEqual([]);
    expect(
      visibleLayer.getEagerPropertyByName('visibilityReason'),
    ).toBeUndefined();
  });

  it('distinguishes occluding and covering layers by alpha', () => {
    const hierarchyRoot = new HierarchyTreeBuilder()
      .setId('LayerTraceEntry')
      .setName('root')
      .setProperties({
        displays: [
          {
            layerStack: 0,
            layerStackSpaceRect: {left: 0, right: 5, top: 0, bottom: 5},
          },
        ],
      })
      .setChildren([
        {
          id: 1,
          name: 'occludedLayer',
          properties: {
            id: 1,
            name: 'occludedLayer',
            parent: -1,
            children: [],
            layerStack: 0,
            z: 0,
            screenBounds: {left: 0, top: 0, bottom: 1, right: 1},
            visibleRegion: {rect: [{left: 0, top: 0, bottom: 1, right: 1}]},
            activeBuffer: {width: 1, height: 1, stride: 1, format: 1},
            cornerRadius: 0,
            flags: 0,
            color: {r: 0, g: 0, b: 0, a: 1},
            isOpaque: true,
            shadowRadius: 0,
            backgroundBlurRadius: 0,
            transform: {
              type: 0,
              dsdx: 1,
              dtdx: 0,
              dsdy: 0,
              dtdy: 1,
            },
          } as android.surfaceflinger.ILayerProto,
        },
        {
          id: 2,
          name: 'occludingLayer',
          properties: {
            id: 2,
            name: 'occludingLayer',
            parent: -1,
            children: [],
            layerStack: 0,
            z: 1,
            screenBounds: {left: 0, top: 0, bottom: 2, right: 2},
            visibleRegion: {rect: [{left: 0, top: 0, bottom: 2, right: 2}]},
            activeBuffer: {width: 1, height: 1, stride: 1, format: 1},
            cornerRadius: 0,
            flags: 0,
            color: {r: 0, g: 0, b: 0, a: 1},
            isOpaque: true,
            shadowRadius: 0,
            backgroundBlurRadius: 0,
            transform: {
              type: 0,
              dsdx: 1,
              dtdx: 0,
              dsdy: 0,
              dtdy: 1,
            },
          } as android.surfaceflinger.ILayerProto,
        },
        {
          id: 3,
          name: 'coveringLayer',
          properties: {
            id: 3,
            name: 'coveringLayer',
            parent: -1,
            children: [],
            layerStack: 0,
            z: 2,
            screenBounds: {left: 0, top: 0, bottom: 2, right: 2},
            visibleRegion: {rect: [{left: 0, top: 0, bottom: 2, right: 2}]},
            activeBuffer: {width: 1, height: 1, stride: 1, format: 1},
            cornerRadius: 0,
            flags: 0,
            color: {r: 0, g: 0, b: 0, a: 0.5},
            isOpaque: true,
            shadowRadius: 0,
            backgroundBlurRadius: 0,
            transform: {
              type: 0,
              dsdx: 1,
              dtdx: 0,
              dsdy: 0,
              dtdy: 1,
            },
          } as android.surfaceflinger.ILayerProto,
        },
      ])
      .build();

    computation.setRoot(hierarchyRoot).executeInPlace();

    const occludedLayer = assertDefined(
      hierarchyRoot.getChildByName('occludedLayer'),
    );
    const occludingLayer = assertDefined(
      hierarchyRoot.getChildByName('occludingLayer'),
    );
    const coveringLayer = assertDefined(
      hierarchyRoot.getChildByName('coveringLayer'),
    );

    expect(
      assertDefined(
        occludedLayer.getEagerPropertyByName('isComputedVisible'),
      ).getValue(),
    ).toBeFalse();
    expect(
      occludedLayer.getEagerPropertyByName('occludedBy')?.getAllChildren()
        .length,
    ).toEqual(1);
    expect(
      occludedLayer
        .getEagerPropertyByName('occludedBy')
        ?.getChildByName('0')
        ?.getValue(),
    ).toEqual('2 occludingLayer');
    expect(
      coveringLayer.getEagerPropertyByName('partiallyOccludedBy')?.getValue(),
    ).toEqual([]);
    expect(
      occludedLayer.getEagerPropertyByName('coveredBy')?.getAllChildren()
        .length,
    ).toEqual(1);
    expect(
      occludedLayer
        .getEagerPropertyByName('coveredBy')
        ?.getChildByName('0')
        ?.getValue(),
    ).toEqual('3 coveringLayer');
    expect(
      occludedLayer.getEagerPropertyByName('visibilityReason'),
    ).toBeUndefined();

    expect(
      assertDefined(
        occludingLayer.getEagerPropertyByName('isComputedVisible'),
      ).getValue(),
    ).toBeTrue();
    expect(
      occludingLayer.getEagerPropertyByName('occludedBy')?.getValue(),
    ).toEqual([]);
    expect(
      coveringLayer.getEagerPropertyByName('partiallyOccludedBy')?.getValue(),
    ).toEqual([]);
    expect(
      occludingLayer.getEagerPropertyByName('coveredBy')?.getAllChildren()
        .length,
    ).toEqual(1);
    expect(
      occludingLayer
        .getEagerPropertyByName('coveredBy')
        ?.getChildByName('0')
        ?.getValue(),
    ).toEqual('3 coveringLayer');
    expect(
      occludingLayer.getEagerPropertyByName('visibilityReason'),
    ).toBeUndefined();

    expect(
      assertDefined(
        coveringLayer.getEagerPropertyByName('isComputedVisible'),
      ).getValue(),
    ).toBeTrue();
    expect(
      coveringLayer.getEagerPropertyByName('occludedBy')?.getValue(),
    ).toEqual([]);
    expect(
      coveringLayer.getEagerPropertyByName('partiallyOccludedBy')?.getValue(),
    ).toEqual([]);
    expect(
      coveringLayer.getEagerPropertyByName('coveredBy')?.getValue(),
    ).toEqual([]);
    expect(
      coveringLayer.getEagerPropertyByName('visibilityReason'),
    ).toBeUndefined();
  });

  it('adds partiallyOccludedBy layers and updates isVisible', () => {
    const hierarchyRoot = new HierarchyTreeBuilder()
      .setId('LayerTraceEntry')
      .setName('root')
      .setProperties({
        displays: [
          {
            layerStack: 0,
            layerStackSpaceRect: {left: 0, right: 5, top: 0, bottom: 5},
          },
        ],
      })
      .setChildren([
        {
          id: 1,
          name: 'partiallyOccludedLayer',
          properties: {
            id: 1,
            name: 'partiallyOccludedLayer',
            parent: -1,
            children: [],
            layerStack: 0,
            isOpaque: true,
            z: 0,
            screenBounds: {left: 0, top: 0, bottom: 2, right: 2},
            visibleRegion: {rect: [{left: 0, top: 0, bottom: 2, right: 2}]},
            activeBuffer: {width: 1, height: 1, stride: 1, format: 1},
            cornerRadius: 0,
            flags: 0,
            color: {r: 0, g: 0, b: 0, a: 1},
            shadowRadius: 0,
            backgroundBlurRadius: 0,
            transform: {
              type: 0,
              dsdx: 1,
              dtdx: 0,
              dsdy: 0,
              dtdy: 1,
            },
          } as android.surfaceflinger.ILayerProto,
        },
        {
          id: 2,
          name: 'partiallyOccludingLayer',
          properties: {
            id: 2,
            name: 'partiallyOccludingLayer',
            parent: -1,
            children: [],
            layerStack: 0,
            isOpaque: true,
            z: 1,
            screenBounds: {left: 0, top: 0, bottom: 1, right: 1},
            visibleRegion: {rect: [{left: 0, top: 0, bottom: 1, right: 1}]},
            activeBuffer: {width: 1, height: 1, stride: 1, format: 1},
            cornerRadius: 1,
            flags: 0,
            color: {r: 0, g: 0, b: 0, a: 1},
            shadowRadius: 0,
            backgroundBlurRadius: 0,
            transform: {
              type: 0,
              dsdx: 1,
              dtdx: 0,
              dsdy: 0,
              dtdy: 1,
            },
          } as android.surfaceflinger.ILayerProto,
        },
      ])
      .build();

    computation.setRoot(hierarchyRoot).executeInPlace();

    const partiallyVisibleLayer = assertDefined(
      hierarchyRoot.getChildByName('partiallyOccludedLayer'),
    );
    const visibleLayer = assertDefined(
      hierarchyRoot.getChildByName('partiallyOccludingLayer'),
    );

    expect(
      assertDefined(
        partiallyVisibleLayer.getEagerPropertyByName('isComputedVisible'),
      ).getValue(),
    ).toBeTrue();
    expect(
      partiallyVisibleLayer
        .getEagerPropertyByName('partiallyOccludedBy')
        ?.getChildByName('0')
        ?.getValue(),
    ).toEqual('2 partiallyOccludingLayer');
    expect(
      visibleLayer.getEagerPropertyByName('visibilityReason'),
    ).toBeUndefined();

    expect(
      assertDefined(
        visibleLayer.getEagerPropertyByName('isComputedVisible'),
      ).getValue(),
    ).toBeTrue();
    expect(
      visibleLayer.getEagerPropertyByName('partiallyOccludedBy')?.getValue(),
    ).toEqual([]);
    expect(
      visibleLayer.getEagerPropertyByName('visibilityReason'),
    ).toBeUndefined();
  });

  it('adds coveredByLayers layers and updates isVisible', () => {
    const hierarchyRoot = new HierarchyTreeBuilder()
      .setId('LayerTraceEntry')
      .setName('root')
      .setProperties({
        displays: [
          {
            layerStack: 0,
            layerStackSpaceRect: {left: 0, right: 5, top: 0, bottom: 5},
          },
        ],
      })
      .setChildren([
        {
          id: 1,
          name: 'coveredLayer',
          properties: {
            id: 1,
            name: 'coveredLayer',
            parent: -1,
            children: [],
            layerStack: 0,
            isOpaque: false,
            z: 0,
            screenBounds: {left: 0, top: 0, bottom: 2, right: 2},
            visibleRegion: {rect: [{left: 0, top: 0, bottom: 2, right: 2}]},
            activeBuffer: {width: 1, height: 1, stride: 1, format: 1},
            cornerRadius: 0,
            flags: 0,
            color: {r: 0, g: 0, b: 0, a: 1},
            shadowRadius: 0,
            backgroundBlurRadius: 0,
            transform: {
              type: 0,
              dsdx: 1,
              dtdx: 0,
              dsdy: 0,
              dtdy: 1,
            },
          } as android.surfaceflinger.ILayerProto,
        },
        {
          id: 2,
          name: 'coveringLayer',
          properties: {
            id: 2,
            name: 'coveringLayer',
            parent: -1,
            children: [],
            layerStack: 0,
            isOpaque: false,
            z: 1,
            screenBounds: {left: 0, top: 0, bottom: 1, right: 1},
            visibleRegion: {rect: [{left: 0, top: 0, bottom: 1, right: 1}]},
            activeBuffer: {width: 1, height: 1, stride: 1, format: 1},
            cornerRadius: 0,
            flags: 0,
            color: {r: 0, g: 0, b: 0, a: 1},
            shadowRadius: 0,
            backgroundBlurRadius: 0,
            transform: {
              type: 0,
              dsdx: 1,
              dtdx: 0,
              dsdy: 0,
              dtdy: 1,
            },
          } as android.surfaceflinger.ILayerProto,
        },
      ])
      .build();

    computation.setRoot(hierarchyRoot).executeInPlace();

    const coveredVisibleLayer = assertDefined(
      hierarchyRoot.getChildByName('coveredLayer'),
    );
    const visibleLayer = assertDefined(
      hierarchyRoot.getChildByName('coveringLayer'),
    );

    expect(
      assertDefined(
        coveredVisibleLayer.getEagerPropertyByName('isComputedVisible'),
      ).getValue(),
    ).toBeTrue();
    expect(
      coveredVisibleLayer
        .getEagerPropertyByName('coveredBy')
        ?.getChildByName('0')
        ?.getValue(),
    ).toEqual('2 coveringLayer');
    expect(
      visibleLayer.getEagerPropertyByName('visibilityReason'),
    ).toBeUndefined();

    expect(
      assertDefined(
        visibleLayer.getEagerPropertyByName('isComputedVisible'),
      ).getValue(),
    ).toBeTrue();
    expect(
      visibleLayer.getEagerPropertyByName('coveredBy')?.getValue(),
    ).toEqual([]);
    expect(
      visibleLayer.getEagerPropertyByName('visibilityReason'),
    ).toBeUndefined();
  });

  function getVisibilityReasons(layer: HierarchyTreeNode): string[] {
    return assertDefined(layer.getEagerPropertyByName('visibilityReason'))
      .getAllChildren()
      .map((child) => child.getValue());
  }
});
