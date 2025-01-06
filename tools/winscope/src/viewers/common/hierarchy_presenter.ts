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
import {InMemoryStorage} from 'common/store/in_memory_storage';
import {Trace, TraceEntry} from 'trace/trace';
import {TraceType} from 'trace/trace_type';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {Operation} from 'trace/tree_node/operations/operation';
import {
  PropertySource,
  PropertyTreeNode,
} from 'trace/tree_node/property_tree_node';
import {TreeNode} from 'trace/tree_node/tree_node';
import {IsModifiedCallbackType} from 'viewers/common/add_diffs';
import {TextFilter} from 'viewers/common/text_filter';
import {UiHierarchyTreeNode} from 'viewers/common/ui_hierarchy_tree_node';
import {TreeNodeFilter, UiTreeUtils} from 'viewers/common/ui_tree_utils';
import {UserOptions} from 'viewers/common/user_options';
import {SimplifyNamesVc} from 'viewers/viewer_view_capture/operations/simplify_names';
import {AddDiffsHierarchyTree} from './add_diffs_hierarchy_tree';
import {AddChips} from './operations/add_chips';
import {Filter} from './operations/filter';
import {FlattenChildren} from './operations/flatten_children';
import {SimplifyNames} from './operations/simplify_names';
import {PropertiesPresenter} from './properties_presenter';
import {UiTreeFormatter} from './ui_tree_formatter';

export type GetHierarchyTreeNameType = (
  entry: TraceEntry<HierarchyTreeNode>,
  tree: HierarchyTreeNode,
) => string;

type FormattedTreeIndex = number;

export interface SelectedTree {
  trace: Trace<HierarchyTreeNode>;
  tree: HierarchyTreeNode;
  index: FormattedTreeIndex;
}

export interface TraceAndTrees {
  trace: Trace<HierarchyTreeNode>;
  trees: HierarchyTreeNode[];
  entry?: TraceEntry<HierarchyTreeNode>;
  formattedTrees?: UiHierarchyTreeNode[];
  displayNames?: string[];
}

export class HierarchyPresenter {
  private hierarchyFilter: TreeNodeFilter;
  private pinnedItems: UiHierarchyTreeNode[] = [];
  private pinnedIds: string[] = [];

  private previousTrees?: TraceAndTrees[] = [];
  private currentTrees?: TraceAndTrees[] = [];
  private selectedTree: SelectedTree | undefined;
  private treeStore: InMemoryStorage | undefined;

  constructor(
    private userOptions: UserOptions,
    private textFilter: TextFilter,
    private denylistProperties: string[],
    private showHeadings: boolean,
    private forceSelectFirstNode: boolean,
    private getHierarchyTreeNameStrategy?: GetHierarchyTreeNameType,
    private customOperations?: Array<
      [TraceType, Array<Operation<UiHierarchyTreeNode>>]
    >,
  ) {
    this.hierarchyFilter = UiTreeUtils.makeNodeFilter(
      textFilter.getFilterPredicate(),
    );
  }

  getUserOptions(): UserOptions {
    return this.userOptions;
  }

  getCurrentEntryForTrace(
    trace: Trace<HierarchyTreeNode>,
  ): TraceEntry<HierarchyTreeNode> | undefined {
    return this.getCurrentTreesByTrace(trace)?.entry;
  }

  getCurrentHierarchyTreesForTrace(
    trace: Trace<HierarchyTreeNode>,
  ): HierarchyTreeNode[] | undefined {
    return this.getCurrentTreesByTrace(trace)?.trees;
  }

  getAllCurrentHierarchyTrees(): TraceAndTrees[] | undefined {
    return this.currentTrees;
  }

  getCurrentHierarchyTreeNames(
    trace: Trace<HierarchyTreeNode>,
  ): string[] | undefined {
    return this.getCurrentTreesByTrace(trace)?.displayNames;
  }

