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
import {Component, ElementRef, Inject, Input} from '@angular/core';
import {DomSanitizer, SafeUrl} from '@angular/platform-browser';
import {ScreenRecordingTraceEntry} from 'trace/screen_recording';

@Component({
  selector: 'viewer-screen-recording',
  template: `
    <mat-card class="container">
      <mat-card-title class="header">
        <button mat-button class="button-drag" cdkDragHandle>
          <mat-icon class="drag-icon">drag_indicator</mat-icon>
          <span class="mat-body-2">Screen recording</span>
        </button>

        <button mat-button class="button-minimize" (click)="onMinimizeButtonClick()">
          <mat-icon>
            {{ isMinimized ? 'maximize' : 'minimize' }}
          </mat-icon>
        </button>
      </mat-card-title>
      <div class="video-container" [style.height]="isMinimized ? '0px' : ''">
        <ng-container *ngIf="hasFrameToShow; then video; else noVideo"> </ng-container>
      </div>
    </mat-card>

    <ng-template #video>
      <video
        *ngIf="hasFrameToShow"
        [currentTime]="videoCurrentTime"
        [src]="videoUrl"
        cdkDragHandle></video>
    </ng-template>

    <ng-template #noVideo>
      <div class="no-video">
        <p class="mat-body-2">No screen recording frame to show.</p>
        <p class="mat-body-1">Current timestamp is still before first frame.</p>
      </div>
    </ng-template>
  `,
  styles: [
    `
      .container {
        width: fit-content;
        height: fit-content;
        display: flex;
        flex-direction: column;
        padding: 0;
      }

      .header {
        display: flex;
        flex-direction: row;
        margin: 0px;
        border: 1px solid var(--border-color);
        border-radius: 4px;
      }

      .button-drag {
        flex-grow: 1;
        cursor: grab;
      }

      .drag-icon {
        float: left;
        margin: 5px 0;
      }

      .button-minimize {
        flex-grow: 0;
      }

      .video-container,
      video {
        border: 1px solid var(--default-border);
        max-width: max(250px, 15vw);
        cursor: grab;
        overflow: hidden;
      }

      .no-video {
        padding: 1rem;
        text-align: center;
      }
    `,
  ],
})
class ViewerScreenRecordingComponent {
  constructor(
    @Inject(ElementRef) elementRef: ElementRef,
    @Inject(DomSanitizer) sanitizer: DomSanitizer
  ) {
    this.elementRef = elementRef;
    this.sanitizer = sanitizer;
  }

  @Input()
  set currentTraceEntry(entry: undefined | ScreenRecordingTraceEntry) {
    if (entry === undefined) {
      this.videoCurrentTime = undefined;
      return;
    }

    if (this.videoUrl === undefined) {
      this.videoUrl = this.sanitizer.bypassSecurityTrustUrl(URL.createObjectURL(entry.videoData));
    }

    this.videoCurrentTime = entry.videoTimeSeconds;
  }

  onMinimizeButtonClick() {
    this.isMinimized = !this.isMinimized;
  }

  videoUrl: undefined | SafeUrl = undefined;
  videoCurrentTime: number | undefined = undefined;
  isMinimized = false;

  private elementRef: ElementRef;
  private sanitizer: DomSanitizer;

  get hasFrameToShow() {
    return this.videoCurrentTime !== undefined;
  }
}

export {ViewerScreenRecordingComponent};
