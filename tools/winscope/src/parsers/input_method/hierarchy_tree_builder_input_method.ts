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
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';

export class HierarchyTreeBuilderInputMethod extends HierarchyTreeBuilder {
  protected override buildIdentifierToChildMap(
    children: PropertiesProvider[],
  ): Map<string | number, PropertiesProvider[]> {
    // only ever one child (client, service, manager service) so map unnecessary
    return new Map<string | number, PropertiesProvider[]>();
  }

  protected override makeRootChildren(
    children: PropertiesProvider[],
    identifierToChild: Map<string | number, PropertiesProvider[]>,
  ): readonly HierarchyTreeNode[] {
    if (children.length === 0) return [];
    return [this.buildSubtree(children[0], identifierToChild)];
  }

  protected override getIdentifierValue(identifier: PropertyTreeNode): number {
    return identifier.getValue();
  }

  protected override getSubtreeName(propertyTreeName: string): string {
    return propertyTreeName;
  }
}