  async addCurrentHierarchyTrees(
    value: TraceAndTrees,
    highlightedItem: string | undefined,
  ) {
    const {trace, trees, entry} = value;
    if (!this.currentTrees) {
      this.currentTrees = [];
    }
    let curr = this.getCurrentTreesByTrace(trace);
    if (curr) {
      curr.trees.push(...trees);
    } else {
      curr = {trace, trees, entry};
      this.currentTrees.push(curr);
    }

    if (!curr.formattedTrees) {
      curr.formattedTrees = [];
    }

    for (let i = 0; i < trees.length; i++) {
      const tree = trees[i];
      const formattedTree = await this.formatTreeAndUpdatePinnedItems(
        trace,
        tree,
        i,
      );
      curr.formattedTrees.push(formattedTree);
    }

    if (!this.selectedTree && highlightedItem) {
      this.applyHighlightedIdChange(highlightedItem);
    }
  }

  getPreviousHierarchyTreeForTrace(
    trace: Trace<HierarchyTreeNode>,
  ): HierarchyTreeNode | undefined {
    return this.previousTrees?.find((p) => p.trace === trace)?.trees[0];
  }

  getPinnedItems(): UiHierarchyTreeNode[] {
    return this.pinnedItems;
  }

  getAllFormattedTrees(): UiHierarchyTreeNode[] | undefined {
    if (!this.currentTrees) {
      return undefined;
    }
    const trees: UiHierarchyTreeNode[] = [];
    this.currentTrees.forEach((curr) => {
      if (curr.formattedTrees) {
        trees.push(...curr.formattedTrees);
      }
    });
    return trees;
  }

  getFormattedTreesByTrace(
    trace: Trace<HierarchyTreeNode>,
  ): UiHierarchyTreeNode[] | undefined {
    return this.getCurrentTreesByTrace(trace)?.formattedTrees;
  }

  getSelectedTree(): SelectedTree | undefined {
    return this.selectedTree;
  }

  setSelectedTree(value: SelectedTree | undefined) {
    this.selectedTree = value;
  }

  getAdjacentVisibleNode(
    treeStore: InMemoryStorage,
    getPrevious: boolean,
  ): UiHierarchyTreeNode | undefined {
    if (!this.selectedTree) {
      return this.currentTrees?.at(0)?.formattedTrees?.at(0);
    }
    let selectedTree: UiHierarchyTreeNode;
    if (this.selectedTree.tree instanceof UiHierarchyTreeNode) {
      selectedTree = this.selectedTree.tree;
    } else {
      selectedTree =
        (this.findSelectedTreeById(this.selectedTree.tree.id, true)
          ?.tree as UiHierarchyTreeNode) ?? undefined;
      if (!selectedTree) {
        return this.currentTrees?.at(0)?.formattedTrees?.at(0);
      }
    }

    this.treeStore = treeStore;
    const adjNode = this.findAdjacentNonHiddenNode(
      selectedTree,
      getPrevious ? (n) => n.getPrevDfs() : (n) => n.getNextDfs(),
    );
    if (adjNode) {
      return adjNode;
    }
    const adjacentNode = getPrevious
      ? this.getPrevNonHiddenNode(this.selectedTree.index)
      : this.getNextNonHiddenNode(selectedTree, this.selectedTree.index);
    this.treeStore = undefined;
    return adjacentNode;
  }

  async updatePreviousHierarchyTrees() {
    if (!this.previousTrees) {
      return;
    }
    for (const prev of this.previousTrees) {
      const previousEntry = assertDefined(prev.entry);
      const previousTree = await previousEntry.getValue();
      prev.trees = [previousTree];
    }
  }

