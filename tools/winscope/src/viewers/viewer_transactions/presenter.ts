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
import {LogSelectFilter} from 'viewers/common/log_filters';
import {LogPresenter} from 'viewers/common/log_presenter';
import {PropertiesPresenter} from 'viewers/common/properties_presenter';
import {TextFilter, TextFilterValues} from 'viewers/common/text_filter';
import {LogField, LogHeader} from 'viewers/common/ui_data_log';
import {UserOptions} from 'viewers/common/user_options';
import {SetRootDisplayNames} from './operations/set_root_display_name';
import {TransactionsEntry, TransactionsEntryType, UiData} from './ui_data';

export class Presenter extends AbstractLogViewerPresenter<UiData> {
  private static readonly COLUMNS = {
    id: {name: 'TX ID', cssClass: 'transaction-id right-align'},
    vsyncId: {name: 'VSYNC ID', cssClass: 'vsyncid right-align'},
    pid: {name: 'PID', cssClass: 'pid right-align'},
    uid: {name: 'UID', cssClass: 'uid right-align'},
    type: {name: 'TYPE', cssClass: 'transaction-type'},
    layerOrDisplayId: {
      name: 'LAYER/DISP ID',
      cssClass: 'layer-or-display-id right-align',
    },
    flags: {name: 'Flags', cssClass: 'flags'},
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
    new TextFilter(
      PersistentStoreProxy.new<TextFilterValues>(
        'TransactionsPropertiesFilter',
        new TextFilterValues('', []),
        this.storage,
      ),
    ),
    [],
    [new SetRootDisplayNames()],
  );

