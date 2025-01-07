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

import {DOMUtils} from 'common/dom_utils';
import {FunctionUtils} from 'common/function_utils';
import {Timestamp} from 'common/time/time';
import {
  TracePositionUpdate,
  WinscopeEvent,
  WinscopeEventType,
} from 'messaging/winscope_event';
import {
  EmitEvent,
  WinscopeEventEmitter,
} from 'messaging/winscope_event_emitter';
import {Trace, TraceEntry} from 'trace/trace';
import {TraceEntryFinder} from 'trace/trace_entry_finder';
import {TracePosition} from 'trace/trace_position';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {PropertiesPresenter} from 'viewers/common/properties_presenter';
import {TextFilter} from 'viewers/common/text_filter';
import {UserOptions} from 'viewers/common/user_options';
import {LogPresenter} from './log_presenter';
import {LogEntry, LogHeader, UiDataLog} from './ui_data_log';
import {
  LogFilterChangeDetail,
  LogTextFilterChangeDetail,
  TimestampClickDetail,
  ViewerEvents,
} from './viewer_events';

export type NotifyLogViewCallbackType<UiData> = (uiData: UiData) => void;

export abstract class AbstractLogViewerPresenter<
  UiData extends UiDataLog,
  TraceEntryType extends object,
> implements WinscopeEventEmitter
{
  protected static readonly VALUE_NA = 'N/A';
  protected emitAppEvent: EmitEvent = FunctionUtils.DO_NOTHING_ASYNC;
  protected abstract logPresenter: LogPresenter<LogEntry>;
  protected propertiesPresenter?: PropertiesPresenter;
  protected keepCalculated?: boolean;
  private activeTrace?: Trace<object>;
  private isInitialized = false;

  protected constructor(
    protected readonly trace: Trace<TraceEntryType>,
    private readonly notifyViewCallback: NotifyLogViewCallbackType<UiData>,
    protected readonly uiData: UiData,
  ) {
    this.notifyViewChanged();
  }

  setEmitEvent(callback: EmitEvent) {
    this.emitAppEvent = callback;
  }

  addEventListeners(htmlElement: HTMLElement) {
    htmlElement.addEventListener(
      ViewerEvents.LogFilterChange,
      async (event) => {
        const detail: LogFilterChangeDetail = (event as CustomEvent).detail;
        await this.onSelectFilterChange(detail.header, detail.value);
      },
    );
    htmlElement.addEventListener(
      ViewerEvents.LogTextFilterChange,
      async (event) => {
        const detail: LogTextFilterChangeDetail = (event as CustomEvent).detail;
        await this.onTextFilterChange(detail.header, detail.filter);
      },
    );
    htmlElement.addEventListener(ViewerEvents.LogEntryClick, async (event) => {
      await this.onLogEntryClick((event as CustomEvent).detail);
    });
    htmlElement.addEventListener(
      ViewerEvents.ArrowDownPress,
      async (event) => await this.onArrowDownPress(),
    );
    htmlElement.addEventListener(
      ViewerEvents.ArrowUpPress,
      async (event) => await this.onArrowUpPress(),
    );
    htmlElement.addEventListener(ViewerEvents.TimestampClick, async (event) => {
      const detail: TimestampClickDetail = (event as CustomEvent).detail;
      if (detail.entry !== undefined) {
        await this.onLogTimestampClick(detail.entry);
      } else if (detail.timestamp !== undefined) {
        await this.onRawTimestampClick(detail.timestamp);
      }
    });
    htmlElement.addEventListener(
      ViewerEvents.PropertiesUserOptionsChange,
      (event) =>
        this.onPropertiesUserOptionsChange(
          (event as CustomEvent).detail.userOptions,
        ),
    );
    htmlElement.addEventListener(
      ViewerEvents.PropertiesFilterChange,
      async (event) => {
        const detail: TextFilter = (event as CustomEvent).detail;
        await this.onPropertiesFilterChange(detail);
      },
    );

    document.addEventListener('keydown', async (event: KeyboardEvent) => {
      const isViewerVisible = DOMUtils.isElementVisible(htmlElement);
      const isPositionChange =
        event.key === 'ArrowRight' || event.key === 'ArrowLeft';
      if (!isViewerVisible || !isPositionChange) {
        return;
      }
      event.preventDefault();
      await this.onPositionChangeByKeyPress(event);
    });

    this.addViewerSpecificListeners(htmlElement);
  }

  async onAppEvent(event: WinscopeEvent) {
    await event.visit(
      WinscopeEventType.TRACE_POSITION_UPDATE,
      async (event) => {
        await this.applyTracePositionUpdate(event);
      },
    );
    await event.visit(WinscopeEventType.DARK_MODE_TOGGLED, async (event) => {
      this.uiData.isDarkMode = event.isDarkMode;
      this.notifyViewChanged();
    });
    await event.visit(WinscopeEventType.ACTIVE_TRACE_CHANGED, async (event) => {
      this.activeTrace = event.trace;
    });
  }

  async onSelectFilterChange(header: LogHeader, value: string[]) {
    this.logPresenter.applySelectFilterChange(header, value);
    await this.updatePropertiesTree();
    this.uiData.currentIndex = this.logPresenter.getCurrentIndex();
    this.uiData.selectedIndex = this.logPresenter.getSelectedIndex();
    this.uiData.scrollToIndex = this.logPresenter.getScrollToIndex();
    this.uiData.entries = this.logPresenter.getFilteredEntries();
    this.notifyViewChanged();
  }

  async onTextFilterChange(header: LogHeader, value: TextFilter) {
    this.logPresenter.applyTextFilterChange(header, value);
    await this.updatePropertiesTree();
    this.uiData.currentIndex = this.logPresenter.getCurrentIndex();
    this.uiData.selectedIndex = this.logPresenter.getSelectedIndex();
    this.uiData.scrollToIndex = this.logPresenter.getScrollToIndex();
    this.uiData.entries = this.logPresenter.getFilteredEntries();
    this.notifyViewChanged();
  }

  async onPropertiesUserOptionsChange(userOptions: UserOptions) {
    if (!this.propertiesPresenter) {
      return;
    }
    this.propertiesPresenter.applyPropertiesUserOptionsChange(userOptions);
    this.uiData.propertiesUserOptions =
      this.propertiesPresenter.getUserOptions();
    await this.updatePropertiesTree(false);
    this.notifyViewChanged();
  }

  async onPropertiesFilterChange(textFilter: TextFilter) {
    if (!this.propertiesPresenter) {
      return;
    }
    this.propertiesPresenter.applyPropertiesFilterChange(textFilter);
    await this.updatePropertiesTree(false);
    this.uiData.propertiesFilter = textFilter;
    this.notifyViewChanged();
  }

  async onLogTimestampClick(traceEntry: TraceEntry<object>) {
    await this.emitAppEvent(
      TracePositionUpdate.fromTraceEntry(traceEntry, true),
    );
  }

  async onRawTimestampClick(timestamp: Timestamp) {
    await this.emitAppEvent(TracePositionUpdate.fromTimestamp(timestamp, true));
  }

  async onLogEntryClick(index: number) {
    this.logPresenter.applyLogEntryClick(index);
    this.updateIndicesUiData();
    await this.updatePropertiesTree();
    this.notifyViewChanged();
  }

  async onArrowDownPress() {
    this.logPresenter.applyArrowDownPress();
    this.updateIndicesUiData();
    await this.updatePropertiesTree();
    this.notifyViewChanged();
  }

  async onArrowUpPress() {
    this.logPresenter.applyArrowUpPress();
    this.updateIndicesUiData();
    await this.updatePropertiesTree();
    this.notifyViewChanged();
  }

  async onPositionChangeByKeyPress(event: KeyboardEvent) {
    const currIndex = this.uiData.currentIndex;
    if (this.activeTrace === this.trace && currIndex !== undefined) {
      if (event.key === 'ArrowRight') {
        event.stopImmediatePropagation();
        if (currIndex < this.uiData.entries.length - 1) {
          const currTimestamp =
            this.uiData.entries[currIndex].traceEntry.getTimestamp();
          const nextEntry = this.uiData.entries
            .slice(currIndex + 1)
            .find((entry) => entry.traceEntry.getTimestamp() > currTimestamp);
          if (nextEntry) {
            return this.emitAppEvent(
              new TracePositionUpdate(
                TracePosition.fromTraceEntry(nextEntry.traceEntry),
                true,
              ),
            );
          }
        }
      } else {
        event.stopImmediatePropagation();
        if (currIndex > 0) {
          let prev = currIndex - 1;
          while (prev >= 0) {
            const prevEntry = this.uiData.entries[prev].traceEntry;
            if (prevEntry.hasValidTimestamp()) {
              return this.emitAppEvent(
                new TracePositionUpdate(
                  TracePosition.fromTraceEntry(prevEntry),
                  true,
                ),
              );
            }
            prev--;
          }
        }
      }
    }
  }

  protected addViewerSpecificListeners(htmlElement: HTMLElement) {
    // do nothing
  }

  protected refreshUiData() {
    this.uiData.headers = this.logPresenter.getHeaders();
    this.uiData.entries = this.logPresenter.getFilteredEntries();
    this.uiData.selectedIndex = this.logPresenter.getSelectedIndex();
    this.uiData.scrollToIndex = this.logPresenter.getScrollToIndex();
    this.uiData.currentIndex = this.logPresenter.getCurrentIndex();
    if (this.propertiesPresenter) {
      this.uiData.propertiesTree = this.propertiesPresenter.getFormattedTree();
      this.uiData.propertiesUserOptions =
        this.propertiesPresenter.getUserOptions();
      this.uiData.propertiesFilter = this.propertiesPresenter.getTextFilter();
    }
  }

  protected async applyTracePositionUpdate(event: TracePositionUpdate) {
    await this.initializeIfNeeded();
    let entry: TraceEntry<TraceEntryType> | undefined;
    if (event.position.entry?.getFullTrace() === this.trace) {
      entry = event.position.entry as TraceEntry<TraceEntryType>;
    } else {
      entry = TraceEntryFinder.findCorrespondingEntry(
        this.trace,
        event.position,
      );
    }
    this.logPresenter.applyTracePositionUpdate(entry);

    this.uiData.selectedIndex = this.logPresenter.getSelectedIndex();
    this.uiData.scrollToIndex = this.logPresenter.getScrollToIndex();
    this.uiData.currentIndex = this.logPresenter.getCurrentIndex();

    if (this.propertiesPresenter) {
      await this.updatePropertiesTree();
      this.uiData.propertiesTree = this.propertiesPresenter.getFormattedTree();
    }

    this.notifyViewChanged();
  }

  protected async updatePropertiesTree(updateDefaultAllowlist = true) {
    if (this.propertiesPresenter) {
      const tree = this.getPropertiesTree();
      this.propertiesPresenter.setPropertiesTree(tree);
      if (updateDefaultAllowlist && this.updateDefaultAllowlist) {
        this.updateDefaultAllowlist(tree);
      }
      await this.propertiesPresenter.formatPropertiesTree(
        undefined,
        undefined,
        this.keepCalculated ?? false,
      );
      this.uiData.propertiesTree = this.propertiesPresenter.getFormattedTree();
    }
  }

  private async initializeIfNeeded() {
    if (this.isInitialized) {
      return;
    }

    if (this.initializeTraceSpecificData) {
      await this.initializeTraceSpecificData();
    }

    const headers = this.makeHeaders();
    const allEntries = await this.makeUiDataEntries(headers);
    if (this.updateFiltersInHeaders) {
      this.updateFiltersInHeaders(headers, allEntries);
    }

    this.logPresenter.setAllEntries(allEntries);
    this.logPresenter.setHeaders(headers);
    this.refreshUiData();
    this.isInitialized = true;
  }

  private updateIndicesUiData() {
    this.uiData.selectedIndex = this.logPresenter.getSelectedIndex();
    this.uiData.currentIndex = this.logPresenter.getCurrentIndex();
    this.uiData.scrollToIndex = this.logPresenter.getScrollToIndex();
  }

  private getPropertiesTree(): PropertyTreeNode | undefined {
    const entries = this.logPresenter.getFilteredEntries();
    const selectedIndex = this.logPresenter.getSelectedIndex();
    const currentIndex = this.logPresenter.getCurrentIndex();
    if (selectedIndex !== undefined) {
      return entries.at(selectedIndex)?.propertiesTree;
    }
    if (currentIndex !== undefined) {
      return entries.at(currentIndex)?.propertiesTree;
    }
    return undefined;
  }

  protected notifyViewChanged() {
    this.notifyViewCallback(this.uiData);
  }

  protected abstract makeHeaders(): LogHeader[];
  protected abstract makeUiDataEntries(
    headers: LogHeader[],
  ): Promise<LogEntry[]>;
  protected initializeTraceSpecificData?(): Promise<void>;
  protected updateFiltersInHeaders?(
    headers: LogHeader[],
    allEntries: LogEntry[],
  ): void;
  protected updateDefaultAllowlist?(tree: PropertyTreeNode | undefined): void;
}
