/*
 * Copyright 2024 Google LLC
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
  AfterViewInit,
  Component,
  ElementRef,
  Input,
  NgZone,
  OnDestroy,
  ViewChild,
} from '@angular/core';
import { VideoSource } from '../video-source';
import { Disposer } from '../../utils/disposer';
import { checkNotNull } from '../../utils/preconditions';

@Component({
  selector: 'app-video-view',
  standalone: true,
  templateUrl: './video-view.component.html',
  styleUrls: ['./video-view.component.scss'],
})
export class VideoViewComponent implements AfterViewInit, OnDestroy {
  private _source?: VideoSource;
  private _sourceDisposer = new Disposer(true);

  private _observerDisposer = new Disposer(true);
  private _observer = new ResizeObserver(() => {
    this.zone.run(() => {
      this.updateVideoSize();
    });
  });

  constructor(private zone: NgZone) {}

  private _container?: ElementRef<HTMLDivElement>;

  @ViewChild('container', { read: ElementRef })
  set container(value: ElementRef<HTMLDivElement> | undefined) {
    this._observerDisposer.dispose();

    this._container = value;

    if (this._container) {
      const containerElement = this._container.nativeElement;
      this._observer.observe(containerElement);
      this._observerDisposer.addFunction(() => {
        this._observer.unobserve(containerElement);
      });
      this.updateVideoSize();
    }
  }

  @ViewChild('canvas', { read: ElementRef })
  canvas?: ElementRef<HTMLCanvasElement>;

  @Input()
  set source(newSource: VideoSource | undefined) {
    if (this._source === newSource) return;

    this._sourceDisposer.dispose();
    this._source = newSource;
    this.updateVideoSize();

    if (newSource) {
      newSource.play();
      this._sourceDisposer.addListener(newSource, `metadata-changed`, () => {
        this.updateVideoSize();
      });
      requestAnimationFrame(() => this._onFrameAvailable());
    }
  }

  get source() {
    return this._source;
  }

  ngAfterViewInit(): void {
    this.updateVideoSize();
  }

  ngOnDestroy() {
    this._observerDisposer.dispose();
  }

  updateVideoSize() {
    if (!this.canvas) return;
    const canvasElement = this.canvas.nativeElement;
    if (!this._container || !this._source) {
      canvasElement.width = canvasElement.height = 0;
      return;
    }

    // Determine the scaling ratio for both, witdth and height, to fit the video into the container.
    const widthScale =
      this._container.nativeElement.clientWidth / this._source.width;
    const heightScale =
      this._container.nativeElement.clientHeight / this._source.height;
    // Pick the smaller scale factor to ensure both width and height will fit, however do not allow
    // scaling up.
    const scale = Math.min(widthScale, heightScale, 1);

    canvasElement.width = this._source.width * scale;
    canvasElement.height = this._source.height * scale;

    this._source.drawCurrentFrame(checkNotNull(canvasElement.getContext('2d')));
  }

  _onFrameAvailable() {
    if (!this._source) return;

    const canvasElement = this.canvas?.nativeElement?.getContext('2d');
    this._source.drawCurrentFrame(checkNotNull(canvasElement));
    requestAnimationFrame(() => this._onFrameAvailable());
  }
}
