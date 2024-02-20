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

export class HierarchyTreeBuilderVc extends HierarchyTreeBuilder {
  protected override buildIdentifierToChildMap(
    nodes: PropertiesProvider[],
  ): Map<string | number, PropertiesProvider[]> {
    const map = nodes.reduce((map, node) => {
      const nodeProperties = node.getEagerProperties();
      const hashcode = assertDefined(
        nodeProperties.getChildByName('hashcode'),
      ).getValue();
      map.set(hashcode, [node]);
      return map;
    }, new Map<string, PropertiesProvider[]>());
    return map;
  }

  protected override makeRootChildren(
    children: PropertiesProvider[],
    identifierToChild: Map<string | number, PropertiesProvider[]>,
  ): readonly HierarchyTreeNode[] {
    const rootChildrenHashcodes = assertDefined(
      assertDefined(this.root).getEagerProperties().getChildByName('children'),
    )
      .getAllChildren()
      .map((child) => child.getValue());

    const rootNodes = children.filter((node) => {
      return rootChildrenHashcodes.includes(
        assertDefined(
          node.getEagerProperties().getChildByName('hashcode'),
        ).getValue(),
      );
    });
    return rootNodes.map((layer) => {
      return this.buildSubtree(layer, identifierToChild);
    });
  }

  protected override getIdentifierValue(identifier: PropertyTreeNode): number {
    return identifier.getValue();
  }

  protected override getSubtreeName(propertyTreeName: string): string {
    return propertyTreeName;
  }
}
