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
import {UserNotifier} from 'common/user_notifier';
import {DuplicateLayerId} from 'messaging/user_warnings';
import {HierarchyTreeBuilder} from 'parsers/hierarchy_tree_builder';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {PropertiesProvider} from 'trace/tree_node/properties_provider';
import {
  PropertySource,
  PropertyTreeNode,
} from 'trace/tree_node/property_tree_node';
import {DEFAULT_PROPERTY_TREE_NODE_FACTORY} from 'trace/tree_node/property_tree_node_factory';

export class HierarchyTreeBuilderSf extends HierarchyTreeBuilder {
  protected override buildIdentifierToChildrenMap(
    layers: PropertiesProvider[],
  ): Map<string | number, readonly HierarchyTreeNode[]> {
    const map = layers.reduce((map, layer) => {
      const layerProperties = layer.getEagerProperties();
      const layerNode = this.makeNode(
        layerProperties.id,
        layerProperties.name,
        layer,
      );
      const layerId = assertDefined(
        layerProperties.getChildByName('id'),
      ).getValue();

      const curr = map.get(layerId);
      if (curr) {
        curr.push(layerNode);
        UserNotifier.add(new DuplicateLayerId(layerId.toString()));
        layer.addEagerProperty(
          DEFAULT_PROPERTY_TREE_NODE_FACTORY.makeCalculatedProperty(
            layerProperties.id,
            'isDuplicate',
            true,
          ),
        );
      } else {
        map.set(layerId, [layerNode]);
      }
      return map;
    }, new Map<string | number, HierarchyTreeNode[]>());
    return map;
  }

  protected override assignParentChildRelationships(
    root: HierarchyTreeNode,
    identifierToChildren: Map<string | number, HierarchyTreeNode[]>,
    isRoot?: boolean,
  ): void {
    for (const children of identifierToChildren.values()) {
      children.forEach((child) => {
        const parentIdNode = assertDefined(
          child.getEagerPropertyByName('parent'),
        );
        const isDefault = parentIdNode.source === PropertySource.DEFAULT;
        const parentId = this.getIdentifierValue(parentIdNode);
        const parent = identifierToChildren.get(parentId)?.at(0);
        if (!isDefault && parent) {
          this.setParentChildRelationship(parent, child);
        } else {
          this.setParentChildRelationship(root, child);
        }
      });
    }
  }

  private getIdentifierValue(identifier: PropertyTreeNode): number {
    return Number(identifier.getValue());
  }
}
