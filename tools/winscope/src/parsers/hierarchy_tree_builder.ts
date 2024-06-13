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
import {Computation} from 'trace/tree_node/computation';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {PropertiesProvider} from 'trace/tree_node/properties_provider';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';

export abstract class HierarchyTreeBuilder {
  protected root: PropertiesProvider | undefined;
  private children: PropertiesProvider[] | undefined;
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
      throw Error('root not set');
    }

    if (!this.children) {
      throw Error('children not set');
    }

    const identifierToChild = this.buildIdentifierToChildMap(this.children);
    const rootChildren = this.makeRootChildren(
      this.children,
      identifierToChild,
    );

    const rootProperties = this.root.getEagerProperties();

    const root = this.buildHierarchyTreeNode(
      rootProperties.id,
      rootProperties.name,
      rootChildren,
      this.root,
    );

    this.computations.forEach((computation) =>
      computation.setRoot(root).executeInPlace(),
    );

    return root;
  }

  protected buildSubtree(
    child: PropertiesProvider,
    identifierToChild: Map<string | number, PropertiesProvider[]>,
  ): HierarchyTreeNode {
    const childProperties = child.getEagerProperties();
    const subChildren: HierarchyTreeNode[] =
      childProperties
        .getChildByName('children')
        ?.getAllChildren()
        .flatMap((identifier: PropertyTreeNode) => {
          const key = this.getIdentifierValue(identifier);
          return assertDefined(identifierToChild.get(key)).map((child) => {
            return this.buildSubtree(child, identifierToChild);
          });
        }) ?? [];

    return this.buildHierarchyTreeNode(
      childProperties.id,
      this.getSubtreeName(childProperties.name),
      subChildren,
      child,
    );
  }

  private buildHierarchyTreeNode(
    id: string,
    name: string,
    children: readonly HierarchyTreeNode[],
    propertiesProvider: PropertiesProvider,
  ): HierarchyTreeNode {
    const node = new HierarchyTreeNode(id, name, propertiesProvider);
    children.forEach((child) => {
      node.addOrReplaceChild(child);
      child.setZParent(node);
    });
    return node;
  }

  protected abstract buildIdentifierToChildMap(
    nodes: PropertiesProvider[],
  ): Map<string | number, PropertiesProvider[]>;

  protected abstract makeRootChildren(
    children: PropertiesProvider[],
    identifierToChild: Map<string | number, PropertiesProvider[]>,
  ): readonly HierarchyTreeNode[];

  protected abstract getIdentifierValue(
    identifier: PropertyTreeNode,
  ): string | number;
  protected abstract getSubtreeName(propertyTreeName: string): string;
}
