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
import {Rect} from 'common/geometry/rect';
import {RawDataUtils} from 'parsers/raw_data_utils';
import {LayerFlag} from 'parsers/surface_flinger/layer_flag';
import {
  Transform,
  TransformType,
} from 'parsers/surface_flinger/transform_utils';
import {GeometryFactory} from 'trace/geometry_factory';
import {Computation} from 'trace/tree_node/computation';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {DEFAULT_PROPERTY_TREE_NODE_FACTORY} from 'trace/tree_node/property_tree_node_factory';
import {compareByZOrderPath} from './compare_by_z_order_path';

export class VisibilityPropertiesComputation implements Computation {
  private root: HierarchyTreeNode | undefined;
  private rootLayers: HierarchyTreeNode[] | undefined;
  private displays: PropertyTreeNode[] = [];
  private static readonly OFFSCREEN_LAYER_ROOT_ID = 0x7ffffffd;

  setRoot(value: HierarchyTreeNode): VisibilityPropertiesComputation {
    this.root = value;
    this.rootLayers = value.getAllChildren().slice();
    return this;
  }

  executeInPlace(): void {
    if (!this.root || !this.rootLayers) {
      throw new Error('root not set in SF visibility computation');
    }

    this.displays =
      this.root.getEagerPropertyByName('displays')?.getAllChildren().slice() ??
      [];

    const sortedLayers = this.rootLayers.sort(compareByZOrderPath);

    const layersOrderedByZ = sortedLayers.flatMap((layer) => {
      return this.layerTopDownTraversal(layer);
    });

    const opaqueLayers: HierarchyTreeNode[] = [];
    const translucentLayers: HierarchyTreeNode[] = [];

    for (const layer of layersOrderedByZ) {
      let isVisible = this.getIsVisible(layer);
      if (!isVisible) {
        layer.addEagerProperty(
          DEFAULT_PROPERTY_TREE_NODE_FACTORY.makeCalculatedProperty(
            layer.id,
            'isComputedVisible',
            isVisible,
          ),
        );
        layer.addEagerProperty(
          DEFAULT_PROPERTY_TREE_NODE_FACTORY.makeCalculatedProperty(
            layer.id,
            'isHiddenByPolicy',
            this.isHiddenByPolicy(layer),
          ),
        );
      } else {
        const displaySize = this.getDisplaySize(layer);

        const occludedBy = opaqueLayers
          .filter((other) => {
            if (
              this.getDefinedValue(other, 'layerStack') !==
              this.getDefinedValue(layer, 'layerStack')
            ) {
              return false;
            }
            if (!this.layerContains(other, layer, displaySize)) {
              return false;
            }
            const cornerRadiusOther =
              other.getEagerPropertyByName('cornerRadius')?.getValue() ?? 0;

            return (
              cornerRadiusOther <= 0 ||
              (cornerRadiusOther ===
                layer.getEagerPropertyByName('cornerRadius')?.getValue() ??
                0)
            );
          })
          .map((other) => other.id);

        if (occludedBy.length > 0) {
          isVisible = false;
        }

        layer.addEagerProperty(
          DEFAULT_PROPERTY_TREE_NODE_FACTORY.makeCalculatedProperty(
            layer.id,
            'isComputedVisible',
            isVisible,
          ),
        );
        layer.addEagerProperty(
          DEFAULT_PROPERTY_TREE_NODE_FACTORY.makeCalculatedProperty(
            layer.id,
            'occludedBy',
            occludedBy,
          ),
        );

        const partiallyOccludedBy = opaqueLayers
          .filter((other) => {
            if (
              this.getDefinedValue(other, 'layerStack') !==
              this.getDefinedValue(layer, 'layerStack')
            ) {
              return false;
            }
            if (!this.layerOverlaps(other, layer, displaySize)) {
              return false;
            }
            return !occludedBy.includes(other.id);
          })
          .map((other) => other.id);

        layer.addEagerProperty(
          DEFAULT_PROPERTY_TREE_NODE_FACTORY.makeCalculatedProperty(
            layer.id,
            'partiallyOccludedBy',
            partiallyOccludedBy,
          ),
        );

        const coveredBy = translucentLayers
          .filter((other) => {
            if (
              this.getDefinedValue(other, 'layerStack') !==
              this.getDefinedValue(layer, 'layerStack')
            ) {
              return false;
            }
            return this.layerOverlaps(other, layer, displaySize);
          })
          .map((other) => other.id);

        layer.addEagerProperty(
          DEFAULT_PROPERTY_TREE_NODE_FACTORY.makeCalculatedProperty(
            layer.id,
            'coveredBy',
            coveredBy,
          ),
        );

        this.isOpaque(layer)
          ? opaqueLayers.push(layer)
          : translucentLayers.push(layer);
      }

      if (!isVisible) {
        layer.addEagerProperty(
          DEFAULT_PROPERTY_TREE_NODE_FACTORY.makeCalculatedProperty(
            layer.id,
            'visibilityReason',
            this.getVisibilityReasons(layer),
          ),
        );
      }
    }
  }

