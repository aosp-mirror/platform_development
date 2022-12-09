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
import {UiData, UiDataEntry, UiDataEntryType} from "./ui_data";
import {ArrayUtils} from "common/utils/array_utils";
import {TraceType} from "common/trace/trace_type";
import {TransactionsTraceEntry} from "common/trace/transactions";
import {PropertiesTreeGenerator} from "viewers/common/properties_tree_generator";
import {PropertiesTreeNode} from "viewers/common/ui_tree_utils";
import {TimeUtils} from "common/utils/time_utils";
import { ElapsedTimestamp, RealTimestamp, TimestampType } from "common/trace/timestamp";

class Presenter {
  constructor(notifyUiDataCallback: (data: UiData) => void) {
    this.notifyUiDataCallback = notifyUiDataCallback;
    this.originalIndicesOfUiDataEntries = [];
    this.uiData = UiData.EMPTY;
    this.notifyUiDataCallback(this.uiData);
  }

  //TODO: replace input with something like iterator/cursor (same for other viewers/presenters)
  public notifyCurrentTraceEntries(entries: Map<TraceType, any>): void {
    this.entry = entries.get(TraceType.TRANSACTIONS) ? entries.get(TraceType.TRANSACTIONS)[0] : undefined;
    if (this.uiData === UiData.EMPTY) {
      this.computeUiData();
    }
    else {
      // update only "position" data
      this.uiData.currentEntryIndex = this.computeCurrentEntryIndex();
      this.uiData.selectedEntryIndex = undefined;
      this.uiData.scrollToIndex = this.uiData.currentEntryIndex;
      this.uiData.currentPropertiesTree = this.computeCurrentPropertiesTree(
        this.uiData.entries,
        this.uiData.currentEntryIndex,
        this.uiData.selectedEntryIndex);
    }
    this.notifyUiDataCallback(this.uiData);
  }

  public onPidFilterChanged(pids: string[]) {
    this.pidFilter = pids;
    this.computeUiData();
    this.notifyUiDataCallback(this.uiData);
  }

  public onUidFilterChanged(uids: string[]) {
    this.uidFilter = uids;
    this.computeUiData();
    this.notifyUiDataCallback(this.uiData);
  }

  public onTypeFilterChanged(types: string[]) {
    this.typeFilter = types;
    this.computeUiData();
    this.notifyUiDataCallback(this.uiData);
  }

  public onIdFilterChanged(ids: string[]) {
    this.idFilter = ids;
    this.computeUiData();
    this.notifyUiDataCallback(this.uiData);
  }

  public onEntryClicked(index: number) {
    if (this.uiData.selectedEntryIndex === index) {
      this.uiData.selectedEntryIndex = undefined; // remove selection when clicked again
    }
    else {
      this.uiData.selectedEntryIndex = index;
    }

    this.uiData.scrollToIndex = undefined; // no scrolling

    this.uiData.currentPropertiesTree = this.computeCurrentPropertiesTree(
      this.uiData.entries,
      this.uiData.currentEntryIndex,
      this.uiData.selectedEntryIndex);

    this.notifyUiDataCallback(this.uiData);
  }

