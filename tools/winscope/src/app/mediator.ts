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

import {Timestamp} from 'common/time';
import {ProgressListener} from 'messaging/progress_listener';
import {UserNotificationListener} from 'messaging/user_notification_listener';
import {WinscopeError} from 'messaging/winscope_error';
import {WinscopeErrorListener} from 'messaging/winscope_error_listener';
import {
  TracePositionUpdate,
  ViewersLoaded,
  ViewersUnloaded,
  WinscopeEvent,
  WinscopeEventType,
} from 'messaging/winscope_event';
import {WinscopeEventEmitter} from 'messaging/winscope_event_emitter';
import {WinscopeEventListener} from 'messaging/winscope_event_listener';
import {TraceEntry} from 'trace/trace';
import {TracePosition} from 'trace/trace_position';
import {View, Viewer, ViewType} from 'viewers/viewer';
import {ViewerFactory} from 'viewers/viewer_factory';
import {FilesSource} from './files_source';
import {TimelineData} from './timeline_data';
import {TracePipeline} from './trace_pipeline';

export class Mediator {
  private abtChromeExtensionProtocol: WinscopeEventEmitter &
    WinscopeEventListener;
  private crossToolProtocol: WinscopeEventEmitter & WinscopeEventListener;
  private uploadTracesComponent?: ProgressListener;
  private collectTracesComponent?: ProgressListener;
  private traceViewComponent?: WinscopeEventEmitter & WinscopeEventListener;
  private timelineComponent?: WinscopeEventEmitter & WinscopeEventListener;
  private appComponent: WinscopeEventListener;
  private userNotificationListener: UserNotificationListener;
  private storage: Storage;

  private tracePipeline: TracePipeline;
  private timelineData: TimelineData;
  private viewers: Viewer[] = [];
  private focusedTabView: undefined | View;
  private areViewersLoaded = false;
  private lastRemoteToolTimestampReceived: Timestamp | undefined;
  private currentProgressListener?: ProgressListener;

  constructor(
    tracePipeline: TracePipeline,
    timelineData: TimelineData,
    abtChromeExtensionProtocol: WinscopeEventEmitter & WinscopeEventListener,
    crossToolProtocol: WinscopeEventEmitter & WinscopeEventListener,
    appComponent: WinscopeEventListener,
    userNotificationListener: UserNotificationListener,
    storage: Storage,
  ) {
    this.tracePipeline = tracePipeline;
    this.timelineData = timelineData;
    this.abtChromeExtensionProtocol = abtChromeExtensionProtocol;
    this.crossToolProtocol = crossToolProtocol;
    this.appComponent = appComponent;
    this.userNotificationListener = userNotificationListener;
    this.storage = storage;

    this.crossToolProtocol.setEmitEvent(async (event) => {
      await this.onWinscopeEvent(event);
    });

    this.abtChromeExtensionProtocol.setEmitEvent(async (event) => {
      await this.onWinscopeEvent(event);
    });
  }

  setUploadTracesComponent(component: ProgressListener | undefined) {
    this.uploadTracesComponent = component;
  }

  setCollectTracesComponent(component: ProgressListener | undefined) {
    this.collectTracesComponent = component;
  }

  setTraceViewComponent(
    component: (WinscopeEventEmitter & WinscopeEventListener) | undefined,
  ) {
    this.traceViewComponent = component;
    this.traceViewComponent?.setEmitEvent(async (event) => {
      await this.onWinscopeEvent(event);
    });
  }

  setTimelineComponent(
    component: (WinscopeEventEmitter & WinscopeEventListener) | undefined,
  ) {
    this.timelineComponent = component;
    this.timelineComponent?.setEmitEvent(async (event) => {
      await this.onWinscopeEvent(event);
    });
  }

