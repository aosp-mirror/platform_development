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

import {HierarchyTreeBuilder} from 'parsers/hierarchy_tree_builder';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {PropertiesProvider} from 'trace/tree_node/properties_provider';

export class HierarchyTreeBuilderLog extends HierarchyTreeBuilder {
  protected override buildIdentifierToChildrenMap(
    traceLogEntries: PropertiesProvider[],
  ): Map<number, readonly HierarchyTreeNode[]> {
    const nodes = traceLogEntries.map((entry) => {
      const properties = entry.getEagerProperties();
      return this.makeNode(properties.id, properties.name, entry);
    });
    return new Map([[0, nodes]]);
  }

  protected override assignParentChildRelationships(
    root: HierarchyTreeNode,
    identifierToChildren: Map<number, HierarchyTreeNode[]>,
  ): void {
    for (const nodes of identifierToChildren.values()) {
      nodes.forEach((node) => {
        this.setParentChildRelationship(root, node);
      });
    }
  }
}
