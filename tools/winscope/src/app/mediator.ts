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
import {Store} from 'common/store';
import {Timestamp} from 'common/time';
import {TimeUtils} from 'common/time_utils';
import {UserNotifier} from 'common/user_notifier';
import {CrossToolProtocol} from 'cross_tool/cross_tool_protocol';
import {Analytics} from 'logging/analytics';
import {ProgressListener} from 'messaging/progress_listener';
import {UserWarning} from 'messaging/user_warning';
import {
  CannotVisualizeTraceEntry,
  FailedToInitializeTimelineData,
  IncompleteFrameMapping,
  NoTraceTargetsSelected,
  NoValidFiles,
} from 'messaging/user_warnings';
import {
  ActiveTraceChanged,
  ExpandedTimelineToggled,
  TracePositionUpdate,
  ViewersLoaded,
  ViewersUnloaded,
  WinscopeEvent,
  WinscopeEventType,
} from 'messaging/winscope_event';
import {WinscopeEventEmitter} from 'messaging/winscope_event_emitter';
import {WinscopeEventListener} from 'messaging/winscope_event_listener';
import {TraceEntry} from 'trace/trace';
import {TRACE_INFO} from 'trace/trace_info';
import {TracePosition} from 'trace/trace_position';
import {TraceType} from 'trace/trace_type';
import {RequestedTraceTypes} from 'trace_collection/adb_files';
import {View, Viewer, ViewType} from 'viewers/viewer';
import {ViewerFactory} from 'viewers/viewer_factory';
import {FilesSource} from './files_source';
import {TimelineData} from './timeline_data';
import {TracePipeline} from './trace_pipeline';

export class Mediator {
  private abtChromeExtensionProtocol: WinscopeEventEmitter &
    WinscopeEventListener;
  private crossToolProtocol: CrossToolProtocol;
  private uploadTracesComponent?: ProgressListener;
  private collectTracesComponent?: ProgressListener &
    WinscopeEventEmitter &
    WinscopeEventListener;
  private traceViewComponent?: WinscopeEventEmitter & WinscopeEventListener;
  private timelineComponent?: WinscopeEventEmitter & WinscopeEventListener;
  private appComponent: WinscopeEventListener;
  private storage: Store;

  private tracePipeline: TracePipeline;
  private timelineData: TimelineData;
  private viewers: Viewer[] = [];
  private focusedTabView: undefined | View;
  private areViewersLoaded = false;
  private lastRemoteToolDeferredTimestampReceived?: () => Timestamp | undefined;
  private currentProgressListener?: ProgressListener;

  constructor(
    tracePipeline: TracePipeline,
    timelineData: TimelineData,
    abtChromeExtensionProtocol: WinscopeEventEmitter & WinscopeEventListener,
    crossToolProtocol: CrossToolProtocol,
    appComponent: WinscopeEventListener,
    storage: Store,
  ) {
    this.tracePipeline = tracePipeline;
    this.timelineData = timelineData;
    this.abtChromeExtensionProtocol = abtChromeExtensionProtocol;
    this.crossToolProtocol = crossToolProtocol;
    this.appComponent = appComponent;
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

  setCollectTracesComponent(
    component:
      | (ProgressListener & WinscopeEventEmitter & WinscopeEventListener)
      | undefined,
  ) {
    this.collectTracesComponent = component;
    this.collectTracesComponent?.setEmitEvent(async (event) => {
      await this.onWinscopeEvent(event);
    });
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
      UserNotifier.notify();
    });

    await event.visit(WinscopeEventType.APP_FILES_COLLECTED, async (event) => {
      this.currentProgressListener = this.collectTracesComponent;
      if (event.files.collected.length > 0) {
        await this.loadFiles(event.files.collected, FilesSource.COLLECTED);
        const traces = this.tracePipeline.getTraces();
        if (traces.getSize() > 0) {
          const failedTraces: string[] = [];
          event.files.requested.forEach((requested: RequestedTraceTypes) => {
            if (
              !requested.types.some((type) => traces.getTraces(type).length > 0)
            ) {
              failedTraces.push(requested.name);
            }
          });
          if (failedTraces.length > 0) {
            UserNotifier.add(new NoValidFiles(failedTraces));
          }
          await this.loadViewers();
        } else {
          this.currentProgressListener?.onOperationFinished(false);
        }
      } else {
        UserNotifier.add(new NoValidFiles());
      }
      UserNotifier.notify();
    });

