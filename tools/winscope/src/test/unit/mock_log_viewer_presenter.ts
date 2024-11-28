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

import {assertDefined} from 'common/assert_utils';
import {Store} from 'common/store';
import {Trace} from 'trace/trace';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {
  AbstractLogViewerPresenter,
  NotifyLogViewCallbackType,
} from 'viewers/common/abstract_log_viewer_presenter';
import {LogSelectFilter, LogTextFilter} from 'viewers/common/log_filters';
import {LogPresenter} from 'viewers/common/log_presenter';
import {PropertiesPresenter} from 'viewers/common/properties_presenter';
import {TextFilter} from 'viewers/common/text_filter';
import {LogEntry, LogHeader, UiDataLog} from 'viewers/common/ui_data_log';
import {UiPropertyTreeNode} from 'viewers/common/ui_property_tree_node';
import {UserOptions} from 'viewers/common/user_options';

export class MockPresenter extends AbstractLogViewerPresenter<
  UiDataLog,
  PropertyTreeNode
> {
  protected override logPresenter = new LogPresenter<LogEntry>();
  protected override propertiesPresenter = new PropertiesPresenter(
    {
      showDefaults: {
        name: 'Show defaults',
        enabled: false,
        tooltip: `
              If checked, shows the value of all properties.
              Otherwise, hides all properties whose value is
              the default for its data type.
            `,
      },
    },
    new TextFilter(),
    [],
  );
  stringColumn = {
    name: 'String Column',
    cssClass: 'string-column',
  };
  numberColumn = {
    name: 'Number Column',
    cssClass: 'number-column',
  };
  timestampColumn = {
    name: 'Timestamp Column',
    cssClass: 'timestamp-column',
  };

  constructor(
    trace: Trace<PropertyTreeNode>,
    readonly storage: Store,
    notifyViewCallback: NotifyLogViewCallbackType<UiDataLog>,
    withProperties: boolean,
  ) {
    super(
      trace,
      notifyViewCallback,
      withProperties
        ? MockDataWithProperties.createEmpty()
        : MockDataWithoutProperties.createEmpty(),
    );
  }

  protected override async makeUiDataEntries(): Promise<LogEntry[]> {
    if (this.trace.lengthEntries === 0) return [];
    const entries: LogEntry[] = [
      {
        traceEntry: this.trace.getEntry(0),
        fields: [
          {spec: this.stringColumn, value: 'stringValue'},
          {spec: this.numberColumn, value: 0},
          {
            spec: this.timestampColumn,
            value: this.trace.getEntry(0).getTimestamp(),
          },
        ],
        propertiesTree: await this.trace.getEntry(0).getValue(),
      },
      {
        traceEntry: this.trace.getEntry(1),
        fields: [
          {spec: this.stringColumn, value: 'differentValue'},
          {spec: this.numberColumn, value: 1},
          {
            spec: this.timestampColumn,
            value: this.trace.getEntry(1).getTimestamp(),
          },
        ],
        propertiesTree: await this.trace.getEntry(1).getValue(),
      },
      {
        traceEntry: this.trace.getEntry(2),
        fields: [
          {spec: this.stringColumn, value: 'stringValue'},
          {spec: this.numberColumn, value: 2},
          {
            spec: this.timestampColumn,
            value: this.trace.getEntry(2).getTimestamp(),
          },
        ],
        propertiesTree: await this.trace.getEntry(2).getValue(),
      },
      {
        traceEntry: this.trace.getEntry(3),
        fields: [
          {spec: this.stringColumn, value: 'differentValue'},
          {spec: this.numberColumn, value: 3},
          {
            spec: this.timestampColumn,
            value: this.trace.getEntry(3).getTimestamp(),
          },
        ],
        propertiesTree: await this.trace.getEntry(3).getValue(),
      },
    ];
    return entries;
  }

  protected override makeHeaders(): LogHeader[] {
    const stringFilter = new LogTextFilter(new TextFilter());
    const numberFilter = new LogSelectFilter(['0', '1', '2', '3']);
    const headers = [
      new LogHeader(this.stringColumn, stringFilter),
      new LogHeader(this.numberColumn, numberFilter),
      new LogHeader(this.timestampColumn),
    ];
    return headers;
  }

  protected override updateFiltersInHeaders(
    headers: LogHeader[],
    allEntries: LogEntry[],
  ) {
    for (const header of headers) {
      if (header.spec === this.stringColumn) {
        (assertDefined(header.filter) as LogSelectFilter).options = [
          'stringValue',
          'differentValue',
        ];
      }
    }
  }
}

export class MockDataWithProperties implements UiDataLog {
  constructor(
    public headers: LogHeader[],
    public entries: LogEntry[],
    public currentIndex: undefined | number,
    public selectedIndex: undefined | number,
    public scrollToIndex: undefined | number,
    public propertiesTree: undefined | UiPropertyTreeNode,
    public propertiesUserOptions: UserOptions,
    public isDarkMode = false,
  ) {}

  static createEmpty(): MockDataWithProperties {
    return new MockDataWithProperties(
      [],
      [],
      undefined,
      undefined,
      undefined,
      undefined,
      {},
    );
  }
}

export class MockDataWithoutProperties implements UiDataLog {
  constructor(
    public headers: LogHeader[],
    public entries: LogEntry[],
    public currentIndex: undefined | number,
    public selectedIndex: undefined | number,
    public scrollToIndex: undefined | number,
    public isDarkMode = false,
  ) {}

  static createEmpty(): MockDataWithoutProperties {
    return new MockDataWithoutProperties(
      [],
      [],
      undefined,
      undefined,
      undefined,
    );
  }
}