  async applyTracePositionUpdate(
    entries: Array<TraceEntry<HierarchyTreeNode>>,
    highlightedItem: string | undefined,
  ): Promise<void> {
    const currTrees: TraceAndTrees[] = [];
    const prevTrees: TraceAndTrees[] = [];

    for (const entry of entries) {
      const trace = entry.getFullTrace();
      const tree = await entry.getValue();
      currTrees.push({trace, trees: [tree], entry});

      const entryIndex = entry.getIndex();
      if (entryIndex > 0) {
        prevTrees.push({
          trace,
          trees: [],
          entry: trace.getEntry(entryIndex - 1),
        });
      }
    }
    this.currentTrees = currTrees.length > 0 ? currTrees : undefined;
    this.previousTrees = prevTrees.length > 0 ? prevTrees : undefined;
    this.selectedTree = undefined;

    if (this.getHierarchyTreeNameStrategy && entries.length > 0) {
      entries.forEach((entry) => {
        const trace = entry.getFullTrace();
        const curr = this.getCurrentTreesByTrace(trace);
        if (curr) {
          curr.displayNames = curr.trees.map((tree) =>
            assertDefined(this.getHierarchyTreeNameStrategy)(entry, tree),
          );
        }
      });
    }

    if (this.userOptions['showDiff']?.isUnavailable !== undefined) {
      this.userOptions['showDiff'].isUnavailable =
        this.previousTrees === undefined;
    }

    if (this.currentTrees) {
      await this.formatHierarchyTreesAndUpdatePinnedItems();

      if (!highlightedItem && this.forceSelectFirstNode) {
        const {trace, trees: firstTrees} = this.currentTrees[0];
        this.selectedTree = {
          trace,
          tree: firstTrees[0],
          index: 0,
        };
      } else if (highlightedItem) {
        this.applyHighlightedIdChange(highlightedItem);
      }
    }
  }

  applyHighlightedIdChange(newId: string) {
    const tree = this.findSelectedTreeById(newId, false);
    if (tree) {
      this.selectedTree = tree;
    }
  }

  applyHighlightedNodeChange(tree: UiHierarchyTreeNode) {
    if (!this.currentTrees) {
      return;
    }
    if (UiTreeUtils.shouldGetProperties(tree)) {
      const idMatchFilter = UiTreeUtils.makeIdMatchFilter(tree.id);
      let offset = 0;
      for (const {trace, trees} of this.currentTrees) {
        const treeIndex = trees.findIndex((t) => t.findDfs(idMatchFilter));
        if (treeIndex !== -1) {
          this.selectedTree = {trace, tree, index: offset + treeIndex};
          break;
        }
        offset += trees.length;
      }
    }
  }

  async applyHierarchyUserOptionsChange(userOptions: UserOptions) {
    this.userOptions = userOptions;
    await this.formatHierarchyTreesAndUpdatePinnedItems();
  }

  async applyHierarchyFilterChange(textFilter: TextFilter) {
    this.textFilter = textFilter;
    this.hierarchyFilter = UiTreeUtils.makeNodeFilter(
      textFilter.getFilterPredicate(),
    );
    await this.formatHierarchyTreesAndUpdatePinnedItems();
  }

  getTextFilter(): TextFilter {
    return this.textFilter;
  }

  applyPinnedItemChange(pinnedItem: UiHierarchyTreeNode) {
    const pinnedId = pinnedItem.id;
    if (this.pinnedItems.map((item) => item.id).includes(pinnedId)) {
      this.pinnedItems = this.pinnedItems.filter(
        (pinned) => pinned.id !== pinnedId,
      );
    } else {
      // Angular change detection requires new array as input
      this.pinnedItems = this.pinnedItems.concat([pinnedItem]);
    }
    this.updatePinnedIds(pinnedId);
  }

  clear() {
    this.previousTrees = undefined;
    this.currentTrees = undefined;
    this.selectedTree = undefined;
  }

  private updatePinnedIds(newId: string) {
    if (this.pinnedIds.includes(newId)) {
      this.pinnedIds = this.pinnedIds.filter((pinned) => pinned !== newId);
    } else {
      this.pinnedIds.push(newId);
    }
  }

  private async formatHierarchyTreesAndUpdatePinnedItems(): Promise<void> {
    if (!this.currentTrees) {
      return;
    }
    this.pinnedItems = [];

    for (const curr of this.currentTrees) {
      const {trees, trace} = curr;
      const formatted = [];
      for (let i = 0; i < trees.length; i++) {
        const tree = trees[i];
        const formattedTree = await this.formatTreeAndUpdatePinnedItems(
          trace,
          tree,
          i,
        );
        formatted.push(formattedTree);
      }
      curr.formattedTrees = formatted;
    }
  }

  private async formatTreeAndUpdatePinnedItems(
    trace: Trace<HierarchyTreeNode>,
    hierarchyTree: HierarchyTreeNode,
    hierarchyTreeIndex: number | undefined,
  ): Promise<UiHierarchyTreeNode> {
    const formattedTree = await this.formatTree(
      trace,
      hierarchyTree,
      hierarchyTreeIndex,
    );
    this.pinnedItems.push(...this.extractPinnedItems(formattedTree));
    const filteredTree = this.filterTree(formattedTree);
    filteredTree.assignDfsOrder();
    return filteredTree;
  }