    await event.visit(WinscopeEventType.APP_RESET_REQUEST, async () => {
      await this.resetAppToInitialState();
    });

    await event.visit(
      WinscopeEventType.APP_REFRESH_DUMPS_REQUEST,
      async (event) => {
        await this.resetAppToInitialState();
        await this.collectTracesComponent?.onWinscopeEvent(event);
      },
    );

    await event.visit(WinscopeEventType.APP_TRACE_VIEW_REQUEST, async () => {
      await this.loadViewers();
      UserNotifier.notify();
    });

    await event.visit(
      WinscopeEventType.REMOTE_TOOL_DOWNLOAD_START,
      async () => {
        Analytics.Tracing.logOpenFromABT();
        await this.resetAppToInitialState();
        this.currentProgressListener = this.uploadTracesComponent;
        this.currentProgressListener?.onProgressUpdate(
          'Downloading files...',
          undefined,
        );
      },
    );

    await event.visit(
      WinscopeEventType.REMOTE_TOOL_FILES_RECEIVED,
      async (event) => {
        await this.processRemoteFilesReceived(
          event.files,
          FilesSource.REMOTE_TOOL,
        );
        if (event.deferredTimestamp) {
          await this.processRemoteToolDeferredTimestampReceived(
            event.deferredTimestamp,
          );
        }
      },
    );

    await event.visit(
      WinscopeEventType.REMOTE_TOOL_TIMESTAMP_RECEIVED,
      async (event) => {
        await this.processRemoteToolDeferredTimestampReceived(
          event.deferredTimestamp,
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
      const newActiveTrace = event.newFocusedView.traces[0];
      if (this.timelineData.trySetActiveTrace(newActiveTrace)) {
        const activeTraceChanged = new ActiveTraceChanged(newActiveTrace);
        await this.timelineComponent?.onWinscopeEvent(activeTraceChanged);
        for (const viewer of this.viewers) {
          await viewer.onWinscopeEvent(activeTraceChanged);
        }
      }
      this.focusedTabView = event.newFocusedView;
      await this.propagateTracePosition(
        this.timelineData.getCurrentPosition(),
        false,
      );
      UserNotifier.notify();
    });

    await event.visit(
      WinscopeEventType.TRACE_POSITION_UPDATE,
      async (event) => {
        if (event.updateTimeline) {
          this.timelineData.setPosition(event.position);
        }
        await this.propagateTracePosition(event.position, false);
        UserNotifier.notify();
      },
    );

    await event.visit(
      WinscopeEventType.EXPANDED_TIMELINE_TOGGLED,
      async (event) => {
        await this.propagateToOverlays(event);
      },
    );

    await event.visit(WinscopeEventType.ACTIVE_TRACE_CHANGED, async (event) => {
      this.timelineData.trySetActiveTrace(event.trace);
      for (const viewer of this.viewers) {
        await viewer.onWinscopeEvent(event);
      }
      await this.timelineComponent?.onWinscopeEvent(event);
    });

    await event.visit(WinscopeEventType.DARK_MODE_TOGGLED, async (event) => {
      await this.timelineComponent?.onWinscopeEvent(event);
      for (const viewer of this.viewers) {
        await viewer.onWinscopeEvent(event);
      }
    });

    await event.visit(
      WinscopeEventType.NO_TRACE_TARGETS_SELECTED,
      async (event) => {
        UserNotifier.add(new NoTraceTargetsSelected()).notify();
      },
    );

    await event.visit(
      WinscopeEventType.FILTER_PRESET_SAVE_REQUEST,
      async (event) => {
        await this.findViewerByType(event.traceType)?.onWinscopeEvent(event);
      },
    );

    await event.visit(
      WinscopeEventType.FILTER_PRESET_APPLY_REQUEST,
      async (event) => {
        await this.findViewerByType(event.traceType)?.onWinscopeEvent(event);
      },
    );
  }

  private async loadFiles(files: File[], source: FilesSource) {
    await this.tracePipeline.loadFiles(
      files,
      source,
      this.currentProgressListener,
    );
  }

