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

import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {Operation} from 'trace/tree_node/operations/operation';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {TreeNode} from 'trace/tree_node/tree_node';
import {TextFilter} from 'viewers/common/text_filter';
import {IsModifiedCallbackType} from './add_diffs';
import {AddDiffsPropertiesTree} from './add_diffs_properties_tree';
import {Filter} from './operations/filter';
import {UiPropertyTreeNode} from './ui_property_tree_node';
import {UiTreeFormatter} from './ui_tree_formatter';
import {TreeNodeFilter, UiTreeUtils} from './ui_tree_utils';
import {UserOptions} from './user_options';

export class PropertiesPresenter {
  private propertiesFilter: TreeNodeFilter;
  private highlightedProperty = '';
  private propertiesTree: PropertyTreeNode | undefined;
  private formattedTree: UiPropertyTreeNode | undefined;

  constructor(
    private userOptions: UserOptions,
    private textFilter: TextFilter,
    private propertiesDenylist: string[],
    private customOperations?: Array<Operation<UiPropertyTreeNode>>,
    private defaultAllowlist: string[] = [],
  ) {
    this.propertiesFilter = UiTreeUtils.makeNodeFilter(
      this.textFilter.getFilterPredicate(),
    );
  }

  getUserOptions(): UserOptions {
    return this.userOptions;
  }

  setPropertiesTree(tree: PropertyTreeNode | undefined) {
    this.propertiesTree = tree;
  }

  getPropertiesTree(): PropertyTreeNode | undefined {
    return this.propertiesTree;
  }

  getFormattedTree(): UiPropertyTreeNode | undefined {
    return this.formattedTree;
  }

  getHighlightedProperty(): string {
    return this.highlightedProperty;
  }

  applyHighlightedPropertyChange(id: string) {
    if (this.highlightedProperty === id) {
      this.highlightedProperty = '';
    } else {
      this.highlightedProperty = id;
    }
  }

  getTextFilter(): TextFilter | undefined {
    return this.textFilter;
  }

  applyPropertiesFilterChange(textFilter: TextFilter) {
    this.textFilter = textFilter;
    this.propertiesFilter = UiTreeUtils.makeNodeFilter(
      textFilter.getFilterPredicate(),
    );
  }

  applyPropertiesUserOptionsChange(userOptions: UserOptions) {
    this.userOptions = userOptions;
  }

  updateDefaultAllowList(value: string[]) {
    this.defaultAllowlist = value;
  }

  async formatPropertiesTree(
    previousHierarchyTree: HierarchyTreeNode | undefined,
    displayName: string | undefined,
    keepCalculated: boolean,
  ): Promise<void> {
    if (!this.propertiesTree) {
      this.formattedTree = undefined;
      return;
    }
    const uiTree = UiPropertyTreeNode.from(this.propertiesTree);

    if (
      this.userOptions['showDiff']?.enabled &&
      !this.userOptions['showDiff']?.isUnavailable
    ) {
      const prevEntryNode = previousHierarchyTree?.findDfs(
        UiTreeUtils.makeIdMatchFilter(this.propertiesTree.id),
      );
      const prevEntryUiTree = prevEntryNode
        ? UiPropertyTreeNode.from(await prevEntryNode.getAllProperties())
        : undefined;
      await new AddDiffsPropertiesTree(
        PropertiesPresenter.isPropertyNodeModified,
        this.propertiesDenylist,
      ).executeInPlace(uiTree, prevEntryUiTree);
    }

    if (displayName) {
      uiTree.setDisplayName(displayName);
    }

    const predicatesKeepingChildren = [this.propertiesFilter];
    const predicatesDiscardingChildren = [];

    if (this.propertiesDenylist) {
      predicatesDiscardingChildren.push(
        UiTreeUtils.makeDenyListFilterByName(this.propertiesDenylist),
      );
    }

    if (!this.userOptions['showDefaults']?.enabled) {
      predicatesDiscardingChildren.push(
        UiTreeUtils.makeIsNotDefaultFilter(this.defaultAllowlist),
      );
    }

    if (!keepCalculated) {
      predicatesDiscardingChildren.push(UiTreeUtils.isNotCalculated);
    }
    const formatter = new UiTreeFormatter<UiPropertyTreeNode>().setUiTree(
      uiTree,
    );
    if (predicatesDiscardingChildren.length > 0) {
      formatter.addOperation(new Filter(predicatesDiscardingChildren, false));
    }
    formatter.addOperation(new Filter(predicatesKeepingChildren, true));

    this.customOperations?.forEach((op) => formatter.addOperation(op));

    this.formattedTree = formatter.format();
  }

  clear() {
    this.propertiesTree = undefined;
    this.formattedTree = undefined;
  }

  static isPropertyNodeModified: IsModifiedCallbackType = async (
    newTree: TreeNode | undefined,
    oldTree: TreeNode | undefined,
  ) => {
    if (!newTree && !oldTree) return false;
    if (!newTree || !oldTree) return true;

    const newValue = (newTree as UiPropertyTreeNode).formattedValue();
    const oldValue = (oldTree as UiPropertyTreeNode).formattedValue();

    return oldValue !== newValue;
  };
}
