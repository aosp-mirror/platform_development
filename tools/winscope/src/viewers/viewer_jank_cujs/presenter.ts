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
import {PersistentStoreProxy} from 'common/persistent_store_proxy';
import {Store} from 'common/store';
import {TimeDuration} from 'common/time_duration';
import {Trace} from 'trace/trace';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {
  AbstractLogViewerPresenter,
  NotifyLogViewCallbackType,
} from 'viewers/common/abstract_log_viewer_presenter';
import {LogPresenter} from 'viewers/common/log_presenter';
import {PropertiesPresenter} from 'viewers/common/properties_presenter';
import {TextFilter} from 'viewers/common/text_filter';
import {LogField, LogFieldType} from 'viewers/common/ui_data_log';
import {CujEntry, CujStatus, UiData} from './ui_data';

export class Presenter extends AbstractLogViewerPresenter<UiData> {
  static readonly FIELD_NAMES = [
    LogFieldType.CUJ_TYPE,
    LogFieldType.START_TIME,
    LogFieldType.END_TIME,
    LogFieldType.DURATION,
    LogFieldType.STATUS,
  ];
  private static readonly VALUE_NA = 'N/A';

  private isInitialized = false;
  private transitionTrace: Trace<PropertyTreeNode>;

  protected override logPresenter = new LogPresenter<CujEntry>();
  protected override propertiesPresenter = new PropertiesPresenter(
    {},
    PersistentStoreProxy.new<TextFilter>(
      'CujsPropertiesFilter',
      new TextFilter('', []),
      this.storage,
    ),
    [],
    [],
  );

  constructor(
    trace: Trace<PropertyTreeNode>,
    private readonly storage: Store,
    notifyViewCallback: NotifyLogViewCallbackType<UiData>,
  ) {
    super(trace, notifyViewCallback, UiData.createEmpty());
    this.transitionTrace = trace;
  }

  protected async initializeIfNeeded() {
    if (this.isInitialized) {
      return;
    }

    const allEntries = await this.makeUiDataEntries();

    this.logPresenter.setAllEntries(allEntries);
    this.logPresenter.setHeaders(Presenter.FIELD_NAMES);
    this.refreshUiData();
    this.isInitialized = true;
  }

  private async makeUiDataEntries(): Promise<CujEntry[]> {
    const cujs: CujEntry[] = [];
    for (
      let traceIndex = 0;
      traceIndex < this.transitionTrace.lengthEntries;
      ++traceIndex
    ) {
      const entry = assertDefined(this.trace.getEntry(traceIndex));
      const cujNode = await entry.getValue();

      let status: CujStatus | undefined;
      let statusIcon: string | undefined;
      let statusIconColor: string | undefined;
      if (assertDefined(cujNode.getChildByName('canceled')).getValue()) {
        status = CujStatus.CANCELLED;
        statusIcon = 'close';
        statusIconColor = 'red';
      } else {
        status = CujStatus.EXECUTED;
        statusIcon = 'check';
        statusIconColor = 'green';
      }

      const startTs = cujNode.getChildByName('startTimestamp')?.getValue();
      const endTs = cujNode.getChildByName('endTimestamp')?.getValue();

      let timeDiff: TimeDuration | undefined = undefined;
      if (startTs && endTs) {
        const timeDiffNs = endTs.minus(startTs.getValueNs()).getValueNs();
        timeDiff = new TimeDuration(timeDiffNs);
      }

      const cujType = assertDefined(
        cujNode.getChildByName('cujType'),
      ).formattedValue();

      const fields: LogField[] = [
        {
          type: LogFieldType.CUJ_TYPE,
          value: cujType,
        },
        {
          type: LogFieldType.START_TIME,
          value: startTs ?? Presenter.VALUE_NA,
        },
        {
          type: LogFieldType.END_TIME,
          value: endTs ?? Presenter.VALUE_NA,
        },
        {
          type: LogFieldType.DURATION,
          value: timeDiff?.format() ?? Presenter.VALUE_NA,
        },
        {
          type: LogFieldType.STATUS,
          value: status ?? Presenter.VALUE_NA,
          icon: statusIcon,
          iconColor: statusIconColor,
        },
      ];
      cujs.push(new CujEntry(entry, fields, cujNode));
    }

    return cujs;
  }
}
