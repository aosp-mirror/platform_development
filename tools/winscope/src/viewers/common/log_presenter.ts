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
import {assertDefined} from 'common/assert_utils';
import {
  FilterFlag,
  makeFilterPredicate,
  StringFilterPredicate,
} from 'common/filter_flag';
import {Timestamp} from 'common/time';
import {TraceEntry} from 'trace/trace';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {TextFilter} from './text_filter';
import {LogEntry, LogFieldType, LogFilter} from './ui_data_log';

export class LogPresenter<Entry extends LogEntry> {
  private allEntries: Entry[] = [];
  private filteredEntries: Entry[] = [];
  private filters: LogFilter[] = [];
  private headers: LogFieldType[] = [];
  private filterPredicates = new Map<LogFieldType, StringFilterPredicate>();
  private currentEntry: TraceEntry<PropertyTreeNode> | undefined;
  private selectedIndex: number | undefined;
  private scrollToIndex: number | undefined;
  private currentIndex: number | undefined;
  private originalIndicesOfAllEntries: number[] = [];

  constructor(private timeOrderedEntries = true) {}

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
    this.filterPredicates = new Map<LogFieldType, StringFilterPredicate>();
    this.filters.forEach((filter) => {
      if (
        filter.textFilter &&
        (filter.textFilter.filterString || filter.textFilter.flags.length > 0)
      ) {
        this.filterPredicates.set(
          filter.type,
          this.makeLogFilterPredicate(
            filter.type,
            filter.textFilter.filterString,
            filter.textFilter.flags,
          ),
        );
      }
    });
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

  applyTextFilterChange(type: LogFieldType, value: TextFilter) {
    assertDefined(
      this.filters.find((filter) => filter.type === type),
    ).textFilter = value;
    if (value.filterString.length > 0) {
      this.filterPredicates.set(
        type,
        this.makeLogFilterPredicate(type, value.filterString, value.flags),
      );
    } else {
      this.filterPredicates.delete(type);
    }
    this.updateEntriesAfterFilterChange();
  }

  applyFilterChange(type: LogFieldType, value: string[] | string) {
    if (value.length > 0) {
      this.filterPredicates.set(
        type,
        this.makeLogFilterPredicate(type, value, []),
      );
    } else {
      this.filterPredicates.delete(type);
    }
    this.updateEntriesAfterFilterChange();
  }

  private updateEntriesAfterFilterChange() {
    this.updateFilteredEntries();
    this.currentIndex = this.getCurrentTracePositionIndex();
    if (
      this.selectedIndex !== undefined &&
      this.selectedIndex > this.filteredEntries.length - 1
    ) {
      this.selectedIndex = this.currentIndex;
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
    this.currentIndex = this.getCurrentTracePositionIndex();
    this.selectedIndex = undefined;
    this.scrollToIndex = this.currentIndex;
  }

  private static shouldFilterBySubstring(type: LogFieldType): boolean {
    switch (type) {
      case LogFieldType.FLAGS:
      case LogFieldType.INPUT_DISPATCH_WINDOWS:
        return true;
      default:
        return false;
    }
  }

  private makeLogFilterPredicate(
    type: LogFieldType,
    filterValue: string | string[],
    flags: FilterFlag[],
  ): StringFilterPredicate {
    if (
      Array.isArray(filterValue) &&
      LogPresenter.shouldFilterBySubstring(type)
    ) {
      return (entryString) =>
        filterValue.some((val) => entryString.includes(val));
    } else if (Array.isArray(filterValue)) {
      return (entryString) => filterValue.includes(entryString);
    } else {
      return makeFilterPredicate(filterValue, flags);
    }
  }

  private updateFilteredEntries() {
    this.filteredEntries = this.allEntries.filter((entry) => {
      for (const [filterType, predicate] of this.filterPredicates) {
        const entryValue = entry.fields.find(
          (f) => f.type === filterType,
        )?.value;

        if (entryValue === undefined || entryValue instanceof Timestamp) {
          continue;
        }

        const entryValueStr = entryValue.toString();

        if (!predicate(entryValueStr)) return false;
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

    const currentIndex = this.originalIndicesOfAllEntries.findIndex(
      (i) => i === target,
    );
    return currentIndex !== -1
      ? currentIndex
      : this.originalIndicesOfAllEntries.length - 1;
  }
}
