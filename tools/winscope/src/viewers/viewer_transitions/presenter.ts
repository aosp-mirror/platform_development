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
import {FunctionUtils} from 'common/function_utils';
import {Timestamp} from 'common/time';
import {
  TracePositionUpdate,
  WinscopeEvent,
  WinscopeEventType,
} from 'messaging/winscope_event';
import {
  EmitEvent,
  WinscopeEventEmitter,
} from 'messaging/winscope_event_emitter';
import {CustomQueryType} from 'trace/custom_query';
import {AbsoluteEntryIndex, Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceEntryFinder} from 'trace/trace_entry_finder';
import {TraceType} from 'trace/trace_type';
import {Transition} from 'trace/transition';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {Filter} from 'viewers/common/operations/filter';
import {UiPropertyTreeNode} from 'viewers/common/ui_property_tree_node';
import {UiTreeFormatter} from 'viewers/common/ui_tree_formatter';
import {UiTreeUtils} from 'viewers/common/ui_tree_utils';
import {UpdateTransitionChangesNames} from './operations/update_transition_changes_names';
import {UiData} from './ui_data';

export class Presenter implements WinscopeEventEmitter {
  private isInitialized = false;
  private transitionTrace: Trace<PropertyTreeNode>;
  private surfaceFlingerTrace: Trace<HierarchyTreeNode> | undefined;
  private windowManagerTrace: Trace<HierarchyTreeNode> | undefined;
  private layerIdToName = new Map<number, string>();
  private windowTokenToTitle = new Map<string, string>();
  private uiData = UiData.EMPTY;
  private readonly notifyUiDataCallback: (data: UiData) => void;
  private emitAppEvent: EmitEvent = FunctionUtils.DO_NOTHING_ASYNC;

  constructor(
    trace: Trace<PropertyTreeNode>,
    traces: Traces,
    notifyUiDataCallback: (data: UiData) => void,
  ) {
    this.transitionTrace = trace;
    this.surfaceFlingerTrace = traces.getTrace(TraceType.SURFACE_FLINGER);
    this.windowManagerTrace = traces.getTrace(TraceType.WINDOW_MANAGER);
    this.notifyUiDataCallback = notifyUiDataCallback;
  }

  setEmitEvent(callback: EmitEvent) {
    this.emitAppEvent = callback;
  }

  async onAppEvent(event: WinscopeEvent) {
    await event.visit(
      WinscopeEventType.TRACE_POSITION_UPDATE,
      async (event) => {
        await this.initializeIfNeeded();

        if (this.uiData === UiData.EMPTY) {
          this.uiData = await this.computeUiData();
        }

        const entry = TraceEntryFinder.findCorrespondingEntry(
          this.transitionTrace,
          event.position,
        );

        const transition = await entry?.getValue();
        if (transition !== undefined) {
          this.onTransitionSelected(transition);
        }

        this.notifyUiDataCallback(this.uiData);
      },
    );
  }

  onTransitionSelected(transition: PropertyTreeNode): void {
    this.uiData.selectedTransition = this.makeUiPropertiesTree(transition);
    this.notifyUiDataCallback(this.uiData);
  }

  async onRawTimestampClicked(timestamp: Timestamp) {
    await this.emitAppEvent(TracePositionUpdate.fromTimestamp(timestamp, true));
  }

  async onLogTimestampClicked(traceIndex: AbsoluteEntryIndex) {
    await this.emitAppEvent(
      TracePositionUpdate.fromTraceEntry(
        this.transitionTrace.getEntry(traceIndex),
        true,
      ),
    );
  }

  private async initializeIfNeeded() {
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

    this.isInitialized = true;
  }

  private async computeUiData(): Promise<UiData> {
    const entryPromises = this.transitionTrace.mapEntry((entry) => {
      return entry.getValue();
    });
    const entries = await Promise.all(entryPromises);
    // TODO(b/339191691): Ideally we should refactor the parsers to
    // keep a map of time -> rowId, instead of relying on table order
    const transitions = this.makeTransitions(entries);
    this.sortTransitions(transitions);
    const selectedTransition = this.uiData?.selectedTransition ?? undefined;

    return new UiData(transitions, selectedTransition);
  }

  private sortTransitions(transitions: Transition[]) {
    transitions.sort((a: Transition, b: Transition) => {
      return a.id <= b.id ? -1 : 1;
    });
  }

  private makeTransitions(entries: PropertyTreeNode[]): Transition[] {
    return entries.map((transitionNode, index) => {
      const wmDataNode = assertDefined(transitionNode.getChildByName('wmData'));
      const shellDataNode = assertDefined(
        transitionNode.getChildByName('shellData'),
      );

      const transition: Transition = {
        id: assertDefined(transitionNode.getChildByName('id')).getValue(),
        type: wmDataNode.getChildByName('type')?.formattedValue() ?? 'NONE',
        sendTime: wmDataNode.getChildByName('sendTimeNs'),
        dispatchTime: shellDataNode.getChildByName('dispatchTimeNs'),
        duration: transitionNode.getChildByName('duration')?.formattedValue(),
        merged: assertDefined(
          transitionNode.getChildByName('merged'),
        ).getValue(),
        aborted: assertDefined(
          transitionNode.getChildByName('aborted'),
        ).getValue(),
        played: assertDefined(
          transitionNode.getChildByName('played'),
        ).getValue(),
        propertiesTree: transitionNode,
        traceIndex: index,
      };
      return transition;
    });
  }

  private makeUiPropertiesTree(
    transitionNode: PropertyTreeNode,
  ): UiPropertyTreeNode {
    const tree = UiPropertyTreeNode.from(transitionNode);

    return new UiTreeFormatter<UiPropertyTreeNode>()
      .setUiTree(tree)
      .addOperation(new Filter([UiTreeUtils.isNotCalculated], false))
      .addOperation(
        new UpdateTransitionChangesNames(
          this.layerIdToName,
          this.windowTokenToTitle,
        ),
      )
      .format();
  }
}
