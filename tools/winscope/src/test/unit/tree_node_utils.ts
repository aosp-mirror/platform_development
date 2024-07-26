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

import {TransformType} from 'parsers/surface_flinger/transform_utils';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {DEFAULT_PROPERTY_TREE_NODE_FACTORY} from 'trace/tree_node/property_tree_node_factory';
import {TreeNode} from 'trace/tree_node/tree_node';
import {DiffNode} from 'viewers/common/diff_node';
import {UiHierarchyTreeNode} from 'viewers/common/ui_hierarchy_tree_node';
import {UiPropertyTreeNode} from 'viewers/common/ui_property_tree_node';
import {ChildHierarchy, HierarchyTreeBuilder} from './hierarchy_tree_builder';
import {PropertyTreeBuilder} from './property_tree_builder';

export class TreeNodeUtils {
  static makeRectNode(
    left: number | undefined,
    top: number | undefined,
    right: number | undefined,
    bottom: number | undefined,
    id = 'test node',
  ): PropertyTreeNode {
    const children = [];
    if (left !== undefined) children.push({name: 'left', value: left});
    if (top !== undefined) children.push({name: 'top', value: top});
    if (right !== undefined) children.push({name: 'right', value: right});
    if (bottom !== undefined) children.push({name: 'bottom', value: bottom});

    return new PropertyTreeBuilder()
      .setRootId(id)
      .setName('rect')
      .setChildren(children)
      .build();
  }

  static makeColorNode(
    r: number | undefined,
    g: number | undefined,
    b: number | undefined,
    a: number | undefined,
  ): PropertyTreeNode {
    const children = [];
    if (r !== undefined) children.push({name: 'r', value: r});
    if (g !== undefined) children.push({name: 'g', value: g});
    if (b !== undefined) children.push({name: 'b', value: b});
    if (a !== undefined) children.push({name: 'a', value: a});

    return new PropertyTreeBuilder()
      .setRootId('test node')
      .setName('color')
      .setChildren(children)
      .build();
  }

  static makeBufferNode(): PropertyTreeNode {
    return new PropertyTreeBuilder()
      .setRootId('test node')
      .setName('buffer')
      .setChildren([
        {name: 'height', value: 0},
        {name: 'width', value: 1},
        {name: 'stride', value: 0},
        {name: 'format', value: 1},
      ])
      .build();
  }

  static makeMatrixNode(
    dsdx: number,
    dtdx: number,
    dsdy: number,
    dtdy: number,
  ): PropertyTreeNode {
    return new PropertyTreeBuilder()
      .setRootId('test node')
      .setName('matrix')
      .setChildren([
        {name: 'dsdx', value: dsdx},
        {name: 'dtdx', value: dtdx},
        {name: 'dsdy', value: dsdy},
        {name: 'dtdy', value: dtdy},
      ])
      .build();
  }

  static makeTransformNode(type: TransformType): PropertyTreeNode {
    return new PropertyTreeBuilder()
      .setRootId('test node')
      .setName('transform')
      .setChildren([{name: 'type', value: type}])
      .build();
  }

  static makeSizeNode(
    w: number | undefined,
    h: number | undefined,
  ): PropertyTreeNode {
    return new PropertyTreeBuilder()
      .setRootId('test node')
      .setName('size')
      .setChildren([
        {name: 'w', value: w},
        {name: 'h', value: h},
      ])
      .build();
  }

  static makePositionNode(
    x: number | undefined,
    y: number | undefined,
  ): PropertyTreeNode {
    return new PropertyTreeBuilder()
      .setRootId('test node')
      .setName('pos')
      .setChildren([
        {name: 'x', value: x},
        {name: 'y', value: y},
      ])
      .build();
  }

  static makeHierarchyNode(
    proto: any,
    children: ChildHierarchy[] = [],
  ): HierarchyTreeNode {
    return new HierarchyTreeBuilder()
      .setId(`${proto.id}`)
      .setName(proto.name)
      .setProperties(proto)
      .setChildren(children)
      .build();
  }

  static makePropertyNode(
    rootId: string,
    name: string,
    value: any,
  ): PropertyTreeNode {
    return DEFAULT_PROPERTY_TREE_NODE_FACTORY.makeProtoProperty(
      rootId,
      name,
      value,
    );
  }

  static makeCalculatedPropertyNode(
    rootId: string,
    name: string,
    value: any,
  ): PropertyTreeNode {
    return DEFAULT_PROPERTY_TREE_NODE_FACTORY.makeCalculatedProperty(
      rootId,
      name,
      value,
    );
  }

  static makeUiHierarchyNode(proto: any): UiHierarchyTreeNode {
    return UiHierarchyTreeNode.from(TreeNodeUtils.makeHierarchyNode(proto));
  }

  static makeUiPropertyNode(
    rootId: string,
    name: string,
    value: any,
  ): UiPropertyTreeNode {
    return UiPropertyTreeNode.from(
      TreeNodeUtils.makePropertyNode(rootId, name, value),
    );
  }

  static treeNodeEqualityTester(first: any, second: any): boolean | undefined {
    if (first instanceof TreeNode && second instanceof TreeNode) {
      return TreeNodeUtils.testTreeNodes(first, second);
    }
    return undefined;
  }

  private static testTreeNodes(
    node: TreeNode,
    expectedNode: TreeNode,
  ): boolean {
    if (node.id !== expectedNode.id) return false;
    if (node.name !== expectedNode.name) return false;

    if ((node as DiffNode).getDiff && (expectedNode as DiffNode).getDiff) {
      if (
        (node as DiffNode).getDiff() !== (expectedNode as DiffNode).getDiff()
      ) {
        return false;
      }
    }

    if (
      node instanceof UiHierarchyTreeNode &&
      expectedNode instanceof UiHierarchyTreeNode
    ) {
      if (node.heading() !== expectedNode.heading()) {
        return false;
      }
      if (node.getDisplayName() !== expectedNode.getDisplayName()) {
        return false;
      }
      if (
        !(
          node.getChips().length === 0 && expectedNode.getChips().length === 0
        ) &&
        node.getChips() !== expectedNode.getChips()
      ) {
        return false;
      }
    }

    const nodeChildren = node.getAllChildren();
    const expectedChildren = expectedNode.getAllChildren();
    if (nodeChildren.length !== expectedChildren.length) return false;

    for (let i = 0; i < nodeChildren.length; i++) {
      const nodeChild = nodeChildren[i];
      const expectedChild = expectedChildren[i];

      if (!TreeNodeUtils.testTreeNodes(nodeChild, expectedChild)) {
        return false;
      }
    }
    return true;
  }
}
