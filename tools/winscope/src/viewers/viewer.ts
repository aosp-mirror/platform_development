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
import {WinscopeEvent} from 'messaging/winscope_event';
import {
  EmitEvent,
  WinscopeEventEmitter,
} from 'messaging/winscope_event_emitter';
import {WinscopeEventListener} from 'messaging/winscope_event_listener';
import {Trace} from 'trace/trace';

enum ViewType {
  TRACE_TAB,
  OVERLAY,
  GLOBAL_SEARCH,
}

class View {
  constructor(
    public type: ViewType,
    public traces: Array<Trace<object>>,
    public htmlElement: HTMLElement,
    public title: string,
  ) {}
}

interface Viewer extends WinscopeEventListener, WinscopeEventEmitter {
  onWinscopeEvent(event: WinscopeEvent): Promise<void>;
  setEmitEvent(callback: EmitEvent): void;
  getViews(): View[];
  getTraces(): Array<Trace<object>>;
}

export {Viewer, View, ViewType};
