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
import {LogMessage} from 'trace/protolog';
import {Trace, TraceEntry} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceEntryFinder} from 'trace/trace_entry_finder';
import {TracePosition} from 'trace/trace_position';
import {TraceType} from 'trace/trace_type';
import {assertDefined} from '../../common/assert_utils';
import {UiData} from './ui_data';

export class Presenter {
  private readonly trace: Trace<LogMessage>;
  private readonly notifyUiDataCallback: (data: UiData) => void;
  private entry?: TraceEntry<LogMessage>;
  private originalIndicesOfFilteredOutputMessages: number[];
  private uiData = UiData.EMPTY;

  private tags: string[] = [];
  private files: string[] = [];
  private levels: string[] = [];
  private searchString = '';

  constructor(traces: Traces, notifyUiDataCallback: (data: UiData) => void) {
    this.trace = assertDefined(traces.getTrace(TraceType.PROTO_LOG));
    this.notifyUiDataCallback = notifyUiDataCallback;
    this.originalIndicesOfFilteredOutputMessages = [];
    this.computeUiDataMessages();
    this.notifyUiDataCallback(this.uiData);
  }

  onTracePositionUpdate(position: TracePosition) {
    this.entry = TraceEntryFinder.findCorrespondingEntry(this.trace, position);
    this.computeUiDataCurrentMessageIndex();
    this.notifyUiDataCallback(this.uiData);
  }

  onLogLevelsFilterChanged(levels: string[]) {
    this.levels = levels;
    this.computeUiDataMessages();
    this.computeUiDataCurrentMessageIndex();
    this.notifyUiDataCallback(this.uiData);
  }

  onTagsFilterChanged(tags: string[]) {
    this.tags = tags;
    this.computeUiDataMessages();
    this.computeUiDataCurrentMessageIndex();
    this.notifyUiDataCallback(this.uiData);
  }

  onSourceFilesFilterChanged(files: string[]) {
    this.files = files;
    this.computeUiDataMessages();
    this.computeUiDataCurrentMessageIndex();
    this.notifyUiDataCallback(this.uiData);
  }

  onSearchStringFilterChanged(searchString: string) {
    this.searchString = searchString;
    this.computeUiDataMessages();
    this.computeUiDataCurrentMessageIndex();
    this.notifyUiDataCallback(this.uiData);
  }

  private computeUiDataMessages() {
    const allLogLevels = this.getUniqueMessageValues((message: LogMessage) => message.level);
    const allTags = this.getUniqueMessageValues((message: LogMessage) => message.tag);
    const allSourceFiles = this.getUniqueMessageValues((message: LogMessage) => message.at);

    let filteredMessagesAndOriginalIndex = new Array<[number, LogMessage]>();
    this.trace.forEachEntry((entry) => {
      filteredMessagesAndOriginalIndex.push([entry.getIndex(), entry.getValue()]);
    });

    if (this.levels.length > 0) {
      filteredMessagesAndOriginalIndex = filteredMessagesAndOriginalIndex.filter((value) =>
        this.levels.includes(value[1].level)
      );
    }

    if (this.tags.length > 0) {
      filteredMessagesAndOriginalIndex = filteredMessagesAndOriginalIndex.filter((value) =>
        this.tags.includes(value[1].tag)
      );
    }

    if (this.files.length > 0) {
      filteredMessagesAndOriginalIndex = filteredMessagesAndOriginalIndex.filter((value) =>
        this.files.includes(value[1].at)
      );
    }

    filteredMessagesAndOriginalIndex = filteredMessagesAndOriginalIndex.filter((value) =>
      value[1].text.includes(this.searchString)
    );

    this.originalIndicesOfFilteredOutputMessages = filteredMessagesAndOriginalIndex.map(
      (value) => value[0]
    );
    const filteredMessages = filteredMessagesAndOriginalIndex.map((value) => value[1]);

    this.uiData = new UiData(allLogLevels, allTags, allSourceFiles, filteredMessages, undefined);
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
        this.entry.getIndex()
      ) ?? this.originalIndicesOfFilteredOutputMessages.length - 1;
  }

  private getUniqueMessageValues(getValue: (message: LogMessage) => string): string[] {
    const uniqueValues = new Set<string>();
    this.trace.forEachEntry((entry) => {
      uniqueValues.add(getValue(entry.getValue()));
    });
    const result = [...uniqueValues];
    result.sort();
    return result;
  }
}
