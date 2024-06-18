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

import {
  PropertySource,
  PropertyTreeNode,
} from 'trace/tree_node/property_tree_node';
import {TreeNode} from 'trace/tree_node/tree_node';
import {DiffType} from './diff_type';
import {UiHierarchyTreeNode} from './ui_hierarchy_tree_node';
import {UiPropertyTreeNode} from './ui_property_tree_node';

export type TreeNodeFilter = (node: TreeNode) => boolean;

export class UiTreeUtils {
  static isHighlighted(item: TreeNode, highlighted: string): boolean {
    return highlighted === item.id;
  }

  static isVisible: TreeNodeFilter = (node: TreeNode) => {
    return (
      node instanceof UiHierarchyTreeNode &&
      node.getEagerPropertyByName('isComputedVisible')?.getValue()
    );
  };

  static isNotDefault: TreeNodeFilter = (node: TreeNode) => {
    return (
      node instanceof UiPropertyTreeNode &&
      node.source !== PropertySource.DEFAULT
    );
  };

  static isNotCalculated: TreeNodeFilter = (node: TreeNode) => {
    return (
      node instanceof UiPropertyTreeNode &&
      node.source !== PropertySource.CALCULATED
    );
  };

  static makeIdFilter(filterString: string): TreeNodeFilter {
    const filter = (node: TreeNode) => {
      const regex = new RegExp(filterString, 'i');
      return filterString.length === 0 || regex.test(node.id);
    };
    return filter;
  }

  static makePropertyFilter(filterString: string): TreeNodeFilter {
    const filter = (node: TreeNode) => {
      const regex = new RegExp(filterString, 'i');
      return (
        filterString.length === 0 ||
        regex.test(node.name) ||
        (node instanceof PropertyTreeNode && regex.test(node.formattedValue()))
      );
    };
    return filter;
  }

  static makeIdMatchFilter(targetId: string): TreeNodeFilter {
    return (node: TreeNode) => node.id === targetId;
  }

  static makePropertyMatchFilter(targetValue: string): TreeNodeFilter {
    return (node: TreeNode) => {
      return (
        node instanceof UiPropertyTreeNode &&
        node.formattedValue() !== targetValue
      );
    };
  }

  static makeDenyListFilterByName(denylist: string[]): TreeNodeFilter {
    return (node: TreeNode) => !denylist.includes(node.name);
  }

  static makeAllowListFilterById(allowlist: string[]): TreeNodeFilter {
    return (node: TreeNode) => allowlist.includes(node.id);
  }

  static shouldGetProperties(node: UiHierarchyTreeNode): boolean {
    return !node.isOldNode() || node.getDiff() === DiffType.DELETED;
  }
}