  async onWinscopeEvent(event: WinscopeEvent) {
    await event.visit(WinscopeEventType.APP_INITIALIZED, async (event) => {
      await this.abtChromeExtensionProtocol.onWinscopeEvent(event);
    });

    await event.visit(WinscopeEventType.APP_FILES_UPLOADED, async (event) => {
      this.currentProgressListener = this.uploadTracesComponent;
      await this.loadFiles(event.files, FilesSource.UPLOADED);
    });

    await event.visit(WinscopeEventType.APP_FILES_COLLECTED, async (event) => {
      this.currentProgressListener = this.collectTracesComponent;
      await this.loadFiles(event.files, FilesSource.COLLECTED);
      await this.loadViewers();
    });

    await event.visit(WinscopeEventType.APP_RESET_REQUEST, async () => {
      await this.resetAppToInitialState();
    });

    await event.visit(WinscopeEventType.APP_TRACE_VIEW_REQUEST, async () => {
      await this.loadViewers();
    });

    await event.visit(
      WinscopeEventType.BUGANIZER_ATTACHMENTS_DOWNLOAD_START,
      async () => {
        await this.resetAppToInitialState();
        this.currentProgressListener = this.uploadTracesComponent;
        this.currentProgressListener?.onProgressUpdate(
          'Downloading files...',
          undefined,
        );
      },
    );

    await event.visit(
      WinscopeEventType.BUGANIZER_ATTACHMENTS_DOWNLOADED,
      async (event) => {
        await this.processRemoteFilesReceived(
          event.files,
          FilesSource.BUGANIZER,
        );
      },
    );

    await event.visit(
      WinscopeEventType.TABBED_VIEW_SWITCH_REQUEST,
      async (event) => {
        await this.traceViewComponent?.onWinscopeEvent(event);
      },
    );

    await event.visit(WinscopeEventType.TABBED_VIEW_SWITCHED, async (event) => {
      await this.appComponent.onWinscopeEvent(event);
      this.timelineData.setActiveViewTraceTypes(
        event.newFocusedView.dependencies,
      );
      this.focusedTabView = event.newFocusedView;
      await this.propagateTracePosition(
        this.timelineData.getCurrentPosition(),
        false,
      );
    });

    await event.visit(
      WinscopeEventType.TRACE_POSITION_UPDATE,
      async (event) => {
        if (event.updateTimeline) {
          this.timelineData.setPosition(event.position);
        }
        await this.propagateTracePosition(event.position, false);
      },
    );

    await event.visit(
      WinscopeEventType.REMOTE_TOOL_BUGREPORT_RECEIVED,
      async (event) => {
        await this.processRemoteFilesReceived(
          [event.bugreport],
          FilesSource.BUGREPORT,
        );
        if (event.timestampNs !== undefined) {
          await this.processRemoteToolTimestampReceived(event.timestampNs);
        }
      },
    );

    await event.visit(
      WinscopeEventType.REMOTE_TOOL_TIMESTAMP_RECEIVED,
      async (event) => {
        await this.processRemoteToolTimestampReceived(event.timestampNs);
      },
    );
  }

  private async loadFiles(files: File[], source: FilesSource) {
    const errors: WinscopeError[] = [];
    const errorListener: WinscopeErrorListener = {
      onError(error: WinscopeError) {
        errors.push(error);
      },
    };
    await this.tracePipeline.loadFiles(
      files,
      source,
      errorListener,
      this.currentProgressListener,
    );

    if (errors.length > 0) {
      this.userNotificationListener.onErrors(errors);
    }
  }

  private async propagateTracePosition(
    position: TracePosition | undefined,
    omitCrossToolProtocol: boolean,
  ) {
    if (!position) {
      return;
    }

    const event = new TracePositionUpdate(position);
    const receivers: WinscopeEventListener[] = [...this.viewers].filter(
      (viewer) => this.isViewerVisible(viewer),
    );
    if (this.timelineComponent) {
      receivers.push(this.timelineComponent);
    }

    const promises = receivers.map((receiver) => {
      return receiver.onWinscopeEvent(event);
    });

    if (!omitCrossToolProtocol) {
      const utcTimestamp = position.timestamp.toUTC();
      const utcPosition = position.entry
        ? TracePosition.fromTraceEntry(position.entry, utcTimestamp)
        : TracePosition.fromTimestamp(utcTimestamp);
      const utcEvent = new TracePositionUpdate(utcPosition);
      promises.push(this.crossToolProtocol.onWinscopeEvent(utcEvent));
    }

    await Promise.all(promises);
  }

  private isViewerVisible(viewer: Viewer): boolean {
    if (!this.focusedTabView) {
      // During initialization no tab is focused.
      // Let's just consider all viewers as visible and to be updated.
      return true;
    }

    return viewer.getViews().some((view) => {
      if (view === this.focusedTabView) {
        return true;
      }
      if (view.type === ViewType.OVERLAY) {
        // Nice to have: update viewer only if overlay view is actually visible (not minimized)
        return true;
      }
      return false;
    });
  }

