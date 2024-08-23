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
  ElementRef,
  HostListener,
  Inject,
  Input,
  SimpleChanges,
} from '@angular/core';
import {DomSanitizer, SafeUrl} from '@angular/platform-browser';
import {MediaBasedTraceEntry} from 'trace/media_based_trace_entry';

@Component({
  selector: 'viewer-media-based',
  template: `
  <div class="overlay">
    <mat-card class="container" cdkDrag cdkDragBoundary=".overlay">
      <mat-card-title class="header">
        <button mat-button class="button-drag" cdkDragHandle>
          <mat-icon class="drag-icon">drag_indicator</mat-icon>
          <span class="mat-body-2">{{ title }}</span>
        </button>

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
        [class.ready]="videoElement.readyState"
        [currentTime]="currentTraceEntry.videoTimeSeconds"
        [src]="safeUrl"
        #videoElement></video>
    </ng-template>

    <ng-template #noVideo>
      <img *ngIf="hasImage()" [src]="safeUrl" />

      <div class="no-video" *ngIf="!hasImage()">
        <p class="mat-body-2">No screen recording frame to show.</p>
        <p class="mat-body-1">Current timestamp is still before first frame.</p>
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
        min-width: 165px;
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
      }

      .button-drag {
        flex-grow: 1;
        cursor: grab;
        padding: 2px;
      }

      .drag-icon {
        float: left;
        margin: 5px 0;
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

  constructor(
    @Inject(DomSanitizer) private sanitizer: DomSanitizer,
    @Inject(ElementRef) private elementRef: ElementRef,
  ) {}

  @Input() currentTraceEntry: MediaBasedTraceEntry | undefined;
  @Input() title = 'Screen recording';
  @Input() forceMinimize = false;

  private frameHeight = 1280; // default for Flicker/Winscope
  private frameWidth = 720; // default for Flicker/Winscope

  private videoObserver: MutationObserver | undefined;

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

  async ngAfterViewInit() {
    const video: HTMLVideoElement | null =
      this.elementRef.nativeElement.querySelector('video');

    if (video) {
      const config = {
        attributes: true,
        CharacterData: true,
      };

      const videoCallback = (
        mutations: MutationRecord[],
        observer: MutationObserver,
      ) => {
        for (const mutation of mutations) {
          if (
            mutation.type === 'attributes' &&
            mutation.attributeName === 'class'
          ) {
            if (video?.className.includes('ready')) {
              this.frameHeight = video?.videoHeight;
              this.frameWidth = video?.videoWidth;
              observer.disconnect();
            }
          }
        }
      };

      this.videoObserver = new MutationObserver(videoCallback);
      this.videoObserver.observe(video, config);
    } else {
      const image: HTMLImageElement | null =
        this.elementRef.nativeElement.querySelector('img');
      if (image) {
        this.frameHeight = image.naturalHeight;
        this.frameWidth = image.naturalWidth;
      }
    }

    this.updateMaxContainerSize();
  }

  ngOnDestroy() {
    this.videoObserver?.disconnect();
  }

  @HostListener('window:resize', ['$event'])
  onResize(event: Event) {
    this.updateMaxContainerSize();
  }

  updateMaxContainerSize() {
    const container = this.elementRef.nativeElement.querySelector('.container');
    const maxHeight = window.innerHeight - 140;
    const headerHeight =
      this.elementRef.nativeElement.querySelector('.header').clientHeight;
    const maxWidth =
      ((maxHeight - headerHeight) * this.frameWidth) / this.frameHeight;
    container.style.maxWidth = `${maxWidth}px`;
  }

  onMinimizeButtonClick() {
    this.shouldMinimize = !this.shouldMinimize;
  }

  isMinimized() {
    return this.forceMinimize || this.shouldMinimize;
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

export {ViewerMediaBasedComponent};