  private computeUiData() {
    if (!this.entry) {
      return;
    }

    const entries = this.makeUiDataEntries(this.entry!);

    const allPids = this.getUniqueUiDataEntryValues(entries, (entry: UiDataEntry) => entry.pid);
    const allUids = this.getUniqueUiDataEntryValues(entries, (entry: UiDataEntry) => entry.uid);
    const allTypes = this.getUniqueUiDataEntryValues(entries, (entry: UiDataEntry) => entry.type);
    const allIds = this.getUniqueUiDataEntryValues(entries, (entry: UiDataEntry) => entry.id);

    let filteredEntries = entries;

    if (this.pidFilter.length > 0) {
      filteredEntries =
        filteredEntries.filter(entry => this.pidFilter.includes(entry.pid));
    }

    if (this.uidFilter.length > 0) {
      filteredEntries =
        filteredEntries.filter(entry => this.uidFilter.includes(entry.uid));
    }

    if (this.typeFilter.length > 0) {
      filteredEntries =
        filteredEntries.filter(entry => this.typeFilter.includes(entry.type));
    }

    if (this.idFilter.length > 0) {
      filteredEntries =
        filteredEntries.filter(entry => this.idFilter.includes(entry.id));
    }

    this.originalIndicesOfUiDataEntries = filteredEntries.map(entry => entry.originalIndexInTraceEntry);

    const currentEntryIndex = this.computeCurrentEntryIndex();
    const selectedEntryIndex = undefined;
    const currentPropertiesTree = this.computeCurrentPropertiesTree(filteredEntries, currentEntryIndex, selectedEntryIndex);

    this.uiData = new UiData(
      allPids,
      allUids,
      allTypes,
      allIds,
      filteredEntries,
      currentEntryIndex,
      selectedEntryIndex,
      currentEntryIndex,
      currentPropertiesTree);
  }

  private computeCurrentEntryIndex(): undefined|number {
    if (!this.entry) {
      return undefined;
    }

    return ArrayUtils.binarySearchLowerOrEqual(
      this.originalIndicesOfUiDataEntries,
      this.entry.currentEntryIndex
    );
  }

  private computeCurrentPropertiesTree(
    entries: UiDataEntry[],
    currentEntryIndex: undefined|number,
    selectedEntryIndex: undefined|number): undefined|PropertiesTreeNode {
    if (selectedEntryIndex !== undefined) {
      return entries[selectedEntryIndex].propertiesTree;
    }
    if (currentEntryIndex !== undefined) {
      return entries[currentEntryIndex].propertiesTree;
    }
    return undefined;
  }

  private makeUiDataEntries(entry: TransactionsTraceEntry): UiDataEntry[] {
    const entriesProto: any[] = entry.entriesProto;
    const timestampType = entry.timestampType;
    const realToElapsedTimeOffsetNs = entry.realToElapsedTimeOffsetNs;
    const treeGenerator = new PropertiesTreeGenerator();

    const entries: UiDataEntry[] = [];

    for (const [originalIndex, entryProto] of entriesProto.entries()) {
      for (const transactionStateProto of entryProto.transactions) {
        for (const layerStateProto of transactionStateProto.layerChanges) {
          entries.push(new UiDataEntry(
            originalIndex,
            this.formatTime(entryProto, timestampType, realToElapsedTimeOffsetNs),
            Number(entryProto.vsyncId),
            transactionStateProto.pid.toString(),
            transactionStateProto.uid.toString(),
            UiDataEntryType.LayerChanged,
            layerStateProto.layerId.toString(),
            treeGenerator.generate("LayerState", layerStateProto)
          ));
        }

        for (const displayStateProto of transactionStateProto.displayChanges) {
          entries.push(new UiDataEntry(
            originalIndex,
            this.formatTime(entryProto, timestampType, realToElapsedTimeOffsetNs),
            Number(entryProto.vsyncId),
            transactionStateProto.pid.toString(),
            transactionStateProto.uid.toString(),
            UiDataEntryType.DisplayChanged,
            displayStateProto.id.toString(),
            treeGenerator.generate("DisplayState", displayStateProto)
          ));
        }
      }

      for (const layerCreationArgsProto of entryProto.addedLayers) {
        entries.push(new UiDataEntry(
          originalIndex,
          this.formatTime(entryProto, timestampType, realToElapsedTimeOffsetNs),
          Number(entryProto.vsyncId),
          Presenter.VALUE_NA,
          Presenter.VALUE_NA,
          UiDataEntryType.LayerAdded,
          layerCreationArgsProto.layerId.toString(),
          treeGenerator.generate("LayerCreationArgs", layerCreationArgsProto)
        ));
      }

      for (const removedLayerId of entryProto.removedLayers) {
        entries.push(new UiDataEntry(
          originalIndex,
          this.formatTime(entryProto, timestampType, realToElapsedTimeOffsetNs),
          Number(entryProto.vsyncId),
          Presenter.VALUE_NA,
          Presenter.VALUE_NA,
          UiDataEntryType.LayerRemoved,
          removedLayerId.toString(),
          treeGenerator.generate("RemovedLayerId", removedLayerId)
        ));
      }

      for (const displayStateProto of entryProto.addedDisplays) {
        entries.push(new UiDataEntry(
          originalIndex,
          this.formatTime(entryProto, timestampType, realToElapsedTimeOffsetNs),
          Number(entryProto.vsyncId),
          Presenter.VALUE_NA,
          Presenter.VALUE_NA,
          UiDataEntryType.DisplayAdded,
          displayStateProto.id.toString(),
          treeGenerator.generate("DisplayState", displayStateProto)
        ));
      }

      for (const removedDisplayId of entryProto.removedDisplays) {
        entries.push(new UiDataEntry(
          originalIndex,
          this.formatTime(entryProto, timestampType, realToElapsedTimeOffsetNs),
          Number(entryProto.vsyncId),
          Presenter.VALUE_NA,
          Presenter.VALUE_NA,
          UiDataEntryType.DisplayRemoved,
          removedDisplayId.toString(),
          treeGenerator.generate("RemovedDisplayId", removedDisplayId)
        ));
      }

      for (const removedLayerHandleId of entryProto.removedLayerHandles) {
        entries.push(new UiDataEntry(
          originalIndex,
          this.formatTime(entryProto, timestampType, realToElapsedTimeOffsetNs),
          Number(entryProto.vsyncId),
          Presenter.VALUE_NA,
          Presenter.VALUE_NA,
          UiDataEntryType.LayerHandleRemoved,
          removedLayerHandleId.toString(),
          treeGenerator.generate("RemovedLayerHandleId", removedLayerHandleId)
        ));
      }
    }

    return entries;
  }

