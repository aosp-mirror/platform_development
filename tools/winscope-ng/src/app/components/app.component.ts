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
    <mat-toolbar class="toolbar">
      <span class="app-title">Winscope</span>

      <button *ngIf="dataLoaded" color="primary" mat-stroked-button (click)="toggleTimestamp()">
        Start/End Timestamp
      </button>

      <div class="spacer"></div>

      <button *ngIf="dataLoaded" color="primary" mat-stroked-button (click)="onUploadNewClick()">
        Upload New
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

    <ng-container *ngIf="dataLoaded; else noLoadedTracesBlock">
      <trace-view
        class="viewers"
        [viewers]="allViewers"
        [store]="store"
        (downloadTracesButtonClick)="onDownloadTracesButtonClick()"
      ></trace-view>

      <mat-divider></mat-divider>

      <mat-slider
        class="timescrub"
        color="primary"
        step="1"
        min="0"
        [max]="this.allTimestamps.length - 1"
        aria-label="units"
        [value]="currentTimestampIndex"
        (input)="updateCurrentTimestamp($event)"
      ></mat-slider>
    </ng-container>

    <ng-template #noLoadedTracesBlock>
      <h1 class="welcome-info mat-headline">
        Welcome to Winscope. Please select source to view traces.
      </h1>

      <div class="card-grid">
        <collect-traces
          class="collect-traces-card homepage-card"
          [traceCoordinator]="traceCoordinator"
          (dataLoadedChange)="onDataLoadedChange($event)"
          [store]="store"
        ></collect-traces>

        <upload-traces
          class="upload-traces-card homepage-card"
          [traceCoordinator]="traceCoordinator"
          (dataLoadedChange)="onDataLoadedChange($event)"
        ></upload-traces>
      </div>
    </ng-template>

    <div class="timestamps">
    </div>
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
      }
      .spacer {
        flex: 1;
      }
      .viewers {
        height: 100%;
        display: flex;
        flex-direction: column;
        overflow: auto;
      }
      .timescrub {
        margin: 8px;
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
  isDarkModeOn!: boolean;
  @Input() dataLoaded = false;

  constructor(
    @Inject(Injector) injector: Injector,
    @Inject(ChangeDetectorRef) changeDetectorRef: ChangeDetectorRef
  ) {
    this.changeDetectorRef = changeDetectorRef;
    this.traceCoordinator = new TraceCoordinator();

    const storeDarkMode = this.store.getFromStore("dark-mode");
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

  public setDarkMode(enabled: boolean) {
    document.body.classList.toggle("dark-mode", enabled);
    this.store.addToStore("dark-mode", `${enabled}`);
    this.isDarkModeOn = enabled;
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
