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
import {FilterType, TreeUtils} from 'common/tree_utils';
import {LayerTraceEntry} from 'trace/flickerlib/layers/LayerTraceEntry';
import {WindowManagerState} from 'trace/flickerlib/windows/WindowManagerState';
import {Trace, TraceEntry} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceEntryFinder} from 'trace/trace_entry_finder';
import {TracePosition} from 'trace/trace_position';
import {TraceTreeNode} from 'trace/trace_tree_node';
import {TraceType} from 'trace/trace_type';
import {ImeAdditionalProperties} from 'viewers/common/ime_additional_properties';
import {ImeUiData} from 'viewers/common/ime_ui_data';
import {ImeLayers, ImeUtils, ProcessedWindowManagerState} from 'viewers/common/ime_utils';
import {TableProperties} from 'viewers/common/table_properties';
import {TreeGenerator} from 'viewers/common/tree_generator';
import {TreeTransformer} from 'viewers/common/tree_transformer';
import {HierarchyTreeNode, PropertiesTreeNode} from 'viewers/common/ui_tree_utils';
import {UserOptions} from 'viewers/common/user_options';

type NotifyImeViewCallbackType = (uiData: ImeUiData) => void;

export abstract class PresenterInputMethod {
  private readonly imeTrace: Trace<object>;
  private readonly wmTrace?: Trace<WindowManagerState>;
  private readonly sfTrace?: Trace<LayerTraceEntry>;
  private hierarchyFilter: FilterType = TreeUtils.makeNodeFilter('');
  private propertiesFilter: FilterType = TreeUtils.makeNodeFilter('');
  private pinnedItems: HierarchyTreeNode[] = [];
  private pinnedIds: string[] = [];
  private selectedHierarchyTree: HierarchyTreeNode | null = null;

