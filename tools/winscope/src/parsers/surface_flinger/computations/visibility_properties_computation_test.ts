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
import {
  ChildHierarchy,
  HierarchyTreeBuilder,
} from 'test/unit/hierarchy_tree_builder';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {VisibilityPropertiesComputation} from './visibility_properties_computation';
import {ZOrderPathsComputation} from './z_order_paths_computation';

describe('VisibilityPropertiesComputation', () => {
  let computation: VisibilityPropertiesComputation;
  const rect1x1 = {left: 0, top: 0, bottom: 1, right: 1};
  const rect2x2 = {left: 0, top: 0, bottom: 2, right: 2};
  const rect5x5 = {left: 0, right: 5, top: 0, bottom: 5};

  const commonProperties: Proto = {
    cornerRadius: 0,
    shadowRadius: 0,
    backgroundBlurRadius: 0,
    transform: {
      type: 0,
      dsdx: 1,
      dtdx: 0,
      dsdy: 0,
      dtdy: 1,
    },
    excludesCompositionState: false,
  };

  const visibleLayerProperties: Proto = Object.assign(
    {
      flags: 0,
      activeBuffer: {width: 1, height: 1, stride: 1, format: 1},
      color: {r: 0, g: 0, b: 0, a: 1},
    },
    commonProperties,
  );

  beforeEach(() => {
    computation = new VisibilityPropertiesComputation();
  });

  it('throws error if root not set', () => {
    expect(() => computation.executeInPlace()).toThrowError();
  });

  it('detects visible layer due to non-empty visible region', () => {
    const hierarchyRoot = new HierarchyTreeBuilder()
      .setId('LayerTraceEntry')
      .setName('root')
      .setChildren([
        {
          id: 1,
          name: 'layerVisibleRegionNonEmpty',
          properties: Object.assign(
            {
              id: 1,
              name: 'layerVisibleRegionNonEmpty',
              parent: -1,
              children: [],
              visibleRegion: {rect: [rect1x1]},
              layerStack: 0,
              z: 0,
              screenBounds: null,
              isOpaque: false,
            } as android.surfaceflinger.ILayerProto,
            visibleLayerProperties,
          ),
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
        getChildHierarchyForHiddenLayer(1, 'layerHiddenByPolicy', []),
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
    expect(
      assertDefined(
        invisibleLayer.getEagerPropertyByName('isHiddenByPolicy'),
      ).getValue(),
    ).toBeTrue();
  });

  it('detects non-visible layer that is hidden by parent, even if rel-z parent is not hidden', () => {
    const hierarchyRoot = new HierarchyTreeBuilder()
      .setId('LayerTraceEntry')
      .setName('root')
      .setChildren([
        getChildHierarchyForHiddenLayer(1, 'parent', [
          {
            id: 2,
            name: 'childHiddenByParent',
            properties: Object.assign(
              {
                id: 2,
                name: 'childHiddenByParent',
                parent: 1,
                children: [],
                visibleRegion: {rect: [rect1x1]},
                layerStack: 0,
                z: 0,
                zOrderRelativeOf: 3,
                screenBounds: null,
                isOpaque: false,
              } as android.surfaceflinger.ILayerProto,
              visibleLayerProperties,
            ),
          },
        ]),
        {
          id: 3,
          name: 'relZParent',
          properties: Object.assign(
            {
              id: 3,
              name: 'relZParent',
              parent: -1,
              children: [],
              visibleRegion: {rect: [rect1x1]},
              layerStack: 0,
              z: 0,
              screenBounds: null,
              isOpaque: false,
            } as android.surfaceflinger.ILayerProto,
            visibleLayerProperties,
          ),
        },
      ])
      .build();
    new ZOrderPathsComputation().setRoot(hierarchyRoot).executeInPlace();

    computation.setRoot(hierarchyRoot).executeInPlace();
    const nonVisibleLayer = assertDefined(
      hierarchyRoot.getChildByName('parent'),
    );
    expect(
      assertDefined(
        nonVisibleLayer.getEagerPropertyByName('isComputedVisible'),
      ).getValue(),
    ).toBeFalse();
    expect(
      assertDefined(
        nonVisibleLayer.getEagerPropertyByName('isHiddenByPolicy'),
      ).getValue(),
    ).toBeTrue();

    const hiddenLayer = assertDefined(
      nonVisibleLayer.getChildByName('childHiddenByParent'),
    );
    expect(
      assertDefined(
        hiddenLayer.getEagerPropertyByName('isComputedVisible'),
      ).getValue(),
    ).toBeFalse();
    expect(getVisibilityReasons(hiddenLayer)).toEqual(['hidden by parent 1']);
    expect(
      assertDefined(
        hiddenLayer.getEagerPropertyByName('isHiddenByPolicy'),
      ).getValue(),
    ).toBeFalse();
  });

  it('detects non-visible layer due to zero alpha', () => {
    const hierarchyRoot = new HierarchyTreeBuilder()
      .setId('LayerTraceEntry')
      .setName('root')
      .setChildren([
        {
          id: 1,
          name: 'layerZeroAlpha',
          properties: Object.assign(
            {
              id: 1,
              name: 'layerZeroAlpha',
              parent: -1,
              children: [],
              visibleRegion: {rect: [rect1x1]},
              activeBuffer: {width: 1, height: 1, stride: 1, format: 1},
              flags: 0,
              color: {r: 0, g: 0, b: 0, a: 0},
              layerStack: 0,
              z: 0,
              screenBounds: null,
              isOpaque: false,
            } as android.surfaceflinger.ILayerProto,
            commonProperties,
          ),
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

  it('detects non-visible layer due to null, undefined or empty active buffer and no effects', () => {
    const hierarchyRoot = new HierarchyTreeBuilder()
      .setId('LayerTraceEntry')
      .setName('root')
      .setChildren([
        getChildHierarchyWithActiveBuffer(1, 'layerNullActiveBuffer', null),
        getChildHierarchyWithActiveBuffer(
          2,
          'layerMissingActiveBuffer',
          undefined,
        ),
        getChildHierarchyWithActiveBuffer(3, 'layerEmptyActiveBuffer', {
          width: 0,
          height: 0,
          stride: 0,
          format: 0,
        }),
      ])
      .build();

    computation.setRoot(hierarchyRoot).executeInPlace();
    checkInvisibleDueToActiveBuffer(hierarchyRoot, 'layerNullActiveBuffer');
    checkInvisibleDueToActiveBuffer(hierarchyRoot, 'layerMissingActiveBuffer');
    checkInvisibleDueToActiveBuffer(hierarchyRoot, 'layerEmptyActiveBuffer');
  });

  it('detects non-visible layer due to empty bounds', () => {
    const properties = Object.assign(
      {
        id: 1,
        name: 'layerEmptyBounds',
        parent: -1,
        children: [],
        visibleRegion: {rect: [rect1x1]},
        bounds: {left: 0, right: 0, top: 0, bottom: 0},
        layerStack: 0,
        z: 0,
        screenBounds: null,
        isOpaque: false,
      },
      visibleLayerProperties,
    );
    properties['excludesCompositionState'] = true;
    const hierarchyRoot = new HierarchyTreeBuilder()
      .setId('LayerTraceEntry')
      .setName('root')
      .setChildren([
        {
          id: 1,
          name: 'layerEmptyBounds',
          properties,
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
          properties: Object.assign(
            {
              id: 1,
              name: 'layerNoVisibleRegion',
              parent: -1,
              children: [],
              visibleRegion: null,
              layerStack: 0,
              z: 0,
              screenBounds: null,
              isOpaque: false,
            } as android.surfaceflinger.ILayerProto,
            visibleLayerProperties,
          ),
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
          properties: Object.assign(
            {
              id: 1,
              name: 'layerEmptyVisibleRegion',
              parent: -1,
              children: [],
              visibleRegion: {rect: [{left: 0, right: 1, top: 0, bottom: 0}]},
              layerStack: 0,
              z: 0,
              screenBounds: null,
              isOpaque: false,
            } as android.surfaceflinger.ILayerProto,
            visibleLayerProperties,
          ),
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

  it('adds occludedBy layers and updates isVisible and visibilityReason', () => {
    const hierarchyRoot = new HierarchyTreeBuilder()
      .setId('LayerTraceEntry')
      .setName('root')
      .setProperties({
        displays: [
          {
            layerStack: 0,
            layerStackSpaceRect: rect5x5,
          },
        ],
      })
      .setChildren([
        getChildHierarchyForOpaqueLayer(1, 'occludedLayer', 0, rect1x1),
        getChildHierarchyForOpaqueLayer(2, 'occludingLayer', 1, rect2x2),
      ])
      .build();

    computation.setRoot(hierarchyRoot).executeInPlace();

    const invisibleLayer = assertDefined(
      hierarchyRoot.getChildByName('occludedLayer'),
    );
    const visibleLayer = assertDefined(
      hierarchyRoot.getChildByName('occludingLayer'),
    );

    checkOccludedLayer(invisibleLayer, ['2 occludingLayer'], []);
    checkVisibleLayer(visibleLayer, [], []);
  });

  it('distinguishes occluding and covering layers by alpha', () => {
    const hierarchyRoot = new HierarchyTreeBuilder()
      .setId('LayerTraceEntry')
      .setName('root')
      .setProperties({
        displays: [
          {
            layerStack: 0,
            layerStackSpaceRect: rect5x5,
          },
        ],
      })
      .setChildren([
        getChildHierarchyForOpaqueLayer(1, 'occludedLayer', 0, rect1x1),
        getChildHierarchyForOpaqueLayer(2, 'occludingLayer', 1, rect2x2),
        {
          id: 3,
          name: 'coveringLayer',
          properties: Object.assign(
            {
              id: 3,
              name: 'coveringLayer',
              parent: -1,
              children: [],
              layerStack: 0,
              z: 2,
              screenBounds: rect2x2,
              visibleRegion: {rect: [rect2x2]},
              activeBuffer: {width: 1, height: 1, stride: 1, format: 1},
              flags: 0,
              color: {r: 0, g: 0, b: 0, a: 0.5},
              isOpaque: true,
            } as android.surfaceflinger.ILayerProto,
            commonProperties,
          ),
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

    checkOccludedLayer(
      occludedLayer,
      ['2 occludingLayer'],
      ['3 coveringLayer'],
    );
    checkVisibleLayer(occludingLayer, [], ['3 coveringLayer']);
    checkVisibleLayer(coveringLayer, [], []);
  });

  it('accounts for layer stack in occlusion calculations', () => {
    const hierarchyRoot = new HierarchyTreeBuilder()
      .setId('LayerTraceEntry')
      .setName('root')
      .setProperties({
        displays: [
          {
            layerStack: 0,
            layerStackSpaceRect: rect5x5,
          },
          {
            layerStack: 1,
            layerStackSpaceRect: rect5x5,
          },
        ],
      })
      .setChildren([
        getChildHierarchyForOpaqueLayer(1, 'visibleLayer1', 0, rect1x1, 0),
        getChildHierarchyForOpaqueLayer(2, 'visibleLayer2', 0, rect1x1, 1),
      ])
      .build();

    computation.setRoot(hierarchyRoot).executeInPlace();

    const visibleLayer1 = assertDefined(
      hierarchyRoot.getChildByName('visibleLayer1'),
    );
    const visibleLayer2 = assertDefined(
      hierarchyRoot.getChildByName('visibleLayer2'),
    );

    checkVisibleLayer(visibleLayer1, [], []);
    checkVisibleLayer(visibleLayer2, [], []);
  });

  it('adds partiallyOccludedBy layers and updates isVisible', () => {
    const hierarchyRoot = new HierarchyTreeBuilder()
      .setId('LayerTraceEntry')
      .setName('root')
      .setProperties({
        displays: [
          {
            layerStack: 0,
            layerStackSpaceRect: rect5x5,
          },
        ],
      })
      .setChildren([
        getChildHierarchyForOpaqueLayer(
          1,
          'partiallyOccludedLayer',
          0,
          rect2x2,
        ),
        getChildHierarchyForOpaqueLayer(
          2,
          'partiallyOccludingLayer',
          1,
          rect1x1,
        ),
        getChildHierarchyForOpaqueLayer(3, 'nonOccludingLayer', 1, {
          left: 3,
          top: 3,
          bottom: 4,
          right: 4,
        }),
      ])
      .build();

    computation.setRoot(hierarchyRoot).executeInPlace();

    const partiallyVisibleLayer = assertDefined(
      hierarchyRoot.getChildByName('partiallyOccludedLayer'),
    );
    const visibleLayer = assertDefined(
      hierarchyRoot.getChildByName('partiallyOccludingLayer'),
    );
    const nonOccludingLayer = assertDefined(
      hierarchyRoot.getChildByName('nonOccludingLayer'),
    );

    checkVisibleLayer(partiallyVisibleLayer, ['2 partiallyOccludingLayer'], []);
    checkVisibleLayer(visibleLayer, [], []);
    checkVisibleLayer(nonOccludingLayer, [], []);
  });

  it('adds coveredByLayers layers and updates isVisible', () => {
    const hierarchyRoot = new HierarchyTreeBuilder()
      .setId('LayerTraceEntry')
      .setName('root')
      .setProperties({
        displays: [
          {
            layerStack: 0,
            layerStackSpaceRect: rect5x5,
          },
        ],
      })
      .setChildren([
        getChildHierarchyForTranslucentLayer(1, 'coveredLayer', 0, rect2x2),
        getChildHierarchyForTranslucentLayer(2, 'coveringLayer', 1, rect1x1),
      ])
      .build();

    computation.setRoot(hierarchyRoot).executeInPlace();

    const coveredVisibleLayer = assertDefined(
      hierarchyRoot.getChildByName('coveredLayer'),
    );
    const visibleLayer = assertDefined(
      hierarchyRoot.getChildByName('coveringLayer'),
    );

    checkVisibleLayer(coveredVisibleLayer, [], ['2 coveringLayer']);
    checkVisibleLayer(visibleLayer, [], []);
  });

  it('computes occlusion state based on z order', () => {
    const hierarchyRoot = new HierarchyTreeBuilder()
      .setId('LayerTraceEntry')
      .setName('root')
      .setProperties({
        displays: [
          {
            layerStack: 0,
            layerStackSpaceRect: rect5x5,
          },
        ],
      })
      .setChildren([
        getChildHierarchyForOpaqueLayer(1, 'occludingLayer', 1, rect2x2),
        getChildHierarchyForOpaqueLayer(2, 'occludedLayer', 0, rect1x1),
      ])
      .build();

    computation.setRoot(hierarchyRoot).executeInPlace();

    const invisibleLayer = assertDefined(
      hierarchyRoot.getChildByName('occludedLayer'),
    );
    const visibleLayer = assertDefined(
      hierarchyRoot.getChildByName('occludingLayer'),
    );

    checkOccludedLayer(invisibleLayer, ['1 occludingLayer'], []);
    checkVisibleLayer(visibleLayer, [], []);
  });

  function getChildHierarchyForHiddenLayer(
    id: number,
    name: string,
    children: ChildHierarchy[],
  ): ChildHierarchy {
    return {
      id,
      name,
      properties: Object.assign(
        {
          id,
          name,
          parent: -1,
          children: children.map((c) => c.id),
          flags: LayerFlag.HIDDEN,
          visibleRegion: {rect: [rect1x1]},
          activeBuffer: {width: 1, height: 1, stride: 1, format: 1},
          color: {r: 0, g: 0, b: 0, a: 1},
          layerStack: 0,
          z: 0,
          screenBounds: null,
          isOpaque: false,
        } as android.surfaceflinger.ILayerProto,
        commonProperties,
      ),
      children,
    };
  }

  function getChildHierarchyWithActiveBuffer(
    id: number,
    name: string,
    activeBuffer: android.surfaceflinger.IActiveBufferProto | null | undefined,
  ): ChildHierarchy {
    return {
      id,
      name,
      properties: Object.assign(
        {
          id,
          name,
          parent: -1,
          children: [],
          visibleRegion: {rect: [rect1x1]},
          flags: 0,
          color: {r: 0, g: 0, b: 0, a: 0},
          activeBuffer,
          layerStack: 0,
          z: 0,
          screenBounds: null,
          isOpaque: false,
        } as android.surfaceflinger.ILayerProto,
        commonProperties,
      ),
    };
  }

  function checkInvisibleDueToActiveBuffer(
    hierarchyRoot: HierarchyTreeNode,
    layerName: string,
  ) {
    const layer = assertDefined(hierarchyRoot.getChildByName(layerName));
    expect(
      assertDefined(
        layer.getEagerPropertyByName('isComputedVisible'),
      ).getValue(),
    ).toBeFalse();
    expect(getVisibilityReasons(layer)).toEqual([
      'buffer is empty',
      'alpha is 0',
      'does not have color fill, shadow or blur',
    ]);
  }

  function getChildHierarchyForOpaqueLayer(
    id: number,
    name: string,
    z: number,
    bounds: android.surfaceflinger.IRectProto,
    layerStack = 0,
  ) {
    return {
      id,
      name,
      properties: Object.assign(
        {
          id,
          name,
          parent: -1,
          children: [],
          layerStack,
          isOpaque: true,
          z,
          screenBounds: bounds,
          visibleRegion: {rect: [bounds]},
        } as android.surfaceflinger.ILayerProto,
        visibleLayerProperties,
      ),
    };
  }

  function getChildHierarchyForTranslucentLayer(
    id: number,
    name: string,
    z: number,
    bounds: android.surfaceflinger.IRectProto,
    layerStack = 0,
  ) {
    return {
      id,
      name,
      properties: Object.assign(
        {
          id,
          name,
          parent: -1,
          children: [],
          layerStack,
          isOpaque: false,
          z,
          screenBounds: bounds,
          visibleRegion: {rect: [bounds]},
        } as android.surfaceflinger.ILayerProto,
        visibleLayerProperties,
      ),
    };
  }

  function getVisibilityReasons(layer: HierarchyTreeNode): string[] {
    return assertDefined(layer.getEagerPropertyByName('visibilityReason'))
      .getAllChildren()
      .map((child) => child.getValue());
  }

  function checkOccludedLayer(
    layer: HierarchyTreeNode,
    occludedBy: string[],
    coveredBy: string[],
  ) {
    expect(
      assertDefined(
        layer.getEagerPropertyByName('isComputedVisible'),
      ).getValue(),
    ).toBeFalse();
    expect(getVisibilityReasons(layer)).toEqual(['occluded']);
    expect(
      layer
        .getEagerPropertyByName('occludedBy')
        ?.getAllChildren()
        .map((c) => c.getValue()),
    ).toEqual(occludedBy);
    expect(
      layer.getEagerPropertyByName('partiallyOccludedBy')?.getValue(),
    ).toEqual([]);
    expect(
      layer
        .getEagerPropertyByName('coveredBy')
        ?.getAllChildren()
        .map((c) => c.getValue()),
    ).toEqual(coveredBy);
  }

  function checkVisibleLayer(
    layer: HierarchyTreeNode,
    partiallyOccludedBy: string[],
    coveredBy: string[],
  ) {
    expect(
      assertDefined(
        layer.getEagerPropertyByName('isComputedVisible'),
      ).getValue(),
    ).toBeTrue();
    expect(layer.getEagerPropertyByName('occludedBy')?.getValue()).toEqual([]);
    expect(layer.getEagerPropertyByName('visibilityReason')).toBeUndefined();
    expect(
      layer
        .getEagerPropertyByName('partiallyOccludedBy')
        ?.getAllChildren()
        .map((c) => c.getValue()),
    ).toEqual(partiallyOccludedBy);
    expect(
      layer
        .getEagerPropertyByName('coveredBy')
        ?.getAllChildren()
        .map((c) => c.getValue()),
    ).toEqual(coveredBy);
  }
});

interface Proto {
  [key: string]: number | string | boolean | null | Proto;
}