  private async formatTree(
    trace: Trace<HierarchyTreeNode>,
    hierarchyTree: HierarchyTreeNode,
    hierarchyTreeIndex: number | undefined,
  ): Promise<UiHierarchyTreeNode> {
    const uiTree = UiHierarchyTreeNode.from(hierarchyTree);

    if (!this.showHeadings) {
      uiTree.forEachNodeDfs((node) => node.setShowHeading(false));
    }
    if (hierarchyTreeIndex !== undefined) {
      const displayName =
        this.getCurrentHierarchyTreeNames(trace)?.at(hierarchyTreeIndex);
      if (displayName) uiTree.setDisplayName(displayName);
    }

    const formatter = new UiTreeFormatter<UiHierarchyTreeNode>().setUiTree(
      uiTree,
    );

    if (
      this.userOptions['showDiff']?.enabled &&
      !this.userOptions['showDiff']?.isUnavailable
    ) {
      const prev = this.previousTrees?.find((p) => p.trace === trace);
      let prevTree = prev?.trees[0];
      if (this.previousTrees && prev?.entry && !prevTree) {
        prevTree = (await prev.entry.getValue()) as HierarchyTreeNode;
        prev.trees = [prevTree];
      }
      const prevEntryUiTree = prevTree
        ? UiHierarchyTreeNode.from(prevTree)
        : undefined;
      await new AddDiffsHierarchyTree(
        HierarchyPresenter.isHierarchyTreeModified,
        this.denylistProperties,
      ).executeInPlace(uiTree, prevEntryUiTree);
    }

    if (this.userOptions['flat']?.enabled) {
      formatter.addOperation(new FlattenChildren());
    }

    formatter.addOperation(new AddChips());

    if (this.userOptions['simplifyNames']?.enabled) {
      formatter.addOperation(
        trace.type === TraceType.VIEW_CAPTURE
          ? new SimplifyNamesVc()
          : new SimplifyNames(),
      );
    }
    this.customOperations?.forEach((traceAndOperations) => {
      const [traceType, operations] = traceAndOperations;
      if (trace.type === traceType) {
        operations.forEach((op) => formatter.addOperation(op));
      }
    });

    return formatter.format();
  }

  private extractPinnedItems(tree: UiHierarchyTreeNode): UiHierarchyTreeNode[] {
    const pinnedNodes = [];

    if (this.pinnedIds.includes(tree.id)) {
      pinnedNodes.push(tree);
    }

    for (const child of tree.getAllChildren()) {
      pinnedNodes.push(...this.extractPinnedItems(child));
    }

    return pinnedNodes;
  }

  private filterTree(formattedTree: UiHierarchyTreeNode): UiHierarchyTreeNode {
    const formatter = new UiTreeFormatter<UiHierarchyTreeNode>().setUiTree(
      formattedTree,
    );
    const predicates = [this.hierarchyFilter];
    if (this.userOptions['showOnlyVisible']?.enabled) {
      predicates.push(UiTreeUtils.isVisible);
    }
    return formatter.addOperation(new Filter(predicates, true)).format();
  }

  private findSelectedTreeById(
    id: string,
    searchFormatted: boolean,
  ): SelectedTree | undefined {
    if (!this.currentTrees) {
      return undefined;
    }
    const idMatchFilter = UiTreeUtils.makeIdMatchFilter(id);
    let indexOffset = 0;
    for (const curr of this.currentTrees) {
      const treesToSearch = searchFormatted
        ? curr.formattedTrees ?? []
        : curr.trees;
      let target: HierarchyTreeNode | undefined;
      const treeIndex = treesToSearch.findIndex((t) => {
        target = t.findDfs(idMatchFilter);
        if (target) {
          return true;
        }
        return false;
      });
      if (target) {
        return {
          trace: curr.trace,
          tree: target,
          index: indexOffset + treeIndex,
        };
      }
      indexOffset += treesToSearch.length;
    }
    return undefined;
  }

