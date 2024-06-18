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
import {HierarchyTreeBuilder} from 'parsers/hierarchy_tree_builder';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {PropertiesProvider} from 'trace/tree_node/properties_provider';

export class HierarchyTreeBuilderVc extends HierarchyTreeBuilder {
  protected override buildIdentifierToChildrenMap(
    views: PropertiesProvider[],
  ): Map<string | number, readonly HierarchyTreeNode[]> {
    const map = views.reduce((map, view) => {
      const viewProperties = view.getEagerProperties();
      const viewNode = this.makeNode(
        viewProperties.id,
        viewProperties.name,
        view,
      );
      const hashcode = assertDefined(
        viewProperties.getChildByName('hashcode'),
      ).getValue();
      map.set(hashcode, [viewNode]);
      return map;
    }, new Map<string, HierarchyTreeNode[]>());
    return map;
  }

  protected override assignParentChildRelationships(
    node: HierarchyTreeNode,
    identifierToChildren: Map<string | number, HierarchyTreeNode[]>,
    isRoot?: boolean,
  ): void {
    const childHashcodes =
      node.getEagerPropertyByName('children')?.getAllChildren() ?? [];
    for (const hashcode of childHashcodes) {
      const child = identifierToChildren.get(hashcode.getValue())?.at(0);
      if (child) {
        this.setParentChildRelationship(node, child);
        this.assignParentChildRelationships(child, identifierToChildren);
      }
    }
  }
}
