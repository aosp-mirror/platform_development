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
import {Timestamp} from 'common/time';
import {TimeUtils} from 'common/time_utils';
import {WinscopeEvent, WinscopeEventType} from 'messaging/winscope_event';
import {Trace, TraceEntry} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceEntryFinder} from 'trace/trace_entry_finder';
import {TracePosition} from 'trace/trace_position';
import {TraceType} from 'trace/trace_type';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {TreeNode} from 'trace/tree_node/tree_node';
import {ImeAdditionalProperties} from 'viewers/common/ime_additional_properties';
import {ImeUiData} from 'viewers/common/ime_ui_data';
import {ImeLayers, ImeUtils, ProcessedWindowManagerState} from 'viewers/common/ime_utils';
import {TableProperties} from 'viewers/common/table_properties';
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
  private readonly imeTrace: Trace<HierarchyTreeNode>;
  private readonly wmTrace?: Trace<HierarchyTreeNode>;
  private readonly sfTrace?: Trace<HierarchyTreeNode>;
  private hierarchyFilter: TreeNodeFilter = UiTreeUtils.makeNodeFilter('');
  private propertiesFilter: TreeNodeFilter = UiTreeUtils.makeNodeFilter('');
  private pinnedItems: UiHierarchyTreeNode[] = [];
  private pinnedIds: string[] = [];
  private selectedHierarchyTree: HierarchyTreeNode | undefined;
  private currentImeEntryTimestamp: string | undefined;

  readonly notifyViewCallback: NotifyImeViewCallbackType;
  protected readonly dependencies: TraceType[];
  protected uiData: ImeUiData;
  protected highlightedItem = '';
  protected entry: HierarchyTreeNode | undefined;
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
    this.imeTrace = traces.getTrace(dependencies[0]) as Trace<HierarchyTreeNode>;
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
        this.entry = await imeEntry.getValue();
        this.uiData.highlightedItem = this.highlightedItem;
        this.uiData.additionalProperties = this.getAdditionalProperties(
          await wmEntry?.getValue(),
          await sfEntry?.getValue(),
          sfEntry?.getTimestamp(),
          wmEntry?.getTimestamp()
        );
        this.uiData.tree = this.formatHierarchyTreeAndUpdatePinnedItems(
          assertDefined(this.entry),
          true
        );
        this.uiData.hierarchyTableProperties = this.updateHierarchyTableProperties();
      }
      this.copyUiDataAndNotifyView();
    });
  }

  onPinnedItemChange(pinnedItem: UiHierarchyTreeNode) {
    const pinnedId = pinnedItem.id;
    if (this.pinnedItems.map((item) => item.id).includes(pinnedId)) {
      this.pinnedItems = this.pinnedItems.filter((pinned) => pinned.id !== pinnedId);
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
    this.uiData.tree = this.formatHierarchyTreeAndUpdatePinnedItems(
      assertDefined(this.entry),
      true
    );
    if (this.uiData.additionalProperties?.sf) {
      this.uiData.sfSubtrees = this.getSfSubtrees(this.uiData.additionalProperties?.sf);
    }
    this.copyUiDataAndNotifyView();
  }

  onHierarchyFilterChange(filterString: string) {
    this.hierarchyFilter = UiTreeUtils.makeNodeFilter(filterString);
    this.uiData.tree = this.formatHierarchyTreeAndUpdatePinnedItems(
      assertDefined(this.entry),
      true
    );
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
    this.propertiesFilter = UiTreeUtils.makeNodeFilter(filterString);
    await this.updateSelectedTreeUiData();
  }

  async onSelectedHierarchyTreeChange(selectedItem: UiHierarchyTreeNode) {
    if (this.selectedHierarchyTree?.id !== selectedItem.id) {
      this.selectedHierarchyTree = selectedItem;
      await this.updateSelectedTreeUiData();
    }
  }

  async onAdditionalPropertySelected(selectedItem: {name: string | undefined; treeNode: TreeNode}) {
    if (selectedItem.treeNode instanceof HierarchyTreeNode) {
      this.highlightedItem = '';
      this.uiData.highlightedItem = this.highlightedItem;
      this.selectedHierarchyTree = selectedItem.treeNode;
      await this.updateSelectedTreeUiData();
    } else if (selectedItem.treeNode instanceof PropertyTreeNode) {
      this.uiData.propertiesTree = this.formatPropertiesTree(selectedItem.treeNode);
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
      const formattedTaskLayer = this.formatHierarchyTreeAndUpdatePinnedItems(
        sfProperties.taskLayerOfImeContainer,
        false
      );
      sfSubtrees.push(formattedTaskLayer);
    }
    if (sfProperties?.taskLayerOfImeSnapshot) {
      const formattedTaskLayer = this.formatHierarchyTreeAndUpdatePinnedItems(
        sfProperties.taskLayerOfImeSnapshot,
        false
      );
      sfSubtrees.push(formattedTaskLayer);
    }
    sfSubtrees.forEach((subtree) => subtree.setDisplayName('SfSubtree - ' + subtree.name));
    return sfSubtrees;
  }

  private async updateSelectedTreeUiData() {
    if (this.selectedHierarchyTree) {
      this.uiData.propertiesTree = await this.getPropertiesTree(this.selectedHierarchyTree);
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

  private async getPropertiesTree(
    selectedHierarchyTree: HierarchyTreeNode
  ): Promise<UiPropertyTreeNode> {
    const propertiesTree = await selectedHierarchyTree.getAllProperties();
    return this.formatPropertiesTree(propertiesTree);
  }

  private findTraceEntries(
    position: TracePosition
  ): [
    TraceEntry<HierarchyTreeNode> | undefined,
    TraceEntry<HierarchyTreeNode> | undefined,
    TraceEntry<HierarchyTreeNode> | undefined
  ] {
    const imeEntry = TraceEntryFinder.findCorrespondingEntry(this.imeTrace, position);
    if (!imeEntry) {
      return [undefined, undefined, undefined];
    }

    this.currentImeEntryTimestamp = TimeUtils.format(imeEntry.getTimestamp());

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

  private formatHierarchyTreeAndUpdatePinnedItems(
    hierarchyTree: HierarchyTreeNode,
    isImeTree: boolean
  ): UiHierarchyTreeNode {
    const uiTree = UiHierarchyTreeNode.from(hierarchyTree);

    if (isImeTree && this.currentImeEntryTimestamp) {
      const where = hierarchyTree.getEagerPropertyByName('where')?.formattedValue();
      uiTree.setDisplayName(this.currentImeEntryTimestamp + ' - ' + where);

      const client = uiTree.getChildByName('client');
      if (client) {
        const view =
          client.getEagerPropertyByName('viewRootImpl')?.getChildByName('view')?.formattedValue() ??
          'null';
        client.setDisplayName(view);
      }
    }

    const formatter = new UiTreeFormatter<UiHierarchyTreeNode>().setUiTree(uiTree);

    if (this.hierarchyUserOptions['flat']?.enabled) {
      formatter.addOperation(new FlattenChildren());
    }

    const predicates = [this.hierarchyFilter];
    if (this.hierarchyUserOptions['onlyVisible']?.enabled) {
      predicates.push(UiTreeUtils.isVisible);
    }

    formatter.addOperation(new Filter(predicates, true));

    if (!isImeTree) {
      formatter.addOperation(new AddChips());
    }

    if (this.hierarchyUserOptions['simplifyNames']?.enabled) {
      formatter.addOperation(new SimplifyNames());
    }

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

  private formatPropertiesTree(propertiesTree: PropertyTreeNode): UiPropertyTreeNode {
    const predicatesKeepingChildren = [this.propertiesFilter];
    const predicatesDiscardingChildren = [
      UiTreeUtils.isNotCalculated,
      UiTreeUtils.makeDenyListFilter(PresenterSurfaceFlinger.DENYLIST_PROPERTY_NAMES),
    ];
    if (!this.propertiesUserOptions['showDefaults']?.enabled) {
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
