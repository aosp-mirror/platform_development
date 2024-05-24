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
import {Computation} from 'trace/tree_node/computation';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {DEFAULT_PROPERTY_TREE_NODE_FACTORY} from 'trace/tree_node/property_tree_node_factory';

export class ZOrderPathsComputation implements Computation {
  private root: HierarchyTreeNode | undefined;

  setRoot(value: HierarchyTreeNode): ZOrderPathsComputation {
    this.root = value;
    return this;
  }

  executeInPlace(): void {
    if (!this.root) {
      throw Error('root not set');
    }

    this.updateZOrderParents(this.root);
    this.root.forEachNodeDfs((node) => {
      if (node.id === 'LayerTraceEntry root') return;
      const zOrderPath = this.getZOrderPath(node);
      node.addEagerProperty(
        DEFAULT_PROPERTY_TREE_NODE_FACTORY.makeCalculatedProperty(
          `${node.id}`,
          'zOrderPath',
          zOrderPath,
        ),
      );
    });
  }

  private updateZOrderParents(root: HierarchyTreeNode) {
    const layerIdToTreeNode = new Map<number, HierarchyTreeNode>();
    root.forEachNodeDfs((node) => {
      if (node.isRoot()) return;
      layerIdToTreeNode.set(
        assertDefined(node.getEagerPropertyByName('id')).getValue(),
        node,
      );
    });

    root.forEachNodeDfs((node) => {
      const zOrderRelativeOf = node
        .getEagerPropertyByName('zOrderRelativeOf')
        ?.getValue();
      if (zOrderRelativeOf && zOrderRelativeOf !== -1) {
        const zParent = layerIdToTreeNode.get(zOrderRelativeOf);
        if (!zParent) {
          node.addEagerProperty(
            DEFAULT_PROPERTY_TREE_NODE_FACTORY.makeCalculatedProperty(
              node.id,
              'isMissingZParent',
              true,
            ),
          );
          return;
        }
        node.setZParent(zParent);
      }
    });
  }

  private getZOrderPath(node: HierarchyTreeNode | undefined): number[] {
    if (!node) return [];

    const zOrderPath = this.getZOrderPath(node.getZParent());
    const z = node.getEagerPropertyByName('z')?.getValue();
    if (z !== undefined) zOrderPath.push(z);
    return zOrderPath;
  }
}
