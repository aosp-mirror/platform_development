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
import {WinscopeEvent} from 'messaging/winscope_event';
import {EmitEvent} from 'messaging/winscope_event_emitter';
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceType} from 'trace/trace_type';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {TimestampClickDetail, ViewerEvents} from 'viewers/common/viewer_events';
import {View, Viewer, ViewType} from 'viewers/viewer';
import {Presenter} from './presenter';
import {UiData} from './ui_data';

class ViewerTransactions implements Viewer {
  static readonly DEPENDENCIES: TraceType[] = [TraceType.TRANSACTIONS];

  private readonly trace: Trace<PropertyTreeNode>;
  private readonly htmlElement: HTMLElement;
  private readonly presenter: Presenter;
  private readonly view: View;

  constructor(
    trace: Trace<PropertyTreeNode>,
    traces: Traces,
    storage: Storage,
  ) {
    this.trace = trace;
    this.htmlElement = document.createElement('viewer-transactions');

    this.presenter = new Presenter(trace, storage, (data: UiData) => {
      (this.htmlElement as any).inputData = data;
    });

    this.htmlElement.addEventListener(
      ViewerEvents.VSyncIdFilterChanged,
      (event) => {
        this.presenter.onVSyncIdFilterChanged((event as CustomEvent).detail);
      },
    );

    this.htmlElement.addEventListener(
      ViewerEvents.PidFilterChanged,
      (event) => {
        this.presenter.onPidFilterChanged((event as CustomEvent).detail);
      },
    );

    this.htmlElement.addEventListener(
      ViewerEvents.UidFilterChanged,
      (event) => {
        this.presenter.onUidFilterChanged((event as CustomEvent).detail);
      },
    );

    this.htmlElement.addEventListener(
      ViewerEvents.TypeFilterChanged,
      (event) => {
        this.presenter.onTypeFilterChanged((event as CustomEvent).detail);
      },
    );

    this.htmlElement.addEventListener(
      ViewerEvents.LayerIdFilterChanged,
      (event) => {
        this.presenter.onLayerIdFilterChanged((event as CustomEvent).detail);
      },
    );

    this.htmlElement.addEventListener(
      ViewerEvents.WhatFilterChanged,
      (event) => {
        this.presenter.onWhatFilterChanged((event as CustomEvent).detail);
      },
    );

    this.htmlElement.addEventListener(
      ViewerEvents.TransactionIdFilterChanged,
      (event) => {
        this.presenter.onTransactionIdFilterChanged(
          (event as CustomEvent).detail,
        );
      },
    );

    this.htmlElement.addEventListener(ViewerEvents.LogClicked, (event) => {
      this.presenter.onEntryClicked((event as CustomEvent).detail);
    });
    this.htmlElement.addEventListener(
      ViewerEvents.LogChangedByKeyPress,
      (event) => {
        this.presenter.onEntryChangedByKeyPress((event as CustomEvent).detail);
      },
    );
    this.htmlElement.addEventListener(
      ViewerEvents.TimestampClick,
      async (event) => {
        const detail: TimestampClickDetail = (event as CustomEvent).detail;
        await this.presenter.onLogTimestampClicked(assertDefined(detail.index));
      },
    );

    this.htmlElement.addEventListener(
      ViewerEvents.PropertiesUserOptionsChange,
      (event) =>
        this.presenter.onPropertiesUserOptionsChange(
          (event as CustomEvent).detail.userOptions,
        ),
    );

    this.view = new View(
      ViewType.TAB,
      this.getTraces(),
      this.htmlElement,
      'Transactions',
    );
  }

  async onWinscopeEvent(event: WinscopeEvent) {
    await this.presenter.onAppEvent(event);
  }

  setEmitEvent(callback: EmitEvent) {
    this.presenter.setEmitEvent(callback);
  }

  getViews(): View[] {
    return [this.view];
  }

  getTraces(): Array<Trace<PropertyTreeNode>> {
    return [this.trace];
  }
}

export {ViewerTransactions};
