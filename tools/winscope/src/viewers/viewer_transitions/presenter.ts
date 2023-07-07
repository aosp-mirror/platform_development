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
import {LayerTraceEntry, Transition, WindowManagerState} from 'trace/flickerlib/common';
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceEntryFinder} from 'trace/trace_entry_finder';
import {TracePosition} from 'trace/trace_position';
import {TraceType} from 'trace/trace_type';
import {PropertiesTreeNode} from 'viewers/common/ui_tree_utils';
import {UiData} from './ui_data';

export class Presenter {
  constructor(traces: Traces, notifyUiDataCallback: (data: UiData) => void) {
    this.transitionTrace = assertDefined(traces.getTrace(TraceType.TRANSITION));
    this.surfaceFlingerTrace = traces.getTrace(TraceType.SURFACE_FLINGER);
    this.windowManagerTrace = traces.getTrace(TraceType.WINDOW_MANAGER);
    this.notifyUiDataCallback = notifyUiDataCallback;
    this.uiData = this.computeUiData();
    this.notifyUiDataCallback(this.uiData);
  }

  onTracePositionUpdate(position: TracePosition): void {
    const entry = TraceEntryFinder.findCorrespondingEntry(this.transitionTrace, position);

    this.uiData.selectedTransition = entry?.getValue();

    this.notifyUiDataCallback(this.uiData);
  }

  onTransitionSelected(transition: Transition): void {
    this.uiData.selectedTransition = transition;
    this.uiData.selectedTransitionPropertiesTree =
      this.makeSelectedTransitionPropertiesTree(transition);
    this.notifyUiDataCallback(this.uiData);
  }

  private computeUiData(): UiData {
    const transitions: Transition[] = [];

    this.transitionTrace.forEachEntry((entry, originalIndex) => {
      transitions.push(entry.getValue());
    });

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

  private makeSelectedTransitionPropertiesTree(transition: Transition): PropertiesTreeNode {
    const changes: PropertiesTreeNode[] = [];

    for (const change of transition.changes) {
      let layerName: string | undefined = undefined;
      let windowName: string | undefined = undefined;

      if (this.surfaceFlingerTrace) {
        this.surfaceFlingerTrace.forEachEntry((entry, originalIndex) => {
          if (layerName !== undefined) {
            return;
          }
          const layerTraceEntry = entry.getValue() as LayerTraceEntry;
          for (const layer of layerTraceEntry.flattenedLayers) {
            if (layer.id === change.layerId) {
              layerName = layer.name;
            }
          }
        });
      }

      if (this.windowManagerTrace) {
        this.windowManagerTrace.forEachEntry((entry, originalIndex) => {
          if (windowName !== undefined) {
            return;
          }
          const wmState = entry.getValue() as WindowManagerState;
          for (const window of wmState.windowContainers) {
            if (window.token.toLowerCase() === change.windowId.toString(16).toLowerCase()) {
              windowName = window.title;
            }
          }
        });
      }

      const layerIdValue = layerName ? `${change.layerId} (${layerName})` : change.layerId;
      const windowIdValue = windowName
        ? `0x${change.windowId.toString(16)} (${windowName})`
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

    if (transition.mergedInto) {
      properties.push({propertyKey: 'mergedInto', propertyValue: transition.mergedInto});
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

  private transitionTrace: Trace<object>;
  private surfaceFlingerTrace: Trace<object> | undefined;
  private windowManagerTrace: Trace<object> | undefined;
  private uiData: UiData;
  private readonly notifyUiDataCallback: (data: UiData) => void;
}
