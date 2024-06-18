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

import {ArrayUtils} from 'common/array_utils';
import {assertDefined} from 'common/assert_utils';
import {PersistentStoreProxy} from 'common/persistent_store_proxy';
import {WinscopeEvent, WinscopeEventType} from 'messaging/winscope_event';
import {Trace, TraceEntry} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceEntryFinder} from 'trace/trace_entry_finder';
import {TraceType} from 'trace/trace_type';
import {TIMESTAMP_FORMATTER} from 'trace/tree_node/formatters';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {DEFAULT_PROPERTY_TREE_NODE_FACTORY} from 'trace/tree_node/property_tree_node_factory';
import {Filter} from 'viewers/common/operations/filter';
import {UiPropertyTreeNode} from 'viewers/common/ui_property_tree_node';
import {UiTreeFormatter} from 'viewers/common/ui_tree_formatter';
import {UiTreeUtils} from 'viewers/common/ui_tree_utils';
import {UserOptions} from 'viewers/common/user_options';
import {SetRootDisplayNames} from './operations/set_root_display_name';
import {UiData, UiDataEntry, UiDataEntryType} from './ui_data';

type NotifyViewCallbackType = (uiData: UiData) => void;

export class Presenter {
  private readonly trace: Trace<PropertyTreeNode>;
  private entry?: TraceEntry<PropertyTreeNode>;
  private originalIndicesOfUiDataEntries: number[];
  private uiData = UiData.EMPTY;

  private isInitialized = false;
  private allUiDataEntries: UiDataEntry[] = [];
  private allVSyncIds: string[] = [];
  private allPids: string[] = [];
  private allUids: string[] = [];
  private allTypes: string[] = [];
  private allLayerAndDisplayIds: string[] = [];
  private allTransactionIds: string[] = [];
  private allFlags: string[] = [];

  private vsyncIdFilter: string[] = [];
  private pidFilter: string[] = [];
  private uidFilter: string[] = [];
  private typeFilter: string[] = [];
  private layerIdFilter: string[] = [];
  private whatFilter: string[] = [];
  private transactionIdFilter: string[] = [];

  private currentPropertiesTree: PropertyTreeNode | undefined;

  private propertiesUserOptions: UserOptions =
    PersistentStoreProxy.new<UserOptions>(
      'TransactionsPropertyOptions',
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
      this.storage,
    );

  private readonly notifyUiDataCallback: NotifyViewCallbackType;
  private static readonly VALUE_NA = 'N/A';

  constructor(
    traces: Traces,
    private readonly storage: Storage,
    notifyViewCallback: NotifyViewCallbackType,
  ) {
    this.trace = assertDefined(traces.getTrace(TraceType.TRANSACTIONS));
    this.notifyUiDataCallback = notifyViewCallback;
    this.originalIndicesOfUiDataEntries = [];
    this.notifyUiDataCallback(this.uiData);
  }

  async onAppEvent(event: WinscopeEvent) {
    await event.visit(
      WinscopeEventType.TRACE_POSITION_UPDATE,
      async (event) => {
        await this.initializeIfNeeded();
        this.entry = TraceEntryFinder.findCorrespondingEntry(
          this.trace,
          event.position,
        );
        this.uiData.currentEntryIndex = this.computeCurrentEntryIndex();
        this.uiData.selectedEntryIndex = undefined;
        this.uiData.scrollToIndex = this.uiData.currentEntryIndex;
        this.currentPropertiesTree = this.computeCurrentPropertiesTree(
          this.uiData.entries,
          this.uiData.currentEntryIndex,
          this.uiData.selectedEntryIndex,
        );
        this.uiData.currentPropertiesTree = this.formatPropertiesTree(
          this.currentPropertiesTree,
        );

        this.notifyUiDataCallback(this.uiData);
      },
    );
  }

  onVSyncIdFilterChanged(vsyncIds: string[]) {
    this.vsyncIdFilter = vsyncIds;
    this.uiData = this.computeUiData();
    this.notifyUiDataCallback(this.uiData);
  }

  onPidFilterChanged(pids: string[]) {
    this.pidFilter = pids;
    this.uiData = this.computeUiData();
    this.notifyUiDataCallback(this.uiData);
  }

  onUidFilterChanged(uids: string[]) {
    this.uidFilter = uids;
    this.uiData = this.computeUiData();
    this.notifyUiDataCallback(this.uiData);
  }

  onTypeFilterChanged(types: string[]) {
    this.typeFilter = types;
    this.uiData = this.computeUiData();
    this.notifyUiDataCallback(this.uiData);
  }

