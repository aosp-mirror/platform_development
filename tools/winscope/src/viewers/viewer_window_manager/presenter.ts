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

import {assertDefined} from 'common/assert_utils';
import {PersistentStoreProxy} from 'common/persistent_store_proxy';
import {TimeUtils} from 'common/time_utils';
import {WinscopeEvent, WinscopeEventType} from 'messaging/winscope_event';
import {Trace, TraceEntry} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceEntryFinder} from 'trace/trace_entry_finder';
import {TraceType} from 'trace/trace_type';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {
  PropertySource,
  PropertyTreeNode,
} from 'trace/tree_node/property_tree_node';
import {TreeNode} from 'trace/tree_node/tree_node';
import {IsModifiedCallbackType} from 'viewers/common/add_diffs';
import {AddDiffsHierarchyTree} from 'viewers/common/add_diffs_hierarchy_tree';
import {AddDiffsPropertiesTree} from 'viewers/common/add_diffs_properties_tree';
import {DiffType} from 'viewers/common/diff_type';
import {DisplayIdentifier} from 'viewers/common/display_identifier';
import {AddChips} from 'viewers/common/operations/add_chips';
import {Filter} from 'viewers/common/operations/filter';
import {FlattenChildren} from 'viewers/common/operations/flatten_children';
import {SimplifyNames} from 'viewers/common/operations/simplify_names';
import {UiHierarchyTreeNode} from 'viewers/common/ui_hierarchy_tree_node';
import {UiPropertyTreeNode} from 'viewers/common/ui_property_tree_node';
import {UI_RECT_FACTORY} from 'viewers/common/ui_rect_factory';
import {UiTreeFormatter} from 'viewers/common/ui_tree_formatter';
import {TreeNodeFilter, UiTreeUtils} from 'viewers/common/ui_tree_utils';
import {UserOptions} from 'viewers/common/user_options';
import {UiRect} from 'viewers/components/rects/types2d';
import {UpdateDisplayNames} from './operations/update_display_names';
import {UiData} from './ui_data';

type NotifyViewCallbackType = (uiData: UiData) => void;

export class Presenter {
  private readonly notifyViewCallback: NotifyViewCallbackType;
  private readonly trace: Trace<HierarchyTreeNode>;
  private uiData: UiData;
  private hierarchyFilter: TreeNodeFilter = UiTreeUtils.makeIdFilter('');
  private propertiesFilter: TreeNodeFilter = UiTreeUtils.makePropertyFilter('');
  private highlightedItem = '';
  private highlightedProperty = '';
  private pinnedItems: UiHierarchyTreeNode[] = [];
  private pinnedIds: string[] = [];
  private selectedHierarchyTree: UiHierarchyTreeNode | undefined;
  private previousEntry: TraceEntry<HierarchyTreeNode> | undefined;
  private previousHierarchyTree: HierarchyTreeNode | undefined;
  private currentHierarchyTree: HierarchyTreeNode | undefined;
  private currentHierarchyTreeName: string | undefined;
  private hierarchyUserOptions: UserOptions =
    PersistentStoreProxy.new<UserOptions>(
      'WmHierarchyOptions',
      {
        showDiff: {
          name: 'Show diff',
          enabled: false,
          isUnavailable: false,
        },
        simplifyNames: {
          name: 'Simplify names',
          enabled: true,
        },
        onlyVisible: {
          name: 'Only visible',
          enabled: false,
        },
        flat: {
          name: 'Flat',
          enabled: false,
        },
      },
      this.storage,
    );
  private propertiesUserOptions: UserOptions =
    PersistentStoreProxy.new<UserOptions>(
      'WmPropertyOptions',
      {
        showDiff: {
          name: 'Show diff',
          enabled: false,
          isUnavailable: false,
        },
        showDefaults: {
          name: 'Show defaults',
          enabled: false,
          tooltip: `
                If checked, shows the value of all properties.
                Otherwise, hides all properties whose value is
                the default for its data type.
              `,
        },
      },
      this.storage,
    );

