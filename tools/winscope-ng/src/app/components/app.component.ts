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
import { Component, Injector, Inject, ViewEncapsulation, Input, ViewChild } from "@angular/core";
import { createCustomElement } from "@angular/elements";
import { TraceCoordinator } from "../trace_coordinator";
import { proxyClient, ProxyState } from "trace_collection/proxy_client";
import { PersistentStore } from "common/persistent_store";
import { ViewerWindowManagerComponent } from "viewers/viewer_window_manager/viewer_window_manager.component";
import { ViewerSurfaceFlingerComponent } from "viewers/viewer_surface_flinger/viewer_surface_flinger.component";
import { Timestamp } from "common/trace/timestamp";
import { MatSliderChange } from "@angular/material/slider";

@Component({
  selector: "app-root",
  template: `
    <mat-toolbar class="app-toolbar">
      <span id="app-title">Winscope</span>
      <span class="toolbar-wrapper">
        <button mat-raised-button *ngIf="dataLoaded" (click)="toggleTimestamp()">Start/End Timestamp</button>
        <button class="upload-new-btn white-btn" mat-raised-button *ngIf="dataLoaded" (click)="clearData()">Upload New</button>
      </span>
    </mat-toolbar>

    <div class="welcome-info" *ngIf="!dataLoaded">
      <span>Welcome to Winscope. Please select source to view traces.</span>
    </div>

    <div *ngIf="!dataLoaded" fxLayout="row wrap" fxLayoutGap="10px grid" class="card-grid">
      <mat-card class="homepage-card" id="collect-traces-card">
        <collect-traces [traceCoordinator]="traceCoordinator" (dataLoadedChange)="onDataLoadedChange($event)"[store]="store"></collect-traces>
      </mat-card>
      <mat-card class="homepage-card" id="upload-traces-card">
        <upload-traces [traceCoordinator]="traceCoordinator" (dataLoadedChange)="onDataLoadedChange($event)"></upload-traces>
      </mat-card>
    </div>

    <div id="viewers" [class]="showViewers()">
      <trace-view
      [store]="store"
      [traceCoordinator]="traceCoordinator"
      ></trace-view>
    </div>

    <div id="timescrub">
      <mat-slider
        *ngIf="dataLoaded"
        step="1"
        min="0"
        [max]="this.allTimestamps.length-1"
        aria-label="units"
        [value]="currentTimestampIndex"
        (input)="updateCurrentTimestamp($event)"
        class="time-slider"
      ></mat-slider>
    </div>
    <div id="timestamps">
    </div>
  `,
  styles: [
    `
      .time-slider {
        width: 100%
      }
      .upload-new-btn {
        float: right;
        position: relative;
        vertical-align: middle;
        display: inline-block;
      }
      .app-toolbar {
        border-bottom: 1px solid var(--default-border);
        box-shadow: none;
        background-color: rgba(1, 1, 1, 0);
        height: 56px;
        vertical-align: middle;
        position: relative;
        display: inline-block;
      }
      .toolbar-wrapper {
        width: 100%;
        height: 100%;
        vertical-align: middle;
        position: relative;
        display: inline-block;
        align-content: center;
      }
      .welcome-info {
        text-align: center;
        font: inherit;
        padding: 40px;
      }
    `
  ],
  encapsulation: ViewEncapsulation.None
})
export class AppComponent {
  title = "winscope-ng";
  traceCoordinator: TraceCoordinator;
  states = ProxyState;
  store: PersistentStore = new PersistentStore();
  currentTimestamp?: Timestamp;
  currentTimestampIndex = 0;
  allTimestamps: Timestamp[] = [];
  @Input() dataLoaded = false;

  constructor(
    @Inject(Injector) injector: Injector
  ) {
    this.traceCoordinator = new TraceCoordinator();
    if (!customElements.get("viewer-window-manager")) {
      customElements.define("viewer-window-manager",
        createCustomElement(ViewerWindowManagerComponent, {injector}));
    }
    if (!customElements.get("viewer-surface-flinger")) {
      customElements.define("viewer-surface-flinger",
        createCustomElement(ViewerSurfaceFlingerComponent, {injector}));
    }
  }

  public updateCurrentTimestamp(event: MatSliderChange) {
    if (event.value) {
      this.currentTimestampIndex = event.value;
      this.notifyCurrentTimestamp();
    }
  }

  public notifyCurrentTimestamp() {
    this.currentTimestamp = this.allTimestamps[this.currentTimestampIndex];
    this.traceCoordinator.notifyCurrentTimestamp(this.currentTimestamp);
  }

  public toggleTimestamp() {
    if (this.currentTimestampIndex===0) {
      this.currentTimestampIndex = this.allTimestamps.length-1;
    } else {
      this.currentTimestampIndex = 0;
    }
    this.notifyCurrentTimestamp();
  }

  public clearData() {
    this.dataLoaded = false;
    this.traceCoordinator.clearData();
    proxyClient.adbData = [];
  }

  public showViewers() {
    const isShown = this.dataLoaded ? "show" : "hide";
    return ["viewers", isShown];
  }

  public onDataLoadedChange(dataLoaded: boolean) {
    if (dataLoaded && !(this.traceCoordinator.getViewers().length > 0)) {
      this.allTimestamps = this.traceCoordinator.getTimestamps();
      this.traceCoordinator.createViewers();
      this.currentTimestampIndex = 0;
      this.notifyCurrentTimestamp();
      this.dataLoaded = dataLoaded;
    }
  }
}
