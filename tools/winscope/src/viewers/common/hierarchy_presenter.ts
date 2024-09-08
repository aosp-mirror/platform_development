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
import {TextFilter} from './text_filter';
import {UiTreeFormatter} from './ui_tree_formatter';

export type GetHierarchyTreeNameType = (
  entry: TraceEntry<HierarchyTreeNode>,
  tree: HierarchyTreeNode,
) => string;

export class HierarchyPresenter {
  private hierarchyFilter: TreeNodeFilter;
  private pinnedItems: UiHierarchyTreeNode[] = [];
  private pinnedIds: string[] = [];

  private previousEntries:
    | Map<Trace<HierarchyTreeNode>, TraceEntry<HierarchyTreeNode>>
    | undefined;
  private previousHierarchyTrees? = new Map<
    Trace<HierarchyTreeNode>,
    HierarchyTreeNode
  >();

  private currentEntries:
    | Map<Trace<HierarchyTreeNode>, TraceEntry<HierarchyTreeNode>>
    | undefined;
  private currentHierarchyTrees? = new Map<
    Trace<HierarchyTreeNode>,
    HierarchyTreeNode[]
  >();
  private currentHierarchyTreeNames:
    | Map<Trace<HierarchyTreeNode>, string[]>
    | undefined;
  private currentFormattedTrees:
    | Map<Trace<HierarchyTreeNode>, UiHierarchyTreeNode[]>
    | undefined;
  private selectedHierarchyTree:
    | [Trace<HierarchyTreeNode>, HierarchyTreeNode]
    | undefined;

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
      textFilter.filterString,
      textFilter.flags,
    );
  }

  getUserOptions(): UserOptions {
    return this.userOptions;
  }

  getCurrentEntryForTrace(
    trace: Trace<HierarchyTreeNode>,
  ): TraceEntry<HierarchyTreeNode> | undefined {
    return this.currentEntries?.get(trace);
  }

  getCurrentHierarchyTreesForTrace(
    trace: Trace<HierarchyTreeNode>,
  ): HierarchyTreeNode[] | undefined {
    return this.currentHierarchyTrees?.get(trace);
  }

  getAllCurrentHierarchyTrees():
    | Array<[Trace<HierarchyTreeNode>, HierarchyTreeNode[]]>
    | undefined {
    const currentTrees = [];
    for (const entry of this.currentHierarchyTrees?.entries() ?? []) {
      currentTrees.push(entry);
    }
    return currentTrees;
  }

  getCurrentHierarchyTreeNames(
    trace: Trace<HierarchyTreeNode>,
  ): string[] | undefined {
    return this.currentHierarchyTreeNames?.get(trace);
  }

  async addCurrentHierarchyTrees(
    value: [Trace<HierarchyTreeNode>, HierarchyTreeNode[]],
    highlightedItem: string | undefined,
  ) {
    const [trace, trees] = value;
    if (!this.currentHierarchyTrees) {
      this.currentHierarchyTrees = new Map();
    }
    const curr = this.currentHierarchyTrees.get(trace);
    if (curr) {
      curr.push(...trees);
    } else {
      this.currentHierarchyTrees.set(trace, trees);
    }

    if (!this.currentFormattedTrees) {
      this.currentFormattedTrees = new Map();
    }
    if (!this.currentFormattedTrees.get(trace)) {
      this.currentFormattedTrees.set(trace, []);
    }

    for (let i = 0; i < trees.length; i++) {
      const tree = trees[i];
      const formattedTree = await this.formatTreeAndUpdatePinnedItems(
        trace,
        tree,
        i,
      );
      assertDefined(this.currentFormattedTrees.get(trace)).push(formattedTree);
    }

    if (!this.selectedHierarchyTree && highlightedItem) {
      this.applyHighlightedIdChange(highlightedItem);
    }
  }

  getPreviousHierarchyTreeForTrace(
    trace: Trace<HierarchyTreeNode>,
  ): HierarchyTreeNode | undefined {
    return this.previousHierarchyTrees?.get(trace);
  }

  getPinnedItems(): UiHierarchyTreeNode[] {
    return this.pinnedItems;
  }

  getAllFormattedTrees(): UiHierarchyTreeNode[] | undefined {
    if (!this.currentFormattedTrees || this.currentFormattedTrees.size === 0) {
      return undefined;
    }
    return Array.from(this.currentFormattedTrees.values()).flat();
  }

  getFormattedTreesByTrace(
    trace: Trace<HierarchyTreeNode>,
  ): UiHierarchyTreeNode[] | undefined {
    return this.currentFormattedTrees?.get(trace);
  }

  getSelectedTree(): [Trace<HierarchyTreeNode>, HierarchyTreeNode] | undefined {
    return this.selectedHierarchyTree;
  }

  setSelectedTree(
    value: [Trace<HierarchyTreeNode>, HierarchyTreeNode] | undefined,
  ) {
    this.selectedHierarchyTree = value;
  }

  async updatePreviousHierarchyTrees() {
    if (!this.previousEntries) {
      this.previousHierarchyTrees = undefined;
      return;
    }
    const previousTrees = new Map<
      Trace<HierarchyTreeNode>,
      HierarchyTreeNode
    >();
    for (const previousEntry of this.previousEntries.values()) {
      const trace = previousEntry.getFullTrace();
      const previousTree = await previousEntry.getValue();
      previousTrees.set(trace, previousTree);
    }
    this.previousHierarchyTrees = previousTrees;
  }

  async applyTracePositionUpdate(
    entries: Array<TraceEntry<HierarchyTreeNode>>,
    highlightedItem: string | undefined,
  ): Promise<void> {
    const currEntries = new Map<
      Trace<HierarchyTreeNode>,
      TraceEntry<HierarchyTreeNode>
    >();
    const currTrees = new Map<Trace<HierarchyTreeNode>, HierarchyTreeNode[]>();
    const prevEntries = new Map<
      Trace<HierarchyTreeNode>,
      TraceEntry<HierarchyTreeNode>
    >();

    for (const entry of entries) {
      const trace = entry.getFullTrace();
      currEntries.set(trace, entry);

      const tree: HierarchyTreeNode | undefined = await entry?.getValue();
      if (tree) currTrees.set(trace, [tree]);

      const entryIndex = entry.getIndex();
      if (entryIndex > 0) {
        prevEntries.set(trace, trace.getEntry(entryIndex - 1));
      }
    }
    this.currentEntries = currEntries.size > 0 ? currEntries : undefined;
    this.currentHierarchyTrees = currTrees.size > 0 ? currTrees : undefined;
    this.previousEntries = prevEntries.size > 0 ? prevEntries : undefined;
    this.previousHierarchyTrees =
      prevEntries.size > 0
        ? new Map<Trace<HierarchyTreeNode>, HierarchyTreeNode>()
        : undefined;
    this.selectedHierarchyTree = undefined;

    const names = new Map<Trace<HierarchyTreeNode>, string[]>();
    if (this.getHierarchyTreeNameStrategy && entries.length > 0) {
      entries.forEach((entry) => {
        const trace = entry.getFullTrace();
        const trees = this.currentHierarchyTrees?.get(trace);
        if (trees) {
          names.set(
            entry.getFullTrace(),
            trees.map((tree) =>
              assertDefined(this.getHierarchyTreeNameStrategy)(entry, tree),
            ),
          );
        }
      });
    }
    this.currentHierarchyTreeNames = names;

    if (this.userOptions['showDiff']?.isUnavailable !== undefined) {
      this.userOptions['showDiff'].isUnavailable =
        this.previousEntries === undefined;
    }

    if (this.currentHierarchyTrees) {
      this.currentFormattedTrees = assertDefined(
        await this.formatHierarchyTreesAndUpdatePinnedItems(
          this.currentHierarchyTrees,
        ),
      );

      if (!highlightedItem && this.forceSelectFirstNode) {
        const firstTrees = Array.from(this.currentHierarchyTrees.entries())[0];
        this.selectedHierarchyTree = [firstTrees[0], firstTrees[1][0]];
      } else if (highlightedItem && this.currentFormattedTrees) {
        this.applyHighlightedIdChange(highlightedItem);
      }
    }
  }

  applyHighlightedIdChange(newId: string) {
    if (!this.currentHierarchyTrees) {
      return;
    }
    const idMatchFilter = UiTreeUtils.makeIdMatchFilter(newId);
    for (const [trace, trees] of this.currentHierarchyTrees) {
      let highlightedNode: HierarchyTreeNode | undefined;
      trees.find((t) => {
        const target = t.findDfs(idMatchFilter);
        if (target) {
          highlightedNode = target;
          return true;
        }
        return false;
      });
      if (highlightedNode) {
        this.selectedHierarchyTree = [trace, highlightedNode];
        break;
      }
    }
  }

  applyHighlightedNodeChange(selectedTree: UiHierarchyTreeNode) {
    if (!this.currentHierarchyTrees) {
      return;
    }
    if (UiTreeUtils.shouldGetProperties(selectedTree)) {
      const idMatchFilter = UiTreeUtils.makeIdMatchFilter(selectedTree.id);
      for (const [trace, trees] of this.currentHierarchyTrees) {
        const hasTree = trees.find((t) => t.findDfs(idMatchFilter));
        if (hasTree) {
          this.selectedHierarchyTree = [trace, selectedTree];
          break;
        }
      }
    }
  }

  async applyHierarchyUserOptionsChange(userOptions: UserOptions) {
    this.userOptions = userOptions;
    this.currentFormattedTrees =
      await this.formatHierarchyTreesAndUpdatePinnedItems(
        this.currentHierarchyTrees,
      );
  }

  async applyHierarchyFilterChange(textFilter: TextFilter) {
    this.textFilter = textFilter;
    this.hierarchyFilter = UiTreeUtils.makeNodeFilter(
      textFilter.filterString,
      textFilter.flags,
    );
    this.currentFormattedTrees =
      await this.formatHierarchyTreesAndUpdatePinnedItems(
        this.currentHierarchyTrees,
      );
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
    this.previousEntries = undefined;
    this.previousHierarchyTrees = undefined;
    this.currentEntries = undefined;
    this.currentHierarchyTrees = undefined;
    this.currentHierarchyTreeNames = undefined;
    this.currentFormattedTrees = undefined;
    this.selectedHierarchyTree = undefined;
  }

  private updatePinnedIds(newId: string) {
    if (this.pinnedIds.includes(newId)) {
      this.pinnedIds = this.pinnedIds.filter((pinned) => pinned !== newId);
    } else {
      this.pinnedIds.push(newId);
    }
  }

  private async formatHierarchyTreesAndUpdatePinnedItems(
    hierarchyTrees:
      | Map<Trace<HierarchyTreeNode>, HierarchyTreeNode[]>
      | undefined,
  ): Promise<Map<Trace<HierarchyTreeNode>, UiHierarchyTreeNode[]> | undefined> {
    this.pinnedItems = [];

    if (!hierarchyTrees) return undefined;

    const formattedTrees = new Map<
      Trace<HierarchyTreeNode>,
      UiHierarchyTreeNode[]
    >();

    for (const [trace, trees] of hierarchyTrees.entries()) {
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
      formattedTrees.set(trace, formatted);
    }
    return formattedTrees;
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
      const displayName = this.currentHierarchyTreeNames
        ?.get(trace)
        ?.at(hierarchyTreeIndex);
      if (displayName) uiTree.setDisplayName(displayName);
    }

    const formatter = new UiTreeFormatter<UiHierarchyTreeNode>().setUiTree(
      uiTree,
    );

    if (
      this.userOptions['showDiff']?.enabled &&
      !this.userOptions['showDiff']?.isUnavailable
    ) {
      let prevTree = this.previousHierarchyTrees?.get(trace);
      if (this.previousHierarchyTrees && !prevTree) {
        prevTree = await this.previousEntries?.get(trace)?.getValue();
        if (prevTree) this.previousHierarchyTrees.set(trace, prevTree);
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

  static isHierarchyTreeModified: IsModifiedCallbackType = async (
    newTree: TreeNode | undefined,
    oldTree: TreeNode | undefined,
    denylistProperties: string[],
  ) => {
    if (!newTree && !oldTree) return false;
    if (!newTree || !oldTree) return true;
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
