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
import {Timestamp} from 'common/time';
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
import {LogField, LogHeader} from 'viewers/common/ui_data_log';
import {CujEntry, CujStatus, UiData} from './ui_data';

export class Presenter extends AbstractLogViewerPresenter<UiData> {
  private static readonly COLUMNS = {
    type: {
      name: 'Type',
      cssClass: 'jank-cuj-type',
    },
    startTime: {
      name: 'Start Time',
      cssClass: 'start-time time',
    },
    endTime: {
      name: 'End Time',
      cssClass: 'end-time time',
    },
    duration: {
      name: 'Duration',
      cssClass: 'duration right-align',
    },
    status: {
      name: 'Status',
      cssClass: 'status right-align',
    },
  };
  private transitionTrace: Trace<PropertyTreeNode>;

  protected override logPresenter = new LogPresenter<CujEntry>();
  protected override propertiesPresenter = new PropertiesPresenter(
    {},
    new TextFilter(),
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

  protected override makeHeaders(): LogHeader[] {
    return [
      new LogHeader(Presenter.COLUMNS.type),
      new LogHeader(Presenter.COLUMNS.startTime),
      new LogHeader(Presenter.COLUMNS.endTime),
      new LogHeader(Presenter.COLUMNS.duration),
      new LogHeader(Presenter.COLUMNS.status),
    ];
  }

  protected override async makeUiDataEntries(): Promise<CujEntry[]> {
    const cujs: CujEntry[] = [];
    for (
      let traceIndex = 0;
      traceIndex < this.transitionTrace.lengthEntries;
      ++traceIndex
    ) {
      const entry = assertDefined(this.trace.getEntry(traceIndex));
      const cujNode = await entry.getValue();

      let status: CujStatus;
      let statusIcon: string;
      let statusIconColor: string;
      if (assertDefined(cujNode.getChildByName('canceled')).getValue()) {
        status = CujStatus.CANCELLED;
        statusIcon = 'close';
        statusIconColor = 'red';
      } else {
        status = CujStatus.EXECUTED;
        statusIcon = 'check';
        statusIconColor = 'green';
      }

      const startTs: Timestamp | undefined = cujNode
        .getChildByName('startTimestamp')
        ?.getValue();
      const endTs: Timestamp | undefined = cujNode
        .getChildByName('endTimestamp')
        ?.getValue();

      let timeDiff: TimeDuration | undefined = undefined;
      if (startTs && endTs) {
        const timeDiffNs = endTs.minus(startTs.getValueNs()).getValueNs();
        timeDiff = new TimeDuration(timeDiffNs);
      }

      const cujType = assertDefined(
        cujNode.getChildByName('cujType'),
      ).formattedValue();

      const fields: LogField[] = [
        {spec: Presenter.COLUMNS.type, value: cujType},
        {
          spec: Presenter.COLUMNS.startTime,
          value: startTs ?? Presenter.VALUE_NA,
        },
        {
          spec: Presenter.COLUMNS.endTime,
          value: endTs ?? Presenter.VALUE_NA,
        },
        {
          spec: Presenter.COLUMNS.duration,
          value: timeDiff?.format() ?? Presenter.VALUE_NA,
        },
        {
          spec: Presenter.COLUMNS.status,
          value: status,
          icon: statusIcon,
          iconColor: statusIconColor,
        },
      ];
      cujs.push(new CujEntry(entry, fields, cujNode));
    }

    return cujs;
  }
}
