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

import {SetFormatters} from 'parsers/operations/set_formatters';
import {OperationChain} from 'trace/tree_node/operations/operation_chain';
import {
  PropertySource,
  PropertyTreeNode,
} from 'trace/tree_node/property_tree_node';
import {DEFAULT_PROPERTY_TREE_NODE_FACTORY} from './property_tree_node_factory';

export type LazyPropertiesStrategyType = () => Promise<PropertyTreeNode>;

export class PropertiesProvider {
  private eagerPropertiesRoot: PropertyTreeNode;
  private lazyPropertiesRoot: PropertyTreeNode | undefined;
  private allPropertiesRoot: PropertyTreeNode | undefined;

  constructor(
    eagerPropertiesRoot: PropertyTreeNode,
    private readonly lazyPropertiesStrategy: LazyPropertiesStrategyType,
    private readonly commonOperations: OperationChain<PropertyTreeNode>,
    private readonly eagerOperations: OperationChain<PropertyTreeNode>,
    private readonly lazyOperations: OperationChain<PropertyTreeNode>,
  ) {
    this.eagerPropertiesRoot = this.commonOperations.apply(
      this.eagerOperations.apply(eagerPropertiesRoot),
    );
  }

  getEagerProperties(): PropertyTreeNode {
    return this.eagerPropertiesRoot;
  }

  addEagerProperty(property: PropertyTreeNode) {
    new SetFormatters().apply(property);
    this.eagerPropertiesRoot.addOrReplaceChild(property);
  }

  async getAll(): Promise<PropertyTreeNode> {
    if (this.allPropertiesRoot) return this.allPropertiesRoot;

    const root = DEFAULT_PROPERTY_TREE_NODE_FACTORY.makePropertyRoot(
      this.eagerPropertiesRoot.id,
      this.eagerPropertiesRoot.name,
      PropertySource.PROTO,
      undefined,
    );
    const children = [...this.eagerPropertiesRoot.getAllChildren()];

    // all eager properties have already had operations applied so no need to reapply
    if (!this.lazyPropertiesRoot) {
      this.lazyPropertiesRoot = this.commonOperations.apply(
        this.lazyOperations.apply(await this.lazyPropertiesStrategy()),
      );
    }

    children.push(...this.lazyPropertiesRoot.getAllChildren());
    children.forEach((child) => root.addOrReplaceChild(child));

    root.setIsRoot(true);

    this.allPropertiesRoot = root;
    return this.allPropertiesRoot;
  }

  private sortChildren(a: PropertyTreeNode, b: PropertyTreeNode): number {
    return a.name < b.name ? -1 : 1;
  }
}