  private findAdjacentNonHiddenNode(
    node: UiHierarchyTreeNode,
    getAdj: (n: UiHierarchyTreeNode) => UiHierarchyTreeNode | undefined,
  ): UiHierarchyTreeNode | undefined {
    const adjNode = getAdj(node);
    if (adjNode && this.isHidden(adjNode)) {
      return this.findAdjacentNonHiddenNode(adjNode, getAdj);
    }
    return adjNode;
  }

  private getPrevNonHiddenNode(
    index: FormattedTreeIndex,
  ): UiHierarchyTreeNode | undefined {
    if (index > 0) {
      const trees = assertDefined(this.getAllFormattedTrees());
      return this.findFinalChild(trees[index - 1]);
    }
    return undefined;
  }

  private findFinalChild(node: UiHierarchyTreeNode): UiHierarchyTreeNode {
    const children = node.getAllChildren();
    if (this.isCollapsed(node) || children.length === 0) {
      return node;
    }
    return this.findFinalChild(children[children.length - 1]);
  }

  private getNextNonHiddenNode(
    tree: UiHierarchyTreeNode,
    index: FormattedTreeIndex,
  ): UiHierarchyTreeNode | undefined {
    const trees = assertDefined(this.getAllFormattedTrees());
    if (index < trees.length - 1) {
      return trees[index + 1];
    }
    if (this.isHidden(tree)) {
      return this.findFirstNonHiddenParent(tree);
    }
    return undefined;
  }

  private findFirstNonHiddenParent(
    node: UiHierarchyTreeNode,
  ): UiHierarchyTreeNode | undefined {
    const parent = assertDefined(node.getParent());
    if (!this.isHidden(parent)) {
      return parent;
    }
    return this.findFirstNonHiddenParent(parent);
  }

  private isHidden(node: UiHierarchyTreeNode): boolean {
    const parent = node.getParent();
    if (!parent) {
      return false;
    }
    if (this.isCollapsed(parent)) {
      return true;
    }
    return this.isHidden(parent);
  }

  private isCollapsed(node: UiHierarchyTreeNode): boolean {
    return (
      assertDefined(this.treeStore).get(`${node.id}.collapsedState`) === 'true'
    );
  }

  private getCurrentTreesByTrace(
    trace: Trace<HierarchyTreeNode>,
  ): TraceAndTrees | undefined {
    return this.currentTrees?.find((c) => c.trace === trace);
  }

  static isHierarchyTreeModified: IsModifiedCallbackType = async (
    newTree: TreeNode,
    oldTree: TreeNode,
    denylistProperties: string[],
  ) => {
    if ((newTree as UiHierarchyTreeNode).isRoot()) return false;
    const newProperties = await (
      newTree as UiHierarchyTreeNode
    ).getAllProperties();
    const oldProperties = await (
      oldTree as UiHierarchyTreeNode
    ).getAllProperties();

    return await HierarchyPresenter.isChildPropertyModified(
      newProperties,
      oldProperties,
      denylistProperties,
    );
  };

  private static async isChildPropertyModified(
    newProperties: PropertyTreeNode,
    oldProperties: PropertyTreeNode,
    denylistProperties: string[],
  ): Promise<boolean> {
    for (const newProperty of newProperties
      .getAllChildren()
      .slice()
      .sort(HierarchyPresenter.sortChildren)) {
      if (denylistProperties.includes(newProperty.name)) {
        continue;
      }
      if (newProperty.source === PropertySource.CALCULATED) {
        continue;
      }

      const oldProperty = oldProperties.getChildByName(newProperty.name);
      if (!oldProperty) {
        return true;
      }

      if (newProperty.getAllChildren().length === 0) {
        if (
          await PropertiesPresenter.isPropertyNodeModified(
            newProperty,
            oldProperty,
            denylistProperties,
          )
        ) {
          return true;
        }
      } else {
        const childrenModified =
          await HierarchyPresenter.isChildPropertyModified(
            newProperty,
            oldProperty,
            denylistProperties,
          );
        if (childrenModified) return true;
      }
    }
    return false;
  }

  private static sortChildren(
    a: PropertyTreeNode,
    b: PropertyTreeNode,
  ): number {
    return a.name < b.name ? -1 : 1;
  }
}
