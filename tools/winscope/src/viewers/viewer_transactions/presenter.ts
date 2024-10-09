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
import {LogPresenter} from 'viewers/common/log_presenter';
import {PropertiesPresenter} from 'viewers/common/properties_presenter';
import {TextFilter} from 'viewers/common/text_filter';
import {LogField, LogFieldType, LogFilter} from 'viewers/common/ui_data_log';
import {UserOptions} from 'viewers/common/user_options';
import {SetRootDisplayNames} from './operations/set_root_display_name';
import {TransactionsEntry, TransactionsEntryType, UiData} from './ui_data';

export class Presenter extends AbstractLogViewerPresenter<UiData> {
  private static readonly FIELD_TYPES = [
    LogFieldType.TRANSACTION_ID,
    LogFieldType.VSYNC_ID,
    LogFieldType.PID,
    LogFieldType.UID,
    LogFieldType.TRANSACTION_TYPE,
    LogFieldType.LAYER_OR_DISPLAY_ID,
    LogFieldType.FLAGS,
  ];
  private static readonly VALUE_NA = 'N/A';
  private isInitialized = false;

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
    PersistentStoreProxy.new<TextFilter>(
      'TransactionsPropertiesFilter',
      new TextFilter('', []),
      this.storage,
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

  protected override async initializeIfNeeded() {
    if (this.isInitialized) {
      return;
    }

    const allEntries = await this.makeUiDataEntries();
    const filters: LogFilter[] = [];

    for (const type of Presenter.FIELD_TYPES) {
      if (type === LogFieldType.FLAGS) {
        filters.push(
          new LogFilter(
            type,
            this.getUniqueUiDataEntryValues(
              allEntries,
              (entry: TransactionsEntry) =>
                assertDefined(
                  entry.fields.find((f) => f.type === type)?.value as string,
                )
                  .split('|')
                  .map((flag) => flag.trim()),
            ),
          ),
        );
      } else {
        filters.push(
          new LogFilter(
            type,
            this.getUniqueUiDataEntryValues(
              allEntries,
              (entry: TransactionsEntry) =>
                assertDefined(
                  entry.fields.find((f) => f.type === type),
                ).value.toString(),
            ),
          ),
        );
      }
    }

    this.logPresenter.setAllEntries(allEntries);
    this.logPresenter.setFilters(filters);
    this.refreshUiData();
    this.isInitialized = true;
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

  private async makeUiDataEntries(): Promise<TransactionsEntry[]> {
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
            {type: LogFieldType.TRANSACTION_ID, value: transactionId},
            {type: LogFieldType.VSYNC_ID, value: vsyncId},
            {type: LogFieldType.PID, value: pid},
            {type: LogFieldType.UID, value: uid},
            {
              type: LogFieldType.TRANSACTION_TYPE,
              value: TransactionsEntryType.LAYER_CHANGED,
            },
            {
              type: LogFieldType.LAYER_OR_DISPLAY_ID,
              value: assertDefined(
                layerState.getChildByName('layerId'),
              ).formattedValue(),
            },
            {
              type: LogFieldType.FLAGS,
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
            {type: LogFieldType.TRANSACTION_ID, value: transactionId},
            {type: LogFieldType.VSYNC_ID, value: vsyncId},
            {type: LogFieldType.PID, value: pid},
            {type: LogFieldType.UID, value: uid},
            {
              type: LogFieldType.TRANSACTION_TYPE,
              value: TransactionsEntryType.DISPLAY_CHANGED,
            },
            {
              type: LogFieldType.LAYER_OR_DISPLAY_ID,
              value: assertDefined(
                displayState.getChildByName('id'),
              ).formattedValue(),
            },
            {
              type: LogFieldType.FLAGS,
              value: assertDefined(
                displayState.getChildByName('what'),
              ).formattedValue(),
            },
          ];
          entries.push(new TransactionsEntry(entry, fields, displayState));
        }

        if (layerChanges.length === 0 && displayChanges.length === 0) {
          const fields: LogField[] = [
            {type: LogFieldType.TRANSACTION_ID, value: transactionId},
            {type: LogFieldType.VSYNC_ID, value: vsyncId},
            {type: LogFieldType.PID, value: pid},
            {type: LogFieldType.UID, value: uid},
            {
              type: LogFieldType.TRANSACTION_TYPE,
              value: TransactionsEntryType.NO_OP,
            },
            {type: LogFieldType.LAYER_OR_DISPLAY_ID, value: ''},
            {type: LogFieldType.FLAGS, value: ''},
          ];
          entries.push(new TransactionsEntry(entry, fields, undefined));
        }
      }

      for (const layerCreationArgs of assertDefined(
        entryNode.getChildByName('addedLayers'),
      ).getAllChildren()) {
        const fields: LogField[] = [
          {type: LogFieldType.TRANSACTION_ID, value: ''},
          {type: LogFieldType.VSYNC_ID, value: vsyncId},
          {type: LogFieldType.PID, value: Presenter.VALUE_NA},
          {type: LogFieldType.UID, value: Presenter.VALUE_NA},
          {
            type: LogFieldType.TRANSACTION_TYPE,
            value: TransactionsEntryType.LAYER_ADDED,
          },
          {
            type: LogFieldType.LAYER_OR_DISPLAY_ID,
            value: assertDefined(
              layerCreationArgs.getChildByName('layerId'),
            ).formattedValue(),
          },
          {type: LogFieldType.FLAGS, value: ''},
        ];
        entries.push(new TransactionsEntry(entry, fields, layerCreationArgs));
      }

      for (const destroyedLayerId of assertDefined(
        entryNode.getChildByName('destroyedLayers'),
      ).getAllChildren()) {
        const fields: LogField[] = [
          {type: LogFieldType.TRANSACTION_ID, value: ''},
          {type: LogFieldType.VSYNC_ID, value: vsyncId},
          {type: LogFieldType.PID, value: Presenter.VALUE_NA},
          {type: LogFieldType.UID, value: Presenter.VALUE_NA},
          {
            type: LogFieldType.TRANSACTION_TYPE,
            value: TransactionsEntryType.LAYER_DESTROYED,
          },
          {
            type: LogFieldType.LAYER_OR_DISPLAY_ID,
            value: destroyedLayerId.formattedValue(),
          },
          {type: LogFieldType.FLAGS, value: ''},
        ];
        entries.push(new TransactionsEntry(entry, fields, destroyedLayerId));
      }

      for (const displayState of assertDefined(
        entryNode.getChildByName('addedDisplays'),
      ).getAllChildren()) {
        const fields: LogField[] = [
          {type: LogFieldType.TRANSACTION_ID, value: ''},
          {type: LogFieldType.VSYNC_ID, value: vsyncId},
          {type: LogFieldType.PID, value: Presenter.VALUE_NA},
          {type: LogFieldType.UID, value: Presenter.VALUE_NA},
          {
            type: LogFieldType.TRANSACTION_TYPE,
            value: TransactionsEntryType.DISPLAY_ADDED,
          },
          {
            type: LogFieldType.LAYER_OR_DISPLAY_ID,
            value: assertDefined(
              displayState.getChildByName('id'),
            ).formattedValue(),
          },
          {
            type: LogFieldType.FLAGS,
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
          {type: LogFieldType.TRANSACTION_ID, value: ''},
          {type: LogFieldType.VSYNC_ID, value: vsyncId},
          {type: LogFieldType.PID, value: Presenter.VALUE_NA},
          {type: LogFieldType.UID, value: Presenter.VALUE_NA},
          {
            type: LogFieldType.TRANSACTION_TYPE,
            value: TransactionsEntryType.DISPLAY_REMOVED,
          },
          {
            type: LogFieldType.LAYER_OR_DISPLAY_ID,
            value: removedDisplayId.formattedValue(),
          },
          {type: LogFieldType.FLAGS, value: ''},
        ];
        entries.push(new TransactionsEntry(entry, fields, removedDisplayId));
      }

      for (const destroyedLayerHandleId of assertDefined(
        entryNode.getChildByName('destroyedLayerHandles'),
      ).getAllChildren()) {
        const fields: LogField[] = [
          {type: LogFieldType.TRANSACTION_ID, value: ''},
          {type: LogFieldType.VSYNC_ID, value: vsyncId},
          {type: LogFieldType.PID, value: Presenter.VALUE_NA},
          {type: LogFieldType.UID, value: Presenter.VALUE_NA},
          {
            type: LogFieldType.TRANSACTION_TYPE,
            value: TransactionsEntryType.LAYER_HANDLE_DESTROYED,
          },
          {
            type: LogFieldType.LAYER_OR_DISPLAY_ID,
            value: destroyedLayerHandleId.formattedValue(),
          },
          {type: LogFieldType.FLAGS, value: ''},
        ];
        entries.push(
          new TransactionsEntry(entry, fields, destroyedLayerHandleId),
        );
      }
    }

    return entries;
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
