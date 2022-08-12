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
import {Component, Inject, Injector, Input} from "@angular/core";
import {createCustomElement} from "@angular/elements";
import {Timestamp, TimestampType} from "common/trace/timestamp";
import {PersistentStore} from "common/persistent_store";
import {ViewerWindowManagerComponent} from "viewers/viewer_window_manager/viewer_window_manager.component";
import {Core} from "./core";
import {ProxyState, proxyClient} from "trace_collection/proxy_client";

@Component({
  selector: "app-root",
  template: `
    <div id="title">
      <span>Winscope Viewer 2.0</span>
    </div>

    <div *ngIf="!dataLoaded" fxLayout="row wrap" fxLayoutGap="10px grid" class="home">
      <mat-card class="homepage-card" id="collect-traces-card">
        <collect-traces [(core)]="core" (dataLoadedChange)="onDataLoadedChange($event)"[store]="store"></collect-traces>
      </mat-card>
      <mat-card class="homepage-card" id="upload-traces-card">
        <upload-traces [(core)]="core" (dataLoadedChange)="onDataLoadedChange($event)"></upload-traces>
      </mat-card>
    </div>

    <div *ngIf="dataLoaded">
      <mat-card class="homepage-card" id="loaded-data-card">
      <mat-card-title>Loaded data</mat-card-title>
        <button mat-raised-button (click)="clearData()">Back to Home</button>
      </mat-card>
    </div>

    <div id="timescrub">
      <button mat-raised-button (click)="notifyCurrentTimestamp()">Update current timestamp</button>
    </div>

    <div id="timestamps">
    </div>

    <div id="viewers">
    </div>
  `,
  styles: [".home{width: 100%; display:flex; flex-direction: row; overflow: auto;}"]
})
export class AppComponent {
  title = "winscope-ng";
  core: Core;
  states = ProxyState;
  store: PersistentStore = new PersistentStore();
  @Input() dataLoaded = false;
  viewersCreated = false;

  constructor(
    @Inject(Injector) injector: Injector
  ) {
    this.core = new Core();
    if (!customElements.get("viewer-window-manager")) {
      customElements.define("viewer-window-manager",
        createCustomElement(ViewerWindowManagerComponent, {injector}));
    }
  }

  onDataLoadedChange(dataLoaded: boolean) {
    if (dataLoaded && !this.viewersCreated) {
      this.core.createViewers();
      const dummyTimestamp = this.core.getTimestamps()[1]; //TODO: get timestamp from time scrub
      this.core.notifyCurrentTimestamp(dummyTimestamp);
      this.viewersCreated = true;
      this.dataLoaded = dataLoaded;
    }
  }

  public notifyCurrentTimestamp() {
    const dummyTimestamp = new Timestamp(TimestampType.ELAPSED, 1000000n);
    this.core.notifyCurrentTimestamp(dummyTimestamp);
  }

  public clearData() {
    this.dataLoaded = false;
    this.viewersCreated = false;
    this.core.clearData();
    proxyClient.adbData = [];
  }
}
