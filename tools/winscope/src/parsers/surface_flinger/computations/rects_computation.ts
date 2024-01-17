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
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';

class RectSfFactory {
  makeDisplayRects(displays: PropertyTreeNode[]): TraceRect[] {
    return displays.map((display) => {
      const size = display.getChildByName('size');

      return new TraceRectBuilder()
        .setX(0)
        .setY(0)
        .setWidth(size?.getChildByName('w')?.getValue() ?? 0)
        .setHeight(size?.getChildByName('h')?.getValue() ?? 0)
        .setId(`Display - ${assertDefined(display.getChildByName('id')).getValue()}`)
        .setName('Display')
        .setCornerRadius(0)
        .setTransform(Transform.EMPTY.matrix)
        .setZOrderPath([])
        .setGroupId(assertDefined(display.getChildByName('layerStack')).getValue())
        .setIsVisible(false)
        .setIsDisplay(true)
        .setIsVirtual(display.getChildByName('isVirtual')?.getValue() ?? false)
        .build();
    });
  }

  makeLayerRect(layer: HierarchyTreeNode): TraceRect | undefined {
    const zOrderPathNode = layer.getEagerPropertyByName('zOrderPath');
    if (zOrderPathNode === undefined) {
      throw Error('Z order path has not been computed');
    }

    const isVisible = layer.getEagerPropertyByName('isVisible')?.getValue();
    if (isVisible === undefined) {
      throw Error('Visibility has not been computed');
    }

    const occludedBy = layer.getEagerPropertyByName('occludedBy');

    if (!isVisible && (occludedBy === undefined || occludedBy.getAllChildren().length === 0)) {
      return undefined;
    }

    const bounds = layer.getEagerPropertyByName('bounds');
    if (!bounds) {
      return undefined;
    }
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
      .setZOrderPath(zOrderPathNode.getAllChildren().map((child) => child.getValue()))
      .setGroupId(assertDefined(layer.getEagerPropertyByName('layerStack')).getValue())
      .setIsVisible(isVisible)
      .setIsDisplay(false)
      .setIsVirtual(false)
      .build();
  }
}

export class RectsComputation {
  private root: HierarchyTreeNode | undefined;
  private displays: PropertyTreeNode[] | undefined;
  private readonly rectsFactory = new RectSfFactory();

  setHierarchyRoot(value: HierarchyTreeNode): this {
    this.root = value;
    return this;
  }

  setDisplays(value: PropertyTreeNode[]): this {
    this.displays = value;
    return this;
  }

  execute() {
    if (!this.root) {
      throw Error('root not set');
    }
    if (!this.displays) {
      throw Error('displays not set');
    }
    const rootLayers = this.root.getAllChildren();

    rootLayers.forEach((rootLayer) => {
      rootLayer.forEachNodeDfs((layer) => {
        const rect = this.rectsFactory.makeLayerRect(layer);
        if (!rect) {
          return;
        }
        layer.setRects([rect]);
      });
    });

    const displayRects = this.rectsFactory.makeDisplayRects(this.displays);
    this.root.setRects(displayRects);

    return this.root;
  }
}
