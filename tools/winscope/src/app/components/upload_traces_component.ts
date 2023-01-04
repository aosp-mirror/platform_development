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
  EventEmitter,
  Inject,
  Input,
  NgZone,
  Output,
} from '@angular/core';
import {MatSnackBar} from '@angular/material/snack-bar';
import {TraceData} from 'app/trace_data';
import {TRACE_INFO} from 'app/trace_info';
import {FileUtils, OnFile} from 'common/file_utils';
import {FilesDownloadListener} from 'interfaces/files_download_listener';
import {Trace, TraceFile} from 'trace/trace';
import {ParserErrorSnackBarComponent} from './parser_error_snack_bar_component';

@Component({
  selector: 'upload-traces',
  template: `
    <mat-card class="upload-card">
      <mat-card-title class="title">Upload Traces</mat-card-title>

      <mat-card-content
        class="drop-box"
        ref="drop-box"
        (dragleave)="onFileDragOut($event)"
        (dragover)="onFileDragIn($event)"
        (drop)="onHandleFileDrop($event)"
        (click)="fileDropRef.click()">
        <input
          id="fileDropRef"
          hidden
          type="file"
          multiple
          #fileDropRef
          (change)="onInputFiles($event)" />

        <load-progress
          *ngIf="isLoadingFiles"
          [progressPercentage]="progressPercentage"
          [message]="progressMessage">
        </load-progress>

        <mat-list
          *ngIf="!isLoadingFiles && this.traceData.getLoadedTraces().length > 0"
          class="uploaded-files">
          <mat-list-item *ngFor="let trace of this.traceData.getLoadedTraces()">
            <mat-icon matListIcon>
              {{ TRACE_INFO[trace.type].icon }}
            </mat-icon>

            <p matLine>{{ trace.traceFile.file.name }} ({{ TRACE_INFO[trace.type].name }})</p>

            <button color="primary" mat-icon-button (click)="onRemoveTrace($event, trace)">
              <mat-icon>close</mat-icon>
            </button>
          </mat-list-item>
        </mat-list>

        <div *ngIf="!isLoadingFiles && traceData.getLoadedTraces().length === 0" class="drop-info">
          <p class="mat-body-3 icon">
            <mat-icon inline fontIcon="upload"></mat-icon>
          </p>
          <p class="mat-body-1">Drag your .winscope file(s) or click to upload</p>
        </div>
      </mat-card-content>

      <div
        *ngIf="!isLoadingFiles && traceData.getLoadedTraces().length > 0"
        class="trace-actions-container">
        <button
          color="primary"
          mat-raised-button
          class="load-btn"
          (click)="onViewTracesButtonClick()">
          View traces
        </button>

        <button color="primary" mat-stroked-button for="fileDropRef" (click)="fileDropRef.click()">
          Upload another file
        </button>

        <button color="primary" mat-stroked-button (click)="onClearButtonClick()">Clear all</button>
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
      .div-progress {
        display: flex;
        height: 100%;
        flex-direction: column;
        justify-content: center;
        align-content: center;
        align-items: center;
      }
      .div-progress p {
        opacity: 0.6;
      }
      .div-progress mat-icon {
        font-size: 3rem;
        width: unset;
        height: unset;
      }
      .div-progress mat-progress-bar {
        max-width: 250px;
      }
      mat-card-content {
        flex-grow: 1;
      }
    `,
  ],
})
export class UploadTracesComponent implements FilesDownloadListener {
  TRACE_INFO = TRACE_INFO;
  isLoadingFiles = false;
  progressMessage = '';
  progressPercentage?: number;

  @Input() traceData!: TraceData;
  @Output() traceDataLoaded = new EventEmitter<void>();

  constructor(
    @Inject(ChangeDetectorRef) private changeDetectorRef: ChangeDetectorRef,
    @Inject(MatSnackBar) private snackBar: MatSnackBar,
    @Inject(NgZone) private ngZone: NgZone
  ) {}

  ngOnInit() {
    this.traceData.clear();
  }

  public onFilesDownloadStart() {
    this.isLoadingFiles = true;
    this.progressMessage = 'Downloading files...';
    this.progressPercentage = undefined;
    this.changeDetectorRef.detectChanges();
  }

  public async onFilesDownloaded(files: File[]) {
    await this.processFiles(files);
  }

  public async onInputFiles(event: Event) {
    const files = this.getInputFiles(event);
    await this.processFiles(files);
  }

  public onViewTracesButtonClick() {
    this.traceDataLoaded.emit();
  }

  public onClearButtonClick() {
    this.traceData.clear();
    this.changeDetectorRef.detectChanges();
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
    if (!droppedFiles) return;
    await this.processFiles(Array.from(droppedFiles));
  }

  public onRemoveTrace(event: MouseEvent, trace: Trace) {
    event.preventDefault();
    event.stopPropagation();
    this.traceData.removeTrace(trace.type);
    this.changeDetectorRef.detectChanges();
  }

  private async processFiles(files: File[]) {
    const UI_PROGRESS_UPDATE_PERIOD_MS = 200;
    let lastUiProgressUpdate = Date.now();

    const onProgressUpdate = (progress: number) => {
      const now = Date.now();
      if (Date.now() - lastUiProgressUpdate < UI_PROGRESS_UPDATE_PERIOD_MS) {
        // Let's limit the amount of UI updates, because the progress bar component
        // renders weird stuff when updated too frequently
        return;
      }
      lastUiProgressUpdate = now;

      this.progressPercentage = progress;
      this.changeDetectorRef.detectChanges();
    };

    const traceFiles: TraceFile[] = [];
    const onFile: OnFile = (file: File, parentArchive?: File) => {
      traceFiles.push(new TraceFile(file, parentArchive));
    };

    this.isLoadingFiles = true;
    this.progressMessage = 'Unzipping files...';
    this.changeDetectorRef.detectChanges();
    await FileUtils.unzipFilesIfNeeded(files, onFile, onProgressUpdate);

    this.progressMessage = 'Parsing files...';
    this.changeDetectorRef.detectChanges();
    const parserErrors = await this.traceData.loadTraces(traceFiles, onProgressUpdate);

    this.isLoadingFiles = false;
    this.changeDetectorRef.detectChanges();

    ParserErrorSnackBarComponent.showIfNeeded(this.ngZone, this.snackBar, parserErrors);
  }

  private getInputFiles(event: Event): File[] {
    const files: FileList | null = (event?.target as HTMLInputElement)?.files;
    if (!files || !files[0]) {
      return [];
    }
    return Array.from(files);
  }
}
