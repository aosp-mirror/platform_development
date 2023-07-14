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

import {FileUtils, OnFile} from 'common/file_utils';
import {AppEventEmitter} from 'interfaces/app_event_emitter';
import {AppEventListener} from 'interfaces/app_event_listener';
import {BuganizerAttachmentsDownloadEmitter} from 'interfaces/buganizer_attachments_download_emitter';
import {ProgressListener} from 'interfaces/progress_listener';
import {RemoteBugreportReceiver} from 'interfaces/remote_bugreport_receiver';
import {RemoteTimestampReceiver} from 'interfaces/remote_timestamp_receiver';
import {RemoteTimestampSender} from 'interfaces/remote_timestamp_sender';
import {Runnable} from 'interfaces/runnable';
import {TraceDataListener} from 'interfaces/trace_data_listener';
import {TracePositionUpdateEmitter} from 'interfaces/trace_position_update_emitter';
import {TracePositionUpdateListener} from 'interfaces/trace_position_update_listener';
import {UserNotificationListener} from 'interfaces/user_notification_listener';
import {Timestamp, TimestampType} from 'trace/timestamp';
import {TraceFile} from 'trace/trace_file';
import {TracePosition} from 'trace/trace_position';
import {Viewer} from 'viewers/viewer';
import {ViewerFactory} from 'viewers/viewer_factory';
import {AppEvent, AppEventType, TracePositionUpdate} from './app_event';
import {TimelineData} from './timeline_data';
import {TracePipeline} from './trace_pipeline';

type TimelineComponentInterface = TracePositionUpdateListener & TracePositionUpdateEmitter;
type CrossToolProtocolInterface = RemoteBugreportReceiver &
  RemoteTimestampReceiver &
  RemoteTimestampSender;
type AbtChromeExtensionProtocolInterface = BuganizerAttachmentsDownloadEmitter & Runnable;

export class Mediator {
  private abtChromeExtensionProtocol: AbtChromeExtensionProtocolInterface;
  private crossToolProtocol: CrossToolProtocolInterface;
  private uploadTracesComponent?: ProgressListener;
  private collectTracesComponent?: ProgressListener;
  private traceViewComponent?: AppEventListener & AppEventEmitter;
  private timelineComponent?: TimelineComponentInterface;
  private appComponent: AppEventListener & TraceDataListener;
  private userNotificationListener: UserNotificationListener;
  private storage: Storage;

  private tracePipeline: TracePipeline;
  private timelineData: TimelineData;
  private viewers: Viewer[] = [];
  private isTraceDataVisualized = false;
  private lastRemoteToolTimestampReceived: Timestamp | undefined;
  private currentProgressListener?: ProgressListener;

  constructor(
    tracePipeline: TracePipeline,
    timelineData: TimelineData,
    abtChromeExtensionProtocol: AbtChromeExtensionProtocolInterface,
    crossToolProtocol: CrossToolProtocolInterface,
    appComponent: AppEventListener & TraceDataListener,
    userNotificationListener: UserNotificationListener,
    storage: Storage
  ) {
    this.tracePipeline = tracePipeline;
    this.timelineData = timelineData;
    this.abtChromeExtensionProtocol = abtChromeExtensionProtocol;
    this.crossToolProtocol = crossToolProtocol;
    this.appComponent = appComponent;
    this.userNotificationListener = userNotificationListener;
    this.storage = storage;

    this.crossToolProtocol.setOnBugreportReceived(
      async (bugreport: File, timestamp?: Timestamp) => {
        await this.onRemoteBugreportReceived(bugreport, timestamp);
      }
    );

    this.crossToolProtocol.setOnTimestampReceived(async (timestamp: Timestamp) => {
      await this.onRemoteTimestampReceived(timestamp);
    });

    this.abtChromeExtensionProtocol.setOnBuganizerAttachmentsDownloadStart(() => {
      this.onBuganizerAttachmentsDownloadStart();
    });

    this.abtChromeExtensionProtocol.setOnBuganizerAttachmentsDownloaded(
      async (attachments: File[]) => {
        await this.onBuganizerAttachmentsDownloaded(attachments);
      }
    );
  }

  setUploadTracesComponent(component: ProgressListener | undefined) {
    this.uploadTracesComponent = component;
  }

  setCollectTracesComponent(component: ProgressListener | undefined) {
    this.collectTracesComponent = component;
  }

  setTraceViewComponent(component: (AppEventListener & AppEventEmitter) | undefined) {
    this.traceViewComponent = component;
    this.traceViewComponent?.setEmitAppEvent(async (event) => {
      await this.onTraceViewAppEvent(event);
    });
  }

  setTimelineComponent(component: TimelineComponentInterface | undefined) {
    this.timelineComponent = component;
    this.timelineComponent?.setOnTracePositionUpdate(async (position) => {
      await this.onTimelineTracePositionUpdate(position);
    });
  }

  onWinscopeInitialized() {
    this.abtChromeExtensionProtocol.run();
  }

  onWinscopeUploadNew() {
    this.resetAppToInitialState();
  }

  async onWinscopeFilesUploaded(files: File[]) {
    this.currentProgressListener = this.uploadTracesComponent;
    await this.processFiles(files);
  }

  async onWinscopeFilesCollected(files: File[]) {
    this.currentProgressListener = this.collectTracesComponent;
    await this.processFiles(files);
    await this.processLoadedTraceFiles();
  }

  async onWinscopeViewTracesRequest() {
    await this.processLoadedTraceFiles();
  }

  async onTimelineTracePositionUpdate(position: TracePosition) {
    await this.propagateTracePosition(position);
  }

