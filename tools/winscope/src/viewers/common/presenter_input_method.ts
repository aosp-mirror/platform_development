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

import {PersistentStoreProxy} from 'common/persistent_store_proxy';
import {Timestamp} from 'common/time';
import {FilterType, TreeUtils} from 'common/tree_utils';
import {WinscopeEvent, WinscopeEventType} from 'messaging/winscope_event';
import {Trace, TraceEntry} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceEntryFinder} from 'trace/trace_entry_finder';
import {TracePosition} from 'trace/trace_position';
import {TraceTreeNode} from 'trace/trace_tree_node';
import {TraceType} from 'trace/trace_type';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {TreeNode} from 'trace/tree_node/tree_node';
import {ImeAdditionalProperties} from 'viewers/common/ime_additional_properties';
import {ImeUiData} from 'viewers/common/ime_ui_data';
import {ImeLayers, ImeUtils, ProcessedWindowManagerState} from 'viewers/common/ime_utils';
import {TableProperties} from 'viewers/common/table_properties';
import {TreeGenerator} from 'viewers/common/tree_generator';
import {TreeTransformer} from 'viewers/common/tree_transformer';
import {
  HierarchyTreeNodeLegacy,
  PropertiesTreeNodeLegacy,
} from 'viewers/common/ui_tree_utils_legacy';
import {UserOptions} from 'viewers/common/user_options';
import {Presenter as PresenterSurfaceFlinger} from 'viewers/viewer_surface_flinger/presenter';
import {AddChips} from './operations/add_chips';
import {Filter} from './operations/filter';
import {FlattenChildren} from './operations/flatten_children';
import {SimplifyNames} from './operations/simplify_names';
import {UiHierarchyTreeNode} from './ui_hierarchy_tree_node';
import {UiPropertyTreeNode} from './ui_property_tree_node';
import {UiTreeFormatter} from './ui_tree_formatter';
import {TreeNodeFilter, UiTreeUtils} from './ui_tree_utils';

type NotifyImeViewCallbackType = (uiData: ImeUiData) => void;

export abstract class PresenterInputMethod {
  private readonly imeTrace: Trace<object>;
  private readonly wmTrace?: Trace<HierarchyTreeNode>;
  private readonly sfTrace?: Trace<HierarchyTreeNode>;
  private hierarchyFilter: FilterType = TreeUtils.makeNodeFilter('');
  private layerHierarchyFilter: TreeNodeFilter = UiTreeUtils.makeNodeFilter('');
  private propertiesFilter: FilterType = TreeUtils.makeNodeFilter('');
  private layerPropertiesFilter: TreeNodeFilter = UiTreeUtils.makeNodeFilter('');
  private pinnedItems: Array<HierarchyTreeNodeLegacy | UiHierarchyTreeNode> = [];
  private pinnedIds: string[] = [];
  private selectedHierarchyTree: HierarchyTreeNodeLegacy | HierarchyTreeNode | undefined;