  onLayerIdFilterChanged(ids: string[]) {
    this.layerIdFilter = ids;
    this.uiData = this.computeUiData();
    this.notifyUiDataCallback(this.uiData);
  }

  onWhatFilterChanged(flags: string[]) {
    this.whatFilter = flags;
    this.uiData = this.computeUiData();
    this.notifyUiDataCallback(this.uiData);
  }

  onTransactionIdFilterChanged(ids: string[]) {
    this.transactionIdFilter = ids;
    this.uiData = this.computeUiData();
    this.notifyUiDataCallback(this.uiData);
  }

  onEntryClicked(index: number) {
    if (this.uiData.selectedEntryIndex === index) {
      return;
    } else {
      this.uiData.selectedEntryIndex = index;
    }

    this.uiData.scrollToIndex = undefined; // no scrolling

    this.currentPropertiesTree = this.computeCurrentPropertiesTree(
      this.uiData.entries,
      this.uiData.currentEntryIndex,
      this.uiData.selectedEntryIndex,
    );

    this.uiData.currentPropertiesTree = this.formatPropertiesTree(
      this.currentPropertiesTree,
    );

    this.notifyUiDataCallback(this.uiData);
  }

  onPropertiesUserOptionsChange(userOptions: UserOptions) {
    this.propertiesUserOptions = userOptions;
    this.uiData.propertiesUserOptions = this.propertiesUserOptions;
    this.uiData.currentPropertiesTree = this.formatPropertiesTree(
      this.currentPropertiesTree,
    );
    this.notifyUiDataCallback(this.uiData);
  }

  private async initializeIfNeeded() {
    if (this.isInitialized) {
      return;
    }

    this.allUiDataEntries = await this.makeUiDataEntries();

    this.allVSyncIds = this.getUniqueUiDataEntryValues(
      this.allUiDataEntries,
      (entry: UiDataEntry) => entry.vsyncId.toString(),
    );
    this.allPids = this.getUniqueUiDataEntryValues(
      this.allUiDataEntries,
      (entry: UiDataEntry) => entry.pid,
    );
    this.allUids = this.getUniqueUiDataEntryValues(
      this.allUiDataEntries,
      (entry: UiDataEntry) => entry.uid,
    );
    this.allTypes = this.getUniqueUiDataEntryValues(
      this.allUiDataEntries,
      (entry: UiDataEntry) => entry.type,
    );
    this.allLayerAndDisplayIds = this.getUniqueUiDataEntryValues(
      this.allUiDataEntries,
      (entry: UiDataEntry) => entry.layerOrDisplayId,
    );
    this.allTransactionIds = this.getUniqueUiDataEntryValues(
      this.allUiDataEntries,
      (entry: UiDataEntry) => entry.transactionId,
    );
    this.allFlags = this.getUniqueUiDataEntryValues(
      this.allUiDataEntries,
      (entry: UiDataEntry) => entry.what.split('|').map((flag) => flag.trim()),
    );

    this.uiData = this.computeUiData();

    this.isInitialized = true;
  }

  private computeUiData(): UiData {
    const entries = this.allUiDataEntries;

    let filteredEntries = entries;

    if (this.vsyncIdFilter.length > 0) {
      filteredEntries = filteredEntries.filter((entry) =>
        this.vsyncIdFilter.includes(entry.vsyncId.toString()),
      );
    }

    if (this.pidFilter.length > 0) {
      filteredEntries = filteredEntries.filter((entry) =>
        this.pidFilter.includes(entry.pid),
      );
    }

    if (this.uidFilter.length > 0) {
      filteredEntries = filteredEntries.filter((entry) =>
        this.uidFilter.includes(entry.uid),
      );
    }

    if (this.typeFilter.length > 0) {
      filteredEntries = filteredEntries.filter((entry) =>
        this.typeFilter.includes(entry.type),
      );
    }

    if (this.layerIdFilter.length > 0) {
      filteredEntries = filteredEntries.filter((entry) =>
        this.layerIdFilter.includes(entry.layerOrDisplayId),
      );
    }

    if (this.whatFilter.length > 0) {
      filteredEntries = filteredEntries.filter(
        (entry) =>
          this.whatFilter.find((flag) => entry.what.includes(flag)) !==
          undefined,
      );
    }

    if (this.transactionIdFilter.length > 0) {
      filteredEntries = filteredEntries.filter((entry) =>
        this.transactionIdFilter.includes(entry.transactionId.toString()),
      );
    }

    this.originalIndicesOfUiDataEntries = filteredEntries.map(
      (entry) => entry.originalIndexInTraceEntry,
    );

    const currentEntryIndex = this.computeCurrentEntryIndex();
    const selectedEntryIndex = undefined;
    this.currentPropertiesTree = this.computeCurrentPropertiesTree(
      filteredEntries,
      currentEntryIndex,
      selectedEntryIndex,
    );

    const formattedPropertiesTree = this.formatPropertiesTree(
      this.currentPropertiesTree,
    );

    return new UiData(
      this.allVSyncIds,
      this.allPids,
      this.allUids,
      this.allTypes,
      this.allLayerAndDisplayIds,
      this.allTransactionIds,
      this.allFlags,
      filteredEntries,
      currentEntryIndex,
      selectedEntryIndex,
      currentEntryIndex,
      formattedPropertiesTree,
      this.propertiesUserOptions,
    );
  }

