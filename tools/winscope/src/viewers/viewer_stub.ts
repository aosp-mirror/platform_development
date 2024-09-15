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
import {WinscopeEvent} from 'messaging/winscope_event';
import {EmitEvent} from 'messaging/winscope_event_emitter';
import {TraceBuilder} from 'test/unit/trace_builder';
import {Trace} from 'trace/trace';
import {TraceType} from 'trace/trace_type';
import {View, Viewer, ViewType} from './viewer';

class ViewerStub implements Viewer {
  private readonly trace: Trace<object>;
  private htmlElement: HTMLElement;
  private title: string;
  private view: View;
  private emitAppEvent: EmitEvent = FunctionUtils.DO_NOTHING_ASYNC;

  constructor(
    title: string,
    viewContent?: string,
    trace?: Trace<object>,
    viewType?: ViewType,
  ) {
    this.title = title;

    if (viewContent !== undefined) {
      this.htmlElement = document.createElement('div');
      this.htmlElement.innerText = viewContent;
    } else {
      this.htmlElement = undefined as unknown as HTMLElement;
    }

    this.trace =
      trace ??
      new TraceBuilder<object>().setType(TraceType.WINDOW_MANAGER).build();

    this.view = new View(
      viewType ?? ViewType.TAB,
      this.getTraces(),
      this.htmlElement,
      this.title,
    );
  }

  onWinscopeEvent(event: WinscopeEvent): Promise<void> {
    return Promise.resolve();
  }

  setEmitEvent(callback: EmitEvent) {
    this.emitAppEvent = callback;
  }

  async emitAppEventForTesting(event: WinscopeEvent) {
    await this.emitAppEvent(event);
  }

  getViews(): View[] {
    return [this.view];
  }

  getTraces(): Array<Trace<object>> {
    return [this.trace];
  }
}

export {ViewerStub};
