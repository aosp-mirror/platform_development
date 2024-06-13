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
import {FunctionUtils} from 'common/function_utils';
import {
  TracePositionUpdate,
  WinscopeEvent,
  WinscopeEventType,
} from 'messaging/winscope_event';
import {
  EmitEvent,
  WinscopeEventEmitter,
} from 'messaging/winscope_event_emitter';
import {AbsoluteEntryIndex, Trace, TraceEntry} from 'trace/trace';
import {TraceEntryFinder} from 'trace/trace_entry_finder';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {UiData, UiDataMessage} from './ui_data';

export class Presenter implements WinscopeEventEmitter {
  private readonly trace: Trace<PropertyTreeNode>;
  private readonly notifyUiDataCallback: (data: UiData) => void;
  private emitAppEvent: EmitEvent = FunctionUtils.DO_NOTHING_ASYNC;
  private entry?: TraceEntry<PropertyTreeNode>;
  private uiData = UiData.EMPTY;
  private originalIndicesOfFilteredOutputMessages: number[] = [];

  private isInitialized = false;
  private allUiDataMessages: UiDataMessage[] = [];
  private allTags: string[] = [];
  private allSourceFiles: string[] = [];
  private allLogLevels: string[] = [];

  private tagsFilter: string[] = [];
  private filesFilter: string[] = [];
  private levelsFilter: string[] = [];
  private searchString = '';

  constructor(
    trace: Trace<PropertyTreeNode>,
    notifyUiDataCallback: (data: UiData) => void,
  ) {
    this.trace = trace;
    this.notifyUiDataCallback = notifyUiDataCallback;
    this.notifyUiDataCallback(this.uiData);
  }

  setEmitEvent(callback: EmitEvent) {
    this.emitAppEvent = callback;
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
        this.computeUiDataCurrentMessageIndex();
        this.notifyUiDataCallback(this.uiData);
      },
    );
  }

  onLogLevelsFilterChanged(levels: string[]) {
    this.levelsFilter = levels;
    this.computeUiData();
    this.computeUiDataCurrentMessageIndex();
    this.notifyUiDataCallback(this.uiData);
  }

  onTagsFilterChanged(tags: string[]) {
    this.tagsFilter = tags;
    this.computeUiData();
    this.computeUiDataCurrentMessageIndex();
    this.notifyUiDataCallback(this.uiData);
  }

  onSourceFilesFilterChanged(files: string[]) {
    this.filesFilter = files;
    this.computeUiData();
    this.computeUiDataCurrentMessageIndex();
    this.notifyUiDataCallback(this.uiData);
  }

  onSearchStringFilterChanged(searchString: string) {
    this.searchString = searchString;
    this.computeUiData();
    this.computeUiDataCurrentMessageIndex();
    this.notifyUiDataCallback(this.uiData);
  }

  onMessageClicked(index: number) {
    if (this.uiData.selectedMessageIndex === index) {
      this.uiData.selectedMessageIndex = undefined;
    } else {
      this.uiData.selectedMessageIndex = index;
    }
    this.notifyUiDataCallback(this.uiData);
  }

  async onLogTimestampClicked(traceIndex: AbsoluteEntryIndex) {
    await this.emitAppEvent(
      TracePositionUpdate.fromTraceEntry(this.trace.getEntry(traceIndex), true),
    );
  }

  private async initializeIfNeeded() {
    if (this.isInitialized) {
      return;
    }
    this.allUiDataMessages = await this.makeAllUiDataMessages();

    this.allLogLevels = this.getUniqueMessageValues(
      this.allUiDataMessages,
      (message: UiDataMessage) => message.level,
    );
    this.allTags = this.getUniqueMessageValues(
      this.allUiDataMessages,
      (message: UiDataMessage) => message.tag,
    );
    this.allSourceFiles = this.getUniqueMessageValues(
      this.allUiDataMessages,
      (message: UiDataMessage) => message.at,
    );

    this.computeUiData();

    this.isInitialized = true;
  }

  private async makeAllUiDataMessages(): Promise<UiDataMessage[]> {
    const messages: PropertyTreeNode[] = [];

    for (
      let traceIndex = 0;
      traceIndex < this.trace.lengthEntries;
      ++traceIndex
    ) {
      const entry = assertDefined(this.trace.getEntry(traceIndex));
      const message = await entry.getValue();
      messages.push(message);
    }

    return messages.map((messageNode, index) => {
      return {
        traceIndex: index,
        text: assertDefined(
          messageNode.getChildByName('text'),
        ).formattedValue(),
        time: assertDefined(messageNode.getChildByName('timestamp')),
        tag: assertDefined(messageNode.getChildByName('tag')).formattedValue(),
        level: assertDefined(
          messageNode.getChildByName('level'),
        ).formattedValue(),
        at: assertDefined(messageNode.getChildByName('at')).formattedValue(),
      };
    });
  }

  private computeUiData() {
    let filteredMessages = this.allUiDataMessages;

    if (this.levelsFilter.length > 0) {
      filteredMessages = filteredMessages.filter((value) =>
        this.levelsFilter.includes(value.level),
      );
    }

    if (this.tagsFilter.length > 0) {
      filteredMessages = filteredMessages.filter((value) =>
        this.tagsFilter.includes(value.tag),
      );
    }

    if (this.filesFilter.length > 0) {
      filteredMessages = filteredMessages.filter((value) =>
        this.filesFilter.includes(value.at),
      );
    }

    filteredMessages = filteredMessages.filter((value) =>
      value.text.includes(this.searchString),
    );

    this.originalIndicesOfFilteredOutputMessages = filteredMessages.map(
      (message) => message.traceIndex,
    );

    this.uiData = new UiData(
      this.allLogLevels,
      this.allTags,
      this.allSourceFiles,
      filteredMessages,
      undefined,
      undefined,
    );
  }

  private computeUiDataCurrentMessageIndex() {
    if (!this.entry) {
      this.uiData.currentMessageIndex = undefined;
      return;
    }

    if (this.originalIndicesOfFilteredOutputMessages.length === 0) {
      this.uiData.currentMessageIndex = undefined;
      return;
    }

    this.uiData.currentMessageIndex =
      ArrayUtils.binarySearchFirstGreaterOrEqual(
        this.originalIndicesOfFilteredOutputMessages,
        this.entry.getIndex(),
      ) ?? this.originalIndicesOfFilteredOutputMessages.length - 1;
  }

  private getUniqueMessageValues(
    allMessages: UiDataMessage[],
    getValue: (message: UiDataMessage) => string,
  ): string[] {
    const uniqueValues = new Set<string>();
    allMessages.forEach((message) => {
      uniqueValues.add(getValue(message));
    });
    const result = [...uniqueValues];
    result.sort();
    return result;
  }
}