  private async processRemoteToolTimestampReceived(timestampNs: bigint) {
    const factory = this.tracePipeline.getTimestampFactory();
    const timestamp = factory.makeRealTimestamp(timestampNs);
    this.lastRemoteToolTimestampReceived = timestamp;

    if (!this.areViewersLoaded) {
      return; // apply timestamp later when traces are visualized
    }

    if (this.timelineData.getTimestampType() !== timestamp.getType()) {
      console.warn(
        'Cannot apply new timestamp received from remote tool.' +
          ` Remote tool notified timestamp type ${timestamp.getType()},` +
          ` but Winscope is accepting timestamp type ${this.timelineData.getTimestampType()}.`,
      );
      return;
    }

    const position = this.timelineData.makePositionFromActiveTrace(timestamp);
    this.timelineData.setPosition(position);

    await this.propagateTracePosition(
      this.timelineData.getCurrentPosition(),
      true,
    );
  }

  private async processRemoteFilesReceived(files: File[], source: FilesSource) {
    await this.resetAppToInitialState();
    this.currentProgressListener = this.uploadTracesComponent;
    await this.loadFiles(files, source);
  }

  private async loadViewers() {
    this.currentProgressListener?.onProgressUpdate(
      'Computing frame mapping...',
      undefined,
    );

    // TODO: move this into the ProgressListener
    // allow the UI to update before making the main thread very busy
    await new Promise<void>((resolve) => setTimeout(resolve, 10));

    await this.tracePipeline.buildTraces();
    this.currentProgressListener?.onOperationFinished();

    this.currentProgressListener?.onProgressUpdate(
      'Initializing UI...',
      undefined,
    );

    // TODO: move this into the ProgressListener
    // allow the UI to update before making the main thread very busy
    await new Promise<void>((resolve) => setTimeout(resolve, 10));

    this.timelineData.initialize(
      this.tracePipeline.getTraces(),
      await this.tracePipeline.getScreenRecordingVideo(),
    );

    this.viewers = new ViewerFactory().createViewers(
      this.tracePipeline.getTraces(),
      this.storage,
    );
    this.viewers.forEach((viewer) =>
      viewer.setEmitEvent(async (event) => {
        await this.onWinscopeEvent(event);
      }),
    );

    // Set initial trace position as soon as UI is created
    const initialPosition = this.getInitialTracePosition();
    this.timelineData.setPosition(initialPosition);

    // Make sure all viewers are initialized and have performed the heavy pre-processing they need
    // at this stage, while the "initializing UI" progress message is still being displayed.
    // The viewers initialization is triggered by sending them a "trace position update".
    await this.propagateTracePosition(initialPosition, true);

    this.areViewersLoaded = true;

    // Notify app component (i.e. render viewers), only after all viewers have been initialized
    // (see above).
    //
    // Notifying the app component first could result in this kind of interleaved execution:
    // 1. Mediator notifies app component
    //    1.1. App component renders UI components
    //    1.2. Mediator receives back a "view switched" event
    //    1.2. Mediator sends "trace position update" to viewers
    // 2. Mediator sends "trace position update" to viewers to initialize them (see above)
    //
    // and because our data load operations are async and involve task suspensions, the two
    // "trace position update" could be processed concurrently within the same viewer.
    // Meaning the viewer could perform twice the initial heavy pre-processing,
    // thus increasing UI initialization times.
    await this.appComponent.onWinscopeEvent(new ViewersLoaded(this.viewers));
  }

  private getInitialTracePosition(): TracePosition | undefined {
    if (
      this.lastRemoteToolTimestampReceived &&
      this.timelineData.getTimestampType() ===
        this.lastRemoteToolTimestampReceived.getType()
    ) {
      return this.timelineData.makePositionFromActiveTrace(
        this.lastRemoteToolTimestampReceived,
      );
    }

    const position = this.timelineData.getCurrentPosition();
    if (position) {
      return position;
    }

    // TimelineData might not provide a TracePosition because all the loaded traces are
    // dumps with invalid timestamps (value zero). In this case let's create a TracePosition
    // out of any entry from the loaded traces (if available).
    const firstEntries = this.tracePipeline
      .getTraces()
      .mapTrace((trace) => {
        if (trace.lengthEntries > 0) {
          return trace.getEntry(0);
        }
        return undefined;
      })
      .filter((entry) => {
        return entry !== undefined;
      }) as Array<TraceEntry<object>>;

    if (firstEntries.length > 0) {
      return TracePosition.fromTraceEntry(firstEntries[0]);
    }

    return undefined;
  }

  private async resetAppToInitialState() {
    this.tracePipeline.clear();
    this.timelineData.clear();
    this.viewers = [];
    this.areViewersLoaded = false;
    this.lastRemoteToolTimestampReceived = undefined;
    this.focusedTabView = undefined;
    await this.appComponent.onWinscopeEvent(new ViewersUnloaded());
  }
}
