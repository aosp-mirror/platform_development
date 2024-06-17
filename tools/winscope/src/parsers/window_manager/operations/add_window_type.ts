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

import {AddOperation} from 'trace/tree_node/operations/add_operation';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {DEFAULT_PROPERTY_TREE_NODE_FACTORY} from 'trace/tree_node/property_tree_node_factory';
import {WindowType, WindowTypePrefix} from 'trace/window_type';

export class AddWindowType extends AddOperation<PropertyTreeNode> {
  protected override makeProperties(
    windowState: PropertyTreeNode,
  ): PropertyTreeNode[] {
    let windowType = WindowType.UNKNOWN;

    if (windowState.name.startsWith(WindowTypePrefix.STARTING)) {
      windowType = WindowType.STARTING;
    } else if (windowState.getChildByName('animatingExit')) {
      windowType = WindowType.EXITING;
    } else if (windowState.name.startsWith(WindowTypePrefix.DEBUGGER)) {
      windowType = WindowType.STARTING;
    }

    return [
      DEFAULT_PROPERTY_TREE_NODE_FACTORY.makeCalculatedProperty(
        windowState.id,
        'windowType',
        windowType,
      ),
    ];
  }
}
