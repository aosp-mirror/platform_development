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
  static extractLayersTopToBottom(
    layer: HierarchyTreeNode,
  ): HierarchyTreeNode[] {
    const traverseList: HierarchyTreeNode[] = [];

    const sortedZChildren = layer
      .getRelativeChildren()
      .concat(
        layer.getAllChildren().filter((child) => child.getZParent() === layer),
      )
      .sort((a, b) => LayerExtractor.compareByZAndLayerId(a, b));

    if (layer.isRoot()) {
      sortedZChildren.forEach((c) => {
        traverseList.push(...LayerExtractor.extractLayersTopToBottom(c));
      });
    } else {
      const layerZ = LayerExtractor.getZ(layer);
      const sortedZChildrenBelowRoot = sortedZChildren.filter(
        (child) => LayerExtractor.getZ(child) < layerZ,
      );
      const sortedZChildrenAboveRoot = sortedZChildren.filter(
        (child) => LayerExtractor.getZ(child) >= layerZ,
      );

      sortedZChildrenAboveRoot.forEach((c) => {
        traverseList.push(...LayerExtractor.extractLayersTopToBottom(c));
      });
      traverseList.push(layer);
      sortedZChildrenBelowRoot.forEach((c) => {
        traverseList.push(...LayerExtractor.extractLayersTopToBottom(c));
      });
    }
    return traverseList;
  }

  private static compareByZAndLayerId(
    a: HierarchyTreeNode,
    b: HierarchyTreeNode,
  ): number {
    const aZ = LayerExtractor.getZ(a);
    const bZ = LayerExtractor.getZ(b);

    if (aZ > bZ) return -1;
    if (aZ < bZ) return 1;

    // When z-order is the same, the layer with larger ID is on top
    const aId = a.getEagerPropertyByName('id')?.getValue();
    const bId = b.getEagerPropertyByName('id')?.getValue();
    return aId < bId ? 1 : -1;
  }

  private static getZ(node: HierarchyTreeNode): number {
    return assertDefined(node.getEagerPropertyByName('z')).getValue();
  }
}
