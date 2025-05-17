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
import {PersistentStoreProxy} from 'common/store/persistent_store_proxy';
import {Store} from 'common/store/store';
import {Trace} from 'trace/trace';
import {TransactionColumnType} from 'trace/transactions/transaction_column_type';
import {TransactionType} from 'trace/transactions/transaction_type';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {LazyPropertiesStrategyType} from 'trace/tree_node/properties_provider';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {
  AbstractLogViewerPresenter,
  NotifyLogViewCallbackType,
} from 'viewers/common/abstract_log_viewer_presenter';
import {LogSelectFilter} from 'viewers/common/log_filters';
import {LogPresenter} from 'viewers/common/log_presenter';
import {PropertiesPresenter} from 'viewers/common/properties_presenter';
import {TextFilter} from 'viewers/common/text_filter';
import {LogField, LogHeader} from 'viewers/common/ui_data_log';
import {UserOptions} from 'viewers/common/user_options';
import {TransactionsEntry, UiData} from './ui_data';

export class Presenter extends AbstractLogViewerPresenter<
  UiData,
  HierarchyTreeNode
> {
  private static readonly COLUMNS = {
    id: {
      name: 'TX ID',
      cssClass: 'transaction-id right-align',
      columnType: TransactionColumnType.TRANSACTION_ID,
    },
    vsyncId: {
      name: 'VSYNC ID',
      cssClass: 'vsyncid right-align',
      columnType: TransactionColumnType.VSYNC_ID,
    },
    pid: {
      name: 'PID',
      cssClass: 'pid right-align',
      columnType: TransactionColumnType.PID,
    },
    uid: {
      name: 'UID',
      cssClass: 'uid right-align',
      columnType: TransactionColumnType.UID,
    },
    process: {
      name: 'PROCESS',
      cssClass: 'process',
      columnType: TransactionColumnType.PROCESS,
    },
    type: {
      name: 'TYPE',
      cssClass: 'transaction-type',
      columnType: TransactionColumnType.TRANSACTION_TYPE,
    },
    layerOrDisplayId: {
      name: 'LAYER/DISP ID',
      cssClass: 'layer-or-display-id right-align',
      columnType: TransactionColumnType.LAYER_OR_DISPLAY_ID,
    },
    flags: {
      name: 'Flags',
      cssClass: 'flags',
      columnType: TransactionColumnType.FLAGS,
    },
  };

  protected override keepCalculated = true;
  protected override logPresenter = new LogPresenter<TransactionsEntry>();
  protected override propertiesPresenter = new PropertiesPresenter(
    PersistentStoreProxy.new<UserOptions>(
      'TransactionsPropertyOptions',
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
      this.storage,
    ),
    new TextFilter(),
    [],
  );

  constructor(
    trace: Trace<HierarchyTreeNode>,
    readonly storage: Store,
    notifyViewCallback: NotifyLogViewCallbackType<UiData>,
  ) {
    super(trace, notifyViewCallback, UiData.createEmpty());
  }

  protected override makeHeaders(): LogHeader[] {
    return [
      new LogHeader(
        Presenter.COLUMNS.id,
        new LogSelectFilter([], false, '125'),
      ),
      new LogHeader(
        Presenter.COLUMNS.vsyncId,
        new LogSelectFilter([], false, '90'),
      ),
      new LogHeader(Presenter.COLUMNS.pid, new LogSelectFilter([])),
      new LogHeader(Presenter.COLUMNS.uid, new LogSelectFilter([])),
      new LogHeader(Presenter.COLUMNS.process, new LogSelectFilter([])),
      new LogHeader(
        Presenter.COLUMNS.type,
        new LogSelectFilter([], false, '175'),
      ),
      new LogHeader(
        Presenter.COLUMNS.layerOrDisplayId,
        new LogSelectFilter([]),
      ),
      new LogHeader(
        Presenter.COLUMNS.flags,
        new LogSelectFilter([], true, '250', '100%'),
      ),
    ];
  }

  protected override updateDefaultAllowlist(
    tree: PropertyTreeNode | undefined,
  ): void {
    if (!tree) {
      return;
    }
    const allowlist: string[] = [];
    tree
      .getChildByName('what')
      ?.formattedValue()
      .split(' | ')
      .forEach((flag) => {
        const properties = layerChangeFlagToPropertiesMap.get(flag);
        if (properties !== undefined) {
          allowlist.push(...properties);
        } else if (flag.startsWith('e')) {
          const candidateProperty = flag.split('Changed')[0].slice(1);
          allowlist.push(
            candidateProperty[0].toLowerCase() + candidateProperty.slice(1),
          );
        }
      });
    this.propertiesPresenter.updateDefaultAllowList(allowlist);
  }

  protected override async makeUiDataEntries(): Promise<TransactionsEntry[]> {
    const entries: TransactionsEntry[] = [];

    const entryProtos = await Promise.all(
      this.trace.mapEntry(async (entry) => {
        return await entry.getValue();
      }),
    );

    for (
      let traceIndex = 0;
      traceIndex < this.trace.lengthEntries;
      ++traceIndex
    ) {
      const entry = this.trace.getEntry(traceIndex);
      const entryNode = entryProtos[traceIndex];
      const vsyncId = entryNode.getEagerPropertyByName('vsyncId')?.getValue();

      for (const transactionNode of entryNode.getAllChildren()) {
        const transactionType = assertDefined(
          transactionNode.getEagerPropertyByName('transactionType'),
        ).formattedValue();
        const transactionId = transactionNode
          .getEagerPropertyByName('transactionId')
          ?.formattedValue();
        const pid = transactionNode
          .getEagerPropertyByName('pid')
          ?.formattedValue();
        const uid = transactionNode
          .getEagerPropertyByName('uid')
          ?.formattedValue();
        const process = transactionNode
          .getEagerPropertyByName('processName')
          ?.formattedValue();
        const layerId = transactionNode
          .getEagerPropertyByName('layerId')
          ?.formattedValue();
        const displayId = transactionNode
          .getEagerPropertyByName('displayId')
          ?.formattedValue();
        const flags = transactionNode
          .getEagerPropertyByName('flagsId')
          ?.formattedValue();

        let getPropertiesTree: LazyPropertiesStrategyType | undefined;
        switch (transactionType) {
          case TransactionType.LAYER_CHANGED:
          case TransactionType.DISPLAY_CHANGED:
          case TransactionType.LAYER_ADDED:
          case TransactionType.DISPLAY_ADDED:
            getPropertiesTree = async () => {
              return await transactionNode.getAllProperties();
            };
            break;

          default:
            // do nothing
            break;
        }

        const layerOrDisplayId =
          (layerId?.length ?? 0) > 0
            ? assertDefined(layerId)
            : displayId ?? Presenter.VALUE_NA;

        const fields: LogField[] = [
          {
            spec: Presenter.COLUMNS.id,
            value: transactionId ?? Presenter.VALUE_NA,
          },
          {spec: Presenter.COLUMNS.vsyncId, value: assertDefined(vsyncId)},
          {spec: Presenter.COLUMNS.pid, value: pid ?? Presenter.VALUE_NA},
          {spec: Presenter.COLUMNS.uid, value: uid ?? Presenter.VALUE_NA},
          {
            spec: Presenter.COLUMNS.process,
            value: process ?? Presenter.VALUE_NA,
          },
          {
            spec: Presenter.COLUMNS.type,
            value: transactionType,
          },
          {
            spec: Presenter.COLUMNS.layerOrDisplayId,
            value: layerOrDisplayId,
          },
          {
            spec: Presenter.COLUMNS.flags,
            value: flags ?? Presenter.VALUE_NA,
          },
        ];
        entries.push(new TransactionsEntry(entry, fields, getPropertiesTree));
      }
    }
    return entries;
  }

  protected override async updateFiltersInHeaders(headers: LogHeader[]) {
    for (const header of headers) {
      this.updateFilterByCustomQuery(header);
    }
  }
}

const layerChangeFlagToPropertiesMap = new Map([
  ['eReparent', ['parentId']],
  ['eRelativeLayerChanged', ['relativeParentId']],
  ['eLayerChanged', ['layerId']],
  ['ePositionChanged', ['x', 'y', 'z']],
]);
