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

import {HierarchyTreeBuilder} from 'parsers/hierarchy_tree_builder';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {PropertiesProvider} from 'trace/tree_node/properties_provider';

export class HierarchyTreeBuilderInputMethod extends HierarchyTreeBuilder {
  private childIdentifier = 'child';

  protected override buildIdentifierToChildrenMap(
    children: PropertiesProvider[],
  ): Map<string, readonly HierarchyTreeNode[]> {
    const map = children.reduce((map, child) => {
      const childProperties = child.getEagerProperties();
      const childNode = this.makeNode(
        childProperties.id,
        childProperties.name,
        child,
      );
      map.set(this.childIdentifier, [childNode]);
      return map;
    }, new Map<string, HierarchyTreeNode[]>());
    return map;
  }

  protected override assignParentChildRelationships(
    node: HierarchyTreeNode,
    identifierToChildren: Map<string | number, HierarchyTreeNode[]>,
    isRoot?: boolean,
  ): void {
    // only ever one child
    const child: HierarchyTreeNode | undefined = identifierToChildren
      .get(this.childIdentifier)
      ?.at(0);
    if (child) {
      this.setParentChildRelationship(node, child);
    }
  }
}
