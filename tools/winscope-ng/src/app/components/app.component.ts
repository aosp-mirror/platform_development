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

import {
  ChangeDetectorRef,
  Component,
  Injector,
  Inject,
  ViewChild,
  ViewEncapsulation
} from "@angular/core";
import { createCustomElement } from "@angular/elements";
import { TimelineComponent} from "./timeline/timeline.component";
import { Mediator } from "app/mediator";
import { TraceData } from "app/trace_data";
import { PersistentStore } from "common/utils/persistent_store";
import { Timestamp } from "common/trace/timestamp";
import { FileUtils } from "common/utils/file_utils";
import { FunctionUtils } from "common/utils/function_utils";
import { proxyClient, ProxyState } from "trace_collection/proxy_client";
import { ViewerInputMethodComponent } from "viewers/components/viewer_input_method.component";
import { View, Viewer } from "viewers/viewer";
import { ViewerProtologComponent} from "viewers/viewer_protolog/viewer_protolog.component";
import { ViewerSurfaceFlingerComponent } from "viewers/viewer_surface_flinger/viewer_surface_flinger.component";
import { ViewerWindowManagerComponent } from "viewers/viewer_window_manager/viewer_window_manager.component";
import { ViewerTransactionsComponent } from "viewers/viewer_transactions/viewer_transactions.component";
import { ViewerScreenRecordingComponent } from "viewers/viewer_screen_recording/viewer_screen_recording.component";
import { TraceType } from "common/trace/trace_type";
import { TimelineData } from "app/timeline_data";
import { TracingConfig } from "trace_collection/tracing_config";
import {TRACE_INFO} from "app/trace_info";

@Component({
  selector: "app-root",
  template: `
    <mat-toolbar class="toolbar">
      <span class="app-title">Winscope</span>

      <div class="spacer"></div>

      <button *ngIf="dataLoaded" color="primary" mat-stroked-button
              (click)="onUploadNewClick()">
        Upload New
      </button>

      <button
          mat-icon-button
          matTooltip="Report bug"
          (click)="goToLink('https://b.corp.google.com/issues/new?component=909476')">
        <mat-icon>
          bug_report
        </mat-icon>
      </button>

      <button
          mat-icon-button
          matTooltip="Switch to {{ isDarkModeOn ? 'light' : 'dark'}} mode"
          (click)="setDarkMode(!isDarkModeOn)">
        <mat-icon>
          {{ isDarkModeOn ? "brightness_5" : "brightness_4" }}
        </mat-icon>
      </button>
    </mat-toolbar>

    <mat-divider></mat-divider>

    <mat-drawer-container class="example-container" autosize disableClose
                          autoFocus>

      <mat-drawer-content>

        <ng-container *ngIf="dataLoaded; else noLoadedTracesBlock">

          <trace-view
              class="viewers"
              [viewers]="viewers"
              [store]="store"
              (onDownloadTracesButtonClick)="onDownloadTracesButtonClick()"
              (onActiveViewChanged)="handleActiveViewChanged($event)"
          ></trace-view>

          <mat-divider></mat-divider>

        </ng-container>

      </mat-drawer-content>

      <mat-drawer #drawer mode="overlay" opened="true"
                  [baseHeight]="collapsedTimelineHeight">

        <!-- TODO: remove redundant videoData parameter below -->
        <timeline
            *ngIf="dataLoaded"
            [timelineData]="timelineData"
            [activeViewTraceTypes]="activeView?.dependencies"
            [availableTraces]="getLoadedTraceTypes()"
            [videoData]="timelineData.getScreenRecordingVideo()"
            (init)="onTimelineInit()"
            (destroy)="onTimelineDestroy()"
            (collapsedTimelineSizeChanged)="onCollapsedTimelineSizeChanged($event)"
        ></timeline>
      </mat-drawer>

    </mat-drawer-container>

    <ng-template #noLoadedTracesBlock>
      <div class="center">
        <div class="landing-content">
          <h1 class="welcome-info mat-headline">
            Welcome to Winscope. Please select source to view traces.
          </h1>

          <div class="card-grid landing-grid">
            <collect-traces
                class="collect-traces-card homepage-card"
                [traceData]="traceData"
                (traceDataLoaded)="onTraceDataLoaded()"
                [store]="store"
            ></collect-traces>

            <upload-traces
                class="upload-traces-card homepage-card"
                [traceData]="traceData"
                (traceDataLoaded)="onTraceDataLoaded()"
            ></upload-traces>
          </div>
        </div>
      </div>
    </ng-template>
  `,
  styles: [
    `
      .toolbar {
        gap: 10px;
      }
      .welcome-info {
        margin: 16px 0 6px 0;
        text-align: center;
      }
      .homepage-card {
        display: flex;
        flex-direction: column;
        flex: 1;
        overflow: auto;
        height: 820px;
      }
      .spacer {
        flex: 1;
      }
      .viewers {
        height: 0;
        flex-grow: 1;
        display: flex;
        flex-direction: column;
        overflow: auto;
      }
      .timescrub {
        margin: 8px;
      }
      .center {
        display: flex;
        align-content: center;
        flex-direction: column;
        justify-content: center;
        align-items: center;
        justify-items: center;
        flex-grow: 1;
      }
      .landing-content {
        width: 100%;
      }
      .landing-content .card-grid {
        max-width: 1800px;
        flex-grow: 1;
        margin: auto;
      }
    `
  ],
  encapsulation: ViewEncapsulation.None
})
export class AppComponent {
  title = "winscope-ng";
  changeDetectorRef: ChangeDetectorRef;
  traceData = new TraceData();
  timelineData = new TimelineData();
  mediator = new Mediator(this.traceData, this.timelineData);
  states = ProxyState;
  store: PersistentStore = new PersistentStore();
  currentTimestamp?: Timestamp;
  viewers: Viewer[] = [];
  isDarkModeOn!: boolean;
  dataLoaded = false;
  activeView: View|undefined;
  collapsedTimelineHeight = 0;
  @ViewChild(TimelineComponent) timelineComponent?: TimelineComponent;