  constructor(
    trace: Trace<PropertyTreeNode>,
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
      const vsyncId = Number(
        assertDefined(entryNode.getChildByName('vsyncId')).getValue(),
      );

      for (const transactionState of assertDefined(
        entryNode.getChildByName('transactions'),
      ).getAllChildren()) {
        const transactionId = assertDefined(
          transactionState.getChildByName('transactionId'),
        ).formattedValue();
        const pid = assertDefined(
          transactionState.getChildByName('pid'),
        ).formattedValue();
        const uid = assertDefined(
          transactionState.getChildByName('uid'),
        ).formattedValue();
        const layerChanges = assertDefined(
          transactionState.getChildByName('layerChanges'),
        ).getAllChildren();

        for (const layerState of layerChanges) {
          const fields: LogField[] = [
            {spec: Presenter.COLUMNS.id, value: transactionId},
            {spec: Presenter.COLUMNS.vsyncId, value: vsyncId},
            {spec: Presenter.COLUMNS.pid, value: pid},
            {spec: Presenter.COLUMNS.uid, value: uid},
            {
              spec: Presenter.COLUMNS.type,
              value: TransactionsEntryType.LAYER_CHANGED,
            },
            {
              spec: Presenter.COLUMNS.layerOrDisplayId,
              value: assertDefined(
                layerState.getChildByName('layerId'),
              ).formattedValue(),
            },
            {
              spec: Presenter.COLUMNS.flags,
              value: assertDefined(
                layerState.getChildByName('what'),
              ).formattedValue(),
            },
          ];
          entries.push(new TransactionsEntry(entry, fields, layerState));
        }

        const displayChanges = assertDefined(
          transactionState.getChildByName('displayChanges'),
        ).getAllChildren();
        for (const displayState of displayChanges) {
          const fields: LogField[] = [
            {spec: Presenter.COLUMNS.id, value: transactionId},
            {spec: Presenter.COLUMNS.vsyncId, value: vsyncId},
            {spec: Presenter.COLUMNS.pid, value: pid},
            {spec: Presenter.COLUMNS.uid, value: uid},
            {
              spec: Presenter.COLUMNS.type,
              value: TransactionsEntryType.DISPLAY_CHANGED,
            },
            {
              spec: Presenter.COLUMNS.layerOrDisplayId,
              value: assertDefined(
                displayState.getChildByName('id'),
              ).formattedValue(),
            },
            {
              spec: Presenter.COLUMNS.flags,
              value: assertDefined(
                displayState.getChildByName('what'),
              ).formattedValue(),
            },
          ];
          entries.push(new TransactionsEntry(entry, fields, displayState));
        }

        if (layerChanges.length === 0 && displayChanges.length === 0) {
          const fields: LogField[] = [
            {spec: Presenter.COLUMNS.id, value: transactionId},
            {spec: Presenter.COLUMNS.vsyncId, value: vsyncId},
            {spec: Presenter.COLUMNS.pid, value: pid},
            {spec: Presenter.COLUMNS.uid, value: uid},
            {
              spec: Presenter.COLUMNS.type,
              value: TransactionsEntryType.NO_OP,
            },
            {spec: Presenter.COLUMNS.layerOrDisplayId, value: ''},
            {spec: Presenter.COLUMNS.flags, value: ''},
          ];
          entries.push(new TransactionsEntry(entry, fields, undefined));
        }
      }

      for (const layerCreationArgs of assertDefined(
        entryNode.getChildByName('addedLayers'),
      ).getAllChildren()) {
        const fields: LogField[] = [
          {spec: Presenter.COLUMNS.id, value: ''},
          {spec: Presenter.COLUMNS.vsyncId, value: vsyncId},
          {spec: Presenter.COLUMNS.pid, value: Presenter.VALUE_NA},
          {spec: Presenter.COLUMNS.uid, value: Presenter.VALUE_NA},
          {
            spec: Presenter.COLUMNS.type,
            value: TransactionsEntryType.LAYER_ADDED,
          },
          {
            spec: Presenter.COLUMNS.layerOrDisplayId,
            value: assertDefined(
              layerCreationArgs.getChildByName('layerId'),
            ).formattedValue(),
          },
          {spec: Presenter.COLUMNS.flags, value: ''},
        ];
        entries.push(new TransactionsEntry(entry, fields, layerCreationArgs));
      }

      for (const destroyedLayerId of assertDefined(
        entryNode.getChildByName('destroyedLayers'),
      ).getAllChildren()) {
        const fields: LogField[] = [
          {spec: Presenter.COLUMNS.id, value: ''},
          {spec: Presenter.COLUMNS.vsyncId, value: vsyncId},
          {spec: Presenter.COLUMNS.pid, value: Presenter.VALUE_NA},
          {spec: Presenter.COLUMNS.uid, value: Presenter.VALUE_NA},
          {
            spec: Presenter.COLUMNS.type,
            value: TransactionsEntryType.LAYER_DESTROYED,
          },
          {
            spec: Presenter.COLUMNS.layerOrDisplayId,
            value: destroyedLayerId.formattedValue(),
          },
          {spec: Presenter.COLUMNS.flags, value: ''},
        ];
        entries.push(new TransactionsEntry(entry, fields, destroyedLayerId));
      }

      for (const displayState of assertDefined(
        entryNode.getChildByName('addedDisplays'),
      ).getAllChildren()) {
        const fields: LogField[] = [
          {spec: Presenter.COLUMNS.id, value: ''},
          {spec: Presenter.COLUMNS.vsyncId, value: vsyncId},
          {spec: Presenter.COLUMNS.pid, value: Presenter.VALUE_NA},
          {spec: Presenter.COLUMNS.uid, value: Presenter.VALUE_NA},
          {
            spec: Presenter.COLUMNS.type,
            value: TransactionsEntryType.DISPLAY_ADDED,
          },
          {
            spec: Presenter.COLUMNS.layerOrDisplayId,
            value: assertDefined(
              displayState.getChildByName('id'),
            ).formattedValue(),
          },
          {
            spec: Presenter.COLUMNS.flags,
            value: assertDefined(
              displayState.getChildByName('what'),
            ).formattedValue(),
          },
        ];
        entries.push(new TransactionsEntry(entry, fields, displayState));
      }

      for (const removedDisplayId of assertDefined(
        entryNode.getChildByName('removedDisplays'),
      ).getAllChildren()) {
        const fields: LogField[] = [
          {spec: Presenter.COLUMNS.id, value: ''},
          {spec: Presenter.COLUMNS.vsyncId, value: vsyncId},
          {spec: Presenter.COLUMNS.pid, value: Presenter.VALUE_NA},
          {spec: Presenter.COLUMNS.uid, value: Presenter.VALUE_NA},
          {
            spec: Presenter.COLUMNS.type,
            value: TransactionsEntryType.DISPLAY_REMOVED,
          },
          {
            spec: Presenter.COLUMNS.layerOrDisplayId,
            value: removedDisplayId.formattedValue(),
          },
          {spec: Presenter.COLUMNS.flags, value: ''},
        ];
        entries.push(new TransactionsEntry(entry, fields, removedDisplayId));
      }

      for (const destroyedLayerHandleId of assertDefined(
        entryNode.getChildByName('destroyedLayerHandles'),
      ).getAllChildren()) {
        const fields: LogField[] = [
          {spec: Presenter.COLUMNS.id, value: ''},
          {spec: Presenter.COLUMNS.vsyncId, value: vsyncId},
          {spec: Presenter.COLUMNS.pid, value: Presenter.VALUE_NA},
          {spec: Presenter.COLUMNS.uid, value: Presenter.VALUE_NA},
          {
            spec: Presenter.COLUMNS.type,
            value: TransactionsEntryType.LAYER_HANDLE_DESTROYED,
          },
          {
            spec: Presenter.COLUMNS.layerOrDisplayId,
            value: destroyedLayerHandleId.formattedValue(),
          },
          {spec: Presenter.COLUMNS.flags, value: ''},
        ];
        entries.push(
          new TransactionsEntry(entry, fields, destroyedLayerHandleId),
        );
      }
    }