  private formatTime(entryProto: any, timestampType: TimestampType, realToElapsedTimeOffsetNs: bigint|undefined): string {
    if (timestampType === TimestampType.REAL && realToElapsedTimeOffsetNs !== undefined) {
      return TimeUtils.format(new RealTimestamp(BigInt(entryProto.elapsedRealtimeNanos) + realToElapsedTimeOffsetNs));
    } else {
      return TimeUtils.format(new ElapsedTimestamp(BigInt(entryProto.elapsedRealtimeNanos)));
    }
  }

  private getUniqueUiDataEntryValues(entries: UiDataEntry[], getValue: (entry: UiDataEntry) => string): string[] {
    const uniqueValues = new Set<string>();
    entries.forEach((entry: UiDataEntry) => {
      uniqueValues.add(getValue(entry));
    });

    const result = [...uniqueValues];

    result.sort((a, b) => {
      const aIsNumber = !isNaN(Number(a));
      const bIsNumber = !isNaN(Number(b));

      if (aIsNumber && bIsNumber) {
        return Number(a) - Number(b);
      }
      else if (aIsNumber) {
        return 1; // place number after strings in the result
      }
      else if (bIsNumber) {
        return -1; // place number after strings in the result
      }

      // a and b are both strings
      if (a < b) {
        return -1;
      }
      else if (a > b) {
        return 1;
      }
      else {
        return 0;
      }
    });

    return result;
  }

  private entry?: TransactionsTraceEntry;
  private originalIndicesOfUiDataEntries: number[];
  private uiData: UiData;
  private readonly notifyUiDataCallback: (data: UiData) => void;
  private static readonly VALUE_NA = "N/A";
  private pidFilter: string[] = [];
  private uidFilter: string[] = [];
  private typeFilter: string[] = [];
  private idFilter: string[] = [];
}

export {Presenter};
