/*
 * Copyright (C) 2023 The Android Open Source Project
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
import {CustomQueryType} from 'trace/custom_query';
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceType} from 'trace/trace_type';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {
  AbstractLogViewerPresenter,
  NotifyLogViewCallbackType,
} from 'viewers/common/abstract_log_viewer_presenter';
import {LogSelectFilter} from 'viewers/common/log_filters';
import {LogPresenter} from 'viewers/common/log_presenter';
import {PropertiesPresenter} from 'viewers/common/properties_presenter';
import {TextFilter, TextFilterValues} from 'viewers/common/text_filter';
import {ColumnSpec, LogField, LogHeader} from 'viewers/common/ui_data_log';
import {UpdateTransitionChangesNames} from './operations/update_transition_changes_names';
import {TransitionsEntry, TransitionStatus, UiData} from './ui_data';

export class Presenter extends AbstractLogViewerPresenter<UiData> {
  private static readonly COLUMNS = {
    id: {name: 'Id', cssClass: 'transition-id right-align'},
    type: {name: 'Type', cssClass: 'transition-type'},
    sendTime: {name: 'Send Time', cssClass: 'send-time time'},
    dispatchTime: {name: 'Dispatch Time', cssClass: 'dispatch-time time'},
    duration: {name: 'Duration', cssClass: 'duration right-align'},
    handler: {name: 'Handler', cssClass: 'handler'},
    participants: {name: 'Participants', cssClass: 'participants'},
    flags: {name: 'Flags', cssClass: 'flags'},
    status: {name: 'Status', cssClass: 'status right-align'},
  };
  private transitionTrace: Trace<PropertyTreeNode>;
  private surfaceFlingerTrace: Trace<HierarchyTreeNode> | undefined;
  private windowManagerTrace: Trace<HierarchyTreeNode> | undefined;
  private layerIdToName = new Map<number, string>();
  private windowTokenToTitle = new Map<string, string>();
  private updateTransitionChangesNames = new UpdateTransitionChangesNames(
    this.layerIdToName,
    this.windowTokenToTitle,
  );
  private uniqueFieldValues = new Map<ColumnSpec, Set<string>>();

  protected override keepCalculated = false;
  protected override logPresenter = new LogPresenter<TransitionsEntry>(false);
  protected override propertiesPresenter = new PropertiesPresenter(
    {},
    new TextFilter(
      PersistentStoreProxy.new<TextFilterValues>(
        'TransitionsPropertiesFilter',
        new TextFilterValues('', []),
        this.storage,
      ),
    ),
    [],
  );

  constructor(
    trace: Trace<PropertyTreeNode>,
    traces: Traces,
    readonly storage: Store,
    notifyViewCallback: NotifyLogViewCallbackType<UiData>,
  ) {
    super(trace, notifyViewCallback, UiData.createEmpty());
    this.transitionTrace = trace;
    this.surfaceFlingerTrace = traces.getTrace(TraceType.SURFACE_FLINGER);
    this.windowManagerTrace = traces.getTrace(TraceType.WINDOW_MANAGER);
  }

  protected override async initializeTraceSpecificData() {
    if (this.surfaceFlingerTrace) {
      const layersIdAndName = await this.surfaceFlingerTrace.customQuery(
        CustomQueryType.SF_LAYERS_ID_AND_NAME,
      );
      layersIdAndName.forEach((value) => {
        this.layerIdToName.set(value.id, value.name);
      });
    }

    if (this.windowManagerTrace) {
      const windowsTokenAndTitle = await this.windowManagerTrace.customQuery(
        CustomQueryType.WM_WINDOWS_TOKEN_AND_TITLE,
      );
      windowsTokenAndTitle.forEach((value) => {
        this.windowTokenToTitle.set(value.token, value.title);
      });
    }
  }

  protected override makeHeaders(): LogHeader[] {
    return [
      new LogHeader(Presenter.COLUMNS.id),
      new LogHeader(
        Presenter.COLUMNS.type,
        new LogSelectFilter([], false, '175'),
      ),
      new LogHeader(Presenter.COLUMNS.sendTime),
      new LogHeader(Presenter.COLUMNS.dispatchTime),
      new LogHeader(Presenter.COLUMNS.duration),
      new LogHeader(
        Presenter.COLUMNS.handler,
        new LogSelectFilter([], false, '250'),
      ),
      new LogHeader(
        Presenter.COLUMNS.participants,
        new LogSelectFilter([], true, '250', '100%'),
      ),
      new LogHeader(
        Presenter.COLUMNS.flags,
        new LogSelectFilter([], true, '250', '100%'),
      ),
      new LogHeader(Presenter.COLUMNS.status, new LogSelectFilter([])),
    ];
  }

  protected override async makeUiDataEntries(
    headers: LogHeader[],
  ): Promise<TransitionsEntry[]> {
    // TODO(b/339191691): Ideally we should refactor the parsers to
    // keep a map of time -> rowId, instead of relying on table order
    headers.forEach((header) => {
      if (!header.filter) return;
      this.uniqueFieldValues.set(header.spec, new Set());
    });
    const transitions = await this.makeTransitions(headers);
    this.sortTransitions(transitions);
    return transitions;
  }

  protected override updateFiltersInHeaders(headers: LogHeader[]) {
    headers.forEach((header) => {
      if (!(header.filter instanceof LogSelectFilter)) return;
      header.filter.options = this.getUniqueUiDataEntryValues(header.spec);
    });
  }

  private getUniqueUiDataEntryValues(spec: ColumnSpec): string[] {
    const result = [...assertDefined(this.uniqueFieldValues.get(spec))];

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

  private sortTransitions(transitions: TransitionsEntry[]) {
    const getId = (a: TransitionsEntry) =>
      assertDefined(
        a.fields.find((field) => field.spec === Presenter.COLUMNS.id),
      ).value;
    transitions.sort((a: TransitionsEntry, b: TransitionsEntry) => {
      return getId(a) <= getId(b) ? -1 : 1;
    });
  }

  private async makeTransitions(
    headers: LogHeader[],
  ): Promise<TransitionsEntry[]> {
    const transitions: TransitionsEntry[] = [];
    for (
      let traceIndex = 0;
      traceIndex < this.transitionTrace.lengthEntries;
      ++traceIndex
    ) {
      const entry = assertDefined(this.trace.getEntry(traceIndex));
      let transitionNode: PropertyTreeNode;
      try {
        transitionNode = await entry.getValue();
      } catch (e) {
        continue;
      }
      const wmDataNode = assertDefined(transitionNode.getChildByName('wmData'));
      const shellDataNode = assertDefined(
        transitionNode.getChildByName('shellData'),
      );
      this.updateTransitionChangesNames.apply(transitionNode);

      const transitionType = this.extractAndFormatTransitionType(wmDataNode);
      const handler = this.extractAndFormatHandler(shellDataNode);
      const participants = this.extractAndFormatParticipants(wmDataNode);
      const flags = this.extractAndFormatFlags(wmDataNode);
      const [status, statusIcon, statusIconColor] =
        this.extractAndFormatStatus(transitionNode);

      const fields: LogField[] = [
        {
          spec: Presenter.COLUMNS.id,
          value: assertDefined(transitionNode.getChildByName('id')).getValue(),
        },
        {spec: Presenter.COLUMNS.type, value: transitionType},
        {
          spec: Presenter.COLUMNS.sendTime,
          value:
            wmDataNode.getChildByName('sendTimeNs')?.getValue() ??
            Presenter.VALUE_NA,
        },
        {
          spec: Presenter.COLUMNS.dispatchTime,
          value:
            shellDataNode.getChildByName('dispatchTimeNs')?.getValue() ??
            Presenter.VALUE_NA,
          propagateEntryTimestamp: true,
        },
        {
          spec: Presenter.COLUMNS.duration,
          value:
            transitionNode.getChildByName('duration')?.formattedValue() ??
            Presenter.VALUE_NA,
        },
        {spec: Presenter.COLUMNS.handler, value: handler},
        {spec: Presenter.COLUMNS.participants, value: participants},
        {spec: Presenter.COLUMNS.flags, value: flags},
        {
          spec: Presenter.COLUMNS.status,
          value: status,
          icon: statusIcon,
          iconColor: statusIconColor,
        },
      ];
      transitions.push(new TransitionsEntry(entry, fields, transitionNode));
    }

    return transitions;
  }

  private extractAndFormatTransitionType(wmDataNode: PropertyTreeNode): string {
    const transitionType =
      wmDataNode.getChildByName('type')?.formattedValue() ?? 'NONE';
    assertDefined(this.uniqueFieldValues.get(Presenter.COLUMNS.type)).add(
      transitionType,
    );
    return transitionType;
  }

  private extractAndFormatHandler(shellDataNode: PropertyTreeNode): string {
    const handler =
      shellDataNode.getChildByName('handler')?.formattedValue() ??
      Presenter.VALUE_NA;
    assertDefined(this.uniqueFieldValues.get(Presenter.COLUMNS.handler)).add(
      handler,
    );
    return handler;
  }

  private extractAndFormatFlags(wmDataNode: PropertyTreeNode): string {
    const flags =
      wmDataNode.getChildByName('flags')?.formattedValue() ??
      Presenter.VALUE_NA;

    const uniqueFlags = assertDefined(
      this.uniqueFieldValues.get(Presenter.COLUMNS.flags),
    );
    flags
      .split('|')
      .map((flag) => flag.trim())
      .forEach((flag) => uniqueFlags.add(flag));

    return flags;
  }

  private extractAndFormatParticipants(wmDataNode: PropertyTreeNode): string {
    const targets = wmDataNode.getChildByName('targets')?.getAllChildren();
    if (!targets) return Presenter.VALUE_NA;

    const layers = new Set<string>();
    const windows = new Set<string>();
    targets.forEach((target) => {
      const layerId = target.getChildByName('layerId');
      if (layerId) layers.add(layerId.formattedValue());
      const windowId = target.getChildByName('windowId');
      if (windowId) windows.add(windowId.formattedValue());
    });
    const uniqueParticipants = assertDefined(
      this.uniqueFieldValues.get(Presenter.COLUMNS.participants),
    );
    layers.forEach((layer) => uniqueParticipants.add(layer));
    windows.forEach((window) => uniqueParticipants.add(window));

    return `Layers: ${
      layers.size > 0 ? [...layers].join(', ') : Presenter.VALUE_NA
    }\nWindows: ${
      windows.size > 0 ? [...windows].join(', ') : Presenter.VALUE_NA
    }`;
  }

  private extractAndFormatStatus(
    transitionNode: PropertyTreeNode,
  ): [string, string | undefined, string | undefined] {
    let status = Presenter.VALUE_NA;
    let statusIcon: string | undefined;
    let statusIconColor: string | undefined;
    if (assertDefined(transitionNode.getChildByName('merged')).getValue()) {
      status = TransitionStatus.MERGED;
      statusIcon = 'merge';
      statusIconColor = 'gray';
    } else if (
      assertDefined(transitionNode.getChildByName('aborted')).getValue()
    ) {
      status = TransitionStatus.ABORTED;
      statusIcon = 'close';
      statusIconColor = 'red';
    } else if (
      assertDefined(transitionNode.getChildByName('played')).getValue()
    ) {
      status = TransitionStatus.PLAYED;
      statusIcon = 'check';
      statusIconColor = 'green';
    }
    assertDefined(this.uniqueFieldValues.get(Presenter.COLUMNS.status)).add(
      status,
    );
    return [status, statusIcon, statusIconColor];
  }
}