    return entries;
  }

  protected override updateFiltersInHeaders(
    headers: LogHeader[],
    allEntries: TransactionsEntry[],
  ) {
    for (const header of headers) {
      if (header.spec === Presenter.COLUMNS.flags) {
        (assertDefined(header.filter) as LogSelectFilter).options =
          this.getUniqueUiDataEntryValues(
            allEntries,
            (entry: TransactionsEntry) =>
              assertDefined(
                entry.fields.find((f) => f.spec === header.spec)
                  ?.value as string,
              )
                .split('|')
                .map((flag) => flag.trim()),
          );
      } else {
        (assertDefined(header.filter) as LogSelectFilter).options =
          this.getUniqueUiDataEntryValues(
            allEntries,
            (entry: TransactionsEntry) =>
              assertDefined(
                entry.fields.find((f) => f.spec === header.spec),
              ).value.toString(),
          );
      }
    }
  }

  private getUniqueUiDataEntryValues<T>(
    entries: TransactionsEntry[],
    getValue: (entry: TransactionsEntry) => T | T[],
  ): T[] {
    const uniqueValues = new Set<T>();
    entries.forEach((entry: TransactionsEntry) => {
      const value = getValue(entry);
      if (Array.isArray(value)) {
        value.forEach((val) => uniqueValues.add(val));
      } else {
        uniqueValues.add(value);
      }
    });

    const result = [...uniqueValues];

    result.sort((a, b) => {
      const aIsNumber = !isNaN(Number(a));
      const bIsNumber = !isNaN(Number(b));

      if (aIsNumber && bIsNumber) {
        return Number(a) - Number(b);
      } else if (aIsNumber) {
        return 1; // place number after strings in the result
      } else if (bIsNumber) {
        return -1; // place number after strings in the result
      }

      // a and b are both strings
      if (a < b) {
        return -1;
      } else if (a > b) {
        return 1;
      } else {
        return 0;
      }
    });

    return result;
  }
}

const layerChangeFlagToPropertiesMap = new Map([
  ['eReparent', ['parentId']],
  ['eRelativeLayerChanged', ['relativeParentId']],
  ['eLayerChanged', ['layerId']],
  ['ePositionChanged', ['x', 'y', 'z']],
]);
