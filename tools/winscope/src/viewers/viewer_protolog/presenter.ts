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

import {assertDefined} from 'common/assert_utils';
import {Trace} from 'trace/trace';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {
  AbstractLogViewerPresenter,
  NotifyLogViewCallbackType,
} from 'viewers/common/abstract_log_viewer_presenter';
import {LogPresenter} from 'viewers/common/log_presenter';
import {LogField, LogFieldName, LogFilter} from 'viewers/common/ui_data_log';
import {ProtologEntry, UiData} from './ui_data';

export class Presenter extends AbstractLogViewerPresenter {
  static readonly FIELD_NAMES = [
    LogFieldName.LOG_LEVEL,
    LogFieldName.TAG,
    LogFieldName.SOURCE_FILE,
    LogFieldName.TEXT,
  ];
  private isInitialized = false;

  protected override logPresenter = new LogPresenter(true);

  constructor(
    trace: Trace<PropertyTreeNode>,
    notifyViewCallback: NotifyLogViewCallbackType,
  ) {
    super(trace, notifyViewCallback, UiData.EMPTY);
  }

  protected override async initializeIfNeeded() {
    if (this.isInitialized) {
      return;
    }
    const allEntries = await this.makeAllUiDataMessages();
    const filters: LogFilter[] = [];

    for (const name of Presenter.FIELD_NAMES) {
      if (name === LogFieldName.TEXT) {
        filters.push({
          name,
        });
      } else {
        filters.push({
          name,
          options: this.getUniqueMessageValues(
            allEntries,
            (entry: ProtologEntry) =>
              assertDefined(
                entry.fields.find((f) => f.name === name),
              ).value.toString(),
          ),
        });
      }
    }

    this.logPresenter.setAllEntries(allEntries);
    this.logPresenter.setFilters(filters);
    this.refreshUIData(UiData.EMPTY);
    this.isInitialized = true;
  }

  private async makeAllUiDataMessages(): Promise<ProtologEntry[]> {
    const messages: ProtologEntry[] = [];

    for (
      let traceIndex = 0;
      traceIndex < this.trace.lengthEntries;
      ++traceIndex
    ) {
      const entry = assertDefined(this.trace.getEntry(traceIndex));
      const messageNode = await entry.getValue();
      const fields: LogField[] = [
        {
          name: LogFieldName.LOG_LEVEL,
          value: assertDefined(
            messageNode.getChildByName('level'),
          ).formattedValue(),
        },
        {
          name: LogFieldName.TAG,
          value: assertDefined(
            messageNode.getChildByName('tag'),
          ).formattedValue(),
        },
        {
          name: LogFieldName.SOURCE_FILE,
          value: assertDefined(
            messageNode.getChildByName('at'),
          ).formattedValue(),
        },
        {
          name: LogFieldName.TEXT,
          value: assertDefined(
            messageNode.getChildByName('text'),
          ).formattedValue(),
        },
      ];
      messages.push(new ProtologEntry(entry, fields));
    }

    return messages;
  }

  private getUniqueMessageValues(
    allMessages: ProtologEntry[],
    getValue: (message: ProtologEntry) => string,
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
