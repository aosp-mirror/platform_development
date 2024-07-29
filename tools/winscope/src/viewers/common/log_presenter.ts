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

import {ArrayUtils} from 'common/array_utils';
import {Timestamp} from 'common/time';
import {TraceEntry} from 'trace/trace';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {LogEntry, LogFieldType, LogFilter} from './ui_data_log';

export class LogPresenter<Entry extends LogEntry> {
  private allEntries: Entry[] = [];
  private filteredEntries: Entry[] = [];
  private filters: LogFilter[] = [];
  private headers: LogFieldType[] = [];
  private filterValues = new Map<LogFieldType, string | string[]>();
  private currentEntry: TraceEntry<PropertyTreeNode> | undefined;
  private selectedIndex: number | undefined;
  private scrollToIndex: number | undefined;
  private currentIndex: number | undefined;
  private originalIndicesOfAllEntries: number[] = [];

  constructor(
    private storeCurrentIndex: boolean,
    private timeOrderedEntries = true,
  ) {}

  setAllEntries(value: Entry[]) {
    this.allEntries = value;
    this.updateFilteredEntries();
  }

  setHeaders(headers: LogFieldType[]) {
    this.headers = headers;
  }

  getHeaders(): LogFieldType[] {
    return this.headers;
  }

  setFilters(filters: LogFilter[]) {
    this.filters = filters;
    this.filterValues = new Map<LogFieldType, string | string[]>();
    this.updateFilteredEntries();
    this.resetIndices();
  }

  getFilters(): LogFilter[] {
    return this.filters;
  }

  getFilteredEntries(): Entry[] {
    return this.filteredEntries;
  }

  getSelectedIndex(): number | undefined {
    return this.selectedIndex;
  }

  getScrollToIndex(): number | undefined {
    return this.scrollToIndex;
  }

  getCurrentIndex(): number | undefined {
    return this.currentIndex;
  }

  applyLogEntryClick(index: number) {
    if (this.selectedIndex === index) {
      this.scrollToIndex = undefined;
      return;
    }
    this.selectedIndex = index;
    this.scrollToIndex = undefined; // no scrolling
  }

  applyArrowDownPress() {
    const index = this.selectedIndex ?? this.currentIndex;
    if (index === undefined) {
      this.changeLogByKeyPress(0);
      return;
    }
    if (index < this.filteredEntries.length - 1) {
      this.changeLogByKeyPress(index + 1);
    }
  }

  applyArrowUpPress() {
    const index = this.selectedIndex ?? this.currentIndex;
    if (index === undefined) {
      this.changeLogByKeyPress(0);
      return;
    }
    if (index > 0) {
      this.changeLogByKeyPress(index - 1);
    }
  }

  applyTracePositionUpdate(entry: TraceEntry<PropertyTreeNode> | undefined) {
    this.currentEntry = entry;
    this.resetIndices();
  }

  applyFilterChange(type: LogFieldType, value: string[] | string) {
    if (value.length > 0) {
      this.filterValues.set(type, value);
    } else {
      this.filterValues.delete(type);
    }
    this.updateFilteredEntries();
    if (this.storeCurrentIndex) {
      this.currentIndex = this.getCurrentTracePositionIndex();
    } else {
      this.selectedIndex = this.getCurrentTracePositionIndex();
    }
    this.scrollToIndex = this.selectedIndex ?? this.currentIndex;
  }

  private changeLogByKeyPress(index: number) {
    if (this.selectedIndex === index || this.filteredEntries.length === 0) {
      return;
    }
    this.selectedIndex = index;
    this.scrollToIndex = index;
  }

  private resetIndices() {
    if (this.storeCurrentIndex) {
      this.currentIndex = this.getCurrentTracePositionIndex();
      this.selectedIndex = undefined;
      this.scrollToIndex = this.currentIndex;
    } else {
      this.selectedIndex = this.getCurrentTracePositionIndex();
      this.scrollToIndex = this.selectedIndex;
    }
  }

  private static shouldFilterBySubstring(type: LogFieldType) {
    switch (type) {
      case LogFieldType.FLAGS:
      case LogFieldType.INPUT_DISPATCH_WINDOWS:
        return true;
      default:
        return false;
    }
  }

  private updateFilteredEntries() {
    this.filteredEntries = this.allEntries.filter((entry) => {
      for (const [filterType, filterValue] of this.filterValues) {
        const entryValue = entry.fields.find(
          (f) => f.type === filterType,
        )?.value;

        if (entryValue === undefined || entryValue instanceof Timestamp) {
          continue;
        }

        const entryValueStr = entryValue.toString();

        if (
          Array.isArray(filterValue) &&
          LogPresenter.shouldFilterBySubstring(filterType)
        ) {
          if (!filterValue.some((flag) => entryValueStr.includes(flag))) {
            return false;
          }
        } else if (
          Array.isArray(filterValue) &&
          !filterValue.includes(entryValueStr)
        ) {
          return false;
        }

        if (
          typeof filterValue === 'string' &&
          !entryValueStr.toLowerCase().includes(filterValue.toLowerCase())
        ) {
          return false;
        }
      }
      return true;
    });
    if (this.filteredEntries.length === 0) {
      this.currentIndex = undefined;
      this.selectedIndex = undefined;
    }
    this.originalIndicesOfAllEntries = this.filteredEntries.map((entry) =>
      entry.traceEntry.getIndex(),
    );
  }

  private getCurrentTracePositionIndex(): number | undefined {
    if (!this.currentEntry) {
      this.currentIndex = undefined;
      return;
    }
    if (this.originalIndicesOfAllEntries.length === 0) {
      this.currentIndex = undefined;
      return;
    }
    const target = this.currentEntry.getIndex();

    if (this.timeOrderedEntries) {
      return (
        ArrayUtils.binarySearchFirstGreaterOrEqual(
          this.originalIndicesOfAllEntries,
          this.currentEntry.getIndex(),
        ) ?? this.originalIndicesOfAllEntries.length - 1
      );
    }
    return (
      this.originalIndicesOfAllEntries.findIndex((i) => i === target) ??
      this.originalIndicesOfAllEntries.length - 1
    );
  }
}
