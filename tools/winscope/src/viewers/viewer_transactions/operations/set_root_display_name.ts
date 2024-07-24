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
import {UiPropertyTreeNode} from 'viewers/common/ui_property_tree_node';

export class SetRootDisplayNames implements Operation<UiPropertyTreeNode> {
  apply(node: UiPropertyTreeNode): void {
    if (node.id.includes('layerChanges')) {
      node.setDisplayName('LayerState');
      return;
    }

    if (
      node.id.includes('displayChanges') ||
      node.id.includes('addedDisplays')
    ) {
      node.setDisplayName('DisplayState');
      return;
    }

    if (node.id.includes('addedLayers')) {
      node.setDisplayName('LayerCreationArgs');
      return;
    }

    if (node.id.includes('destroyedLayers')) {
      node.setDisplayName('destroyedLayerId');
      return;
    }

    if (node.id.includes('removedDisplays')) {
      node.setDisplayName('removedDisplayId');
      return;
    }

    if (node.id.includes('destroyedLayerHandles')) {
      node.setDisplayName('destroyedLayerHandleId');
      return;
    }
  }
}
