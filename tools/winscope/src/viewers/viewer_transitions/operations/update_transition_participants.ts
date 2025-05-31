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

import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {AbstractUpdateLayersAndWindows} from './abstract_update_layers_and_windows';

export class UpdateTransitionParticipants extends AbstractUpdateLayersAndWindows<HierarchyTreeNode> {
  apply(node: HierarchyTreeNode): void {
    node
      .getEagerPropertyByName('layers')
      ?.getAllChildren()
      .forEach((layerId) => {
        this.updateLayerId(layerId);
      });

    node
      .getEagerPropertyByName('windows')
      ?.getAllChildren()
      .forEach((windowId) => {
        this.updateWindowId(windowId);
      });
  }
}