  constructor(
    @Inject(Injector) injector: Injector,
    @Inject(ChangeDetectorRef) changeDetectorRef: ChangeDetectorRef
  ) {
    this.changeDetectorRef = changeDetectorRef;

    const storeDarkMode = this.store.get("dark-mode");
    const prefersDarkQuery = window.matchMedia?.("(prefers-color-scheme: dark)");
    this.setDarkMode(storeDarkMode != null ? storeDarkMode == "true" : prefersDarkQuery.matches);

    if (!customElements.get("viewer-input-method")) {
      customElements.define("viewer-input-method",
        createCustomElement(ViewerInputMethodComponent, {injector}));
    }
    if (!customElements.get("viewer-protolog")) {
      customElements.define("viewer-protolog",
        createCustomElement(ViewerProtologComponent, {injector}));
    }
    if (!customElements.get("viewer-screen-recording")) {
      customElements.define("viewer-screen-recording",
        createCustomElement(ViewerScreenRecordingComponent, {injector}));
    }
    if (!customElements.get("viewer-surface-flinger")) {
      customElements.define("viewer-surface-flinger",
        createCustomElement(ViewerSurfaceFlingerComponent, {injector}));
    }
    if (!customElements.get("viewer-transactions")) {
      customElements.define("viewer-transactions",
        createCustomElement(ViewerTransactionsComponent, {injector}));
    }
    if (!customElements.get("viewer-window-manager")) {
      customElements.define("viewer-window-manager",
        createCustomElement(ViewerWindowManagerComponent, {injector}));
    }

    TracingConfig.getInstance().initialize(localStorage);
  }

  onTimelineInit() {
    this.mediator.setNotifyCurrentTimestampChangedToTimelineComponentCallback((timestamp: Timestamp|undefined) => {
      this.timelineComponent?.onCurrentTimestampChanged(timestamp);
    });
  }

  onTimelineDestroy() {
    this.mediator.setNotifyCurrentTimestampChangedToTimelineComponentCallback(
      FunctionUtils.DO_NOTHING
    );
  }

  onCollapsedTimelineSizeChanged(height: number) {
    this.collapsedTimelineHeight = height;
    this.changeDetectorRef.detectChanges();
  }

  getLoadedTraceTypes(): TraceType[] {
    return this.traceData.getLoadedTraces().map((trace) => trace.type);
  }

  getVideoData(): Blob|undefined {
    return this.timelineData.getScreenRecordingVideo();
  }

  public onUploadNewClick() {
    this.dataLoaded = false;
    this.mediator.clearData();
    proxyClient.adbData = [];
    this.changeDetectorRef.detectChanges();
  }

  public setDarkMode(enabled: boolean) {
    document.body.classList.toggle("dark-mode", enabled);
    this.store.add("dark-mode", `${enabled}`);
    this.isDarkModeOn = enabled;
  }

  public onTraceDataLoaded() {
    this.mediator.onTraceDataLoaded(localStorage);
    this.viewers = this.mediator.getViewers();
    this.dataLoaded = true;
    this.changeDetectorRef.detectChanges();
  }

  async onDownloadTracesButtonClick() {
    const traceFiles = await this.makeTraceFilesForDownload();
    const zipFileBlob = await FileUtils.createZipArchive(traceFiles);
    const zipFileName = "winscope.zip";

    const a = document.createElement("a");
    document.body.appendChild(a);
    const url = window.URL.createObjectURL(zipFileBlob);
    a.href = url;
    a.download = zipFileName;
    a.click();
    window.URL.revokeObjectURL(url);
    document.body.removeChild(a);
  }

  private async makeTraceFilesForDownload(): Promise<File[]> {
    return this.traceData.getLoadedTraces().map(trace => {
      const traceType = TRACE_INFO[trace.type].name;
      const newName = traceType + "/" + FileUtils.removeDirFromFileName(trace.file.name);
      return new File([trace.file], newName);
    });
  }

  handleActiveViewChanged(view: View) {
    this.activeView = view;
    this.timelineData.setActiveViewTraceTypes(view.dependencies);
  }

  getActiveTraceType(): TraceType|undefined {
    if (this.activeView === undefined) {
      return undefined;
    }
    if (this.activeView.dependencies.length !== 1) {
      throw Error("Viewers with dependencies length !== 1 are not supported.");
    }
    return this.activeView.dependencies[0];
  }

  goToLink(url: string){
    window.open(url, "_blank");
  }
}
