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

import {assertDefined} from 'common/assert_utils';
import {HierarchyTreeNode} from './hierarchy_tree_node';
import {PropertiesProvider} from './properties_provider';

export class HierarchyTreeNodeBuilder {
  private id: string | number | undefined;
  private name: string | undefined;
  private propertiesProvider: PropertiesProvider | undefined;
  private children: HierarchyTreeNode[] = [];
  private duplicateCount: number = 0;

  setId(value: string | number): HierarchyTreeNodeBuilder {
    this.id = value;
    return this;
  }

  setName(value: string): HierarchyTreeNodeBuilder {
    this.name = value;
    return this;
  }

  setDuplicateCount(value: number): HierarchyTreeNodeBuilder {
    this.duplicateCount = value;
    return this;
  }

  setChildren(value: HierarchyTreeNode[]): HierarchyTreeNodeBuilder {
    this.children = this.children.concat(value);
    return this;
  }

  setPropertiesProvider(value: PropertiesProvider): HierarchyTreeNodeBuilder {
    this.propertiesProvider = value;
    return this;
  }

  build(): HierarchyTreeNode {
    if (!this.name) {
      throw Error('name not set');
    }
    if (!this.propertiesProvider) {
      throw Error('properties provider not set');
    }

    const nodeId = this.makeNodeId();
    const node = new HierarchyTreeNode(nodeId, this.name, this.propertiesProvider);
    this.children.forEach((child) => {
      node.addChild(child);
      child.setZParent(node);
    });

    return node;
  }

  private makeNodeId(): string {
    let nodeId = `${assertDefined(this.id)} ${assertDefined(this.name)}`;
    if (this.duplicateCount > 0) {
      nodeId += ` duplicate(${this.duplicateCount})`;
    }
    return nodeId;
  }
}
