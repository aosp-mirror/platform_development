/*
 * Copyright (C) 2025 The Android Open Source Project
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
import {UiPropertyTreeNode} from 'viewers/common/ui_property_tree_node';

export class PropagateHashCodes implements Operation<UiPropertyTreeNode> {
  private readonly layerFields = [
    'surfaceControl',
    'leash',
    'capturedLeash',
    'startLeash',
  ];

  apply(tree: UiPropertyTreeNode): void {
    tree.forEachNodeDfs((node) => {
      if (this.layerFields.includes(node.name)) {
        return;
      }
      node.getAllChildren().forEach((child) => {
        if (child.name !== 'hashCode') {
          return;
        }
        const hex = (child.getValue() ?? 0).toString(16);
        if (child.id.split(' ').at(1) === hex) {
          return;
        }
        child.setCanPropagate(true);
      });
    });
  }
}
