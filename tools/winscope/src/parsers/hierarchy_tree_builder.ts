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

import {Computation} from 'trace/tree_node/computation';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {PropertiesProvider} from 'trace/tree_node/properties_provider';

export abstract class HierarchyTreeBuilder {
  protected root: PropertiesProvider | undefined;
  protected children: PropertiesProvider[] | undefined;
  private computations: Computation[] = [];

  setRoot(value: PropertiesProvider): this {
    this.root = value;
    return this;
  }

  setChildren(value: PropertiesProvider[]): this {
    this.children = value;
    return this;
  }

  setComputations(value: Computation[]): this {
    this.computations = value;
    return this;
  }

  build(): HierarchyTreeNode {
    if (!this.root) {
      throw new Error('root not set');
    }

    if (!this.children) {
      throw new Error('children not set');
    }

    const identifierToChildren = this.buildIdentifierToChildrenMap(
      this.children,
    );

    const root = this.buildHierarchyTree(this.root, identifierToChildren);

    this.computations.forEach((computation) =>
      computation.setRoot(root).executeInPlace(),
    );

    return root;
  }

  private buildHierarchyTree(
    root: PropertiesProvider,
    identifierToChildren: Map<string | number, readonly HierarchyTreeNode[]>,
  ): HierarchyTreeNode {
    const rootProperties = root.getEagerProperties();
    const node = this.makeNode(rootProperties.id, rootProperties.name, root);
    this.assignParentChildRelationships(node, identifierToChildren, true);
    return node;
  }

  protected makeNode(
    id: string,
    name: string,
    propertiesProvider: PropertiesProvider,
  ): HierarchyTreeNode {
    return new HierarchyTreeNode(id, name, propertiesProvider);
  }

  protected setParentChildRelationship(
    parent: HierarchyTreeNode,
    child: HierarchyTreeNode,
  ) {
    parent.addOrReplaceChild(child);
    child.setParent(parent);
  }

  protected abstract buildIdentifierToChildrenMap(
    nodes: PropertiesProvider[],
  ): Map<string | number, readonly HierarchyTreeNode[]>;

  protected abstract assignParentChildRelationships(
    node: HierarchyTreeNode,
    identifierToChildren: Map<string | number, readonly HierarchyTreeNode[]>,
    isRoot?: boolean,
  ): void;
}
