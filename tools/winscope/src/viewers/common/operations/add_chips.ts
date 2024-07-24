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

import {LayerCompositionType} from 'trace/layer_composition_type';
import {Operation} from 'trace/tree_node/operations/operation';
import {
  DUPLICATE_CHIP,
  GPU_CHIP,
  HWC_CHIP,
  MISSING_Z_PARENT_CHIP,
  RELATIVE_Z_CHIP,
  RELATIVE_Z_PARENT_CHIP,
  VISIBLE_CHIP,
} from 'viewers/common/chip';
import {UiHierarchyTreeNode} from 'viewers/common/ui_hierarchy_tree_node';

export class AddChips implements Operation<UiHierarchyTreeNode> {
  private relZParentIds: string[] = [];

  apply(node: UiHierarchyTreeNode): void {
    this.addAllChipsExceptRelZParent(node);
    this.addRelZParentChips(node);
  }

  private addAllChipsExceptRelZParent(node: UiHierarchyTreeNode) {
    if (!node.isRoot()) {
      const compositionType = node
        .getEagerPropertyByName('compositionType')
        ?.getValue();
      if (compositionType === LayerCompositionType.GPU) {
        node.addChip(GPU_CHIP);
      } else if (compositionType === LayerCompositionType.HWC) {
        node.addChip(HWC_CHIP);
      }

      if (node.getEagerPropertyByName('isComputedVisible')?.getValue()) {
        node.addChip(VISIBLE_CHIP);
      }

      if (node.getEagerPropertyByName('isDuplicate')?.getValue()) {
        node.addChip(DUPLICATE_CHIP);
      }

      const zOrderRelativeOfId = node
        .getEagerPropertyByName('zOrderRelativeOf')
        ?.getValue();
      if (zOrderRelativeOfId && zOrderRelativeOfId !== -1) {
        node.addChip(RELATIVE_Z_CHIP);
        this.relZParentIds.push(zOrderRelativeOfId);

        if (node.getEagerPropertyByName('isMissingZParent')?.getValue()) {
          node.addChip(MISSING_Z_PARENT_CHIP);
        }
      }
    }

    node
      .getAllChildren()
      .forEach((child) => this.addAllChipsExceptRelZParent(child));
  }

  private addRelZParentChips(node: UiHierarchyTreeNode) {
    const treeLayerId = node.getEagerPropertyByName('id')?.getValue();
    if (this.relZParentIds.includes(treeLayerId)) {
      node.addChip(RELATIVE_Z_PARENT_CHIP);
    }

    node.getAllChildren().forEach((child) => this.addRelZParentChips(child));
  }
}
