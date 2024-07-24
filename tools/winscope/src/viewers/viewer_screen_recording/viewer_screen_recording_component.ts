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
import {Component, Inject, Input, SimpleChanges} from '@angular/core';
import {DomSanitizer, SafeUrl} from '@angular/platform-browser';
import {ScreenRecordingTraceEntry} from 'trace/screen_recording';

@Component({
  selector: 'viewer-screen-recording',
  template: `
    <mat-card class="container">
      <mat-card-title class="header">
        <button mat-button class="button-drag" cdkDragHandle>
          <mat-icon class="drag-icon">drag_indicator</mat-icon>
          <span class="mat-body-2">{{ title }}</span>
        </button>

        <button mat-button class="button-minimize" (click)="onMinimizeButtonClick()">
          <mat-icon>
            {{ isMinimized ? 'maximize' : 'minimize' }}
          </mat-icon>
        </button>
      </mat-card-title>
      <div class="video-container" [style.height]="isMinimized ? '0px' : ''">
        <ng-container *ngIf="hasFrameToShow(); then video; else noVideo"> </ng-container>
      </div>
    </mat-card>

    <ng-template #video>
      <video
        *ngIf="hasFrameToShow()"
        [currentTime]="currentTraceEntry.videoTimeSeconds"
        [src]="safeUrl"
        cdkDragHandle></video>
    </ng-template>

    <ng-template #noVideo>
      <img *ngIf="hasImage()" [src]="safeUrl" />

      <div class="no-video" *ngIf="!hasImage()">
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
      video,
      img {
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
  safeUrl: undefined | SafeUrl = undefined;
  isMinimized = false;

  constructor(@Inject(DomSanitizer) private sanitizer: DomSanitizer) {}

  @Input() currentTraceEntry: ScreenRecordingTraceEntry | undefined;
  @Input() title = 'Screen recording';

  ngOnChanges(changes: SimpleChanges) {
    if (this.currentTraceEntry === undefined) {
      return;
    }

    if (!changes['currentTraceEntry']) {
      return;
    }

    if (this.safeUrl === undefined) {
      this.safeUrl = this.sanitizer.bypassSecurityTrustUrl(
        URL.createObjectURL(this.currentTraceEntry.videoData),
      );
    }
  }

  onMinimizeButtonClick() {
    this.isMinimized = !this.isMinimized;
  }

  hasFrameToShow() {
    return (
      !this.currentTraceEntry?.isImage &&
      this.currentTraceEntry?.videoTimeSeconds !== undefined
    );
  }

  hasImage() {
    return this.currentTraceEntry?.isImage ?? false;
  }
}

export {ViewerScreenRecordingComponent};
