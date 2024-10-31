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
import {PersistentStoreProxy} from 'common/persistent_store_proxy';
import {Store} from 'common/store';
import {Trace} from 'trace/trace';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {
  AbstractLogViewerPresenter,
  NotifyLogViewCallbackType,
} from 'viewers/common/abstract_log_viewer_presenter';
import {LogSelectFilter, LogTextFilter} from 'viewers/common/log_filters';
import {LogPresenter} from 'viewers/common/log_presenter';
import {TextFilter, TextFilterValues} from 'viewers/common/text_filter';
import {LogEntry, LogField, LogHeader} from 'viewers/common/ui_data_log';
import {ProtologEntry, UiData} from './ui_data';

export class Presenter extends AbstractLogViewerPresenter<UiData> {
  private static readonly COLUMNS = {
    logLevel: {
      name: 'Log Level',
      cssClass: 'log-level',
    },
    tag: {
      name: 'Tag',
      cssClass: 'tag',
    },
    sourceFile: {
      name: 'Source files',
      cssClass: 'source-file',
    },
    text: {
      name: 'Search text',
      cssClass: 'text',
    },
  };
  protected override logPresenter = new LogPresenter<LogEntry>();

  constructor(
    trace: Trace<PropertyTreeNode>,
    notifyViewCallback: NotifyLogViewCallbackType<UiData>,
    private storage: Store,
  ) {
    super(trace, notifyViewCallback, UiData.createEmpty());
  }

  protected override makeHeaders(): LogHeader[] {
    return [
      new LogHeader(Presenter.COLUMNS.logLevel, new LogSelectFilter([])),
      new LogHeader(
        Presenter.COLUMNS.tag,
        new LogSelectFilter([], false, '150'),
      ),
      new LogHeader(
        Presenter.COLUMNS.sourceFile,
        new LogSelectFilter([], false, '300'),
      ),
      new LogHeader(
        Presenter.COLUMNS.text,
        new LogTextFilter(new TextFilter()),
      ),
    ];
  }

  protected override async makeUiDataEntries(): Promise<ProtologEntry[]> {
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
          spec: Presenter.COLUMNS.logLevel,
          value: assertDefined(
            messageNode.getChildByName('level'),
          ).formattedValue(),
        },
        {
          spec: Presenter.COLUMNS.tag,
          value: assertDefined(
            messageNode.getChildByName('tag'),
          ).formattedValue(),
        },
        {
          spec: Presenter.COLUMNS.sourceFile,
          value: assertDefined(
            messageNode.getChildByName('at'),
          ).formattedValue(),
        },
        {
          spec: Presenter.COLUMNS.text,
          value: assertDefined(
            messageNode.getChildByName('text'),
          ).formattedValue(),
        },
      ];
      messages.push(new ProtologEntry(entry, fields));
    }

    return messages;
  }

  protected override updateFiltersInHeaders(
    headers: LogHeader[],
    allEntries: ProtologEntry[],
  ) {
    for (const header of headers) {
      if (header.filter instanceof LogTextFilter) {
        header.filter.textFilter = new TextFilter(
          PersistentStoreProxy.new<TextFilterValues>(
            'ProtoLog' + header.spec.name,
            new TextFilterValues('', []),
            this.storage,
          ),
        );
      } else if (header.filter instanceof LogSelectFilter) {
        assertDefined(header.filter).options = this.getUniqueMessageValues(
          allEntries,
          (entry: ProtologEntry) =>
            assertDefined(
              entry.fields.find((f) => f.spec === header.spec),
            ).value.toString(),
        );
      }
    }
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