  private async propagateTracePosition(
    position: TracePosition | undefined,
    omitCrossToolProtocol: boolean,
  ) {
    if (!position) {
      return;
    }

    const event = new TracePositionUpdate(position);
    const viewers: Viewer[] = [...this.viewers].filter((viewer) =>
      this.isViewerVisible(viewer),
    );

    const warnings: UserWarning[] = [];

    for (const viewer of viewers) {
      try {
        await viewer.onWinscopeEvent(event);
      } catch (e) {
        const traceType = assertDefined(viewer.getTraces().at(0)?.type);
        warnings.push(
          new CannotVisualizeTraceEntry(
            `Cannot parse entry for ${TRACE_INFO[traceType].name} trace: Trace may be corrupted.`,
          ),
        );
      }
    }

    if (this.timelineComponent) {
      await this.timelineComponent.onWinscopeEvent(event);
    }

    if (!omitCrossToolProtocol) {
      await this.crossToolProtocol.onWinscopeEvent(event);
    }

    if (warnings.length > 0) {
      warnings.forEach((w) => UserNotifier.add(w));
    }
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

  private async processRemoteToolDeferredTimestampReceived(
    deferredTimestamp: () => Timestamp | undefined,
  ) {
    this.lastRemoteToolDeferredTimestampReceived = deferredTimestamp;

    if (!this.areViewersLoaded) {
      return; // apply timestamp later when traces are visualized
    }

    const timestamp = deferredTimestamp();
    if (!timestamp) {
      return;
    }

    const position = this.timelineData.makePositionFromActiveTrace(timestamp);
    this.timelineData.setPosition(position);

    await this.propagateTracePosition(
      this.timelineData.getCurrentPosition(),
      true,
    );
    UserNotifier.notify();
  }

  private async processRemoteFilesReceived(files: File[], source: FilesSource) {
    await this.resetAppToInitialState();
    this.currentProgressListener = this.uploadTracesComponent;
    await this.loadFiles(files, source);
    UserNotifier.notify();
  }

  private async loadViewers() {
    this.currentProgressListener?.onProgressUpdate(
      'Computing frame mapping...',
      undefined,
    );

    // TODO: move this into the ProgressListener
    // allow the UI to update before making the main thread very busy
    await TimeUtils.sleepMs(10);

    this.tracePipeline.filterTracesWithoutVisualization();
    if (this.tracePipeline.getTraces().getSize() === 0) {
      this.currentProgressListener?.onOperationFinished(false);
      return;
    }

    try {
      await this.tracePipeline.buildTraces();
      this.currentProgressListener?.onOperationFinished(true);
    } catch (e) {
      UserNotifier.add(new IncompleteFrameMapping((e as Error).message));
      this.currentProgressListener?.onOperationFinished(false);
    }

    this.currentProgressListener?.onProgressUpdate(
      'Initializing UI...',
      undefined,
    );

    // TODO: move this into the ProgressListener
    // allow the UI to update before making the main thread very busy
    await TimeUtils.sleepMs(10);

    try {
      await this.timelineData.initialize(
        this.tracePipeline.getTraces(),
        await this.tracePipeline.getScreenRecordingVideo(),
        this.tracePipeline.getTimestampConverter(),
      );
    } catch {
      this.currentProgressListener?.onOperationFinished(false);
      UserNotifier.add(new FailedToInitializeTimelineData());
      return;
    }

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

    this.focusedTabView = this.viewers
      .find((v) => v.getViews()[0].type !== ViewType.OVERLAY)
      ?.getViews()[0];
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
    if (this.lastRemoteToolDeferredTimestampReceived) {
      const lastRemoteToolTimestamp =
        this.lastRemoteToolDeferredTimestampReceived();
      if (lastRemoteToolTimestamp) {
        return this.timelineData.makePositionFromActiveTrace(
          lastRemoteToolTimestamp,
        );
      }
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
    this.lastRemoteToolDeferredTimestampReceived = undefined;
    this.focusedTabView = undefined;
    await this.appComponent.onWinscopeEvent(new ViewersUnloaded());
  }

  private async propagateToOverlays(event: ExpandedTimelineToggled) {
    const overlayViewers = this.viewers.filter((viewer) =>
      viewer.getViews().some((view) => view.type === ViewType.OVERLAY),
    );
    for (const overlay of overlayViewers) {
      await overlay.onWinscopeEvent(event);
    }
  }

  private findViewerByType(type: TraceType): Viewer | undefined {
    return this.viewers.find((viewer) => viewer.getTraces()[0].type === type);
  }
}
