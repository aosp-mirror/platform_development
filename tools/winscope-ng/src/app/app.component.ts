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
import { Component, Injector, Inject } from "@angular/core";
import { createCustomElement } from "@angular/elements";
import { ViewerWindowManagerComponent } from "viewers/viewer_window_manager/viewer_window_manager.component";
import { Core } from "./core";
import { ProxyState } from "trace_collection/proxy_client";
import { PersistentStore } from "../common/persistent_store";

@Component({
  selector: "app-root",
  template: `
    <div id="title">
      <span>Winscope Viewer 2.0</span>
    </div>
    <div *ngIf="!dataLoaded" class="card-container" fxLayout="row wrap" fxLayoutGap="10px grid">
      <mat-card class="homepage-card">
        <collect-traces [(core)]="core" [(dataLoaded)]="dataLoaded" [store]="store"></collect-traces>
      </mat-card>
      <mat-card class="homepage-card">
        <mat-card-title>Upload Traces</mat-card-title>
        <div id="inputfile">
          <input mat-input type="file" (change)="onInputFile($event)" #fileUpload>
        </div>
      </mat-card>
    </div>

    <div *ngIf="dataLoaded">
      <mat-card class="homepage-card">
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
  styles: [".card-container{width: 100%; display:flex; flex-direction: row; overflow: auto;}"]
})
export class AppComponent {
  title = "winscope-ng";

  core: Core = new Core();
  states = ProxyState;
  store: PersistentStore = new PersistentStore();
  dataLoaded: boolean = false;

  constructor(
    @Inject(Injector) injector: Injector
  ) {
    customElements.define("viewer-window-manager",
      createCustomElement(ViewerWindowManagerComponent, {injector}));
  }

  onCoreChange(newCore: Core) {
    this.core = newCore;
  }

  onDataLoadedChange(loaded: boolean) {
    this.dataLoaded = loaded;
  }

  public async onInputFile(event: Event) {
    const files = this.getInputFiles(event);
    await this.core.bootstrap(files);

    const viewersDiv = document.querySelector("div#viewers")!;
    viewersDiv.innerHTML = "";
    this.core.getViews().forEach(view => viewersDiv!.appendChild(view) );

    const timestampsDiv = document.querySelector("div#timestamps")!;
    timestampsDiv.innerHTML = `Retrieved ${this.core.getTimestamps().length} unique timestamps`;
  }

  public notifyCurrentTimestamp() {
    const dummyTimestamp = 1000000; //TODO: get timestamp from time scrub
    this.core.notifyCurrentTimestamp(dummyTimestamp);
  }

  //TODO: extend with support for multiple files, archives, etc...
  private getInputFiles(event: Event): File[] {
    const files: any = (event?.target as HTMLInputElement)?.files;

    if (!files || !files[0]) {
      return [];
    }

    return [files[0]];
  }

  public clearData() {
    this.dataLoaded = false;
    this.core.clearData();
  }
}
