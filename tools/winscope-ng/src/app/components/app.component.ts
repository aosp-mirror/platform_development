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
import { Component, Injector, Inject, ViewEncapsulation, Input } from "@angular/core";
import { createCustomElement } from "@angular/elements";
import { TraceCoordinator } from "../trace_coordinator";
import { proxyClient, ProxyState } from "trace_collection/proxy_client";
import { PersistentStore } from "common/persistent_store";
import { ViewerWindowManagerComponent } from "viewers/viewer_window_manager/viewer_window_manager.component";
import { ViewerSurfaceFlingerComponent } from "viewers/viewer_surface_flinger/viewer_surface_flinger.component";
import { TraceViewComponent } from "./trace_view.component";
import { Timestamp } from "common/trace/timestamp";
import { MatSliderChange } from "@angular/material/slider";
import { Viewer } from "viewers/viewer";

@Component({
  selector: "app-root",
  template: `
    <div id="app-title">
      <span>Winscope Viewer 2.0</span>
        <button mat-raised-button *ngIf="dataLoaded" (click)="clearData()">Back to Home</button>
        <button mat-raised-button *ngIf="dataLoaded" (click)="toggleTimestamp()">Start/End Timestamp</button>
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

    <div *ngIf="!dataLoaded" fxLayout="row wrap" fxLayoutGap="10px grid" class="card-grid">
      <mat-card class="homepage-card" id="collect-traces-card">
        <collect-traces [(traceCoordinator)]="traceCoordinator" (dataLoadedChange)="onDataLoadedChange($event)"[store]="store"></collect-traces>
      </mat-card>
      <mat-card class="homepage-card" id="upload-traces-card">
        <upload-traces [(traceCoordinator)]="traceCoordinator" (dataLoadedChange)="onDataLoadedChange($event)"></upload-traces>
      </mat-card>
    </div>

    <div id="timescrub">
    </div>

    <div id="timestamps">
    </div>

    <div id="viewers" [class]="showViewers()">
    </div>
  `,
  styles: [".time-slider {width: 100%}"],
  encapsulation: ViewEncapsulation.None
})
export class AppComponent {
  title = "winscope-ng";
  traceCoordinator: TraceCoordinator;
  states = ProxyState;
  store: PersistentStore = new PersistentStore();
  @Input() dataLoaded = false;
  viewersCreated = false;
  currentTimestamp?: Timestamp;
  currentTimestampIndex = 0;
  allTimestamps: Timestamp[] = [];

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
    if (!customElements.get("trace-view")) {
      customElements.define("trace-view",
        createCustomElement(TraceViewComponent, {injector}));
    }
  }

  onDataLoadedChange(dataLoaded: boolean) {
    if (dataLoaded && !this.viewersCreated) {
      this.allTimestamps = this.traceCoordinator.getTimestamps();
      this.traceCoordinator.createViewers();
      this.createViewerElements();
      this.currentTimestampIndex = 0;
      this.notifyCurrentTimestamp();
      this.viewersCreated = true;
      this.dataLoaded = dataLoaded;
    }
  }

  createViewerElements() {
    const viewersDiv = document.querySelector("div#viewers")!;
    viewersDiv.innerHTML = "";

    let cardCounter = 0;
    this.traceCoordinator.getViewers().forEach((viewer: Viewer) => {
      const traceView = document.createElement("trace-view");
      (traceView as any).title = viewer.getTitle();
      (traceView as any).dependencies = viewer.getDependencies();
      (traceView as any).showTrace = true;
      traceView.addEventListener("saveTraces", ($event: any) => {
        this.traceCoordinator.saveTraces($event.detail);
      });
      viewersDiv.appendChild(traceView);

      const traceCard = traceView.querySelector(".trace-card")!;
      traceCard.id = `card-${cardCounter}`;
      (traceView as any).cardId = cardCounter;
      cardCounter++;

      const traceCardContent = traceCard.querySelector(".trace-card-content")!;
      const view = viewer.getView();
      (view as any).showTrace = (traceView as any).showTrace;
      traceCardContent.appendChild(view);
    });
  }

  updateCurrentTimestamp(event: MatSliderChange) {
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
    this.viewersCreated = false;
    this.traceCoordinator.clearData();
    proxyClient.adbData = [];
  }

  public showViewers() {
    const isShown = this.dataLoaded ? "show" : "hide";
    return ["viewers", isShown];
  }
}