  constructor(
    traces: Traces,
    private storage: Storage,
    notifyViewCallback: NotifyViewCallbackType,
  ) {
    this.trace = assertDefined(traces.getTrace(TraceType.WINDOW_MANAGER));
    this.notifyViewCallback = notifyViewCallback;
    this.uiData = new UiData([TraceType.WINDOW_MANAGER]);
    this.copyUiDataAndNotifyView();
  }

  async onAppEvent(event: WinscopeEvent) {
    await event.visit(
      WinscopeEventType.TRACE_POSITION_UPDATE,
      async (event) => {
        const entry = TraceEntryFinder.findCorrespondingEntry(
          this.trace,
          event.position,
        );
        this.currentHierarchyTree = await entry?.getValue();
        if (entry) {
          this.currentHierarchyTreeName = TimeUtils.format(
            entry.getTimestamp(),
          );
        }

        this.previousEntry =
          entry && entry.getIndex() > 0
            ? this.trace.getEntry(entry.getIndex() - 1)
            : undefined;
        this.previousHierarchyTree = undefined;

        if (this.hierarchyUserOptions['showDiff'].isUnavailable !== undefined) {
          this.hierarchyUserOptions['showDiff'].isUnavailable =
            this.previousEntry == null;
        }
        if (
          this.propertiesUserOptions['showDiff'].isUnavailable !== undefined
        ) {
          this.propertiesUserOptions['showDiff'].isUnavailable =
            this.previousEntry == null;
        }

        this.uiData = new UiData();
        this.uiData.hierarchyUserOptions = this.hierarchyUserOptions;
        this.uiData.propertiesUserOptions = this.propertiesUserOptions;

        if (this.currentHierarchyTree) {
          this.uiData.highlightedItem = this.highlightedItem;
          this.uiData.highlightedProperty = this.highlightedProperty;
          this.uiData.rects = UI_RECT_FACTORY.makeUiRects(
            this.currentHierarchyTree,
          );
          this.uiData.displays = this.getDisplays(this.uiData.rects);
          this.pinnedItems = [];
          this.uiData.tree = await this.formatHierarchyTreeAndUpdatePinnedItems(
            this.currentHierarchyTree,
          );
        }

        this.copyUiDataAndNotifyView();
      },
    );
  }

  onPinnedItemChange(pinnedItem: UiHierarchyTreeNode) {
    const pinnedId = pinnedItem.id;
    if (this.pinnedItems.map((item) => item.id).includes(pinnedId)) {
      this.pinnedItems = this.pinnedItems.filter(
        (pinned) => pinned.id !== pinnedId,
      );
    } else {
      this.pinnedItems.push(pinnedItem);
    }
    this.updatePinnedIds(pinnedId);
    this.uiData.pinnedItems = this.pinnedItems;
    this.copyUiDataAndNotifyView();
  }

  onHighlightedItemChange(id: string) {
    if (this.highlightedItem === id) {
      this.highlightedItem = '';
    } else {
      this.highlightedItem = id;
    }
    this.uiData.highlightedItem = this.highlightedItem;
    this.copyUiDataAndNotifyView();
  }

  onHighlightedPropertyChange(id: string) {
    if (this.highlightedProperty === id) {
      this.highlightedProperty = '';
    } else {
      this.highlightedProperty = id;
    }
    this.uiData.highlightedProperty = this.highlightedProperty;
    this.copyUiDataAndNotifyView();
  }

  async onHierarchyUserOptionsChange(userOptions: UserOptions) {
    this.hierarchyUserOptions = userOptions;
    this.uiData.hierarchyUserOptions = this.hierarchyUserOptions;
    this.uiData.tree = await this.formatHierarchyTreeAndUpdatePinnedItems(
      this.currentHierarchyTree,
    );
    this.copyUiDataAndNotifyView();
  }

  async onHierarchyFilterChange(filterString: string) {
    this.hierarchyFilter = UiTreeUtils.makeIdFilter(filterString);
    this.uiData.tree = await this.formatHierarchyTreeAndUpdatePinnedItems(
      this.currentHierarchyTree,
    );
    this.copyUiDataAndNotifyView();
  }

