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

import {Timestamp} from 'trace/timestamp';
import {TraceEntry} from 'trace/trace';
import {TracePosition} from 'trace/trace_position';
import {TraceType} from 'trace/trace_type';
import {View} from 'viewers/viewer';

export enum AppEventType {
  TABBED_VIEW_SWITCHED = 'TABBED_VIEW_SWITCHED',
  TABBED_VIEW_SWITCH_REQUEST = 'TABBED_VIEW_SWITCH_REQUEST',
  TRACE_POSITION_UPDATE = 'TRACE_POSITION_UPDATE',
}

interface TypeMap {
  [AppEventType.TABBED_VIEW_SWITCHED]: TabbedViewSwitched;
  [AppEventType.TABBED_VIEW_SWITCH_REQUEST]: TabbedViewSwitchRequest;
  [AppEventType.TRACE_POSITION_UPDATE]: TracePositionUpdate;
}

export abstract class AppEvent {
  abstract readonly type: AppEventType;

  async visit<T extends AppEventType>(type: T, callback: (event: TypeMap[T]) => Promise<void>) {
    if (this.type === type) {
      const event = this as unknown as TypeMap[T];
      await callback(event);
    }
  }
}

export class TabbedViewSwitched extends AppEvent {
  override readonly type = AppEventType.TABBED_VIEW_SWITCHED;
  readonly newFocusedView: View;

  constructor(view: View) {
    super();
    this.newFocusedView = view;
  }
}

export class TabbedViewSwitchRequest extends AppEvent {
  override readonly type = AppEventType.TABBED_VIEW_SWITCH_REQUEST;

  //TODO(b/263779536): use proper view/viewer ID, instead of abusing trace type.
  readonly newFocusedViewId: TraceType;

  constructor(newFocusedViewId: TraceType) {
    super();
    this.newFocusedViewId = newFocusedViewId;
  }
}

export class TracePositionUpdate extends AppEvent {
  override readonly type = AppEventType.TRACE_POSITION_UPDATE;
  readonly position: TracePosition;

  constructor(position: TracePosition) {
    super();
    this.position = position;
  }

  static fromTimestamp(timestamp: Timestamp): TracePositionUpdate {
    const position = TracePosition.fromTimestamp(timestamp);
    return new TracePositionUpdate(position);
  }

  static fromTraceEntry(entry: TraceEntry<object>): TracePositionUpdate {
    const position = TracePosition.fromTraceEntry(entry);
    return new TracePositionUpdate(position);
  }
}
