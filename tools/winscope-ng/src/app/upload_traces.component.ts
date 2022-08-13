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
import { Component, Input, Inject, Output, EventEmitter, NgZone } from "@angular/core";
import { Core } from "app/core";
import { TRACE_ICONS } from "app/trace_icons";
import { LoadedTrace } from "app/loaded_trace";

@Component({
  selector: "upload-traces",
  template: `
      <mat-card-title id="title">Upload Traces</mat-card-title>
      <mat-card-content>
        <div
          class="drop-box"
          ref="drop-box"
          (dragleave)="onFileDragOut($event)"
          (dragover)="onFileDragIn($event)"
          (drop)="onHandleFileDrop($event)"
        >
          <div id="inputfile">
            <input
              hidden
              class="input-files"
              id="fileDropRef"
              type="file"
              (change)="onInputFile($event)"
              #fileDropRef
              multiple
            />
            <h3 class="drop-info">Drag and drop</h3>
            <h3 class="drop-info">or click to upload</h3>
            <button mat-raised-button for="fileDropRef" (click)="fileDropRef.click()">
              Choose File
            </button>
            <div *ngIf="this.loadedTraces.length > 0">
              <button mat-raised-button class="load-btn" (click)="onLoadData()">Load Data</button>
              <button mat-raised-button (click)="onClearData()">Clear All</button>
            </div>
          </div>
        </div>

        <mat-list
          class="uploaded-files"
          *ngIf="this.loadedTraces.length > 0"
        >
        <mat-list-item *ngFor="let trace of loadedTraces">
            <mat-icon>{{TRACE_ICONS[trace.type]}}</mat-icon>
            <span>{{trace.name}} ({{trace.type}})
            </span>
            <button
              (click)="onRemoveTrace(trace)"
            ><mat-icon class="file-icon">close</mat-icon>
            </button>
          </mat-list-item>
        </mat-list>
      </mat-card-content>
  `,
  styles: [".drop-info{font-weight: normal;}"]
})
export class UploadTracesComponent {
  @Input()
    core?: Core;

  @Output()
    coreChange = new EventEmitter<Core>();

  dataLoaded = false;

  @Output()
    dataLoadedChange = new EventEmitter<boolean>();

  loadedTraces: LoadedTrace[] = [];
  TRACE_ICONS = TRACE_ICONS;

  constructor(@Inject(NgZone) private ngZone: NgZone) {}

  public async onInputFile(event: Event) {
    const files = this.getInputFiles(event);
    await this.processFiles(files);
  }

  public async processFiles(files: File[]) {
    await this.core?.addTraces(files);
    this.ngZone.run(() => {
      if (this.core) this.loadedTraces = this.core.getLoadedTraces();
    });
  }

  //TODO: extend with support for multiple files, archives, etc...
  private getInputFiles(event: Event): File[] {
    const files: any = (event?.target as HTMLInputElement)?.files;
    if (!files || !files[0]) {
      return [];
    }
    return Array.from(files);
  }

  public onLoadData() {
    this.dataLoaded = true;
    this.dataLoadedChange.emit(this.dataLoaded);
  }

  public onClearData() {
    this.core?.clearData();
  }

  public onFileDragIn(e: DragEvent) {
    e.preventDefault();
    e.stopPropagation();
  }

  public onFileDragOut(e: DragEvent) {
    e.preventDefault();
    e.stopPropagation();
  }

  async onHandleFileDrop(e: DragEvent) {
    e.preventDefault();
    e.stopPropagation();
    const droppedFiles = e.dataTransfer?.files;
    if(!droppedFiles) return;
    await this.processFiles(Array.from(droppedFiles));
  }

  public onRemoveTrace(trace: LoadedTrace) {
    this.core?.removeTrace(trace.type);
    this.loadedTraces = this.loadedTraces.filter(loaded => loaded.type !== trace.type);
  }
}
