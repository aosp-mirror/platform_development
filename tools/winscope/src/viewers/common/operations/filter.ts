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
import {TreeNode} from 'trace/tree_node/tree_node';
import {TreeNodeFilter} from 'viewers/common/ui_tree_utils';

export class Filter<T extends TreeNode> implements Operation<T> {
  constructor(
    private predicates: TreeNodeFilter[],
    private keepParentsAndChildren: boolean,
  ) {}

  apply(node: T): void {
    for (const child of node.getAllChildren()) {
      if (!this.testPredicates(child)) {
        if (
          !this.keepParentsAndChildren ||
          child.getAllChildren().length === 0
        ) {
          node.removeChild(child.id);
        } else if (this.keepParentsAndChildren) {
          this.apply(child);

          if (child.getAllChildren().length === 0) {
            node.removeChild(child.id);
          }
        }
      } else if (!this.keepParentsAndChildren) {
        this.apply(child);
      }
    }
  }

  testPredicates(node: T): boolean {
    return this.predicates.every((predicate) => predicate(node));
  }
}
