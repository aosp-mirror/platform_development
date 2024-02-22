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

import {FunctionUtils} from 'common/function_utils';
import {Timestamp} from 'common/time';
import {TracePositionUpdate, WinscopeEvent} from 'messaging/winscope_event';
import {EmitEvent} from 'messaging/winscope_event_emitter';
import {Traces} from 'trace/traces';
import {TraceType} from 'trace/trace_type';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {ViewerEvents} from 'viewers/common/viewer_events';
import {View, Viewer, ViewType} from 'viewers/viewer';
import {Events} from './events';
import {Presenter} from './presenter';
import {UiData} from './ui_data';

class ViewerProtoLog implements Viewer {
  static readonly DEPENDENCIES: TraceType[] = [TraceType.PROTO_LOG];

  private readonly htmlElement: HTMLElement;
  private readonly presenter: Presenter;
  private readonly view: View;
  private emitAppEvent: EmitEvent = FunctionUtils.DO_NOTHING_ASYNC;

  constructor(traces: Traces) {
    this.htmlElement = document.createElement('viewer-protolog');

    this.presenter = new Presenter(traces, (data: UiData) => {
      (this.htmlElement as any).inputData = data;
    });

    this.htmlElement.addEventListener(
      Events.LogLevelsFilterChanged,
      (event) => {
        this.presenter.onLogLevelsFilterChanged((event as CustomEvent).detail);
      },
    );
    this.htmlElement.addEventListener(Events.TagsFilterChanged, (event) => {
      this.presenter.onTagsFilterChanged((event as CustomEvent).detail);
    });
    this.htmlElement.addEventListener(
      Events.SourceFilesFilterChanged,
      (event) => {
        this.presenter.onSourceFilesFilterChanged(
          (event as CustomEvent).detail,
        );
      },
    );
    this.htmlElement.addEventListener(
      Events.SearchStringFilterChanged,
      (event) => {
        this.presenter.onSearchStringFilterChanged(
          (event as CustomEvent).detail,
        );
      },
    );
    this.htmlElement.addEventListener(Events.MessageClicked, (event) => {
      this.presenter.onMessageClicked((event as CustomEvent).detail);
    });
    this.htmlElement.addEventListener(ViewerEvents.TimestampClick, (event) => {
      this.propagateTimestamp((event as CustomEvent).detail);
    });

    this.view = new View(
      ViewType.TAB,
      this.getDependencies(),
      this.htmlElement,
      'ProtoLog',
      TraceType.PROTO_LOG,
    );
  }

  async onWinscopeEvent(event: WinscopeEvent) {
    await this.presenter.onAppEvent(event);
  }

  setEmitEvent(callback: EmitEvent) {
    this.emitAppEvent = callback;
  }

  async propagateTimestamp(timestampNode: PropertyTreeNode) {
    const timestamp: Timestamp = timestampNode.getValue();
    await this.emitAppEvent(TracePositionUpdate.fromTimestamp(timestamp, true));
  }

  getViews(): View[] {
    return [this.view];
  }

  getDependencies(): TraceType[] {
    return ViewerProtoLog.DEPENDENCIES;
  }
}

export {ViewerProtoLog};
