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
  Component,
  Input,
  Output,
  EventEmitter,
  Inject,
  NgZone,
  ChangeDetectorRef} from "@angular/core";
import { MatSnackBar } from "@angular/material/snack-bar";
import { TraceData} from "app/trace_data";
import { TRACE_INFO } from "app/trace_info";
import {Trace} from "common/trace/trace";
import {FileUtils} from "common/utils/file_utils";
import { ParserError } from "parsers/parser_factory";
import { ParserErrorSnackBarComponent } from "./parser_error_snack_bar_component";

@Component({
  selector: "upload-traces",
  template: `
    <mat-card class="upload-card">
      <mat-card-title class="title">Upload Traces</mat-card-title>

      <mat-card-content
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

        <mat-list *ngIf="this.traceData.getLoadedTraces().length > 0"
                  class="uploaded-files">
          <mat-list-item *ngFor="let trace of this.traceData.getLoadedTraces()">
            <mat-icon matListIcon>
              {{TRACE_INFO[trace.type].icon}}
            </mat-icon>

            <p matLine>
              {{trace.file.name}} ({{TRACE_INFO[trace.type].name}})
            </p>

            <button color="primary" mat-icon-button (click)="onRemoveTrace($event, trace)">
              <mat-icon>close</mat-icon>
            </button>
          </mat-list-item>
        </mat-list>

        <div *ngIf="this.loadedTraces.length === 0" class="drop-info">
          <p class="mat-body-3 icon">
            <mat-icon inline fontIcon="upload"></mat-icon>
          </p>
          <p class="mat-body-1">
            Drag your .winscope file(s) or click to upload
          </p>
        </div>
      </mat-card-content>

      <div *ngIf="this.loadedTraces.length > 0" class="trace-actions-container">
        <button color="primary" mat-raised-button class="load-btn" (click)="onViewTracesButtonClick()">
          View traces
        </button>

        <button color="primary" mat-stroked-button for="fileDropRef" (click)="fileDropRef.click()">
          Upload another file
        </button>

        <button color="primary" mat-stroked-button (click)="onClearButtonClick()">
          Clear all
        </button>
      </div>
    </mat-card>
  `,
  styles: [
    `
      .upload-card {
        height: 100%;
        display: flex;
        flex-direction: column;
        overflow: auto;
        margin: 10px;
      }
      .drop-box {
        display: flex;
        flex-direction: column;
        overflow: auto;
        border: 2px dashed var(--border-color);
        cursor: pointer;
      }
      .uploaded-files {
        flex: 400px;
        padding: 0;
      }
      .drop-info {
        flex: 400px;
        display: flex;
        flex-direction: column;
        justify-content: center;
        align-items: center;
        pointer-events: none;
      }
      .drop-info p {
        opacity: 0.6;
        font-size: 1.2rem;
      }
      .drop-info .icon {
        font-size: 3rem;
        margin: 0;
      }
      .trace-actions-container {
        display: flex;
        flex-direction: row;
        flex-wrap: wrap;
        gap: 10px;
      }

      mat-card-content {
        flex-grow: 1;
      }
    `
  ]
})
export class UploadTracesComponent {
  loadedTraces: Trace[] = [];
  TRACE_INFO = TRACE_INFO;

  @Input() traceData!: TraceData;
  @Output() traceDataLoaded = new EventEmitter<void>();

  constructor(
    @Inject(ChangeDetectorRef) private changeDetectorRef: ChangeDetectorRef,
    @Inject(NgZone) private ngZone: NgZone,
    @Inject(MatSnackBar) private snackBar: MatSnackBar
  ) {}

  public async onInputFile(event: Event) {
    const files = this.getInputFiles(event);
    await this.processFiles(files);
  }

  public async processFiles(files: File[]) {
    const unzippedFiles = await FileUtils.getUnzippedFiles(files);
    const parserErrors = await this.traceData.loadTraces(unzippedFiles);
    if (parserErrors.length > 0) {
      this.openTempSnackBar(parserErrors);
    }
    this.ngZone.run(() => {
      this.loadedTraces = this.traceData.getLoadedTraces();
    });
  }

  public onViewTracesButtonClick() {
    this.traceDataLoaded.emit();
  }

  public onClearButtonClick() {
    this.traceData.clear();
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

  public onRemoveTrace(event: MouseEvent, trace: Trace) {
    event.preventDefault();
    event.stopPropagation();
    this.traceData.removeTrace(trace.type);
    this.changeDetectorRef.detectChanges();
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