  async onPropertiesUserOptionsChange(userOptions: UserOptions) {
    this.propertiesUserOptions = userOptions;
    this.uiData.propertiesUserOptions = this.propertiesUserOptions;
    await this.updateSelectedTreeUiData();
  }

  async onPropertiesFilterChange(filterString: string) {
    this.propertiesFilter = UiTreeUtils.makePropertyFilter(filterString);
    await this.updateSelectedTreeUiData();
  }

  async onSelectedHierarchyTreeChange(selectedTree: UiHierarchyTreeNode) {
    if (
      !selectedTree.isOldNode() ||
      selectedTree.getDiff() === DiffType.DELETED
    ) {
      this.selectedHierarchyTree = selectedTree;
      await this.updateSelectedTreeUiData();
    }
  }

  private getDisplays(rects: UiRect[]): DisplayIdentifier[] {
    const ids: DisplayIdentifier[] = [];
    rects.forEach((rect: UiRect) => {
      if (!rect.isDisplay) return;
      const displayName = rect.label.slice(10, rect.label.length);
      ids.push({displayId: rect.id, groupId: rect.groupId, name: displayName});
    });
    return ids.sort((a, b) => {
      if (a.name < b.name) {
        return -1;
      }
      if (a.name > b.name) {
        return 1;
      }
      return 0;
    });
  }

  private async updateSelectedTreeUiData() {
    if (this.selectedHierarchyTree) {
      const propertiesTree =
        await this.selectedHierarchyTree.getAllProperties();

      this.uiData.propertiesTree = await this.formatPropertiesTree(
        propertiesTree,
        this.selectedHierarchyTree.isRoot(),
      );
    }
    this.copyUiDataAndNotifyView();
  }

  private async formatHierarchyTreeAndUpdatePinnedItems(
    hierarchyTree: HierarchyTreeNode | undefined,
  ): Promise<UiHierarchyTreeNode | undefined> {
    if (!hierarchyTree) return undefined;

    const uiTree = UiHierarchyTreeNode.from(hierarchyTree);

    if (this.currentHierarchyTreeName) {
      uiTree.setDisplayName(this.currentHierarchyTreeName);
    }

    const formatter = new UiTreeFormatter<UiHierarchyTreeNode>().setUiTree(
      uiTree,
    );

    if (
      this.hierarchyUserOptions['showDiff']?.enabled &&
      !this.hierarchyUserOptions['showDiff']?.isUnavailable
    ) {
      if (this.previousEntry && !this.previousHierarchyTree) {
        this.previousHierarchyTree = await this.previousEntry.getValue();
      }
      const prevEntryUiTree = this.previousHierarchyTree
        ? UiHierarchyTreeNode.from(this.previousHierarchyTree)
        : undefined;
      await new AddDiffsHierarchyTree(
        this.isHierarchyTreeModified,
      ).executeInPlace(uiTree, prevEntryUiTree);
    }

    if (this.hierarchyUserOptions['flat']?.enabled) {
      formatter.addOperation(new FlattenChildren());
    }

    const predicates = [this.hierarchyFilter];
    if (this.hierarchyUserOptions['onlyVisible']?.enabled) {
      predicates.push(UiTreeUtils.isVisible);
    }

    formatter
      .addOperation(new Filter(predicates, true))
      .addOperation(new AddChips());

    if (this.hierarchyUserOptions['simplifyNames']?.enabled) {
      formatter.addOperation(new SimplifyNames());
    }

    formatter.addOperation(new UpdateDisplayNames());

    const formattedTree = formatter.format();
    this.pinnedItems.push(...this.getPinnedItems(formattedTree));
    this.uiData.pinnedItems = this.pinnedItems;
    return formattedTree;
  }

  private getPinnedItems(tree: UiHierarchyTreeNode): UiHierarchyTreeNode[] {
    const pinnedNodes = [];

    if (this.pinnedIds.includes(tree.id)) {
      pinnedNodes.push(tree);
    }

    for (const child of tree.getAllChildren()) {
      pinnedNodes.push(...this.getPinnedItems(child));
    }

    return pinnedNodes;
  }

