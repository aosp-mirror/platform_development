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
import {Component, ElementRef, Inject, Input} from "@angular/core";
import {DomSanitizer, SafeUrl} from "@angular/platform-browser";
import {ScreenRecordingTraceEntry} from "common/trace/screen_recording";

@Component({
  selector: "viewer-screen-recording",
  template: `
    <div class="container">
      <div class="header">
        <button mat-button class="button-drag" cdkDragHandle>
          <mat-icon class="drag-icon">drag_indicator</mat-icon>
          <span class="mat-body-2">Screen recording</span>
        </button>

        <button mat-button class="button-minimize"
                (click)="onMinimizeButtonClick()">
          <mat-icon>
            {{isMinimized ? "maximize" : "minimize"}}
          </mat-icon>
        </button>
      </div>
      <video
        [currentTime]="videoCurrentTime"
        [src]=videoUrl
        [style.height]="isMinimized ? '0px' : ''"
        cdkDragHandle>
      </video>
    </div>
  `,
  styles: [
    `
      .container {
        width: fit-content;
        height: fit-content;
        display: flex;
        flex-direction: column;
      }

      .header {
        background-color: white;
        border: 1px solid var(--default-border);
        display: flex;
        flex-direction: row;
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

      video {
        width: 15vw;
        cursor: grab;
      }
    `,
  ]
})
class ViewerScreenRecordingComponent {
  constructor(
    @Inject(ElementRef) elementRef: ElementRef,
    @Inject(DomSanitizer) sanitizer: DomSanitizer) {
    this.elementRef = elementRef;
    this.sanitizer = sanitizer;
  }

  @Input()
  public set currentTraceEntry(entry: undefined|ScreenRecordingTraceEntry) {
    if (entry === undefined) {
      return;
    }

    if (this.videoUrl === undefined) {
      this.videoUrl = this.sanitizer.bypassSecurityTrustUrl(URL.createObjectURL(entry.videoData));
    }

    this.videoCurrentTime = entry.videoTimeSeconds;
  }

  public onMinimizeButtonClick() {
    this.isMinimized = !this.isMinimized;
  }

  public videoUrl: undefined|SafeUrl = undefined;
  public videoCurrentTime = 0;
  public isMinimized = false;

  private elementRef: ElementRef;
  private sanitizer: DomSanitizer;
}

export {ViewerScreenRecordingComponent};
