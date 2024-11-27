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
import {TraceEntry} from 'trace/trace';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {StringFilterPredicate} from 'viewers/common/string_filter_predicate';
import {TextFilter} from 'viewers/common/text_filter';
import {ColumnSpec, LogEntry, LogHeader} from './ui_data_log';

export class LogPresenter<Entry extends LogEntry> {
  private allEntries: Entry[] = [];
  private filteredEntries: Entry[] = [];
  private headers: LogHeader[] = [];
  private filterPredicates = new Map<ColumnSpec, StringFilterPredicate>();
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

  setHeaders(headers: LogHeader[]) {
    this.headers = headers;

    this.filterPredicates = new Map<ColumnSpec, StringFilterPredicate>();
    this.headers.forEach((header) => {
      if (!header.filter) return;
      this.filterPredicates.set(
        header.spec,
        header.filter.getFilterPredicate(),
      );
    });
    this.updateFilteredEntries();
    this.resetIndices();
  }

  getHeaders(): LogHeader[] {
    return this.headers;
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

  applyTextFilterChange(header: LogHeader, value: TextFilter) {
    const filter = assertDefined(header.filter);
    const filterString = value.filterString;
    filter.updateFilterValue([filterString]);
    if (filterString.length > 0) {
      this.filterPredicates.set(header.spec, filter.getFilterPredicate());
    } else {
      this.filterPredicates.delete(header.spec);
    }
    this.updateEntriesAfterFilterChange();
  }

  applySelectFilterChange(header: LogHeader, value: string[]) {
    const filter = assertDefined(header.filter);
    filter.updateFilterValue(value);
    if (value.length > 0) {
      this.filterPredicates.set(
        header.spec,
        assertDefined(header.filter).getFilterPredicate(),
      );
    } else {
      this.filterPredicates.delete(header.spec);
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

  private updateFilteredEntries() {
    this.filteredEntries = this.allEntries.filter((entry) => {
      for (const [spec, predicate] of this.filterPredicates) {
        const entryValue = entry.fields.find((f) => f.spec === spec)?.value;

        if (entryValue === undefined) {
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
      this.scrollToIndex = undefined;
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
