
/*
 * Copyright (C) 2022 The Android Open Source Project
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
import { ImeUiData } from "./ime_ui_data";
import { TraceType } from "common/trace/trace_type";
import { UserOptions } from "viewers/common/user_options";
import { HierarchyTreeNode, PropertiesTreeNode } from "viewers/common/ui_tree_utils";
import { TreeGenerator } from "viewers/common/tree_generator";
import { TreeTransformer } from "viewers/common/tree_transformer";
import { TreeUtils, FilterType } from "common/utils/tree_utils";

export type NotifyImeViewCallbackType = (uiData: ImeUiData) => void;

export class ImePresenter {
  constructor(
    notifyViewCallback: NotifyImeViewCallbackType,
    dependencies: Array<TraceType>
  ) {
    this.notifyViewCallback = notifyViewCallback;
    this.dependencies = dependencies;
    this.uiData = new ImeUiData(dependencies);
    this.notifyViewCallback(this.uiData);
  }

  public updatePinnedItems(pinnedItem: HierarchyTreeNode) {
    const pinnedId = `${pinnedItem.id}`;
    if (this.pinnedItems.map(item => `${item.id}`).includes(pinnedId)) {
      this.pinnedItems = this.pinnedItems.filter(pinned => `${pinned.id}` != pinnedId);
    } else {
      this.pinnedItems.push(pinnedItem);
    }
    this.updatePinnedIds(pinnedId);
    this.uiData.pinnedItems = this.pinnedItems;
    this.notifyViewCallback(this.uiData);
  }

  public updateHighlightedItems(id: string) {
    if (this.highlightedItems.includes(id)) {
      this.highlightedItems = this.highlightedItems.filter(hl => hl != id);
    } else {
      this.highlightedItems = []; //if multi-select surfaces implemented, remove this line
      this.highlightedItems.push(id);
    }
    this.uiData.highlightedItems = this.highlightedItems;
    this.notifyViewCallback(this.uiData);
  }

  public updateHierarchyTree(userOptions: UserOptions) {
    this.hierarchyUserOptions = userOptions;
    this.uiData.hierarchyUserOptions = this.hierarchyUserOptions;
    this.uiData.tree = this.generateTree();
    this.notifyViewCallback(this.uiData);
  }

  public filterHierarchyTree(filterString: string) {
    this.hierarchyFilter = TreeUtils.makeNodeFilter(filterString);
    this.uiData.tree = this.generateTree();
    this.notifyViewCallback(this.uiData);
  }

  public updatePropertiesTree(userOptions: UserOptions) {
    this.propertiesUserOptions = userOptions;
    this.uiData.propertiesUserOptions = this.propertiesUserOptions;
    this.updateSelectedTreeUiData();
  }

  public filterPropertiesTree(filterString: string) {
    this.propertiesFilter = TreeUtils.makeNodeFilter(filterString);
    this.updateSelectedTreeUiData();
  }

  public newPropertiesTree(selectedItem: HierarchyTreeNode) {
    this.selectedHierarchyTree = selectedItem;
    this.updateSelectedTreeUiData();
  }

  public notifyCurrentTraceEntries(entries: Map<TraceType, [any, any]>) {
    this.uiData = new ImeUiData(this.dependencies);
    this.uiData.hierarchyUserOptions = this.hierarchyUserOptions;
    this.uiData.propertiesUserOptions = this.propertiesUserOptions;

    const imeEntries = entries.get(this.dependencies[0]);
    if (imeEntries) {
      this.entry = imeEntries[0];
      if (this.entry) {
        this.uiData.highlightedItems = this.highlightedItems;
        this.uiData.tree = this.generateTree();
      }
    }
    this.notifyViewCallback(this.uiData);
  }

  private updateSelectedTreeUiData() {
    if (this.selectedHierarchyTree) {
      this.uiData.propertiesTree = this.getTreeWithTransformedProperties(this.selectedHierarchyTree);
    }
    this.notifyViewCallback(this.uiData);
  }

  private generateTree() {
    if (!this.entry) {
      return null;
    }

    const generator = new TreeGenerator(this.entry, this.hierarchyFilter, this.pinnedIds)
      .setIsOnlyVisibleView(this.hierarchyUserOptions["onlyVisible"]?.enabled)
      .setIsSimplifyNames(this.hierarchyUserOptions["simplifyNames"]?.enabled)
      .setIsFlatView(this.hierarchyUserOptions["flat"]?.enabled)
      .withUniqueNodeId();
    const tree: HierarchyTreeNode | null = generator.generateTree();
    this.pinnedItems = generator.getPinnedItems();
    this.uiData.pinnedItems = this.pinnedItems;
    return tree;
  }

  private updatePinnedIds(newId: string) {
    if (this.pinnedIds.includes(newId)) {
      this.pinnedIds = this.pinnedIds.filter(pinned => pinned != newId);
    } else {
      this.pinnedIds.push(newId);
    }
  }

  private getTreeWithTransformedProperties(selectedTree: HierarchyTreeNode): PropertiesTreeNode {
    const transformer = new TreeTransformer(selectedTree, this.propertiesFilter)
      .setIsShowDefaults(this.propertiesUserOptions["showDefaults"]?.enabled)
      .setTransformerOptions({skip: selectedTree.skip})
      .setProperties(this.entry);
    const transformedTree = transformer.transform();
    return transformedTree;
  }

  readonly notifyViewCallback: NotifyImeViewCallbackType;
  readonly dependencies: Array<TraceType>;
  uiData: ImeUiData;
  private hierarchyFilter: FilterType = TreeUtils.makeNodeFilter("");
  private propertiesFilter: FilterType = TreeUtils.makeNodeFilter("");
  private highlightedItems: Array<string> = [];
  private pinnedItems: Array<HierarchyTreeNode> = [];
  private pinnedIds: Array<string> = [];
  private selectedHierarchyTree: HierarchyTreeNode | null = null;
  private entry: any = null;
  private hierarchyUserOptions: UserOptions = {
    simplifyNames: {
      name: "Simplify names",
      enabled: true
    },
    onlyVisible: {
      name: "Only visible",
      enabled: false
    },
    flat: {
      name: "Flat",
      enabled: false
    }
  };

  private propertiesUserOptions: UserOptions = {
    showDefaults: {
      name: "Show defaults",
      enabled: true,
      tooltip: `
                If checked, shows the value of all properties.
                Otherwise, hides all properties whose value is
                the default for its data type.
              `
    },
  };
}
