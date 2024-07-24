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
import {PropertyFormatter} from 'trace/tree_node/formatters';
import {
  PropertySource,
  PropertyTreeNode,
} from 'trace/tree_node/property_tree_node';
import {TreeBuilder} from './tree_builder';

export class PropertyTreeBuilder extends TreeBuilder<
  PropertyTreeNode,
  ChildProperty
> {
  isRoot: boolean = false;
  source = PropertySource.PROTO;
  value: any;
  formatter: PropertyFormatter | undefined;

  setIsRoot(value: boolean): this {
    this.isRoot = value;
    return this;
  }

  setRootId(value: string): this {
    this.id = value;
    return this;
  }

  setSource(value: PropertySource): this {
    this.source = value;
    return this;
  }

  setValue(value: any): this {
    this.value = value;
    return this;
  }

  setFormatter(value: PropertyFormatter | undefined): this {
    this.formatter = value;
    return this;
  }

  protected override makeRootNode(): PropertyTreeNode {
    const node = new PropertyTreeNode(
      this.isRoot ? this.makeRootId() : this.makePropertyNodeId(),
      assertDefined(this.name),
      this.source,
      this.value,
    );
    if (this.formatter) node.setFormatter(this.formatter);
    return node;
  }

  protected override addOrReplaceChildNode(
    rootNode: PropertyTreeNode,
    child: ChildProperty,
  ): void {
    const childNode = new PropertyTreeBuilder()
      .setRootId(rootNode.id)
      .setName(child.name)
      .setSource(child.source ?? assertDefined(this.source))
      .setValue(child.value)
      .setChildren(child.children ?? [])
      .setFormatter(child.formatter)
      .build();
    rootNode.addOrReplaceChild(childNode);
  }

  private makePropertyNodeId() {
    return `${this.id}.${this.name}`;
  }

  private makeRootId() {
    return `${this.id} ${this.name}`;
  }
}

export interface ChildProperty {
  name: string;
  value?: any;
  children?: ChildProperty[];
  source?: PropertySource;
  formatter?: PropertyFormatter;
}