  private getIsVisible(layer: HierarchyTreeNode): boolean {
    if (this.isHiddenByParent(layer) || this.isHiddenByPolicy(layer)) {
      return false;
    }
    if (this.hasZeroAlpha(layer)) {
      return false;
    }
    if (
      this.isActiveBufferEmpty(layer.getEagerPropertyByName('activeBuffer')) &&
      !this.hasEffects(layer)
    ) {
      return false;
    }
    return this.hasVisibleRegion(layer);
  }

  private hasVisibleRegion(layer: HierarchyTreeNode): boolean {
    let hasVisibleRegion = false;
    if (layer.getEagerPropertyByName('excludesCompositionState')?.getValue()) {
      // Doesn't include state sent during composition like visible region and
      // composition type, so we fallback on the bounds as the visible region
      const bounds = layer.getEagerPropertyByName('bounds');
      hasVisibleRegion =
        bounds !== undefined && !RawDataUtils.isEmptyObj(bounds);
    } else {
      const visibleRegion = layer.getEagerPropertyByName('visibleRegion');
      if (
        visibleRegion === undefined ||
        visibleRegion.getAllChildren().length === 0
      ) {
        hasVisibleRegion = false;
      } else {
        hasVisibleRegion = !this.hasValidEmptyVisibleRegion(visibleRegion);
      }
    }
    return hasVisibleRegion;
  }

  private hasValidEmptyVisibleRegion(visibleRegion: PropertyTreeNode): boolean {
    const visibleRegionRectsNode = visibleRegion.getChildByName('rect');
    if (!visibleRegionRectsNode) return false;

    const rects = visibleRegionRectsNode.getAllChildren();
    return rects.every((node) => {
      return RawDataUtils.isEmptyObj(node);
    });
  }

  private getVisibilityReasons(layer: HierarchyTreeNode): string[] {
    const reasons: string[] = [];

    if (this.isHiddenByPolicy(layer)) reasons.push('flag is hidden');

    if (this.isHiddenByParent(layer)) {
      reasons.push(`hidden by parent ${this.getDefinedValue(layer, 'parent')}`);
    }

    if (
      this.isActiveBufferEmpty(layer.getEagerPropertyByName('activeBuffer'))
    ) {
      reasons.push('buffer is empty');
    }

    if (this.hasZeroAlpha(layer)) {
      reasons.push('alpha is 0');
    }

    const bounds = layer.getEagerPropertyByName('bounds');
    if (bounds && RawDataUtils.isEmptyObj(bounds)) {
      reasons.push('bounds is 0x0');
    }

    const color = this.getColor(layer);
    if (
      color &&
      bounds &&
      RawDataUtils.isEmptyObj(bounds) &&
      RawDataUtils.isEmptyObj(color)
    ) {
      reasons.push('crop is 0x0');
    }
    const transform = layer.getEagerPropertyByName('transform');
    if (transform && !Transform.from(transform).matrix.isValid()) {
      reasons.push('transform is invalid');
    }

    const zOrderRelativeOf = layer
      .getEagerPropertyByName('isRelativeOf')
      ?.getValue();
    if (zOrderRelativeOf === -1) {
      reasons.push('relativeOf layer has been removed');
    }

    if (
      this.isActiveBufferEmpty(layer.getEagerPropertyByName('activeBuffer')) &&
      !this.hasEffects(layer) &&
      !this.hasBlur(layer)
    ) {
      reasons.push('does not have color fill, shadow or blur');
    }

    const visibleRegionNode = layer.getEagerPropertyByName('visibleRegion');
    if (
      visibleRegionNode &&
      this.hasValidEmptyVisibleRegion(visibleRegionNode)
    ) {
      reasons.push('visible region calculated by Composition Engine is empty');
    }

    if (
      visibleRegionNode?.getValue() === null &&
      !layer.getEagerPropertyByName('excludesCompositionState')?.getValue()
    ) {
      reasons.push('null visible region');
    }

    const occludedByNode = layer.getEagerPropertyByName('occludedBy');
    if (occludedByNode && occludedByNode.getAllChildren().length > 0) {
      reasons.push('occluded');
    }

    if (reasons.length === 0) reasons.push('unknown');
    return reasons;
  }

  private layerTopDownTraversal(layer: HierarchyTreeNode): HierarchyTreeNode[] {
    const traverseList: HierarchyTreeNode[] = [layer];
    const children: HierarchyTreeNode[] = [
      ...layer.getAllChildren().values(),
    ].slice();
    children.sort(compareByZOrderPath).forEach((child) => {
      traverseList.push(...this.layerTopDownTraversal(child));
    });
    return traverseList;
  }

  private getRect(rectNode: PropertyTreeNode): Rect | undefined {
    if (rectNode.getAllChildren().length === 0) return undefined;
    return GeometryFactory.makeRect(rectNode);
  }

