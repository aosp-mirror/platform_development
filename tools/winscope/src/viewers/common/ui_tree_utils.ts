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

import {FilterFlag, makeFilterPredicate} from 'common/filter_flag';
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

  static makeIsNotDefaultFilter(allowList: string[]): TreeNodeFilter {
    return (node: TreeNode) => {
      return (
        node instanceof UiPropertyTreeNode &&
        (node.source !== PropertySource.DEFAULT ||
          allowList.includes(node.name))
      );
    };
  }

  static isNotCalculated: TreeNodeFilter = (node: TreeNode) => {
    return (
      node instanceof UiPropertyTreeNode &&
      node.source !== PropertySource.CALCULATED
    );
  };

  static makeNodeFilter(
    filterString: string,
    flags: FilterFlag[] = [],
  ): TreeNodeFilter {
    const predicate = makeFilterPredicate(filterString, flags);
    return (node: TreeNode) => {
      return (
        predicate(node.id) ||
        (node instanceof PropertyTreeNode && predicate(node.formattedValue()))
      );
    };
  }

  static makeIdMatchFilter(targetId: string): TreeNodeFilter {
    return (node: TreeNode) => node.id === targetId;
  }

  static makeDenyListFilterByName(denylist: string[]): TreeNodeFilter {
    return (node: TreeNode) => !denylist.includes(node.name);
  }

  static shouldGetProperties(node: UiHierarchyTreeNode): boolean {
    return !node.isOldNode() || node.getDiff() === DiffType.DELETED;
  }
}
