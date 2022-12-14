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

import {AppComponentDependencyInversion} from "./components/app_component_dependency_inversion";
import {TimelineComponentDependencyInversion}
  from "./components/timeline/timeline_component_dependency_inversion";
import {TimelineData} from "./timeline_data";
import {TraceData} from "./trace_data";
import {AbtChromeExtensionProtocolDependencyInversion}
  from "abt_chrome_extension/abt_chrome_extension_protocol_dependency_inversion";
import {CrossToolProtocolDependencyInversion}
  from "cross_tool/cross_tool_protocol_dependency_inversion";
import {FileUtils} from "common/utils/file_utils";
import {Timestamp, TimestampType} from "common/trace/timestamp";
import {TraceType} from "common/trace/trace_type";
import {Viewer} from "viewers/viewer";
import {ViewerFactory} from "viewers/viewer_factory";

export class Mediator {
  private traceData: TraceData;
  private timelineData: TimelineData;
  private abtChromeExtensionProtocol: AbtChromeExtensionProtocolDependencyInversion;
  private crossToolProtocol: CrossToolProtocolDependencyInversion;
  private appComponent: AppComponentDependencyInversion;
  private timelineComponent?: TimelineComponentDependencyInversion;
  private storage: Storage;
  private viewers: Viewer[] = [];
  private isChangingCurrentTimestamp = false;
  private blockWhileRemoteToolBugreportIsBeingLoaded = Promise.resolve();

  constructor(
    traceData: TraceData,
    timelineData: TimelineData,
    abtChromeExtensionProtocol: AbtChromeExtensionProtocolDependencyInversion,
    crossToolProtocol: CrossToolProtocolDependencyInversion,
    appComponent: AppComponentDependencyInversion,
    storage: Storage) {

    this.traceData = traceData;
    this.timelineData = timelineData;
    this.abtChromeExtensionProtocol = abtChromeExtensionProtocol;
    this.crossToolProtocol = crossToolProtocol;
    this.appComponent = appComponent;
    this.storage = storage;

    this.timelineData.setOnCurrentTimestampChanged(timestamp => {
      this.onWinscopeCurrentTimestampChanged(timestamp);
    });

    this.crossToolProtocol.setOnBugreportReceived(async (bugreport: File, timestamp?: Timestamp) => {
      await this.onRemoteToolBugreportReceived(bugreport, timestamp);
    });

    this.crossToolProtocol.setOnTimestampReceived(async (timestamp: Timestamp) => {
      await this.onRemoteToolTimestampReceived(timestamp);
    });

    this.abtChromeExtensionProtocol.setOnBugAttachmentsReceived(async (attachments: File[]) => {
      await this.onAbtChromeExtensionBugAttachmentsReceived(attachments);
    });
    this.abtChromeExtensionProtocol.run();
  }

  public setTimelineComponent(timelineComponent: TimelineComponentDependencyInversion|undefined) {
    this.timelineComponent = timelineComponent;
  }

  public onWinscopeTraceDataLoaded() {
    this.processTraceData();
  }

  public async onRemoteToolBugreportReceived(bugreport: File, timestamp?: Timestamp) {
    let unblockOtherRemoteToolEventHandlers: () => void;

    this.blockWhileRemoteToolBugreportIsBeingLoaded = new Promise<void>(resolve => {
      unblockOtherRemoteToolEventHandlers = resolve;
    });

    try {
      await this.processFiles([bugreport]);
    } finally {
      unblockOtherRemoteToolEventHandlers!();
    }

    if (timestamp !== undefined) {
      await this.onRemoteToolTimestampReceived(timestamp);
    }
  }

  public async onAbtChromeExtensionBugAttachmentsReceived(attachments: File[]) {
    await this.processFiles(attachments);
  }

  public onWinscopeCurrentTimestampChanged(timestamp: Timestamp|undefined) {
    this.executeIgnoringRecursiveTimestampNotifications(() => {
      const entries = this.traceData.getTraceEntries(timestamp);
      this.viewers.forEach(viewer => {
        viewer.notifyCurrentTraceEntries(entries);
      });

      if (timestamp) {
        if (timestamp.getType() !== TimestampType.REAL) {
          console.warn(
            "Cannot propagate timestamp change to remote tool." +
            ` Remote tool expects timestamp type ${TimestampType.REAL},` +
            ` but Winscope wants to notify timestamp type ${timestamp.getType()}.`
          );
        } else {
          this.crossToolProtocol.sendTimestamp(timestamp);
        }
      }

      this.timelineComponent?.onCurrentTimestampChanged(timestamp);
    });
  }

  public async onRemoteToolTimestampReceived(timestamp: Timestamp) {
    await this.executeIgnoringRecursiveTimestampNotificationsAsync(async () => {
      if (this.timelineData.getTimestampType() != TimestampType.REAL) {
        console.warn(
          "Cannot apply new timestamp received from remote tool." +
          ` Remote tool notified timestamp type type ${TimestampType.REAL},` +
          ` but Winscope is accepting timestamp type ${this.timelineData.getTimestampType()}.`
        );
      }

      if (this.timelineData.getCurrentTimestamp() === timestamp) {
        return; // no timestamp change
      }

      // Make sure we finished loading the bugreport, before notifying the timestamp to the rest of
      // the system. Otherwise, the timestamp notification would just get lost.
      await this.blockWhileRemoteToolBugreportIsBeingLoaded;

      const entries = this.traceData.getTraceEntries(timestamp);
      this.viewers.forEach(viewer => {
        viewer.notifyCurrentTraceEntries(entries);
      });

      this.timelineData.setCurrentTimestamp(timestamp);
      this.timelineComponent?.onCurrentTimestampChanged(timestamp);
    });
  }

  public onWinscopeUploadNew() {
    this.traceData.clear();
    this.timelineData.clear();
    this.viewers = [];
  }

  private async processFiles(files: File[]) {
    const unzippedFiles = await FileUtils.unzipFilesIfNeeded(files);
    this.traceData.clear();
    await this.traceData.loadTraces(unzippedFiles);
    this.processTraceData();
  }

  private processTraceData() {
    this.timelineData.initialize(
      this.traceData.getTimelines(),
      this.traceData.getScreenRecordingVideo()
    );
    this.createViewers();
    this.appComponent.onTraceDataLoaded(this.viewers);
  }

  private createViewers() {
    const traceTypes = this.traceData.getLoadedTraces().map(trace => trace.type);
    this.viewers = new ViewerFactory().createViewers(new Set<TraceType>(traceTypes), this.storage);

    // Make sure to update the viewers active entries as soon as they are created.
    if (this.timelineData.getCurrentTimestamp()) {
      this.onWinscopeCurrentTimestampChanged(this.timelineData.getCurrentTimestamp());
    }
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

  private async executeIgnoringRecursiveTimestampNotificationsAsync(op: () => Promise<void>) {
    if (this.isChangingCurrentTimestamp) {
      return;
    }
    this.isChangingCurrentTimestamp = true;
    try {
      await op();
    } finally {
      this.isChangingCurrentTimestamp = false;
    }
  }
}
