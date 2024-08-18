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
import {LogPresenter} from 'viewers/common/log_presenter';
import {PropertiesPresenter} from 'viewers/common/properties_presenter';
import {LogField, LogFieldType} from 'viewers/common/ui_data_log';
import {UpdateTransitionChangesNames} from './operations/update_transition_changes_names';
import {TransitionsEntry, TransitionStatus, UiData} from './ui_data';

export class Presenter extends AbstractLogViewerPresenter<UiData> {
  static readonly FIELD_TYPES = [
    LogFieldType.TRANSITION_ID,
    LogFieldType.TRANSITION_TYPE,
    LogFieldType.SEND_TIME,
    LogFieldType.DISPATCH_TIME,
    LogFieldType.DURATION,
    LogFieldType.STATUS,
  ];
  private static readonly VALUE_NA = 'N/A';

  private isInitialized = false;
  private transitionTrace: Trace<PropertyTreeNode>;
  private surfaceFlingerTrace: Trace<HierarchyTreeNode> | undefined;
  private windowManagerTrace: Trace<HierarchyTreeNode> | undefined;
  private layerIdToName = new Map<number, string>();
  private windowTokenToTitle = new Map<string, string>();

  protected override keepCalculated = false;
  protected override logPresenter = new LogPresenter<TransitionsEntry>(
    false,
    false,
  );
  protected override propertiesPresenter = new PropertiesPresenter(
    {},
    [],
    [
      new UpdateTransitionChangesNames(
        this.layerIdToName,
        this.windowTokenToTitle,
      ),
    ],
  );

  constructor(
    trace: Trace<PropertyTreeNode>,
    traces: Traces,
    notifyViewCallback: NotifyLogViewCallbackType<UiData>,
  ) {
    super(trace, notifyViewCallback, UiData.createEmpty());
    this.transitionTrace = trace;
    this.surfaceFlingerTrace = traces.getTrace(TraceType.SURFACE_FLINGER);
    this.windowManagerTrace = traces.getTrace(TraceType.WINDOW_MANAGER);
  }

  protected async initializeIfNeeded() {
    if (this.isInitialized) {
      return;
    }

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

    const allEntries = await this.makeUiDataEntries();

    this.logPresenter.setAllEntries(allEntries);
    this.logPresenter.setHeaders(Presenter.FIELD_TYPES);
    this.refreshUiData();
    this.isInitialized = true;
  }

  private async makeUiDataEntries(): Promise<TransitionsEntry[]> {
    // TODO(b/339191691): Ideally we should refactor the parsers to
    // keep a map of time -> rowId, instead of relying on table order
    const transitions = await this.makeTransitions();
    this.sortTransitions(transitions);
    return transitions;
  }

  private sortTransitions(transitions: TransitionsEntry[]) {
    const getId = (a: TransitionsEntry) =>
      assertDefined(a.fields.find((f) => f.type === LogFieldType.TRANSITION_ID))
        .value;
    transitions.sort((a: TransitionsEntry, b: TransitionsEntry) => {
      return getId(a) <= getId(b) ? -1 : 1;
    });
  }

  private async makeTransitions(): Promise<TransitionsEntry[]> {
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

      let status: TransitionStatus | undefined;
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

      const fields: LogField[] = [
        {
          type: LogFieldType.TRANSITION_ID,
          value: assertDefined(transitionNode.getChildByName('id')).getValue(),
        },
        {
          type: LogFieldType.TRANSITION_TYPE,
          value: wmDataNode.getChildByName('type')?.formattedValue() ?? 'NONE',
        },
        {
          type: LogFieldType.SEND_TIME,
          value:
            wmDataNode.getChildByName('sendTimeNs')?.getValue() ??
            Presenter.VALUE_NA,
        },
        {
          type: LogFieldType.DISPATCH_TIME,
          value:
            shellDataNode.getChildByName('dispatchTimeNs')?.getValue() ??
            Presenter.VALUE_NA,
        },
        {
          type: LogFieldType.DURATION,
          value:
            transitionNode.getChildByName('duration')?.formattedValue() ??
            Presenter.VALUE_NA,
        },
        {
          type: LogFieldType.STATUS,
          value: status ?? Presenter.VALUE_NA,
          icon: statusIcon,
          iconColor: statusIconColor,
        },
      ];
      transitions.push(new TransitionsEntry(entry, fields, transitionNode));
    }

    return transitions;
  }
}
