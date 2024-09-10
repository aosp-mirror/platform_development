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
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';

export class HierarchyTreeBuilderWm extends HierarchyTreeBuilder {
  protected override buildIdentifierToChildrenMap(
    containers: PropertiesProvider[],
  ): Map<string, readonly HierarchyTreeNode[]> {
    const map = containers.reduce((map, container) => {
      const containerProperties = container.getEagerProperties();
      const containerNode = this.makeNode(
        containerProperties.id,
        this.getSubtreeName(containerProperties.name),
        container,
      );
      const token = assertDefined(
        containerProperties.getChildByName('token'),
      ).getValue();
      map.set(token, [containerNode]);
      return map;
    }, new Map<string, HierarchyTreeNode[]>());
    return map;
  }

  protected override assignParentChildRelationships(
    node: HierarchyTreeNode,
    identifierToChildren: Map<string | number, HierarchyTreeNode[]>,
    isRoot?: boolean,
  ): void {
    let childrenTokens: readonly PropertyTreeNode[] | undefined;
    if (isRoot) {
      const rootWindowContainerProps = assertDefined(this.children)
        .at(0)
        ?.getEagerProperties();
      childrenTokens =
        rootWindowContainerProps
          ?.getChildByName('children')
          ?.getAllChildren() ?? [];
    } else {
      childrenTokens =
        node.getEagerPropertyByName('children')?.getAllChildren() ?? [];
    }
    for (const childToken of childrenTokens) {
      const child = identifierToChildren.get(childToken.getValue())?.at(0);
      if (child) {
        this.setParentChildRelationship(node, child);
        this.assignParentChildRelationships(child, identifierToChildren);
      }
    }
  }

  private getSubtreeName(tokenAndName: string): string {
    const splitId = tokenAndName.split(' ');
    return splitId.slice(1, splitId.length).join(' ');
  }
}