  private computeCurrentEntryIndex(): undefined | number {
    if (!this.entry) {
      return undefined;
    }

    if (this.originalIndicesOfUiDataEntries.length === 0) {
      return undefined;
    }

    return (
      ArrayUtils.binarySearchFirstGreaterOrEqual(
        this.originalIndicesOfUiDataEntries,
        this.entry.getIndex(),
      ) ?? this.originalIndicesOfUiDataEntries.length - 1
    );
  }

  private computeCurrentPropertiesTree(
    entries: UiDataEntry[],
    currentEntryIndex: undefined | number,
    selectedEntryIndex: undefined | number,
  ): PropertyTreeNode | undefined {
    if (selectedEntryIndex !== undefined) {
      return entries[selectedEntryIndex].propertiesTree;
    }
    if (currentEntryIndex !== undefined) {
      return entries[currentEntryIndex].propertiesTree;
    }
    return undefined;
  }

  private formatPropertiesTree(
    propertiesTree: PropertyTreeNode | undefined,
  ): UiPropertyTreeNode | undefined {
    if (!propertiesTree) return undefined;

    const uiTree = UiPropertyTreeNode.from(propertiesTree);
    const formatter = new UiTreeFormatter<UiPropertyTreeNode>().setUiTree(
      uiTree,
    );

    if (!this.propertiesUserOptions['showDefaults']?.enabled) {
      formatter.addOperation(
        new Filter(
          [
            UiTreeUtils.isNotDefault,
            UiTreeUtils.makePropertyMatchFilter('IDENTITY'),
          ],
          false,
        ),
      );
    }

    return formatter.addOperation(new SetRootDisplayNames()).format();
  }