  private getColor(layer: HierarchyTreeNode): PropertyTreeNode | undefined {
    const colorNode = layer.getEagerPropertyByName('color');
    if (!colorNode || !colorNode.getChildByName('a')) return undefined;
    return colorNode;
  }

  private getDisplaySize(layer: HierarchyTreeNode): Rect {
    const displaySize = new Rect(0, 0, 0, 0);
    const matchingDisplay = this.displays.find(
      (display) =>
        this.getDefinedValue(display, 'layerStack') ===
        this.getDefinedValue(layer, 'layerStack'),
    );
    if (matchingDisplay) {
      const rectNode = assertDefined(
        matchingDisplay.getChildByName('layerStackSpaceRect'),
      );
      return this.getRect(rectNode) ?? displaySize;
    }
    return displaySize;
  }

  private layerContains(
    layer: HierarchyTreeNode,
    other: HierarchyTreeNode,
    crop = new Rect(0, 0, 0, 0),
  ): boolean {
    if (
      !TransformType.isSimpleRotation(
        assertDefined(layer.getEagerPropertyByName('transform'))
          .getChildByName('type')
          ?.getValue() ?? 0,
      ) ||
      !TransformType.isSimpleRotation(
        assertDefined(other.getEagerPropertyByName('transform'))
          .getChildByName('type')
          ?.getValue() ?? 0,
      )
    ) {
      return false;
    } else {
      const layerBounds = this.getCroppedScreenBounds(layer, crop);
      const otherBounds = this.getCroppedScreenBounds(other, crop);
      return layerBounds && otherBounds
        ? layerBounds.containsRect(otherBounds)
        : false;
    }
  }

  private layerOverlaps(
    layer: HierarchyTreeNode,
    other: HierarchyTreeNode,
    crop = new Rect(0, 0, 0, 0),
  ): boolean {
    const layerBounds = this.getCroppedScreenBounds(layer, crop);
    const otherBounds = this.getCroppedScreenBounds(other, crop);
    return layerBounds && otherBounds
      ? layerBounds.intersectsRect(otherBounds)
      : false;
  }

  private getCroppedScreenBounds(
    layer: HierarchyTreeNode,
    crop: Rect,
  ): Rect | undefined {
    const layerScreenBoundsNode = assertDefined(
      layer.getEagerPropertyByName('screenBounds'),
    );
    const layerScreenBounds = this.getRect(layerScreenBoundsNode);

    if (layerScreenBounds && !crop.isEmpty()) {
      return layerScreenBounds.cropRect(crop);
    }

    return layerScreenBounds;
  }

  private isHiddenByParent(layer: HierarchyTreeNode): boolean {
    const parentLayer = assertDefined(layer.getParent());
    return (
      !parentLayer.isRoot() &&
      (this.isHiddenByPolicy(parentLayer) || this.isHiddenByParent(parentLayer))
    );
  }

  private isHiddenByPolicy(layer: HierarchyTreeNode): boolean {
    return (
      (this.getDefinedValue(layer, 'flags') & LayerFlag.HIDDEN) !== 0x0 ||
      this.getDefinedValue(layer, 'id') ===
        VisibilityPropertiesComputation.OFFSCREEN_LAYER_ROOT_ID
    );
  }

  private hasZeroAlpha(layer: HierarchyTreeNode): boolean {
    const alpha = this.getColor(layer)?.getChildByName('a')?.getValue() ?? 0;
    return alpha === 0;
  }

  private isOpaque(layer: HierarchyTreeNode): boolean {
    const alpha = this.getColor(layer)?.getChildByName('a')?.getValue();
    if (alpha !== 1) {
      return false;
    }
    return this.getDefinedValue(layer, 'isOpaque');
  }

  private isActiveBufferEmpty(buffer: PropertyTreeNode | undefined): boolean {
    if (buffer === undefined) return true;
    return (
      buffer.getAllChildren().length === 0 ||
      (this.getDefinedValue(buffer, 'width') === 0 &&
        this.getDefinedValue(buffer, 'height') === 0 &&
        this.getDefinedValue(buffer, 'stride') === 0 &&
        this.getDefinedValue(buffer, 'format') === 0)
    );
  }

  private hasEffects(layer: HierarchyTreeNode): boolean {
    const color = this.getColor(layer);
    return (
      (color && !RawDataUtils.isEmptyObj(color)) ||
      (layer.getEagerPropertyByName('shadowRadius')?.getValue() ?? 0) > 0
    );
  }

  private hasBlur(layer: HierarchyTreeNode): boolean {
    return (
      (layer.getEagerPropertyByName('backgroundBlurRadius')?.getValue() ?? 0) >
      0
    );
  }

  private getDefinedValue(
    node: HierarchyTreeNode | PropertyTreeNode,
    name: string,
  ): any {
    if (node instanceof HierarchyTreeNode) {
      return assertDefined(node.getEagerPropertyByName(name)).getValue();
    } else {
      return assertDefined(node.getChildByName(name)).getValue();
    }
  }
}
