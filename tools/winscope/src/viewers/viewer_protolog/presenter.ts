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
import {LogMessage, ProtoLogTraceEntry} from 'trace/protolog';
import {TraceType} from 'trace/trace_type';
import {UiData} from './ui_data';

export class Presenter {
  constructor(notifyUiDataCallback: (data: UiData) => void) {
    this.notifyUiDataCallback = notifyUiDataCallback;
    this.originalIndicesOfFilteredOutputMessages = [];
    this.uiData = UiData.EMPTY;
    this.notifyUiDataCallback(this.uiData);
  }

  //TODO: replace input with something like iterator/cursor (same for other viewers/presenters)
  public notifyCurrentTraceEntries(entries: Map<TraceType, any>): void {
    this.entry = entries.get(TraceType.PROTO_LOG) ? entries.get(TraceType.PROTO_LOG)[0] : undefined;
    if (this.uiData === UiData.EMPTY) {
      this.computeUiDataMessages();
    }
    this.computeUiDataCurrentMessageIndex();
    this.notifyUiDataCallback(this.uiData);
  }

  public onLogLevelsFilterChanged(levels: string[]) {
    this.levels = levels;
    this.computeUiDataMessages();
    this.computeUiDataCurrentMessageIndex();
    this.notifyUiDataCallback(this.uiData);
  }

  public onTagsFilterChanged(tags: string[]) {
    this.tags = tags;
    this.computeUiDataMessages();
    this.computeUiDataCurrentMessageIndex();
    this.notifyUiDataCallback(this.uiData);
  }

  public onSourceFilesFilterChanged(files: string[]) {
    this.files = files;
    this.computeUiDataMessages();
    this.computeUiDataCurrentMessageIndex();
    this.notifyUiDataCallback(this.uiData);
  }

  public onSearchStringFilterChanged(searchString: string) {
    this.searchString = searchString;
    this.computeUiDataMessages();
    this.computeUiDataCurrentMessageIndex();
    this.notifyUiDataCallback(this.uiData);
  }

  private computeUiDataMessages() {
    if (!this.entry) {
      return;
    }

    const allLogLevels = this.getUniqueMessageValues(
      this.entry!.messages,
      (message: LogMessage) => message.level
    );
    const allTags = this.getUniqueMessageValues(
      this.entry!.messages,
      (message: LogMessage) => message.tag
    );
    const allSourceFiles = this.getUniqueMessageValues(
      this.entry!.messages,
      (message: LogMessage) => message.at
    );

    let filteredMessagesAndOriginalIndex: [number, LogMessage][] = [
      ...this.entry!.messages.entries(),
    ];

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

    this.uiData = new UiData(allLogLevels, allTags, allSourceFiles, filteredMessages, 0);
  }

  private computeUiDataCurrentMessageIndex() {
    if (!this.entry) {
      return;
    }

    this.uiData.currentMessageIndex = ArrayUtils.binarySearchLowerOrEqual(
      this.originalIndicesOfFilteredOutputMessages,
      this.entry.currentMessageIndex
    );
  }

  private getUniqueMessageValues(
    messages: LogMessage[],
    getValue: (message: LogMessage) => string
  ): string[] {
    const uniqueValues = new Set<string>();
    messages.forEach((message) => {
      uniqueValues.add(getValue(message));
    });
    const result = [...uniqueValues];
    result.sort();
    return result;
  }

  private entry?: ProtoLogTraceEntry;
  private originalIndicesOfFilteredOutputMessages: number[];
  private uiData: UiData;
  private readonly notifyUiDataCallback: (data: UiData) => void;

  private tags: string[] = [];
  private files: string[] = [];
  private levels: string[] = [];
  private searchString = '';
}
