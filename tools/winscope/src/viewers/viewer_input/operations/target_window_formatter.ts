/*
 * Copyright 2024 The Android Open Source Project
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

import {FixedStringFormatter} from 'trace/tree_node/formatters';
import {Operation} from 'trace/tree_node/operations/operation';
import {UiPropertyTreeNode} from 'viewers/common/ui_property_tree_node';

export class TargetWindowFormatter implements Operation<UiPropertyTreeNode> {
  constructor(private readonly layerIdToName: Map<number, string>) {}

  apply(node: UiPropertyTreeNode): void {
    node.getAllChildren().forEach((dispatchEntry) => {
      const windowIdNode = dispatchEntry.getChildByName('windowId');
      if (windowIdNode === undefined) {
        return;
      }
      windowIdNode.setDisplayName('TargetWindow');
      const windowId = Number(windowIdNode.getValue() ?? -1);
      const layerName = this.layerIdToName.get(windowId);
      windowIdNode.setFormatter(
        new FixedStringFormatter(
          `${windowId} - ${layerName ?? '<Unknown Name>'}`,
        ),
      );
    });
  }
}
