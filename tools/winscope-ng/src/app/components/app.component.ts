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
import {Component, Injector, Inject, ViewEncapsulation, Input, ChangeDetectorRef} from "@angular/core";
import { createCustomElement } from "@angular/elements";
import { MatSliderChange } from "@angular/material/slider";
import { TraceCoordinator } from "app/trace_coordinator";
import { PersistentStore } from "common/persistent_store";
import { Timestamp } from "common/trace/timestamp";
import { FileUtils } from "common/utils/file_utils";
import { proxyClient, ProxyState } from "trace_collection/proxy_client";
import { ViewerInputMethodComponent } from "viewers/components/viewer_input_method.component";
import { Viewer } from "viewers/viewer";
import { ViewerProtologComponent} from "viewers/viewer_protolog/viewer_protolog.component";
import { ViewerSurfaceFlingerComponent } from "viewers/viewer_surface_flinger/viewer_surface_flinger.component";
import { ViewerWindowManagerComponent } from "viewers/viewer_window_manager/viewer_window_manager.component";
import { ViewerTransactionsComponent } from "viewers/viewer_transactions/viewer_transactions.component";
import { ViewerScreenRecordingComponent } from "viewers/viewer_screen_recording/viewer_screen_recording.component";

@Component({
  selector: "app-root",
  template: `
    <mat-toolbar class="app-toolbar">
      <p id="app-title" class="mat-display-1">Winscope</p>
      <span class="toolbar-wrapper">
        <button *ngIf="dataLoaded" color="primary" mat-stroked-button (click)="toggleTimestamp()">Start/End Timestamp</button>
        <button *ngIf="dataLoaded" color="primary" mat-stroked-button (click)="onUploadNewClick()">Upload New</button>
      </span>
    </mat-toolbar>

    <h1 *ngIf="!dataLoaded" class="welcome-info mat-headline">Welcome to Winscope. Please select source to view traces.</h1>

    <div *ngIf="!dataLoaded" class="card-grid">
      <mat-card id="collect-traces-card" class="homepage-card">
        <collect-traces [traceCoordinator]="traceCoordinator" (dataLoadedChange)="onDataLoadedChange($event)" [store]="store"></collect-traces>
      </mat-card>
      <mat-card id="upload-traces-card" class="homepage-card">
        <upload-traces [traceCoordinator]="traceCoordinator" (dataLoadedChange)="onDataLoadedChange($event)"></upload-traces>
      </mat-card>
    </div>

    <trace-view
      *ngIf="dataLoaded"
      id="viewers"
      [viewers]="allViewers"
      [store]="store"
      (downloadTracesButtonClick)="onDownloadTracesButtonClick()"
    ></trace-view>

    <div *ngIf="dataLoaded" id="timescrub">
      <mat-slider
        color="primary"
        class="time-slider"
        step="1"
        min="0"
        [max]="this.allTimestamps.length-1"
        aria-label="units"
        [value]="currentTimestampIndex"
        (input)="updateCurrentTimestamp($event)"
      ></mat-slider>
    </div>
    <div id="timestamps">
    </div>
  `,
  styles: [
    `
      .app-toolbar {
        background-color: white;
        border-bottom: 1px solid var(--default-border);
      }
      #app-title {
        margin: 12px 0;
      }
      .toolbar-wrapper {
        width: 100%;
        height: 100%;
        display: flex;
        flex-direction: row;
        justify-content: space-between;
        align-items: center;
      }
      .welcome-info {
        margin: 16px 0;
        text-align: center;
      }
      .homepage-card {
        flex: 1;
        margin: 10px;
        overflow: auto;
        border: 1px solid var(--default-border);
      }

      .homepage-card mat-card-content {
        display: flex;
        flex-direction: column;
        overflow: auto;
      }
      #viewers {
        height: 100%;
        width: 100%;
        display: flex;
        flex-direction: column;
        overflow: auto;
      }
      #timescrub {
        padding: 8px;
        border-top: 1px solid var(--default-border);
      }
      .time-slider {
        width: 100%
      }
    `
  ],
  encapsulation: ViewEncapsulation.None
})
export class AppComponent {
  title = "winscope-ng";
  changeDetectorRef: ChangeDetectorRef;
  traceCoordinator: TraceCoordinator;
  states = ProxyState;
  store: PersistentStore = new PersistentStore();
  currentTimestamp?: Timestamp;
  currentTimestampIndex = 0;
  allTimestamps: Timestamp[] = [];
  allViewers: Viewer[] = [];
  @Input() dataLoaded = false;

  constructor(
    @Inject(Injector) injector: Injector,
    @Inject(ChangeDetectorRef) changeDetectorRef: ChangeDetectorRef
  ) {
    this.changeDetectorRef = changeDetectorRef;
    this.traceCoordinator = new TraceCoordinator();

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
  }

  public updateCurrentTimestamp(event: MatSliderChange) {
    if (event.value) {
      this.currentTimestampIndex = event.value;
      this.notifyCurrentTimestamp();
    }
  }

  public toggleTimestamp() {
    if (this.currentTimestampIndex===0) {
      this.currentTimestampIndex = this.allTimestamps.length-1;
    } else {
      this.currentTimestampIndex = 0;
    }
    this.notifyCurrentTimestamp();
  }

  public onUploadNewClick() {
    this.dataLoaded = false;
    this.traceCoordinator.clearData();
    proxyClient.adbData = [];
    this.changeDetectorRef.detectChanges();
  }

  public onDataLoadedChange(dataLoaded: boolean) {
    if (dataLoaded && !(this.traceCoordinator.getViewers().length > 0)) {
      this.traceCoordinator.createViewers();
      this.allViewers = this.traceCoordinator.getViewers();
      this.allTimestamps = this.traceCoordinator.getTimestamps();
      this.currentTimestampIndex = 0;
      this.notifyCurrentTimestamp();
      this.dataLoaded = dataLoaded;
      this.changeDetectorRef.detectChanges();
    }
  }

  private notifyCurrentTimestamp() {
    this.currentTimestamp = this.allTimestamps[this.currentTimestampIndex];
    this.traceCoordinator.notifyCurrentTimestamp(this.currentTimestamp);
  }

  private async onDownloadTracesButtonClick() {
    const traces = await this.traceCoordinator.getAllTracesForDownload();
    const zipFileBlob = await FileUtils.createZipArchive(traces);
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
}