  private async makeUiDataEntries(): Promise<UiDataEntry[]> {
    const entries: UiDataEntry[] = [];

    const entryProtos = await Promise.all(
      this.trace.mapEntry(async (entry) => {
        return await entry.getValue();
      }),
    );

    for (
      let originalIndex = 0;
      originalIndex < this.trace.lengthEntries;
      ++originalIndex
    ) {
      const entry = this.trace.getEntry(originalIndex);
      const entryNode = entryProtos[originalIndex];
      const vsyncId = Number(
        assertDefined(entryNode.getChildByName('vsyncId')).getValue(),
      );

      const entryTimestamp =
        DEFAULT_PROPERTY_TREE_NODE_FACTORY.makeCalculatedProperty(
          'TransactionsTraceEntry',
          'timestamp',
          entry.getTimestamp(),
        );
      entryTimestamp.setFormatter(TIMESTAMP_FORMATTER);

      for (const transactionState of assertDefined(
        entryNode.getChildByName('transactions'),
      ).getAllChildren()) {
        const pid = assertDefined(
          transactionState.getChildByName('pid'),
        ).formattedValue();
        const uid = assertDefined(
          transactionState.getChildByName('uid'),
        ).formattedValue();
        const transactionId = assertDefined(
          transactionState.getChildByName('transactionId'),
        ).formattedValue();

        const layerChanges = assertDefined(
          transactionState.getChildByName('layerChanges'),
        ).getAllChildren();
        for (const layerState of layerChanges) {
          entries.push(
            new UiDataEntry(
              originalIndex,
              entryTimestamp,
              vsyncId,
              pid,
              uid,
              UiDataEntryType.LAYER_CHANGED,
              assertDefined(
                layerState.getChildByName('layerId'),
              ).formattedValue(),
              transactionId,
              assertDefined(layerState.getChildByName('what')).formattedValue(),
              layerState,
            ),
          );
        }

        const displayChanges = assertDefined(
          transactionState.getChildByName('displayChanges'),
        ).getAllChildren();
        for (const displayState of displayChanges) {
          entries.push(
            new UiDataEntry(
              originalIndex,
              entryTimestamp,
              vsyncId,
              pid,
              uid,
              UiDataEntryType.DISPLAY_CHANGED,
              assertDefined(displayState.getChildByName('id')).formattedValue(),
              transactionId,
              assertDefined(
                displayState.getChildByName('what'),
              ).formattedValue(),
              displayState,
            ),
          );
        }

        if (layerChanges.length === 0 && displayChanges.length === 0) {
          entries.push(
            new UiDataEntry(
              originalIndex,
              entryTimestamp,
              vsyncId,
              pid,
              uid,
              UiDataEntryType.NO_OP,
              '',
              transactionId,
              '',
              undefined,
            ),
          );
        }
      }

      for (const layerCreationArgs of assertDefined(
        entryNode.getChildByName('addedLayers'),
      ).getAllChildren()) {
        entries.push(
          new UiDataEntry(
            originalIndex,
            entryTimestamp,
            vsyncId,
            Presenter.VALUE_NA,
            Presenter.VALUE_NA,
            UiDataEntryType.LAYER_ADDED,
            assertDefined(
              layerCreationArgs.getChildByName('layerId'),
            ).formattedValue(),
            '',
            '',
            layerCreationArgs,
          ),
        );
      }

      for (const destroyedLayerId of assertDefined(
        entryNode.getChildByName('destroyedLayers'),
      ).getAllChildren()) {
        entries.push(
          new UiDataEntry(
            originalIndex,
            entryTimestamp,
            vsyncId,
            Presenter.VALUE_NA,
            Presenter.VALUE_NA,
            UiDataEntryType.LAYER_DESTROYED,
            destroyedLayerId.formattedValue(),
            '',
            '',
            destroyedLayerId,
          ),
        );
      }

      for (const displayState of assertDefined(
        entryNode.getChildByName('addedDisplays'),
      ).getAllChildren()) {
        entries.push(
          new UiDataEntry(
            originalIndex,
            entryTimestamp,
            vsyncId,
            Presenter.VALUE_NA,
            Presenter.VALUE_NA,
            UiDataEntryType.DISPLAY_ADDED,
            assertDefined(displayState.getChildByName('id')).formattedValue(),
            '',
            assertDefined(displayState.getChildByName('what')).formattedValue(),
            displayState,
          ),
        );
      }

      for (const removedDisplayId of assertDefined(
        entryNode.getChildByName('removedDisplays'),
      ).getAllChildren()) {
        entries.push(
          new UiDataEntry(
            originalIndex,
            entryTimestamp,
            vsyncId,
            Presenter.VALUE_NA,
            Presenter.VALUE_NA,
            UiDataEntryType.DISPLAY_REMOVED,
            removedDisplayId.formattedValue(),
            '',
            '',
            removedDisplayId,
          ),
        );
      }

      for (const destroyedLayerHandleId of assertDefined(
        entryNode.getChildByName('destroyedLayerHandles'),
      ).getAllChildren()) {
        entries.push(
          new UiDataEntry(
            originalIndex,
            entryTimestamp,
            vsyncId,
            Presenter.VALUE_NA,
            Presenter.VALUE_NA,
            UiDataEntryType.LAYER_HANDLE_DESTROYED,
            destroyedLayerHandleId.formattedValue(),
            '',
            '',
            destroyedLayerHandleId,
          ),
        );
      }
    }

    return entries;
  }

  private getUniqueUiDataEntryValues<T>(
    entries: UiDataEntry[],
    getValue: (entry: UiDataEntry) => T | T[],
  ): T[] {
    const uniqueValues = new Set<T>();
    entries.forEach((entry: UiDataEntry) => {
      const value = getValue(entry);
      if (Array.isArray(value)) {
        value.forEach((val) => uniqueValues.add(val));
      } else {
        uniqueValues.add(value);
      }
    });

    const result = [...uniqueValues];

    result.sort((a, b) => {
      const aIsNumber = !isNaN(Number(a));
      const bIsNumber = !isNaN(Number(b));

      if (aIsNumber && bIsNumber) {
        return Number(a) - Number(b);
      } else if (aIsNumber) {
        return 1; // place number after strings in the result
      } else if (bIsNumber) {
        return -1; // place number after strings in the result
      }

      // a and b are both strings
      if (a < b) {
        return -1;
      } else if (a > b) {
        return 1;
      } else {
        return 0;
      }
    });

    return result;
  }
}
