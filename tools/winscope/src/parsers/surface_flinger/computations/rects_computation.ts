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
import {TraceRect} from 'trace/trace_rect';
import {TraceRectBuilder} from 'trace/trace_rect_builder';
import {Computation} from 'trace/tree_node/computation';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';

class RectSfFactory {
  makeDisplayRects(displays: ReadonlyArray<PropertyTreeNode>): TraceRect[] {
    const names = new Set<string>();
    return displays.map((display, index) => {
      const size = display.getChildByName('size');
      const layerStack = assertDefined(display.getChildByName('layerStack')).getValue();

      let displayName = assertDefined(display.getChildByName('name')).getValue();
      if (names.has(displayName)) {
        displayName += ' (Mirror)';
      } else {
        names.add(displayName);
      }

      return new TraceRectBuilder()
        .setX(0)
        .setY(0)
        .setWidth(size?.getChildByName('w')?.getValue() ?? 0)
        .setHeight(size?.getChildByName('h')?.getValue() ?? 0)
        .setId(`Display - ${assertDefined(display.getChildByName('id')).getValue()}`)
        .setName(displayName)
        .setCornerRadius(0)
        .setTransform(Transform.EMPTY.matrix)
        .setGroupId(layerStack)
        .setIsVisible(false)
        .setIsDisplay(true)
        .setIsVirtual(display.getChildByName('isVirtual')?.getValue() ?? false)
        .setDepth(index)
        .build();
    });
  }

  makeLayerRect(layer: HierarchyTreeNode, absoluteZ: number): TraceRect {
    const isVisible = assertDefined(layer.getEagerPropertyByName('isComputedVisible')).getValue();

    const bounds = assertDefined(layer.getEagerPropertyByName('bounds'));

    const name = assertDefined(layer.getEagerPropertyByName('name')).getValue();
    const boundsLeft = assertDefined(bounds.getChildByName('left')).getValue();
    const boundsTop = assertDefined(bounds.getChildByName('top')).getValue();
    const boundsRight = assertDefined(bounds.getChildByName('right')).getValue();
    const boundsBottom = assertDefined(bounds.getChildByName('bottom')).getValue();

    return new TraceRectBuilder()
      .setX(boundsLeft)
      .setY(boundsTop)
      .setWidth(boundsRight - boundsLeft)
      .setHeight(boundsBottom - boundsTop)
      .setId(`${assertDefined(layer.getEagerPropertyByName('id')).getValue()} ${name}`)
      .setName(name)
      .setCornerRadius(layer.getEagerPropertyByName('cornerRadius')?.getValue() ?? 0)
      .setTransform(Transform.from(assertDefined(layer.getEagerPropertyByName('transform'))).matrix)
      .setGroupId(assertDefined(layer.getEagerPropertyByName('layerStack')).getValue())
      .setIsVisible(isVisible)
      .setIsDisplay(false)
      .setIsVirtual(false)
      .setDepth(absoluteZ)
      .build();
  }
}

export class RectsComputation implements Computation {
  private root: HierarchyTreeNode | undefined;
  private readonly rectsFactory = new RectSfFactory();

  setRoot(value: HierarchyTreeNode): this {
    this.root = value;
    return this;
  }

  executeInPlace(): void {
    if (!this.root) {
      throw Error('root not set');
    }

    const displays = this.root.getEagerPropertyByName('displays')?.getAllChildren() ?? [];
    const displayRects = this.rectsFactory.makeDisplayRects(displays);
    this.root.setRects(displayRects);

    const layersWithRects = this.extractLayersWithRects(this.root);
    layersWithRects.sort(this.compareLayerZ);

    let absoluteZ = displayRects.length;
    for (let i = layersWithRects.length - 1; i > -1; i--) {
      const layer = layersWithRects[i];
      const rect = this.rectsFactory.makeLayerRect(layer, absoluteZ);
      layer.setRects([rect]);
      absoluteZ++;
    }
  }

  private extractLayersWithRects(hierarchyRoot: HierarchyTreeNode): HierarchyTreeNode[] {
    return hierarchyRoot.filterDfs(this.hasLayerRect);
  }

  private hasLayerRect(node: HierarchyTreeNode): boolean {
    if (node.isRoot()) return false;

    const isVisible = node.getEagerPropertyByName('isComputedVisible')?.getValue();
    if (isVisible === undefined) {
      throw Error('Visibility has not been computed');
    }

    const occludedBy = node.getEagerPropertyByName('occludedBy');
    if (!isVisible && (occludedBy === undefined || occludedBy.getAllChildren().length === 0)) {
      return false;
    }

    const bounds = node.getEagerPropertyByName('bounds');
    if (!bounds) return false;

    return true;
  }

  private compareLayerZ(a: HierarchyTreeNode, b: HierarchyTreeNode): number {
    const aZOrderPath: number[] = assertDefined(a.getEagerPropertyByName('zOrderPath'))
      .getAllChildren()
      .map((child) => child.getValue());
    const bZOrderPath: number[] = assertDefined(b.getEagerPropertyByName('zOrderPath'))
      .getAllChildren()
      .map((child) => child.getValue());

    const zipLength = Math.min(aZOrderPath.length, bZOrderPath.length);
    for (let i = 0; i < zipLength; ++i) {
      const zOrderA = aZOrderPath[i];
      const zOrderB = bZOrderPath[i];
      if (zOrderA > zOrderB) return -1;
      if (zOrderA < zOrderB) return 1;
    }
    // When z-order is the same, the layer with larger ID is on top
    return a.id > b.id ? -1 : 1;
  }
}
