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

import {Operation} from 'trace/tree_node/operations/operation';
import {UiHierarchyTreeNode} from 'viewers/common/ui_hierarchy_tree_node';

export class SimplifyNames implements Operation<UiHierarchyTreeNode> {
  apply(node: UiHierarchyTreeNode): void {
    node.forEachNodeDfs(this.shortenName);
  }

  private shortenName(node: UiHierarchyTreeNode) {
    const classParts = (node.name + '').split('.');
    if (classParts.length <= 3) {
      return;
    }

    const className = classParts.slice(-1)[0]; // last element
    node.setDisplayName(`${classParts[0]}.${classParts[1]}.(...).${className}`);
  }
}
