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

export class VisibilityComputation implements Computation {
  private static readonly VISIBLE = 0;

  private root: HierarchyTreeNode | undefined;

  setRoot(value: HierarchyTreeNode): VisibilityComputation {
    this.root = value;
    return this;
  }

  executeInPlace(): void {
    if (!this.root) {
      throw Error('root not set');
    }

    this.root.forEachNodeDfs((node) => {
      const isVisible =
        assertDefined(node.getEagerPropertyByName('visibility')).getValue() ===
          VisibilityComputation.VISIBLE &&
        (node.isRoot() ||
          node
            .getZParent()
            ?.getEagerPropertyByName('isComputedVisible')
            ?.getValue());

      node.addEagerProperty(
        DEFAULT_PROPERTY_TREE_NODE_FACTORY.makeCalculatedProperty(
          node.id,
          'isComputedVisible',
          isVisible,
        ),
      );
    });
  }
}
