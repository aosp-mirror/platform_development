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
import {TimeUtils} from 'common/time_utils';
import {LayerTraceEntry, Transition, WindowManagerState} from 'flickerlib/common';
import {WinscopeEvent, WinscopeEventType} from 'messaging/winscope_event';
import {CustomQueryType} from 'trace/custom_query';
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceEntryFinder} from 'trace/trace_entry_finder';
import {TraceType} from 'trace/trace_type';
import {PropertiesTreeNode} from 'viewers/common/ui_tree_utils';
import {UiData} from './ui_data';

export class Presenter {
  private isInitialized = false;
  private transitionTrace: Trace<object>;
  private surfaceFlingerTrace: Trace<LayerTraceEntry> | undefined;
  private windowManagerTrace: Trace<WindowManagerState> | undefined;
  private layerIdToName = new Map<number, string>();
  private windowTokenToTitle = new Map<string, string>();
  private uiData = UiData.EMPTY;
  private readonly notifyUiDataCallback: (data: UiData) => void;

  constructor(traces: Traces, notifyUiDataCallback: (data: UiData) => void) {
    this.transitionTrace = assertDefined(traces.getTrace(TraceType.TRANSITION));
    this.surfaceFlingerTrace = traces.getTrace(TraceType.SURFACE_FLINGER);
    this.windowManagerTrace = traces.getTrace(TraceType.WINDOW_MANAGER);
    this.notifyUiDataCallback = notifyUiDataCallback;
  }

  async onAppEvent(event: WinscopeEvent) {
    await event.visit(WinscopeEventType.TRACE_POSITION_UPDATE, async (event) => {
      await this.initializeIfNeeded();

      if (this.uiData === UiData.EMPTY) {
        this.uiData = await this.computeUiData();
      }

      const entry = TraceEntryFinder.findCorrespondingEntry(this.transitionTrace, event.position);

      this.uiData.selectedTransition = await entry?.getValue();
      if (this.uiData.selectedTransition !== undefined) {
        await this.onTransitionSelected(this.uiData.selectedTransition);
      }

      this.notifyUiDataCallback(this.uiData);
    });
  }

  async onTransitionSelected(transition: Transition): Promise<void> {
    this.uiData.selectedTransition = transition;
    this.uiData.selectedTransitionPropertiesTree = await this.makeSelectedTransitionPropertiesTree(
      transition
    );
    this.notifyUiDataCallback(this.uiData);
  }

  private async initializeIfNeeded() {
    if (this.isInitialized) {
      return;
    }

    if (this.surfaceFlingerTrace) {
      const layersIdAndName = await this.surfaceFlingerTrace.customQuery(
        CustomQueryType.SF_LAYERS_ID_AND_NAME
      );
      layersIdAndName.forEach((value) => {
        this.layerIdToName.set(value.id, value.name);
      });
    }

    if (this.windowManagerTrace) {
      const windowsTokenAndTitle = await this.windowManagerTrace.customQuery(
        CustomQueryType.WM_WINDOWS_TOKEN_AND_TITLE
      );
      windowsTokenAndTitle.forEach((value) => {
        this.windowTokenToTitle.set(value.token, value.title);
      });
    }

    this.isInitialized = true;
  }

  private async computeUiData(): Promise<UiData> {
    const entryPromises = this.transitionTrace.mapEntry((entry, originalIndex) => {
      return entry.getValue();
    });
    const transitions = await Promise.all(entryPromises);

    const selectedTransition = this.uiData?.selectedTransition ?? undefined;
    const selectedTransitionPropertiesTree =
      this.uiData?.selectedTransitionPropertiesTree ?? undefined;

    const timestampType = this.transitionTrace.getTimestampType();
    if (timestampType === undefined) {
      throw new Error('Missing timestamp type in trace!');
    }
    return new UiData(
      transitions,
      selectedTransition,
      timestampType,
      selectedTransitionPropertiesTree
    );
  }

  private async makeSelectedTransitionPropertiesTree(
    transition: Transition
  ): Promise<PropertiesTreeNode> {
    const changes: PropertiesTreeNode[] = [];

    for (const change of transition.changes) {
      const layerName = this.layerIdToName.get(change.layerId);
      const windowTitle = this.windowTokenToTitle.get(change.windowId.toString(16));

      const layerIdValue = layerName ? `${change.layerId} (${layerName})` : change.layerId;
      const windowIdValue = windowTitle
        ? `0x${change.windowId.toString(16)} (${windowTitle})`
        : `0x${change.windowId.toString(16)}`;

      changes.push({
        propertyKey: 'change',
        children: [
          {propertyKey: 'transitMode', propertyValue: change.transitMode},
          {propertyKey: 'layerId', propertyValue: layerIdValue},
          {propertyKey: 'windowId', propertyValue: windowIdValue},
        ],
      });
    }

    const properties: PropertiesTreeNode[] = [
      {propertyKey: 'id', propertyValue: transition.id},
      {propertyKey: 'type', propertyValue: transition.type},
      {propertyKey: 'aborted', propertyValue: `${transition.aborted}`},
    ];

    if (transition.handler) {
      properties.push({propertyKey: 'handler', propertyValue: transition.handler});
    }

    const timestampType = this.transitionTrace.getTimestampType();

    if (!transition.createTime.isMin) {
      properties.push({
        propertyKey: 'createTime',
        propertyValue: TimeUtils.formattedKotlinTimestamp(transition.createTime, timestampType),
      });
    }

    if (!transition.sendTime.isMin) {
      properties.push({
        propertyKey: 'sendTime',
        propertyValue: TimeUtils.formattedKotlinTimestamp(transition.sendTime, timestampType),
      });
    }

    if (!transition.dispatchTime.isMin) {
      properties.push({
        propertyKey: 'dispatchTime',
        propertyValue: TimeUtils.formattedKotlinTimestamp(transition.dispatchTime, timestampType),
      });
    }

    if (!transition.finishTime.isMax) {
      properties.push({
        propertyKey: 'finishTime',
        propertyValue: TimeUtils.formattedKotlinTimestamp(transition.finishTime, timestampType),
      });
    }

    if (transition.mergeRequestTime) {
      properties.push({
        propertyKey: 'mergeRequestTime',
        propertyValue: TimeUtils.formattedKotlinTimestamp(
          transition.mergeRequestTime,
          timestampType
        ),
      });
    }

    if (transition.shellAbortTime) {
      properties.push({
        propertyKey: 'shellAbortTime',
        propertyValue: TimeUtils.formattedKotlinTimestamp(transition.shellAbortTime, timestampType),
      });
    }

    if (transition.mergeTime) {
      properties.push({
        propertyKey: 'mergeTime',
        propertyValue: TimeUtils.formattedKotlinTimestamp(transition.mergeTime, timestampType),
      });
    }

    if (transition.mergeTarget) {
      properties.push({
        propertyKey: 'mergeTarget',
        propertyValue: transition.mergeTarget,
      });
    }

    if (transition.startTransactionId !== -1) {
      properties.push({
        propertyKey: 'startTransactionId',
        propertyValue: transition.startTransactionId,
      });
    }
    if (transition.finishTransactionId !== -1) {
      properties.push({
        propertyKey: 'finishTransactionId',
        propertyValue: transition.finishTransactionId,
      });
    }
    if (changes.length > 0) {
      properties.push({propertyKey: 'changes', children: changes});
    }

    return {
      children: properties,
      propertyKey: 'Selected Transition',
    };
  }
}
