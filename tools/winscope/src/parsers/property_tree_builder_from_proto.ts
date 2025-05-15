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

import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {PropertyTreeNodeFactory} from 'trace/tree_node/property_tree_node_factory';
import {AbstractPropertyTreeBuilder} from './abstract_property_tree_builder';

export class PropertyTreeBuilderFromProto extends AbstractPropertyTreeBuilder<object> {
  private denylistProperties: string[] = [];

  setDenyList(value: string[]): this {
    this.denylistProperties = value;
    return this;
  }

  protected override buildPropertiesTree(rootNodeId: string): PropertyTreeNode {
    const factory = new PropertyTreeNodeFactory(this.denylistProperties);
    return factory.makeProtoProperty(rootNodeId, '', this.data);
  }
}
