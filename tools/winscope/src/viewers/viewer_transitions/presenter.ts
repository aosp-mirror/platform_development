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
import {Store} from 'common/store/store';
import {Timestamp} from 'common/time/time';
import {CustomQueryType} from 'trace/custom_query';
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceType} from 'trace/trace_type';
import {TransitionStatus} from 'trace/transitions/status';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {
  AbstractLogViewerPresenter,
  NotifyLogViewCallbackType,
} from 'viewers/common/abstract_log_viewer_presenter';
import {LogSelectFilter} from 'viewers/common/log_filters';
import {LogPresenter} from 'viewers/common/log_presenter';
import {PropertiesPresenter} from 'viewers/common/properties_presenter';
import {TextFilter} from 'viewers/common/text_filter';
import {ColumnSpec, LogField, LogHeader} from 'viewers/common/ui_data_log';
import {UpdateTransitionParticipants} from './operations/update_transition_participants';
import {UpdateTransitionTargets} from './operations/update_transition_targets';
import {TransitionsEntry, UiData} from './ui_data';

export class Presenter extends AbstractLogViewerPresenter<
  UiData,
  HierarchyTreeNode
> {
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
  private transitionTrace: Trace<HierarchyTreeNode>;
  private surfaceFlingerTrace: Trace<HierarchyTreeNode> | undefined;
  private windowManagerTrace: Trace<HierarchyTreeNode> | undefined;
  private layerIdToName = new Map<number, string>();
  private windowTokenToTitle = new Map<string, string>();
  private updateTransitionParticipants = new UpdateTransitionParticipants(
    this.layerIdToName,
    this.windowTokenToTitle,
  );
  private updateTransitionTargets = new UpdateTransitionTargets(
    this.layerIdToName,
    this.windowTokenToTitle,
  );
  private uniqueFieldValues = new Map<ColumnSpec, Set<string>>();

  protected override keepCalculated = false;
  protected override logPresenter = new LogPresenter<TransitionsEntry>(false);
  protected override propertiesPresenter = new PropertiesPresenter(
    {},
    new TextFilter(),
    ['layers', 'windows'],
  );

  constructor(
    trace: Trace<HierarchyTreeNode>,
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

  protected override async updateFiltersInHeaders(headers: LogHeader[]) {
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
      let transitionNode: HierarchyTreeNode;
      try {
        transitionNode = await entry.getValue();
      } catch (e) {
        console.error(e);
        continue;
      }
      this.updateTransitionParticipants.apply(transitionNode);

      const transitionType =
        this.extractAndFormatTransitionType(transitionNode);
      const handler = this.extractAndFormatHandler(transitionNode);
      const participants = this.extractAndFormatParticipants(transitionNode);
      const flags = this.extractAndFormatFlags(transitionNode);
      const [status, statusIcon, statusIconColor] =
        this.extractAndFormatStatus(transitionNode);

      const sendTs: Timestamp | undefined = transitionNode
        .getEagerPropertyByName('sendTimeNs')
        ?.getValue();
      const dispatchTs: Timestamp | undefined = transitionNode
        .getEagerPropertyByName('dispatchTimeNs')
        ?.getValue();

      const fields: LogField[] = [
        {
          spec: Presenter.COLUMNS.id,
          value: assertDefined(
            transitionNode.getEagerPropertyByName('transitionId'),
          ).getValue(),
        },
        {spec: Presenter.COLUMNS.type, value: transitionType},
        {
          spec: Presenter.COLUMNS.sendTime,
          value: sendTs ?? Presenter.VALUE_NA,
          propagateEntryTimestamp:
            dispatchTs === undefined && sendTs !== undefined,
        },
        {
          spec: Presenter.COLUMNS.dispatchTime,
          value: dispatchTs ?? Presenter.VALUE_NA,
          propagateEntryTimestamp: dispatchTs !== undefined,
        },
        {
          spec: Presenter.COLUMNS.duration,
          value:
            transitionNode
              .getEagerPropertyByName('durationNs')
              ?.formattedValue() ?? Presenter.VALUE_NA,
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
      transitions.push(
        new TransitionsEntry(entry, fields, async () => {
          const properties = await transitionNode.getAllProperties();
          this.updateTransitionTargets.apply(properties);
          return properties;
        }),
      );
    }

    return transitions;
  }

  private extractAndFormatTransitionType(
    transition: HierarchyTreeNode,
  ): string {
    const transitionType =
      transition.getEagerPropertyByName('transitionType')?.formattedValue() ??
      'NONE';
    assertDefined(this.uniqueFieldValues.get(Presenter.COLUMNS.type)).add(
      transitionType,
    );
    return transitionType;
  }

  private extractAndFormatHandler(transition: HierarchyTreeNode): string {
    const handler =
      transition.getEagerPropertyByName('handler')?.formattedValue() ??
      Presenter.VALUE_NA;
    assertDefined(this.uniqueFieldValues.get(Presenter.COLUMNS.handler)).add(
      handler,
    );
    return handler;
  }

  private extractAndFormatFlags(transition: HierarchyTreeNode): string {
    const flags =
      transition.getEagerPropertyByName('flags')?.formattedValue() ??
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

  private extractAndFormatParticipants(transition: HierarchyTreeNode): string {
    const layers = transition
      .getEagerPropertyByName('layers')
      ?.getAllChildren()
      .map((layer) => {
        return layer.formattedValue();
      });
    const windows = transition
      .getEagerPropertyByName('windows')
      ?.getAllChildren()
      .map((window) => {
        return window.formattedValue();
      });
    if (!layers && !windows) return Presenter.VALUE_NA;

    const uniqueParticipants = assertDefined(
      this.uniqueFieldValues.get(Presenter.COLUMNS.participants),
    );
    layers?.forEach((layer) => uniqueParticipants.add(layer));
    windows?.forEach((window) => uniqueParticipants.add(window));

    return `Layers: ${
      (layers?.length ?? 0) > 0
        ? assertDefined(layers).join(', ')
        : Presenter.VALUE_NA
    }\nWindows: ${
      (windows?.length ?? 0) > 0
        ? assertDefined(windows).join(', ')
        : Presenter.VALUE_NA
    }`;
  }

  private extractAndFormatStatus(
    transitionNode: HierarchyTreeNode,
  ): [string, string | undefined, string | undefined] {
    const status =
      transitionNode.getEagerPropertyByName('status')?.formattedValue() ??
      Presenter.VALUE_NA;
    let statusIcon: string | undefined;
    let statusIconColor: string | undefined;
    if (status === TransitionStatus.MERGED) {
      statusIcon = 'merge';
      statusIconColor = 'gray';
    } else if (status === TransitionStatus.ABORTED) {
      statusIcon = 'close';
      statusIconColor = 'red';
    } else if (status === TransitionStatus.PLAYED) {
      statusIcon = 'check';
      statusIconColor = 'green';
    }
    assertDefined(this.uniqueFieldValues.get(Presenter.COLUMNS.status)).add(
      status,
    );
    return [status, statusIcon, statusIconColor];
  }
}