  readonly notifyViewCallback: NotifyImeViewCallbackType;
  protected readonly dependencies: TraceType[];
  protected uiData: ImeUiData;
  protected highlightedItems: string[] = [];
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
    this.notifyViewCallback(this.uiData);
  }

  async onTracePositionUpdate(position: TracePosition) {
    this.uiData = new ImeUiData(this.dependencies);
    this.uiData.hierarchyUserOptions = this.hierarchyUserOptions;
    this.uiData.propertiesUserOptions = this.propertiesUserOptions;

    const [imeEntry, sfEntry, wmEntry] = this.findTraceEntries(position);

    if (imeEntry) {
      this.entry = (await imeEntry.getValue()) as TraceTreeNode;
      this.uiData.highlightedItems = this.highlightedItems;
      this.uiData.additionalProperties = this.getAdditionalProperties(
        await wmEntry?.getValue(),
        await sfEntry?.getValue()
      );
      this.uiData.tree = this.generateTree();
      this.uiData.hierarchyTableProperties = this.updateHierarchyTableProperties();
    }
    this.notifyViewCallback(this.uiData);
  }

  updatePinnedItems(pinnedItem: HierarchyTreeNode) {
    const pinnedId = `${pinnedItem.id}`;
    if (this.pinnedItems.map((item) => `${item.id}`).includes(pinnedId)) {
      this.pinnedItems = this.pinnedItems.filter((pinned) => `${pinned.id}` !== pinnedId);
    } else {
      this.pinnedItems.push(pinnedItem);
    }
    this.updatePinnedIds(pinnedId);
    this.uiData.pinnedItems = this.pinnedItems;
    this.notifyViewCallback(this.uiData);
  }

  updateHighlightedItems(id: string) {
    if (this.highlightedItems.includes(id)) {
      this.highlightedItems = this.highlightedItems.filter((hl) => hl !== id);
    } else {
      this.highlightedItems = []; //if multi-select surfaces implemented, remove this line
      this.highlightedItems.push(id);
    }
    this.uiData.highlightedItems = this.highlightedItems;
    this.notifyViewCallback(this.uiData);
  }

  updateHierarchyTree(userOptions: UserOptions) {
    this.hierarchyUserOptions = userOptions;
    this.uiData.hierarchyUserOptions = this.hierarchyUserOptions;
    this.uiData.tree = this.generateTree();
    this.notifyViewCallback(this.uiData);
  }

  filterHierarchyTree(filterString: string) {
    this.hierarchyFilter = TreeUtils.makeNodeFilter(filterString);
    this.uiData.tree = this.generateTree();
    this.notifyViewCallback(this.uiData);
  }

  updatePropertiesTree(userOptions: UserOptions) {
    this.propertiesUserOptions = userOptions;
    this.uiData.propertiesUserOptions = this.propertiesUserOptions;
    this.updateSelectedTreeUiData();
  }

  filterPropertiesTree(filterString: string) {
    this.propertiesFilter = TreeUtils.makeNodeFilter(filterString);
    this.updateSelectedTreeUiData();
  }

  newPropertiesTree(selectedItem: HierarchyTreeNode) {
    this.additionalPropertyEntry = null;
    this.selectedHierarchyTree = selectedItem;
    this.updateSelectedTreeUiData();
  }

  newAdditionalPropertiesTree(selectedItem: any) {
    this.selectedHierarchyTree = new HierarchyTreeNode(
      selectedItem.name,
      'AdditionalProperty',
      'AdditionalProperty'
    );
    this.additionalPropertyEntry = {
      name: selectedItem.name,
      kind: 'AdditionalProperty',
      children: [],
      stableId: 'AdditionalProperty',
      proto: selectedItem.proto,
    };
    this.updateSelectedTreeUiData();
  }

  protected getAdditionalProperties(
    wmEntry: TraceTreeNode | undefined,
    sfEntry: TraceTreeNode | undefined
  ) {
    let wmProperties: ProcessedWindowManagerState | undefined;
    let sfProperties: ImeLayers | undefined;
    let sfSubtrees: any[];

    if (wmEntry) {
      wmProperties = ImeUtils.processWindowManagerTraceEntry(wmEntry);

      if (sfEntry) {
        sfProperties = ImeUtils.getImeLayers(sfEntry, wmProperties);
        sfSubtrees = [sfProperties?.taskOfImeContainer, sfProperties?.taskOfImeSnapshot]
          .filter((node) => node) // filter away null values
          .map((node) => {
            node.kind = 'SF subtree - ' + node.id;
            return node;
          });
        this.entry?.children.push(...sfSubtrees);
      }
    }

    return new ImeAdditionalProperties(wmProperties, sfProperties);
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
    const tree: HierarchyTreeNode | null = generator.generateTree();
    this.pinnedItems = generator.getPinnedItems();
    this.uiData.pinnedItems = this.pinnedItems;
    return tree;
  }

  private updateSelectedTreeUiData() {
    if (this.selectedHierarchyTree) {
      this.uiData.propertiesTree = this.getTreeWithTransformedProperties(
        this.selectedHierarchyTree
      );
    }
    this.notifyViewCallback(this.uiData);
  }
  private updatePinnedIds(newId: string) {
    if (this.pinnedIds.includes(newId)) {
      this.pinnedIds = this.pinnedIds.filter((pinned) => pinned !== newId);
    } else {
      this.pinnedIds.push(newId);
    }
  }

  private getTreeWithTransformedProperties(selectedTree: HierarchyTreeNode): PropertiesTreeNode {
    const transformer = new TreeTransformer(selectedTree, this.propertiesFilter)
      .setOnlyProtoDump(this.additionalPropertyEntry != null)
      .setIsShowDefaults(this.propertiesUserOptions['showDefaults']?.enabled)
      .setTransformerOptions({skip: selectedTree.skip})
      .setProperties(this.additionalPropertyEntry ?? this.entry);
    const transformedTree = transformer.transform();
    return transformedTree;
  }

  private findTraceEntries(
    position: TracePosition
  ): [
    TraceEntry<object> | undefined,
    TraceEntry<LayerTraceEntry> | undefined,
    TraceEntry<WindowManagerState> | undefined
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

  protected abstract updateHierarchyTableProperties(): TableProperties;
}
