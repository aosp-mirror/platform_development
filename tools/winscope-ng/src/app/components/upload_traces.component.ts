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
import { Component, Input, Output, EventEmitter, Inject, NgZone } from "@angular/core";
import { TraceCoordinator } from "app/trace_coordinator";
import { TRACE_INFO } from "app/trace_info";
import { LoadedTrace } from "app/loaded_trace";
import { MatSnackBar } from "@angular/material/snack-bar";
import { ParserErrorSnackBarComponent } from "./parser_error_snack_bar_component";
import { ParserError } from "parsers/parser_factory";

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
        (click)="fileDropRef.click()"
      >
        <input
          id="fileDropRef"
          hidden
          type="file"
          multiple
          #fileDropRef
          (change)="onInputFile($event)"
        />
        <mat-list *ngIf="this.loadedTraces.length > 0" class="uploaded-files">
          <mat-list-item *ngFor="let trace of loadedTraces">
            <mat-icon>{{TRACE_INFO[trace.type].icon}}</mat-icon>
            {{trace.name}} ({{TRACE_INFO[trace.type].name}})
            <button
              color="primary"
              mat-icon-button
              class="close-btn"
              (click)="onRemoveTrace($event, trace)"
            ><mat-icon>close</mat-icon>
            </button>
          </mat-list-item>
        </mat-list>
        <p *ngIf="this.loadedTraces.length === 0" class="drop-info mat-body-1">Drag your .winscope file(s) or click to upload</p>
      </div>

      <div *ngIf="this.loadedTraces.length > 0">
        <button color="primary" mat-raised-button class="load-btn" (click)="onLoadData()">View traces</button>
        <button color="primary" mat-stroked-button for="fileDropRef" (click)="fileDropRef.click()">Upload another file</button>
        <button color="primary" mat-stroked-button (click)="onClearData()">Clear all</button>
      </div>
    </mat-card-content>
  `,
  styles: [
    `
      .drop-box {
        height: 400px;
        padding: 10px;
        display: flex;
        flex-direction: column;
        overflow: auto;
        outline: 2px dashed var(--default-border);
        outline-offset: -10px;
        cursor: pointer;
      }
      .uploaded-files {
        height: 100%;
        width: 100%;
        overflow: auto;
      }
      .close-btn {
        margin-left: auto;
      }
      .drop-info {
        pointer-events: none;
        margin: auto;
      }
    `
  ]
})
export class UploadTracesComponent {
  loadedTraces: LoadedTrace[] = [];
  TRACE_INFO = TRACE_INFO;
  dataLoaded = false;

  @Input() traceCoordinator!: TraceCoordinator;
  @Output() dataLoadedChange = new EventEmitter<boolean>();

  constructor(
    @Inject(NgZone) private ngZone: NgZone,
    @Inject(MatSnackBar) private snackBar: MatSnackBar
  ) {}

  public async onInputFile(event: Event) {
    const files = this.getInputFiles(event);
    await this.processFiles(files);
  }

  public async processFiles(files: File[]) {
    const unzippedFiles = await this.traceCoordinator.getUnzippedFiles(files);
    const parserErrors = await this.traceCoordinator.addTraces(unzippedFiles);
    if (parserErrors.length > 0) {
      this.openTempSnackBar(parserErrors);
    }
    this.ngZone.run(() => {
      this.loadedTraces = this.traceCoordinator.getLoadedTraces();
    });
  }

  public onLoadData() {
    this.dataLoaded = true;
    this.dataLoadedChange.emit(this.dataLoaded);
  }

  public onClearData() {
    this.traceCoordinator.clearData();
    this.dataLoaded = false;
    this.loadedTraces = [];
    this.dataLoadedChange.emit(this.dataLoaded);
  }

  public onFileDragIn(e: DragEvent) {
    e.preventDefault();
    e.stopPropagation();
  }

  public onFileDragOut(e: DragEvent) {
    e.preventDefault();
    e.stopPropagation();
  }

  public async onHandleFileDrop(e: DragEvent) {
    e.preventDefault();
    e.stopPropagation();
    const droppedFiles = e.dataTransfer?.files;
    if(!droppedFiles) return;
    await this.processFiles(Array.from(droppedFiles));
  }

  public onRemoveTrace(event: MouseEvent, trace: LoadedTrace) {
    event.preventDefault();
    event.stopPropagation();
    this.traceCoordinator.removeTrace(trace.type);
    this.loadedTraces = this.loadedTraces.filter(loaded => loaded.type !== trace.type);
  }

  private openTempSnackBar(parserErrors: ParserError[]) {
    this.snackBar.openFromComponent(ParserErrorSnackBarComponent, {
      data: parserErrors,
      duration: 7500,
    });
  }

  private getInputFiles(event: Event): File[] {
    const files: FileList | null = (event?.target as HTMLInputElement)?.files;
    if (!files || !files[0]) {
      return [];
    }
    return Array.from(files);
  }
}
