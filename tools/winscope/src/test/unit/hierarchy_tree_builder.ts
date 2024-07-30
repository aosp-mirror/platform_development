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
import {TraceRect} from 'trace/trace_rect';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {OperationChain} from 'trace/tree_node/operations/operation_chain';
import {PropertiesProvider} from 'trace/tree_node/properties_provider';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {PropertyTreeNodeFactory} from 'trace/tree_node/property_tree_node_factory';
import {ChildProperty, PropertyTreeBuilder} from './property_tree_builder';
import {TreeBuilder} from './tree_builder';

export class HierarchyTreeBuilder extends TreeBuilder<
  HierarchyTreeNode,
  ChildHierarchy
> {
  private properties: any;
  private additionalProperties: ChildProperty[] = [];

  setId(value: any): this {
    this.id = value;
    return this;
  }

  setProperties(value: any): this {
    this.properties = value;
    return this;
  }

  addChildProperty(value: ChildProperty): this {
    this.additionalProperties.push(value);
    return this;
  }

  protected override makeRootNode(): HierarchyTreeNode {
    const rootId = this.makeHierarchyNodeId();

    const propertiesTree = new PropertyTreeNodeFactory().makeProtoProperty(
      rootId,
      assertDefined(this.name),
      this.properties,
    );
    this.additionalProperties.forEach((property) => {
      const childNode = new PropertyTreeBuilder()
        .setRootId(propertiesTree.id)
        .setName(property.name)
        .setSource(property.source ?? propertiesTree.source)
        .setValue(property.value)
        .setChildren(property.children ?? [])
        .build();
      propertiesTree.addOrReplaceChild(childNode);
    });
    const provider = new PropertiesProvider(
      propertiesTree,
      async () => propertiesTree,
      OperationChain.emptyChain<PropertyTreeNode>(),
      OperationChain.emptyChain<PropertyTreeNode>(),
      OperationChain.emptyChain<PropertyTreeNode>(),
    );

    return new HierarchyTreeNode(rootId, assertDefined(this.name), provider);
  }

  protected override addOrReplaceChildNode(
    rootNode: HierarchyTreeNode,
    child: ChildHierarchy,
  ): void {
    const childNode = new HierarchyTreeBuilder()
      .setId(child.id)
      .setName(child.name)
      .setProperties(child.properties)
      .setChildren(child.children ?? [])
      .build();
    rootNode.addOrReplaceChild(childNode);
    childNode.setParent(rootNode);
    if (child.rects !== undefined) {
      childNode.setRects(child.rects);
    }
    if (child.secondaryRects !== undefined) {
      childNode.setSecondaryRects(child.secondaryRects);
    }
  }

  private makeHierarchyNodeId() {
    return `${this.id} ${this.name}`;
  }
}

export interface ChildHierarchy {
  id: string | number;
  name: string;
  properties?: any;
  children?: ChildHierarchy[];
  rects?: TraceRect[];
  secondaryRects?: TraceRect[];
}
