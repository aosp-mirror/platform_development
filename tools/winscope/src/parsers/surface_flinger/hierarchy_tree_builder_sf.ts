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
import {DEFAULT_PROPERTY_TREE_NODE_FACTORY} from 'trace/tree_node/property_tree_node_factory';

export class HierarchyTreeBuilderSf extends HierarchyTreeBuilder {
  protected override buildIdentifierToChildMap(
    layers: PropertiesProvider[],
  ): Map<string | number, PropertiesProvider[]> {
    const map = layers.reduce((map, layer) => {
      const layerProperties = layer.getEagerProperties();
      const id = assertDefined(layerProperties.getChildByName('id')).getValue();
      const curr = map.get(id);
      if (curr) {
        curr.push(layer);
        console.warn(
          `Duplicate layer id ${id} found. Adding it as duplicate to the hierarchy`,
        );
        layer.addEagerProperty(
          DEFAULT_PROPERTY_TREE_NODE_FACTORY.makeCalculatedProperty(
            layerProperties.id,
            'isDuplicate',
            true,
          ),
        );
      } else {
        map.set(id, [layer]);
      }
      return map;
    }, new Map<string | number, PropertiesProvider[]>());
    return map;
  }

  protected override makeRootChildren(
    children: PropertiesProvider[],
    identifierToChild: Map<string | number, PropertiesProvider[]>,
  ): readonly HierarchyTreeNode[] {
    const rootLayers = children.filter((layer) => {
      const hasParent =
        assertDefined(
          layer.getEagerProperties().getChildByName('parent'),
        ).getValue() !== -1;
      return !hasParent;
    });

    return rootLayers.map((layer) => {
      return this.buildSubtree(layer, identifierToChild);
    });
  }

  protected override getIdentifierValue(identifier: PropertyTreeNode): number {
    return Number(identifier.getValue());
  }

  protected override getSubtreeName(propertyTreeName: string): string {
    return propertyTreeName;
  }
}