  readonly notifyViewCallback: NotifyImeViewCallbackType;
  protected readonly dependencies: TraceType[];
  protected uiData: ImeUiData;
  protected highlightedItem: string = '';
  protected entry: TraceTreeNode | null = null;
  protected additionalPropertyEntry: TraceTreeNode | null = null;
  protected hierarchyUserOptions: UserOptions = PersistentStoreProxy.new<UserOptions>(
    'ImeHierarchyOptions',
    {
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
    this.storage
  );
  protected propertiesUserOptions: UserOptions = PersistentStoreProxy.new<UserOptions>(
    'ImePropertiesOptions',
    {
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
    this.storage
  );

  constructor(
    traces: Traces,
    private storage: Storage,
    dependencies: TraceType[],
    notifyViewCallback: NotifyImeViewCallbackType
  ) {
    this.imeTrace = traces.getTrace(dependencies[0]) as Trace<TraceTreeNode>;
    this.sfTrace = traces.getTrace(TraceType.SURFACE_FLINGER);
    this.wmTrace = traces.getTrace(TraceType.WINDOW_MANAGER);

    this.dependencies = dependencies;
    this.notifyViewCallback = notifyViewCallback;
    this.uiData = new ImeUiData(dependencies);
    this.copyUiDataAndNotifyView();
  }

  async onAppEvent(event: WinscopeEvent) {
    await event.visit(WinscopeEventType.TRACE_POSITION_UPDATE, async (event) => {
      this.uiData = new ImeUiData(this.dependencies);
      this.uiData.hierarchyUserOptions = this.hierarchyUserOptions;
      this.uiData.propertiesUserOptions = this.propertiesUserOptions;
      this.selectedHierarchyTree = undefined;

      const [imeEntry, sfEntry, wmEntry] = this.findTraceEntries(event.position);

      if (imeEntry) {
        this.entry = (await imeEntry.getValue()) as TraceTreeNode;
        this.uiData.highlightedItem = this.highlightedItem;
        this.uiData.additionalProperties = this.getAdditionalProperties(
          await wmEntry?.getValue(),
          await sfEntry?.getValue(),
          sfEntry?.getTimestamp(),
          wmEntry?.getTimestamp()
        );
        this.uiData.tree = this.generateTree();
        this.uiData.hierarchyTableProperties = this.updateHierarchyTableProperties();
      }
      this.copyUiDataAndNotifyView();
    });
  }

  onPinnedItemChange(pinnedItem: HierarchyTreeNodeLegacy | UiHierarchyTreeNode) {
    const pinnedId = `${pinnedItem.id}`;
    if (this.pinnedItems.map((item) => `${item.id}`).includes(pinnedId)) {
      this.pinnedItems = this.pinnedItems.filter((pinned) => `${pinned.id}` !== pinnedId);
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
      this.highlightedItem = id; //if multi-select surfaces implemented, remove this line
    }
    this.uiData.highlightedItem = this.highlightedItem;
    this.notifyViewCallback(this.uiData);
  }

  onHierarchyUserOptionsChange(userOptions: UserOptions) {
    this.hierarchyUserOptions = userOptions;
    this.uiData.hierarchyUserOptions = this.hierarchyUserOptions;
    this.uiData.tree = this.generateTree();
    if (this.uiData.additionalProperties?.sf) {
      this.uiData.sfSubtrees = this.getSfSubtrees(this.uiData.additionalProperties?.sf);
    }
    this.copyUiDataAndNotifyView();
  }

  onHierarchyFilterChange(filterString: string) {
    this.hierarchyFilter = TreeUtils.makeNodeFilter(filterString);
    this.layerHierarchyFilter = UiTreeUtils.makeNodeFilter(filterString);
    this.uiData.tree = this.generateTree();
    if (this.uiData.additionalProperties?.sf) {
      this.uiData.sfSubtrees = this.getSfSubtrees(this.uiData.additionalProperties?.sf);
    }
    this.copyUiDataAndNotifyView();
  }

  async onPropertiesUserOptionsChange(userOptions: UserOptions) {
    this.propertiesUserOptions = userOptions;
    this.uiData.propertiesUserOptions = this.propertiesUserOptions;
    await this.updateSelectedTreeUiData();
  }

  async onPropertiesFilterChange(filterString: string) {
    this.propertiesFilter = TreeUtils.makeNodeFilter(filterString);
    this.layerPropertiesFilter = UiTreeUtils.makeNodeFilter(filterString);
    await this.updateSelectedTreeUiData();
  }

  async onSelectedHierarchyTreeChange(selectedItem: HierarchyTreeNodeLegacy | UiHierarchyTreeNode) {
    if (this.selectedHierarchyTree?.id !== selectedItem.id) {
      this.additionalPropertyEntry = null;
      this.selectedHierarchyTree = selectedItem;
      await this.updateSelectedTreeUiData();
    }
  }

  async onAdditionalPropertySelected(selectedItem: {name: string | undefined; treeNode: TreeNode}) {
    if (selectedItem.treeNode instanceof HierarchyTreeNode) {
      this.highlightedItem = '';
      this.uiData.highlightedItem = this.highlightedItem;

      this.selectedHierarchyTree = selectedItem.treeNode;
      this.additionalPropertyEntry = {
        name: selectedItem.name ?? '',
        kind: 'AdditionalProperty',
        children: [],
        stableId: 'AdditionalProperty',
        proto: selectedItem,
      };
      await this.updateSelectedTreeUiData();
    } else if (selectedItem.treeNode instanceof PropertyTreeNode) {
      this.uiData.propertiesTree = this.formatAdditionalPropertiesTree(selectedItem.treeNode);
      this.copyUiDataAndNotifyView();
    }
  }

  protected getAdditionalProperties(
    wmEntry: HierarchyTreeNode | undefined,
    sfEntry: HierarchyTreeNode | undefined,
    sfEntryTimestamp: Timestamp | undefined,
    wmEntryTimestamp: Timestamp | undefined
  ): ImeAdditionalProperties {
    let wmProperties: ProcessedWindowManagerState | undefined;
    let sfProperties: ImeLayers | undefined;

    if (wmEntry) {
      wmProperties = ImeUtils.processWindowManagerTraceEntry(wmEntry, wmEntryTimestamp);

      if (sfEntry) {
        sfProperties = ImeUtils.getImeLayers(sfEntry, wmProperties, sfEntryTimestamp);

        if (sfProperties) this.uiData.sfSubtrees = this.getSfSubtrees(sfProperties);
      }
    }

    return new ImeAdditionalProperties(wmProperties, sfProperties);
  }

  private getSfSubtrees(sfProperties: ImeLayers): UiHierarchyTreeNode[] {
    const sfSubtrees: UiHierarchyTreeNode[] = [];
    if (sfProperties?.taskLayerOfImeContainer) {
      const formattedTaskLayer = this.formatSfSubtreeAndUpdatePinnedItems(
        sfProperties.taskLayerOfImeContainer
      );
      sfSubtrees.push(formattedTaskLayer);
    }
    if (sfProperties?.taskLayerOfImeSnapshot) {
      const formattedTaskLayer = this.formatSfSubtreeAndUpdatePinnedItems(
        sfProperties.taskLayerOfImeSnapshot
      );
      sfSubtrees.push(formattedTaskLayer);
    }
    sfSubtrees.forEach((subtree) => subtree.setDisplayName('SfSubtree - ' + subtree.name));
    return sfSubtrees;
  }

  protected generateTree() {
    if (!this.entry) {
      return null;
    }

    const generator = new TreeGenerator(this.entry, this.hierarchyFilter, this.pinnedIds)
      .setIsOnlyVisibleView(this.hierarchyUserOptions['onlyVisible']?.enabled)
      .setIsSimplifyNames(this.hierarchyUserOptions['simplifyNames']?.enabled)
      .setIsFlatView(this.hierarchyUserOptions['flat']?.enabled)
      .withUniqueNodeId();
    const tree: HierarchyTreeNodeLegacy | null = generator.generateTree();
    this.pinnedItems = generator.getPinnedItems();
    this.uiData.pinnedItems = this.pinnedItems;
    return tree;
  }

  private async updateSelectedTreeUiData() {
    if (this.selectedHierarchyTree instanceof HierarchyTreeNodeLegacy) {
      this.uiData.propertiesTree = this.getTreeWithTransformedProperties(
        this.selectedHierarchyTree
      );
    } else if (this.selectedHierarchyTree instanceof HierarchyTreeNode) {
      this.uiData.propertiesTree = await this.getAdditionalPropertiesTree(
        this.selectedHierarchyTree
      );
    }
    this.copyUiDataAndNotifyView();
  }

  private updatePinnedIds(newId: string) {
    if (this.pinnedIds.includes(newId)) {
      this.pinnedIds = this.pinnedIds.filter((pinned) => pinned !== newId);
    } else {
      this.pinnedIds.push(newId);
    }
  }

  private getTreeWithTransformedProperties(
    selectedTree: HierarchyTreeNodeLegacy
  ): PropertiesTreeNodeLegacy {
    const transformer = new TreeTransformer(selectedTree, this.propertiesFilter)
      .setOnlyProtoDump(this.additionalPropertyEntry != null)
      .setIsShowDefaults(this.propertiesUserOptions['showDefaults']?.enabled)
      .setTransformerOptions({skip: selectedTree.skip})
      .setProperties(this.additionalPropertyEntry ?? this.entry);
    const transformedTree = transformer.transform();
    return transformedTree;
  }

  private async getAdditionalPropertiesTree(
    selectedHierarchyTree: HierarchyTreeNode
  ): Promise<UiPropertyTreeNode> {
    const propertiesTree = await selectedHierarchyTree.getAllProperties();
    return this.formatAdditionalPropertiesTree(propertiesTree);
  }

  private findTraceEntries(
    position: TracePosition
  ): [
    TraceEntry<object> | undefined,
    TraceEntry<HierarchyTreeNode> | undefined,
    TraceEntry<HierarchyTreeNode> | undefined
  ] {
    const imeEntry = TraceEntryFinder.findCorrespondingEntry(this.imeTrace, position);
    if (!imeEntry) {
      return [undefined, undefined, undefined];
    }

    if (!this.imeTrace.hasFrameInfo()) {
      return [imeEntry, undefined, undefined];
    }

    const frames = imeEntry.getFramesRange();
    if (!frames || frames.start === frames.end) {
      return [imeEntry, undefined, undefined];
    }

    const frame = frames.start;
    const sfEntry = this.sfTrace?.getFrame(frame)?.findClosestEntry(imeEntry.getTimestamp());
    const wmEntry = this.wmTrace?.getFrame(frame)?.findClosestEntry(imeEntry.getTimestamp());

    return [imeEntry, sfEntry, wmEntry];
  }

  private formatSfSubtreeAndUpdatePinnedItems(subtree: HierarchyTreeNode): UiHierarchyTreeNode {
    const formatter = new UiTreeFormatter<UiHierarchyTreeNode>().setUiTree(
      UiHierarchyTreeNode.from(subtree)
    );
    if (this.hierarchyUserOptions['flat']?.enabled) {
      formatter.addOperation(new FlattenChildren());
    }

    const predicates = [this.layerHierarchyFilter];
    if (this.hierarchyUserOptions['onlyVisible']?.enabled) {
      predicates.push(UiTreeUtils.isVisible);
    }

    formatter.addOperation(new Filter(predicates, true)).addOperation(new AddChips());

    if (this.hierarchyUserOptions['simplifyNames']?.enabled) {
      formatter.addOperation(new SimplifyNames());
    }

    const formattedTree = formatter.format();
    this.pinnedItems.push(...this.getSfPinnedItems(formattedTree));
    this.uiData.pinnedItems = this.pinnedItems;
    return formattedTree;
  }

  private getSfPinnedItems(tree: UiHierarchyTreeNode): UiHierarchyTreeNode[] {
    const pinnedNodes = [];

    if (this.pinnedIds.includes(tree.id)) {
      pinnedNodes.push(tree);
    }

    for (const child of tree.getAllChildren()) {
      pinnedNodes.push(...this.getSfPinnedItems(child));
    }

    return pinnedNodes;
  }

  private formatAdditionalPropertiesTree(propertiesTree: PropertyTreeNode): UiPropertyTreeNode {
    const predicatesKeepingChildren = [this.layerPropertiesFilter];
    const predicatesDiscardingChildren = [
      UiTreeUtils.isNotCalculated,
      UiTreeUtils.makeDenyListFilter(PresenterSurfaceFlinger.DENYLIST_PROPERTY_NAMES),
    ];
    if (this.propertiesUserOptions['showDefaults']?.enabled) {
      predicatesDiscardingChildren.push(UiTreeUtils.isNotDefault);
    }

    return new UiTreeFormatter<UiPropertyTreeNode>()
      .setUiTree(UiPropertyTreeNode.from(propertiesTree))
      .addOperation(new Filter(predicatesDiscardingChildren, false))
      .addOperation(new Filter(predicatesKeepingChildren, true))
      .format();
  }

  private copyUiDataAndNotifyView() {
    // Create a shallow copy of the data, otherwise the Angular OnPush change detection strategy
    // won't detect the new input
    const copy = Object.assign({}, this.uiData);
    this.notifyViewCallback(copy);
  }

  protected abstract updateHierarchyTableProperties(): TableProperties;
}
