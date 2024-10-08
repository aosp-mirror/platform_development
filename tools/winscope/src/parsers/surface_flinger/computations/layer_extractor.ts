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
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';

export class LayerExtractor {
  static extractLayersSortedByZ(layer: HierarchyTreeNode): HierarchyTreeNode[] {
    const traverseList: HierarchyTreeNode[] = [];

    const layerAndChildrenSortedByZ = layer
      .getRelativeChildren()
      .concat(
        layer.getAllChildren().filter((child) => child.getZParent() === layer),
      )
      .concat(layer.isRoot() ? [] : [layer])
      .sort(LayerExtractor.compareByZOrderPath);
    if (layer.isRoot()) {
      layerAndChildrenSortedByZ.forEach((c) => {
        traverseList.push(...LayerExtractor.extractLayersSortedByZ(c));
      });
    } else {
      const layerIndex = layerAndChildrenSortedByZ.findIndex(
        (value) => value === layer,
      );
      layerAndChildrenSortedByZ.slice(0, layerIndex).forEach((c) => {
        traverseList.push(...LayerExtractor.extractLayersSortedByZ(c));
      });
      traverseList.push(layer);
      layerAndChildrenSortedByZ.slice(layerIndex + 1).forEach((c) => {
        traverseList.push(...LayerExtractor.extractLayersSortedByZ(c));
      });
    }
    return traverseList;
  }

  private static compareByZOrderPath(
    a: HierarchyTreeNode,
    b: HierarchyTreeNode,
  ): number {
    const aZOrderPath: number[] = LayerExtractor.getZOrderPath(a);
    const bZOrderPath: number[] = LayerExtractor.getZOrderPath(b);

    const zipLength = Math.min(aZOrderPath.length, bZOrderPath.length);
    for (let i = 0; i < zipLength; ++i) {
      const zOrderA = aZOrderPath[i];
      const zOrderB = bZOrderPath[i];
      if (zOrderA > zOrderB) return -1;
      if (zOrderA < zOrderB) return 1;
    }
    // When z-order is the same, the layer with larger ID is on top
    const aId = a.getEagerPropertyByName('id')?.getValue();
    const bId = b.getEagerPropertyByName('id')?.getValue();
    return aId > bId ? -1 : 1;
  }

  private static getZOrderPath(node: HierarchyTreeNode): number[] {
    return assertDefined(node.getEagerPropertyByName('zOrderPath'))
      .getAllChildren()
      .map((child) => child.getValue());
  }
}
