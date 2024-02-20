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

export class FlattenChildren implements Operation<UiHierarchyTreeNode> {
  apply(node: UiHierarchyTreeNode): void {
    const flattenedChildren = this.extractFlattenedChildren(node);

    node.removeAllChildren();
    flattenedChildren.forEach((child) => {
      child.removeAllChildren();
      node.addOrReplaceChild(child);
    });
  }

  private extractFlattenedChildren(
    node: UiHierarchyTreeNode,
  ): UiHierarchyTreeNode[] {
    const children: UiHierarchyTreeNode[] = [];
    node.getAllChildren().forEach((child) => {
      children.push(child);
      children.push(...this.extractFlattenedChildren(child));
    });
    return children;
  }
}