  async onTraceViewAppEvent(event: AppEvent) {
    await event.visit(AppEventType.TABBED_VIEW_SWITCHED, async (event) => {
      await this.appComponent.onAppEvent(event);
      this.timelineData.setActiveViewTraceTypes(event.newFocusedView.dependencies);
      await this.propagateTracePosition(this.timelineData.getCurrentPosition());
    });
  }

  async onViewerAppEvent(event: AppEvent) {
    await event.visit(AppEventType.TABBED_VIEW_SWITCH_REQUEST, async (event) => {
      await this.traceViewComponent?.onAppEvent(event);
    });
  }

  private async propagateTracePosition(
    position?: TracePosition,
    omitCrossToolProtocol: boolean = false
  ) {
    if (!position) {
      return;
    }

    //TODO (b/289478304): update only visible viewers (1 tab viewer + overlay viewers)
    const event = new TracePositionUpdate(position);
    const promises = this.viewers.map((viewer) => {
      return viewer.onAppEvent(event);
    });
    await Promise.all(promises);

    this.timelineComponent?.onTracePositionUpdate(position);

    if (omitCrossToolProtocol) {
      return;
    }

    const timestamp = position.timestamp;
    if (timestamp.getType() !== TimestampType.REAL) {
      console.warn(
        'Cannot propagate timestamp change to remote tool.' +
          ` Remote tool expects timestamp type ${TimestampType.REAL},` +
          ` but Winscope wants to notify timestamp type ${timestamp.getType()}.`
      );
      return;
    }

    this.crossToolProtocol.sendTimestamp(timestamp);
  }

  private onBuganizerAttachmentsDownloadStart() {
    this.resetAppToInitialState();
    this.currentProgressListener = this.uploadTracesComponent;
    this.currentProgressListener?.onProgressUpdate('Downloading files...', undefined);
  }

  private async onBuganizerAttachmentsDownloaded(attachments: File[]) {
    this.currentProgressListener = this.uploadTracesComponent;
    await this.processRemoteFilesReceived(attachments);
  }

  private async onRemoteBugreportReceived(bugreport: File, timestamp?: Timestamp) {
    this.currentProgressListener = this.uploadTracesComponent;
    await this.processRemoteFilesReceived([bugreport]);
    if (timestamp !== undefined) {
      await this.onRemoteTimestampReceived(timestamp);
    }
  }

  private async onRemoteTimestampReceived(timestamp: Timestamp) {
    this.lastRemoteToolTimestampReceived = timestamp;

    if (!this.isTraceDataVisualized) {
      return; // apply timestamp later when traces are visualized
    }

    if (this.timelineData.getTimestampType() !== timestamp.getType()) {
      console.warn(
        'Cannot apply new timestamp received from remote tool.' +
          ` Remote tool notified timestamp type ${timestamp.getType()},` +
          ` but Winscope is accepting timestamp type ${this.timelineData.getTimestampType()}.`
      );
      return;
    }

    const position = TracePosition.fromTimestamp(timestamp);
    this.timelineData.setPosition(position);

    await this.propagateTracePosition(this.timelineData.getCurrentPosition(), true);
  }

  private async processRemoteFilesReceived(files: File[]) {
    this.resetAppToInitialState();
    await this.processFiles(files);
  }

  private async processFiles(files: File[]) {
    let progressMessage = '';
    const onProgressUpdate = (progressPercentage: number) => {
      this.currentProgressListener?.onProgressUpdate(progressMessage, progressPercentage);
    };

    const traceFiles: TraceFile[] = [];
    const onFile: OnFile = (file: File, parentArchive?: File) => {
      traceFiles.push(new TraceFile(file, parentArchive));
    };

    //TODO(b/290183109): push unzip logic into trace pipeline, once the trace
    // pipeline test will run with Karma. Currently, we can't execute
    // FileUtils.unzipFilesIfNeeded() within Node.js because of the dependency
    // on type File (which is Web API stuff).
    progressMessage = 'Unzipping files...';
    this.currentProgressListener?.onProgressUpdate(progressMessage, 0);
    await FileUtils.unzipFilesIfNeeded(files, onFile, onProgressUpdate);

    const parserErrors = await this.tracePipeline.loadTraceFiles(
      traceFiles,
      this.currentProgressListener
    );
    this.currentProgressListener?.onOperationFinished();
    this.userNotificationListener?.onParserErrors(parserErrors);
  }

  private async processLoadedTraceFiles() {
    this.currentProgressListener?.onProgressUpdate('Computing frame mapping...', undefined);

    // allow the UI to update before making the main thread very busy
    await new Promise<void>((resolve) => setTimeout(resolve, 10));

    await this.tracePipeline.buildTraces();
    this.currentProgressListener?.onOperationFinished();

    this.timelineData.initialize(
      this.tracePipeline.getTraces(),
      await this.tracePipeline.getScreenRecordingVideo()
    );

    this.viewers = new ViewerFactory().createViewers(this.tracePipeline.getTraces(), this.storage);
    this.viewers.forEach((viewer) =>
      viewer.setEmitAppEvent(async (event) => {
        await this.onViewerAppEvent(event);
      })
    );

    // Set position as soon as the viewers are created
    await this.propagateTracePosition(this.timelineData.getCurrentPosition(), true);

    this.appComponent.onTraceDataLoaded(this.viewers);
    this.isTraceDataVisualized = true;

    if (this.lastRemoteToolTimestampReceived !== undefined) {
      await this.onRemoteTimestampReceived(this.lastRemoteToolTimestampReceived);
    }
  }

  private resetAppToInitialState() {
    this.tracePipeline.clear();
    this.timelineData.clear();
    this.viewers = [];
    this.isTraceDataVisualized = false;
    this.lastRemoteToolTimestampReceived = undefined;
    this.appComponent.onTraceDataUnloaded();
  }
}