  private async formatPropertiesTree(
    propertiesTree: PropertyTreeNode,
    isEntryTree: boolean,
  ): Promise<UiPropertyTreeNode> {
    const uiTree = UiPropertyTreeNode.from(propertiesTree);

    if (
      this.propertiesUserOptions['showDiff']?.enabled &&
      !this.propertiesUserOptions['showDiff']?.isUnavailable
    ) {
      if (this.previousEntry && !this.previousHierarchyTree) {
        this.previousHierarchyTree = await this.previousEntry.getValue();
      }
      const prevEntryNode = this.previousHierarchyTree?.findDfs(
        UiTreeUtils.makeIdMatchFilter(propertiesTree.id),
      );
      const prevEntryUiTree = prevEntryNode
        ? UiPropertyTreeNode.from(await prevEntryNode.getAllProperties())
        : undefined;
      await new AddDiffsPropertiesTree(
        this.isPropertyNodeModified,
      ).executeInPlace(uiTree, prevEntryUiTree);
    }

    if (isEntryTree && this.currentHierarchyTreeName) {
      uiTree.setDisplayName(this.currentHierarchyTreeName);
    }

    const predicatesKeepingChildren = [this.propertiesFilter];
    const predicatesDiscardingChildren = [
      UiTreeUtils.makeDenyListFilter(Presenter.DENYLIST_PROPERTY_NAMES),
    ];

    if (!this.propertiesUserOptions['showDefaults']?.enabled) {
      predicatesDiscardingChildren.push(UiTreeUtils.isNotDefault);
    }

    if (!isEntryTree) {
      predicatesDiscardingChildren.push(UiTreeUtils.isNotCalculated);
    }

    return new UiTreeFormatter<UiPropertyTreeNode>()
      .setUiTree(uiTree)
      .addOperation(new Filter(predicatesDiscardingChildren, false))
      .addOperation(new Filter(predicatesKeepingChildren, true))
      .format();
  }

  private updatePinnedIds(newId: string) {
    if (this.pinnedIds.includes(newId)) {
      this.pinnedIds = this.pinnedIds.filter((pinned) => pinned !== newId);
    } else {
      this.pinnedIds.push(newId);
    }
  }

  private copyUiDataAndNotifyView() {
    // Create a shallow copy of the data, otherwise the Angular OnPush change detection strategy
    // won't detect the new input
    const copy = Object.assign({}, this.uiData);
    this.notifyViewCallback(copy);
  }

  private isHierarchyTreeModified: IsModifiedCallbackType = async (
    newTree: TreeNode | undefined,
    oldTree: TreeNode | undefined,
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

    return await this.isChildPropertyModified(newProperties, oldProperties);
  };

  private async isChildPropertyModified(
    newProperties: PropertyTreeNode,
    oldProperties: PropertyTreeNode,
  ): Promise<boolean> {
    for (const newProperty of newProperties
      .getAllChildren()
      .slice()
      .sort(this.sortChildren)) {
      if (Presenter.DENYLIST_PROPERTY_NAMES.includes(newProperty.name)) {
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
        if (await this.isPropertyNodeModified(newProperty, oldProperty)) {
          return true;
        }
      } else {
        const childrenModified = await this.isChildPropertyModified(
          newProperty,
          oldProperty,
        );
        if (childrenModified) return true;
      }
    }
    return false;
  }

  private isPropertyNodeModified: IsModifiedCallbackType = async (
    newTree: TreeNode | undefined,
    oldTree: TreeNode | undefined,
  ) => {
    if (!newTree && !oldTree) return false;
    if (!newTree || !oldTree) return true;

    const newValue = (newTree as UiPropertyTreeNode).formattedValue();
    const oldValue = (oldTree as UiPropertyTreeNode).formattedValue();

    return oldValue !== newValue;
  };

  private sortChildren(a: PropertyTreeNode, b: PropertyTreeNode): number {
    return a.name < b.name ? -1 : 1;
  }

  static readonly DENYLIST_PROPERTY_NAMES = [
    'name',
    'children',
    'dpiX',
    'dpiY',
  ];
}
