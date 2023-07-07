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
import {BuganizerAttachmentsDownloadEmitter} from 'interfaces/buganizer_attachments_download_emitter';
import {ProgressListener} from 'interfaces/progress_listener';
import {RemoteBugreportReceiver} from 'interfaces/remote_bugreport_receiver';
import {RemoteTimestampReceiver} from 'interfaces/remote_timestamp_receiver';
import {RemoteTimestampSender} from 'interfaces/remote_timestamp_sender';
import {Runnable} from 'interfaces/runnable';
import {TraceDataListener} from 'interfaces/trace_data_listener';
import {TracePositionUpdateListener} from 'interfaces/trace_position_update_listener';
import {UserNotificationListener} from 'interfaces/user_notification_listener';
import {Timestamp, TimestampType} from 'trace/timestamp';
import {TraceFile} from 'trace/trace_file';
import {TracePosition} from 'trace/trace_position';
import {TraceType} from 'trace/trace_type';
import {Viewer} from 'viewers/viewer';
import {ViewerFactory} from 'viewers/viewer_factory';
import {TimelineData} from './timeline_data';
import {TracePipeline} from './trace_pipeline';

type CrossToolProtocolInterface = RemoteBugreportReceiver &
  RemoteTimestampReceiver &
  RemoteTimestampSender;
type AbtChromeExtensionProtocolInterface = BuganizerAttachmentsDownloadEmitter & Runnable;

export class Mediator {
  private abtChromeExtensionProtocol: AbtChromeExtensionProtocolInterface;
  private crossToolProtocol: CrossToolProtocolInterface;
  private uploadTracesComponent?: ProgressListener;
  private collectTracesComponent?: ProgressListener;
  private timelineComponent?: TracePositionUpdateListener;
  private appComponent: TraceDataListener;
  private userNotificationListener: UserNotificationListener;
  private storage: Storage;

  private tracePipeline: TracePipeline;
  private timelineData: TimelineData;
  private viewers: Viewer[] = [];
  private isChangingCurrentTimestamp = false;
  private isTraceDataVisualized = false;
  private lastRemoteToolTimestampReceived: Timestamp | undefined;
  private currentProgressListener?: ProgressListener;

  constructor(
    tracePipeline: TracePipeline,
    timelineData: TimelineData,
    abtChromeExtensionProtocol: AbtChromeExtensionProtocolInterface,
    crossToolProtocol: CrossToolProtocolInterface,
    appComponent: TraceDataListener,
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

    this.timelineData.setOnTracePositionUpdate((position) => {
      this.onWinscopeTracePositionUpdate(position);
    });

    this.crossToolProtocol.setOnBugreportReceived(
      async (bugreport: File, timestamp?: Timestamp) => {
        await this.onRemoteBugreportReceived(bugreport, timestamp);
      }
    );

    this.crossToolProtocol.setOnTimestampReceived((timestamp: Timestamp) => {
      this.onRemoteTimestampReceived(timestamp);
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

  setUploadTracesComponent(uploadTracesComponent: ProgressListener | undefined) {
    this.uploadTracesComponent = uploadTracesComponent;
  }

  setCollectTracesComponent(collectTracesComponent: ProgressListener | undefined) {
    this.collectTracesComponent = collectTracesComponent;
  }

  setTimelineComponent(timelineComponent: TracePositionUpdateListener | undefined) {
    this.timelineComponent = timelineComponent;
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

  onWinscopeTracePositionUpdate(position: TracePosition) {
    this.executeIgnoringRecursiveTimestampNotifications(() => {
      this.updateViewersTracePosition(position);

      const timestamp = position.timestamp;
      if (timestamp.getType() !== TimestampType.REAL) {
        console.warn(
          'Cannot propagate timestamp change to remote tool.' +
            ` Remote tool expects timestamp type ${TimestampType.REAL},` +
            ` but Winscope wants to notify timestamp type ${timestamp.getType()}.`
        );
      } else {
        this.crossToolProtocol.sendTimestamp(timestamp);
      }

      this.timelineComponent?.onTracePositionUpdate(position);
    });
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
      this.onRemoteTimestampReceived(timestamp);
    }
  }

  private onRemoteTimestampReceived(timestamp: Timestamp) {
    this.executeIgnoringRecursiveTimestampNotifications(() => {
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

      if (
        this.timelineData.getCurrentPosition()?.timestamp.getValueNs() === timestamp.getValueNs()
      ) {
        return; // no timestamp change
      }

      const position = TracePosition.fromTimestamp(timestamp);
      this.updateViewersTracePosition(position);
      this.timelineData.setPosition(position);
      this.timelineComponent?.onTracePositionUpdate(position); //TODO: is this redundant?
    });
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

    progressMessage = 'Unzipping files...';
    this.currentProgressListener?.onProgressUpdate(progressMessage, 0);
    await FileUtils.unzipFilesIfNeeded(files, onFile, onProgressUpdate);

    progressMessage = 'Parsing files...';
    this.currentProgressListener?.onProgressUpdate(progressMessage, 0);
    const parserErrors = await this.tracePipeline.loadTraceFiles(traceFiles, onProgressUpdate);
    this.currentProgressListener?.onOperationFinished();
    this.userNotificationListener?.onParserErrors(parserErrors);
  }

  private async processLoadedTraceFiles() {
    this.currentProgressListener?.onProgressUpdate('Computing frame mapping...', undefined);

    // allow the UI to update before making the main thread very busy
    await new Promise<void>((resolve) => setTimeout(resolve, 10));

    this.tracePipeline.buildTraces();
    this.currentProgressListener?.onOperationFinished();

    this.timelineData.initialize(
      this.tracePipeline.getTraces(),
      this.tracePipeline.getScreenRecordingVideo()
    );
    this.createViewers();
    this.appComponent.onTraceDataLoaded(this.viewers);
    this.isTraceDataVisualized = true;

    if (this.lastRemoteToolTimestampReceived !== undefined) {
      this.onRemoteTimestampReceived(this.lastRemoteToolTimestampReceived);
    }
  }

  private createViewers() {
    const traces = this.tracePipeline.getTraces();
    const traceTypes = new Set<TraceType>();
    traces.forEachTrace((trace) => {
      traceTypes.add(trace.type);
    });
    this.viewers = new ViewerFactory().createViewers(traceTypes, traces, this.storage);

    // Update the viewers as soon as they are created
    const position = this.timelineData.getCurrentPosition();
    if (position) {
      this.onWinscopeTracePositionUpdate(position);
    }
  }

  private updateViewersTracePosition(position: TracePosition) {
    this.viewers.forEach((viewer) => {
      viewer.onTracePositionUpdate(position);
    });
  }

  private executeIgnoringRecursiveTimestampNotifications(op: () => void) {
    if (this.isChangingCurrentTimestamp) {
      return;
    }
    this.isChangingCurrentTimestamp = true;
    try {
      op();
    } finally {
      this.isChangingCurrentTimestamp = false;
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
