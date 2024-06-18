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

import {assertTrue} from 'common/assert_utils';
import {Timestamp} from 'common/time';
import {Trace, TraceEntry} from 'trace/trace';
import {TracePosition} from 'trace/trace_position';
import {View, Viewer, ViewType} from 'viewers/viewer';

export enum WinscopeEventType {
  APP_INITIALIZED,
  APP_FILES_COLLECTED,
  APP_FILES_UPLOADED,
  APP_RESET_REQUEST,
  APP_TRACE_VIEW_REQUEST,
  APP_REFRESH_DUMPS_REQUEST,
  REMOTE_TOOL_DOWNLOAD_START,
  REMOTE_TOOL_FILES_RECEIVED,
  REMOTE_TOOL_TIMESTAMP_RECEIVED,
  TABBED_VIEW_SWITCHED,
  TABBED_VIEW_SWITCH_REQUEST,
  TRACE_POSITION_UPDATE,
  VIEWERS_LOADED,
  VIEWERS_UNLOADED,
  EXPANDED_TIMELINE_TOGGLED,
  ACTIVE_TRACE_CHANGED,
  DARK_MODE_TOGGLED,
}

interface TypeMap {
  [WinscopeEventType.APP_INITIALIZED]: AppInitialized;
  [WinscopeEventType.APP_FILES_COLLECTED]: AppFilesCollected;
  [WinscopeEventType.APP_FILES_UPLOADED]: AppFilesUploaded;
  [WinscopeEventType.APP_RESET_REQUEST]: AppResetRequest;
  [WinscopeEventType.APP_TRACE_VIEW_REQUEST]: AppTraceViewRequest;
  [WinscopeEventType.APP_REFRESH_DUMPS_REQUEST]: AppRefreshDumpsRequest;
  [WinscopeEventType.REMOTE_TOOL_DOWNLOAD_START]: RemoteToolDownloadStart;
  [WinscopeEventType.REMOTE_TOOL_FILES_RECEIVED]: RemoteToolFilesReceived;
  [WinscopeEventType.REMOTE_TOOL_TIMESTAMP_RECEIVED]: RemoteToolTimestampReceived;
  [WinscopeEventType.TABBED_VIEW_SWITCHED]: TabbedViewSwitched;
  [WinscopeEventType.TABBED_VIEW_SWITCH_REQUEST]: TabbedViewSwitchRequest;
  [WinscopeEventType.TRACE_POSITION_UPDATE]: TracePositionUpdate;
  [WinscopeEventType.VIEWERS_LOADED]: ViewersLoaded;
  [WinscopeEventType.VIEWERS_UNLOADED]: ViewersUnloaded;
  [WinscopeEventType.EXPANDED_TIMELINE_TOGGLED]: ExpandedTimelineToggled;
  [WinscopeEventType.ACTIVE_TRACE_CHANGED]: ActiveTraceChanged;
  [WinscopeEventType.DARK_MODE_TOGGLED]: DarkModeToggled;
}

export abstract class WinscopeEvent {
  abstract readonly type: WinscopeEventType;

  async visit<T extends WinscopeEventType>(
    type: T,
    callback: (event: TypeMap[T]) => Promise<void>,
  ) {
    if (this.type === type) {
      const event = this as unknown as TypeMap[T];
      await callback(event);
    }
  }
}

export class AppInitialized extends WinscopeEvent {
  override readonly type = WinscopeEventType.APP_INITIALIZED;
}

export class AppFilesCollected extends WinscopeEvent {
  override readonly type = WinscopeEventType.APP_FILES_COLLECTED;

  constructor(readonly files: File[]) {
    super();
  }
}

export class AppFilesUploaded extends WinscopeEvent {
  override readonly type = WinscopeEventType.APP_FILES_UPLOADED;

  constructor(readonly files: File[]) {
    super();
  }
}

export class AppResetRequest extends WinscopeEvent {
  override readonly type = WinscopeEventType.APP_RESET_REQUEST;
}

export class AppTraceViewRequest extends WinscopeEvent {
  override readonly type = WinscopeEventType.APP_TRACE_VIEW_REQUEST;
}

export class AppRefreshDumpsRequest extends WinscopeEvent {
  override readonly type = WinscopeEventType.APP_REFRESH_DUMPS_REQUEST;
}

export class RemoteToolDownloadStart extends WinscopeEvent {
  override readonly type = WinscopeEventType.REMOTE_TOOL_DOWNLOAD_START;
}

export class RemoteToolFilesReceived extends WinscopeEvent {
  override readonly type = WinscopeEventType.REMOTE_TOOL_FILES_RECEIVED;

  constructor(
    readonly files: File[],
    readonly deferredTimestamp?: () => Timestamp | undefined,
  ) {
    super();
  }
}

export class RemoteToolTimestampReceived extends WinscopeEvent {
  override readonly type = WinscopeEventType.REMOTE_TOOL_TIMESTAMP_RECEIVED;

  constructor(readonly deferredTimestamp: () => Timestamp | undefined) {
    super();
  }
}

export class TabbedViewSwitched extends WinscopeEvent {
  override readonly type = WinscopeEventType.TABBED_VIEW_SWITCHED;
  readonly newFocusedView: View;

  constructor(view: View) {
    super();
    assertTrue(view.type === ViewType.TAB);
    this.newFocusedView = view;
  }
}

export class TabbedViewSwitchRequest extends WinscopeEvent {
  override readonly type = WinscopeEventType.TABBED_VIEW_SWITCH_REQUEST;

  readonly newActiveTrace: Trace<object>;

  constructor(newActiveTrace: Trace<object>) {
    super();
    this.newActiveTrace = newActiveTrace;
  }
}

export class TracePositionUpdate extends WinscopeEvent {
  override readonly type = WinscopeEventType.TRACE_POSITION_UPDATE;
  readonly position: TracePosition;
  readonly updateTimeline: boolean;

  constructor(position: TracePosition, updateTimeline = false) {
    super();
    this.position = position;
    this.updateTimeline = updateTimeline;
  }

  static fromTimestamp(
    timestamp: Timestamp,
    updateTimeline = false,
  ): TracePositionUpdate {
    const position = TracePosition.fromTimestamp(timestamp);
    return new TracePositionUpdate(position, updateTimeline);
  }

  static fromTraceEntry(
    entry: TraceEntry<object>,
    updateTimeline = false,
  ): TracePositionUpdate {
    const position = TracePosition.fromTraceEntry(entry);
    return new TracePositionUpdate(position, updateTimeline);
  }
}

export class ViewersLoaded extends WinscopeEvent {
  override readonly type = WinscopeEventType.VIEWERS_LOADED;

  constructor(readonly viewers: Viewer[]) {
    super();
  }
}

export class ViewersUnloaded extends WinscopeEvent {
  override readonly type = WinscopeEventType.VIEWERS_UNLOADED;
}

export class ExpandedTimelineToggled extends WinscopeEvent {
  override readonly type = WinscopeEventType.EXPANDED_TIMELINE_TOGGLED;
  constructor(readonly isTimelineExpanded: boolean) {
    super();
  }
}

export class ActiveTraceChanged extends WinscopeEvent {
  override readonly type = WinscopeEventType.ACTIVE_TRACE_CHANGED;
  constructor(readonly trace: Trace<object>) {
    super();
  }
}

export class DarkModeToggled extends WinscopeEvent {
  override readonly type = WinscopeEventType.DARK_MODE_TOGGLED;
  constructor(readonly isDarkMode: boolean) {
    super();
  }
}
