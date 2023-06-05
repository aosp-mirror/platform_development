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
import {Traces} from 'trace/traces';
import {TracePosition} from 'trace/trace_position';
import {TraceType} from 'trace/trace_type';
import {View, Viewer, ViewType} from 'viewers/viewer';
import {Events} from './events';
import {Presenter} from './presenter';
import {UiData} from './ui_data';

export class ViewerTransitions implements Viewer {
  constructor(traces: Traces) {
    this.htmlElement = document.createElement('viewer-transitions');

    this.presenter = new Presenter(traces, (data: UiData) => {
      (this.htmlElement as any).inputData = data;
    });

    this.htmlElement.addEventListener(Events.TransitionSelected, (event) => {
      this.presenter.onTransitionSelected((event as CustomEvent).detail);
    });
  }

  onTracePositionUpdate(position: TracePosition): void {
    this.presenter.onTracePositionUpdate(position);
  }

  getViews(): View[] {
    return [
      new View(
        ViewType.TAB,
        this.getDependencies(),
        this.htmlElement,
        'Transitions',
        TraceType.TRANSITION
      ),
    ];
  }

  getDependencies(): TraceType[] {
    return ViewerTransitions.DEPENDENCIES;
  }

  static readonly DEPENDENCIES: TraceType[] = [TraceType.TRANSITION];
  private htmlElement: HTMLElement;
  private presenter: Presenter;
}
