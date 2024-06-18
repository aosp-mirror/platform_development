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

export class AddIsVisible extends AddOperation<PropertyTreeNode> {
  protected override makeProperties(
    value: PropertyTreeNode,
  ): PropertyTreeNode[] {
    // ActivityRecordProto
    const visible = value.getChildByName('visible')?.getValue();
    if (visible !== undefined) {
      return [
        DEFAULT_PROPERTY_TREE_NODE_FACTORY.makeCalculatedProperty(
          value.id,
          'isComputedVisible',
          visible,
        ),
      ];
    }

    // WindowStateProto
    const isVisible = value.getChildByName('isVisible')?.getValue();
    const alpha = value
      .getChildByName('attributes')
      ?.getChildByName('alpha')
      ?.getValue();
    if (alpha !== undefined) {
      return [
        DEFAULT_PROPERTY_TREE_NODE_FACTORY.makeCalculatedProperty(
          value.id,
          'isComputedVisible',
          isVisible && alpha > 0,
        ),
      ];
    }

    return [];
  }
}
