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

import {FunctionUtils} from 'common/function_utils';
import {Timestamp} from 'common/time';
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
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {PropertiesPresenter} from 'viewers/common/properties_presenter';
import {UserOptions} from 'viewers/common/user_options';
import {LogPresenter} from './log_presenter';
import {LogFieldType, UiDataLog} from './ui_data_log';
import {
  LogFilterChangeDetail,
  TimestampClickDetail,
  ViewerEvents,
} from './viewer_events';

export type NotifyLogViewCallbackType = (uiData: UiDataLog) => void;

export abstract class AbstractLogViewerPresenter
  implements WinscopeEventEmitter
{
  protected emitAppEvent: EmitEvent = FunctionUtils.DO_NOTHING_ASYNC;
  protected abstract logPresenter: LogPresenter;
  protected propertiesPresenter?: PropertiesPresenter;
  protected keepCalculated?: boolean;

  constructor(
    protected readonly trace: Trace<PropertyTreeNode>,
    private readonly notifyViewCallback: NotifyLogViewCallbackType,
    protected uiData: UiDataLog,
  ) {
    this.notifyViewCallback(this.uiData);
  }

  setEmitEvent(callback: EmitEvent) {
    this.emitAppEvent = callback;
  }

  addEventListeners(htmlElement: HTMLElement) {
    htmlElement.addEventListener(
      ViewerEvents.LogFilterChange,
      async (event) => {
        const detail: LogFilterChangeDetail = (event as CustomEvent).detail;
        await this.onFilterChange(detail.type, detail.value);
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
  }

  async onAppEvent(event: WinscopeEvent) {
    await event.visit(
      WinscopeEventType.TRACE_POSITION_UPDATE,
      async (event) => {
        await this.applyTracePositionUpdate(event);
      },
    );
  }

  async onFilterChange(type: LogFieldType, value: string[] | string) {
    this.logPresenter.applyFilterChange(type, value);
    await this.updatePropertiesTree();
    this.uiData.currentIndex = this.logPresenter.getCurrentIndex();
    this.uiData.selectedIndex = this.logPresenter.getSelectedIndex();
    this.uiData.scrollToIndex =
      this.logPresenter.getCurrentIndex() ??
      this.logPresenter.getSelectedIndex();
    this.uiData.entries = this.logPresenter.getFilteredEntries();
    this.notifyViewCallback(this.uiData);
  }

  async onPropertiesUserOptionsChange(userOptions: UserOptions) {
    if (!this.propertiesPresenter) {
      return;
    }
    this.propertiesPresenter.applyPropertiesUserOptionsChange(userOptions);
    this.uiData.propertiesUserOptions =
      this.propertiesPresenter.getUserOptions();
    await this.updatePropertiesTree();
    this.notifyViewCallback(this.uiData);
  }

  async onLogTimestampClick(traceEntry: TraceEntry<PropertyTreeNode>) {
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
    this.notifyViewCallback(this.uiData);
  }

  async onArrowDownPress() {
    this.logPresenter.applyArrowDownPress();
    this.updateIndicesUiData();
    await this.updatePropertiesTree();
    this.notifyViewCallback(this.uiData);
  }

  async onArrowUpPress() {
    this.logPresenter.applyArrowUpPress();
    this.updateIndicesUiData();
    await this.updatePropertiesTree();
    this.notifyViewCallback(this.uiData);
  }

  protected refreshUIData(uiData: UiDataLog) {
    this.uiData = uiData;
    this.uiData.headers = this.logPresenter.getHeaders();
    this.uiData.filters = this.logPresenter.getFilters();
    this.uiData.entries = this.logPresenter.getFilteredEntries();
    this.uiData.selectedIndex = this.logPresenter.getSelectedIndex();
    this.uiData.scrollToIndex = this.logPresenter.getScrollToIndex();
    this.uiData.currentIndex = this.logPresenter.getCurrentIndex();
    if (this.propertiesPresenter) {
      this.uiData.propertiesTree = this.propertiesPresenter.getFormattedTree();
      this.uiData.propertiesUserOptions =
        this.propertiesPresenter.getUserOptions();
    }
  }

  protected async applyTracePositionUpdate(event: TracePositionUpdate) {
    await this.initializeIfNeeded();
    const entry = TraceEntryFinder.findCorrespondingEntry(
      this.trace,
      event.position,
    );
    this.logPresenter.applyTracePositionUpdate(entry);

    this.uiData.selectedIndex = this.logPresenter.getSelectedIndex();
    this.uiData.scrollToIndex = this.logPresenter.getScrollToIndex();
    this.uiData.currentIndex = this.logPresenter.getCurrentIndex();

    if (this.propertiesPresenter) {
      await this.updatePropertiesTree();
      this.uiData.propertiesTree = this.propertiesPresenter.getFormattedTree();
    }

    this.notifyViewCallback(this.uiData);
  }

  protected async updatePropertiesTree() {
    if (this.propertiesPresenter) {
      const tree = this.getPropertiesTree();
      this.propertiesPresenter.setPropertiesTree(tree);
      await this.propertiesPresenter.formatPropertiesTree(
        undefined,
        undefined,
        this.keepCalculated ?? false,
      );
      this.uiData.propertiesTree = this.propertiesPresenter.getFormattedTree();
    }
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
      return entries[selectedIndex].propertiesTree;
    }
    if (currentIndex !== undefined) {
      return entries[currentIndex].propertiesTree;
    }
    return undefined;
  }

  protected abstract initializeIfNeeded(): Promise<void>;
}
