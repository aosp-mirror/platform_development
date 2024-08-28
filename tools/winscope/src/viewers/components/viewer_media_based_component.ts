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
  ElementRef,
  HostListener,
  Inject,
  Input,
  NgZone,
  SimpleChanges,
} from '@angular/core';
import {MatSelectChange} from '@angular/material/select';
import {DomSanitizer, SafeUrl} from '@angular/platform-browser';
import {assertDefined} from 'common/assert_utils';
import {Size} from 'common/geometry/size';
import {MediaBasedTraceEntry} from 'trace/media_based_trace_entry';

@Component({
  selector: 'viewer-media-based',
  template: `
  <div class="overlay">
    <mat-card class="container" cdkDrag cdkDragBoundary=".overlay">
      <mat-card-title class="header">
        <button mat-button class="button-drag" cdkDragHandle>
          <mat-icon class="drag-icon">drag_indicator</mat-icon>
        </button>
        <span
          #titleText
          *ngIf="titles.length <= 1"
          class="mat-body-2 overlay-title"
          [matTooltip]="titles.at(index)"
          matTooltipPosition="above"
          [matTooltipShowDelay]="300"
          >{{ titles.at(0)?.split(".")[0] ?? 'Screen recording'}}</span>

        <mat-select
          *ngIf="titles.length > 1"
          class="overlay-title select-title"
          [matTooltip]="titles.at(index)"
          matTooltipPosition="above"
          [matTooltipShowDelay]="300"
          (selectionChange)="onSelectChange($event)"
          [value]="index">
          <mat-option
            *ngFor="let title of titles; index as i"
            [value]="i">
            {{ titles[i].split(".")[0] }}
          </mat-option>
        </mat-select>

        <button mat-button class="button-minimize" [disabled]="forceMinimize" (click)="onMinimizeButtonClick()">
          <mat-icon>
            {{ isMinimized() ? 'maximize' : 'minimize' }}
          </mat-icon>
        </button>
      </mat-card-title>
      <div class="video-container" cdkDragHandle [style.height]="isMinimized() ? '0px' : ''">
        <ng-container *ngIf="hasFrameToShow(); then video; else noVideo"> </ng-container>
      </div>
    </mat-card>

    <ng-template #video>
      <video
        *ngIf="hasFrameToShow()"
        [currentTime]="getCurrentTime()"
        [src]="safeUrl"
        #videoElement></video>
    </ng-template>

    <ng-template #noVideo>
      <img *ngIf="hasImage()" [src]="safeUrl" />

      <div class="no-video" *ngIf="!hasImage()">
        <p class="mat-body-2">No frame to show.</p>
      </div>
    </ng-template>
    </div>
  `,
  styles: [
    `
      .overlay {
        z-index: 30;
        position: fixed;
        top: 0px;
        left: 0px;
        width: 100%;
        height: 100%;
        pointer-events: none;
      }

      .container {
        pointer-events: all;
        width: max(250px, 15vw);
        min-width: 200px;
        resize: horizontal;
        overflow: hidden;
        display: flex;
        flex-direction: column;
        padding: 0;
        left: 80vw;
        top: 20vh;
      }

      .header {
        display: flex;
        flex-direction: row;
        margin: 0px;
        border: 1px solid var(--border-color);
        border-radius: 4px;
        justify-content: space-between;
        align-items: center;
      }

      .button-drag {
        cursor: grab;
        padding: 2px;
        min-width: fit-content;
      }

      .overlay-title {
        overflow: hidden;
        text-overflow: ellipsis;
        font-size: 14px;
        width: unset;
      }

      .select-title {
        display: flex;
        align-items: center;
      }

      .button-minimize {
        flex-grow: 0;
        padding: 2px;
        min-width: 24px;
      }

      .video-container, video, img {
        border: 1px solid var(--default-border);
        width: 100%;
        height: auto;
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
class ViewerMediaBasedComponent {
  safeUrl: undefined | SafeUrl = undefined;
  shouldMinimize = false;
  index = 0;

  constructor(
    @Inject(DomSanitizer) private sanitizer: DomSanitizer,
    @Inject(ElementRef) private elementRef: ElementRef<HTMLElement>,
    @Inject(ChangeDetectorRef) private changeDetectorRef: ChangeDetectorRef,
    @Inject(NgZone) private ngZone: NgZone,
  ) {}

  @Input() currentTraceEntries: MediaBasedTraceEntry[] = [];
  @Input() titles: string[] = [];
  @Input() forceMinimize = false;

  private frameSize: Size = {width: 720, height: 1280}; // default for Flicker
  private frameSizeWorker: number | undefined;

  ngOnChanges(changes: SimpleChanges) {
    this.changeDetectorRef.detectChanges();
    if (this.currentTraceEntries.length === 0) {
      return;
    }

    if (!changes['currentTraceEntries']) {
      return;
    }

    if (this.safeUrl === undefined) {
      this.updateSafeUrl();
    }
  }

  ngAfterViewInit() {
    this.resetFrameSizeWorker();
    this.updateMaxContainerSize();
  }

  ngOnDestroy() {
    this.clearFrameSizeWorker();
  }

  @HostListener('window:resize', ['$event'])
  onResize(event: Event) {
    this.updateMaxContainerSize();
  }

  onMinimizeButtonClick() {
    this.shouldMinimize = !this.shouldMinimize;
  }

  isMinimized() {
    return this.forceMinimize || this.shouldMinimize;
  }

  hasFrameToShow() {
    const curr = this.currentTraceEntries.at(this.index);
    return curr && !curr.isImage && curr.videoTimeSeconds !== undefined;
  }

  hasImage() {
    return this.currentTraceEntries.at(this.index)?.isImage ?? false;
  }

  getCurrentTime(): number {
    return this.currentTraceEntries.at(this.index)?.videoTimeSeconds ?? 0;
  }

  onSelectChange(event: MatSelectChange) {
    this.index = event.value;
    this.updateSafeUrl();
  }

  private resetFrameSizeWorker() {
    if (this.frameSizeWorker === undefined) {
      this.frameSizeWorker = window.setInterval(
        () => this.updateFrameSize(),
        50,
      );
    }
  }

  private updateFrameSize() {
    const video =
      this.elementRef.nativeElement.querySelector<HTMLVideoElement>('video');
    if (video && video.readyState) {
      this.frameSize = {
        width: video.videoWidth,
        height: video.videoHeight,
      };
      this.clearFrameSizeWorker();
      this.updateMaxContainerSize();
      return;
    }
    const image =
      this.elementRef.nativeElement.querySelector<HTMLImageElement>('img');
    if (image) {
      this.frameSize = {
        width: image.naturalWidth,
        height: image.naturalHeight,
      };
      this.clearFrameSizeWorker();
      this.updateMaxContainerSize();
    }
  }

  private updateSafeUrl() {
    const curr = this.currentTraceEntries.at(this.index);
    if (curr) {
      this.safeUrl = this.sanitizer.bypassSecurityTrustUrl(
        URL.createObjectURL(curr.videoData),
      );
      this.resetFrameSizeWorker();
    }
  }

  private updateMaxContainerSize() {
    this.ngZone.run(() => {
      const container = assertDefined(
        this.elementRef.nativeElement.querySelector<HTMLElement>('.container'),
      );
      const maxHeight = window.innerHeight - 140;
      const headerHeight =
        this.elementRef.nativeElement.querySelector('.header')?.clientHeight ??
        0;
      const maxWidth =
        ((maxHeight - headerHeight) * this.frameSize.width) /
        this.frameSize.height;
      container.style.maxWidth = `${maxWidth}px`;
      this.changeDetectorRef.detectChanges();
    });
  }

  private clearFrameSizeWorker() {
    window.clearInterval(this.frameSizeWorker);
    this.frameSizeWorker = undefined;
  }
}

export {ViewerMediaBasedComponent};
