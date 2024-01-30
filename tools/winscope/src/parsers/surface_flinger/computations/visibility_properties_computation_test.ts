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
import {TreeNodeUtils} from 'test/unit/tree_node_utils';
import {Item} from 'trace/item';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {VisibilityPropertiesComputation} from './visibility_properties_computation';

describe('VisibilityPropertiesComputation', () => {
  let hierarchyRoot: HierarchyTreeNode;
  let computation: VisibilityPropertiesComputation;
  let displays: PropertyTreeNode;

  beforeEach(() => {
    hierarchyRoot = TreeNodeUtils.makeHierarchyNode({id: 'LayerTraceEntry', name: 'root'} as Item);
    computation = new VisibilityPropertiesComputation();
    displays = TreeNodeUtils.makePropertyNode(hierarchyRoot.id, 'displays', [
      {
        layerStack: 0,
        layerStackSpaceRect: {left: 0, right: 5, top: 0, bottom: 5},
      },
    ] as android.surfaceflinger.IDisplayProto[]);
  });

  it('detects visible layer due to non-empty visible region', () => {
    const layerVisibleRegionNonEmpty = TreeNodeUtils.makeHierarchyNode({
      id: 1,
      name: 'layerVisibleRegionNonEmpty',
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
    } as android.surfaceflinger.ILayerProto);
    hierarchyRoot.addOrReplaceChild(layerVisibleRegionNonEmpty);

    computation.setRoot(hierarchyRoot).executeInPlace();
    const visibleLayer = assertDefined(hierarchyRoot.getChildByName('layerVisibleRegionNonEmpty'));
    expect(
      assertDefined(visibleLayer.getEagerPropertyByName('isComputedVisible')).getValue()
    ).toBeTrue();
  });

  it('detects non-visible layer that is hidden by policy or parent', () => {
    const layerHiddenByPolicy = TreeNodeUtils.makeHierarchyNode({
      id: 1,
      name: 'layerHiddenByPolicy',
      parent: -1,
      children: [],
      flags: 0x01,
      visibleRegion: {rect: [{left: 0, right: 1, top: 0, bottom: 1}]},
      activeBuffer: {width: 1, height: 1, stride: 1, format: 1},
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
    } as android.surfaceflinger.ILayerProto);
    hierarchyRoot.addOrReplaceChild(layerHiddenByPolicy);

    computation.setRoot(hierarchyRoot).executeInPlace();
    const invisibleLayer = assertDefined(hierarchyRoot.getChildByName('layerHiddenByPolicy'));
    expect(
      assertDefined(invisibleLayer.getEagerPropertyByName('isComputedVisible')).getValue()
    ).toBeFalse();
  });

  it('detects non-visible layer that is hidden by parent', () => {
    const parent = TreeNodeUtils.makeHierarchyNode({
      id: 1,
      name: 'parent',
      parent: -1,
      children: [2],
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
    } as android.surfaceflinger.ILayerProto);
    const childHiddenByParent = TreeNodeUtils.makeHierarchyNode({
      id: 2,
      name: 'childHiddenByParent',
      parent: 1,
      children: [],
      flags: 0x01,
      visibleRegion: {rect: [{left: 0, right: 1, top: 0, bottom: 1}]},
      activeBuffer: {width: 1, height: 1, stride: 1, format: 1},
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
    } as android.surfaceflinger.ILayerProto);
    parent.addOrReplaceChild(childHiddenByParent);
    hierarchyRoot.addOrReplaceChild(parent);

    computation.setRoot(hierarchyRoot).executeInPlace();
    const visibleLayer = assertDefined(hierarchyRoot.getChildByName('parent'));
    expect(
      assertDefined(visibleLayer.getEagerPropertyByName('isComputedVisible')).getValue()
    ).toBeTrue();

    const hiddenLayer = assertDefined(visibleLayer.getChildByName('childHiddenByParent'));
    expect(
      assertDefined(hiddenLayer.getEagerPropertyByName('isComputedVisible')).getValue()
    ).toBeFalse();
  });

  it('detects non-visible layer due to null active buffer', () => {
    const layerNoActiveBuffer = TreeNodeUtils.makeHierarchyNode({
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
    } as android.surfaceflinger.ILayerProto);
    hierarchyRoot.addOrReplaceChild(layerNoActiveBuffer);

    computation.setRoot(hierarchyRoot).executeInPlace();
    const invisibleLayer = assertDefined(hierarchyRoot.getChildByName('layerNoActiveBuffer'));
    expect(
      assertDefined(invisibleLayer.getEagerPropertyByName('isComputedVisible')).getValue()
    ).toBeFalse();
  });

  it('detects non-visible layer due to empty bounds', () => {
    const layerEmptyBounds = TreeNodeUtils.makeHierarchyNode({
      id: 1,
      name: 'layerEmptyBounds',
      parent: -1,
      children: [],
      activeBuffer: {width: 1, height: 1, stride: 1, format: 1},
      visibleRegion: {rect: [{left: 0, right: 1, top: 0, bottom: 1}]},
      bounds: {left: 0, right: 0, top: 0, bottom: 0},
      excludesCompositionState: true,
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
    } as android.surfaceflinger.ILayerProto);
    hierarchyRoot.addOrReplaceChild(layerEmptyBounds);

    computation.setRoot(hierarchyRoot).executeInPlace();
    const invisibleLayer = assertDefined(hierarchyRoot.getChildByName('layerEmptyBounds'));
    expect(
      assertDefined(invisibleLayer.getEagerPropertyByName('isComputedVisible')).getValue()
    ).toBeFalse();
  });

  it('detects non-visible layer due to no visible region', () => {
    const layerNoVisibleRegion = TreeNodeUtils.makeHierarchyNode({
      id: 1,
      name: 'layerNoVisibleRegion',
      parent: -1,
      children: [],
      activeBuffer: {width: 1, height: 1, stride: 1, format: 1},
      visibleRegion: null,
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
    } as android.surfaceflinger.ILayerProto);
    hierarchyRoot.addOrReplaceChild(layerNoVisibleRegion);

    computation.setRoot(hierarchyRoot).executeInPlace();
    const invisibleLayer = assertDefined(hierarchyRoot.getChildByName('layerNoVisibleRegion'));
    expect(
      assertDefined(invisibleLayer.getEagerPropertyByName('isComputedVisible')).getValue()
    ).toBeFalse();
  });

  it('detects non-visible layer due to empty visible region', () => {
    const layerEmptyVisibleRegion = TreeNodeUtils.makeHierarchyNode({
      id: 1,
      name: 'layerEmptyVisibleRegion',
      parent: -1,
      children: [],
      visibleRegion: {rect: [{left: 0, right: 1, top: 0, bottom: 0}]},
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
    } as android.surfaceflinger.ILayerProto);
    hierarchyRoot.addOrReplaceChild(layerEmptyVisibleRegion);

    computation.setRoot(hierarchyRoot).executeInPlace();
    const invisibleLayer = assertDefined(hierarchyRoot.getChildByName('layerEmptyVisibleRegion'));
    expect(
      assertDefined(invisibleLayer.getEagerPropertyByName('isComputedVisible')).getValue()
    ).toBeFalse();
  });

  it('adds occludedBy layers and updates isVisible', () => {
    const occludedLayer = TreeNodeUtils.makeHierarchyNode({
      id: 1,
      name: 'occludedLayer',
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
      color: {r: 0, g: 0, b: 0, a: 0},
      shadowRadius: 0,
      backgroundBlurRadius: 0,
      transform: {
        type: 0,
        dsdx: 1,
        dtdx: 0,
        dsdy: 0,
        dtdy: 1,
      },
    } as android.surfaceflinger.ILayerProto);
    const occludingLayer = TreeNodeUtils.makeHierarchyNode({
      id: 2,
      name: 'occludingLayer',
      parent: -1,
      children: [],
      layerStack: 0,
      isOpaque: true,
      z: 1,
      screenBounds: {left: 0, top: 0, bottom: 1, right: 1},
      visibleRegion: {rect: [{left: 0, top: 0, bottom: 1, right: 1}]},
      activeBuffer: {width: 1, height: 1, stride: 1, format: 1},
      cornerRadius: 0,
      flags: 0,
      color: {r: 0, g: 0, b: 0, a: 0},
      shadowRadius: 0,
      backgroundBlurRadius: 0,
      transform: {
        type: 0,
        dsdx: 1,
        dtdx: 0,
        dsdy: 0,
        dtdy: 1,
      },
    } as android.surfaceflinger.ILayerProto);
    hierarchyRoot.addOrReplaceChild(occludingLayer);
    hierarchyRoot.addOrReplaceChild(occludedLayer);
    hierarchyRoot.addEagerProperty(displays);
    computation.setRoot(hierarchyRoot).executeInPlace();

    const invisibleLayer = assertDefined(hierarchyRoot.getChildByName('occludedLayer'));
    const visibleLayer = assertDefined(hierarchyRoot.getChildByName('occludingLayer'));

    expect(
      assertDefined(invisibleLayer.getEagerPropertyByName('isComputedVisible')).getValue()
    ).toBeFalse();
    expect(
      invisibleLayer.getEagerPropertyByName('occludedBy')?.getChildByName('0')?.getValue()
    ).toEqual(2);

    expect(
      assertDefined(visibleLayer.getEagerPropertyByName('isComputedVisible')).getValue()
    ).toBeTrue();
    expect(visibleLayer.getEagerPropertyByName('occludedBy')?.getValue()).toEqual([]);
  });

  it('adds partiallyOccludedBy layers and updates isVisible', () => {
    const partiallyOccludedLayer = TreeNodeUtils.makeHierarchyNode({
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
      color: {r: 0, g: 0, b: 0, a: 0},
      shadowRadius: 0,
      backgroundBlurRadius: 0,
      transform: {
        type: 0,
        dsdx: 1,
        dtdx: 0,
        dsdy: 0,
        dtdy: 1,
      },
    } as android.surfaceflinger.ILayerProto);
    const partiallyOccludingLayer = TreeNodeUtils.makeHierarchyNode({
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
      color: {r: 0, g: 0, b: 0, a: 0},
      shadowRadius: 0,
      backgroundBlurRadius: 0,
      transform: {
        type: 0,
        dsdx: 1,
        dtdx: 0,
        dsdy: 0,
        dtdy: 1,
      },
    } as android.surfaceflinger.ILayerProto);
    hierarchyRoot.addOrReplaceChild(partiallyOccludingLayer);
    hierarchyRoot.addOrReplaceChild(partiallyOccludedLayer);
    hierarchyRoot.addEagerProperty(displays);
    computation.setRoot(hierarchyRoot).executeInPlace();

    const partiallyVisibleLayer = assertDefined(
      hierarchyRoot.getChildByName('partiallyOccludedLayer')
    );
    const visibleLayer = assertDefined(hierarchyRoot.getChildByName('partiallyOccludingLayer'));

    expect(
      assertDefined(partiallyVisibleLayer.getEagerPropertyByName('isComputedVisible')).getValue()
    ).toBeTrue();
    expect(
      partiallyVisibleLayer
        .getEagerPropertyByName('partiallyOccludedBy')
        ?.getChildByName('0')
        ?.getValue()
    ).toEqual(2);

    expect(
      assertDefined(visibleLayer.getEagerPropertyByName('isComputedVisible')).getValue()
    ).toBeTrue();
    expect(visibleLayer.getEagerPropertyByName('partiallyOccludedBy')?.getValue()).toEqual([]);
  });

  it('adds coveredByLayers layers and updates isVisible', () => {
    const coveredLayer = TreeNodeUtils.makeHierarchyNode({
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
      color: {r: 0, g: 0, b: 0, a: 0},
      shadowRadius: 0,
      backgroundBlurRadius: 0,
      transform: {
        type: 0,
        dsdx: 1,
        dtdx: 0,
        dsdy: 0,
        dtdy: 1,
      },
    } as android.surfaceflinger.ILayerProto);
    const coveringLayer = TreeNodeUtils.makeHierarchyNode({
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
      color: {r: 0, g: 0, b: 0, a: 0},
      shadowRadius: 0,
      backgroundBlurRadius: 0,
      transform: {
        type: 0,
        dsdx: 1,
        dtdx: 0,
        dsdy: 0,
        dtdy: 1,
      },
    } as android.surfaceflinger.ILayerProto);
    hierarchyRoot.addOrReplaceChild(coveringLayer);
    hierarchyRoot.addOrReplaceChild(coveredLayer);
    hierarchyRoot.addEagerProperty(displays);
    computation.setRoot(hierarchyRoot).executeInPlace();

    const coveredVisibleLayer = assertDefined(hierarchyRoot.getChildByName('coveredLayer'));
    const visibleLayer = assertDefined(hierarchyRoot.getChildByName('coveringLayer'));

    expect(
      assertDefined(coveredVisibleLayer.getEagerPropertyByName('isComputedVisible')).getValue()
    ).toBeTrue();
    expect(
      coveredVisibleLayer.getEagerPropertyByName('coveredBy')?.getChildByName('0')?.getValue()
    ).toEqual(2);

    expect(
      assertDefined(visibleLayer.getEagerPropertyByName('isComputedVisible')).getValue()
    ).toBeTrue();
    expect(visibleLayer.getEagerPropertyByName('coveredBy')?.getValue()).toEqual([]);
  });
});
