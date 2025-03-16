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

import { Disposable, Disposer } from '../util/disposer';
import { checkNotNull } from '../util/preconditions';
import { Deferred } from '../util/util';
import { Timeline } from './timeline';

export type VideoSourceState = 'stop' | 'play' | 'seek';

export class VideoSource
  extends EventTarget
  implements VideoSource, Disposable
{
  readonly seekable = true;
  private _videoElement: HTMLVideoElement | null = null;

  constructor(recordingUrl: string, readonly timeline: Timeline) {
    super();
    const videoElement = document.createElement('video');
    videoElement.muted = true;
    videoElement.src = recordingUrl;
    videoElement.addEventListener('timeupdate', (e) => {
      this.dispatchEvent(new Event('timeupdate'));
    }),
      videoElement.addEventListener('resize', (e) =>
        this.dispatchEvent(new Event('metadata-changed'))
      );

    this._videoElement = videoElement;
  }

  drawCurrentFrame(ctx: CanvasRenderingContext2D): void {
    if (!this._videoElement) return;
    ctx.drawImage(
      this._videoElement,
      0,
      0,
      ctx.canvas.width,
      ctx.canvas.height
    );
  }

  get width(): number {
    return this._videoElement?.videoWidth ?? 0;
  }

  get height(): number {
    return this._videoElement?.videoHeight ?? 0;
  }

  get loop(): boolean {
    return this._videoElement?.loop ?? false;
  }

  set loop(value: boolean) {
    checkNotNull(this._videoElement).loop = value;
  }

  get playbackRate(): number {
    return this._videoElement?.playbackRate ?? 1;
  }

  set playbackRate(value: number) {
    checkNotNull(this._videoElement).playbackRate = value;
  }

  get state() {
    if (!this._videoElement) return 'stop';
    if (this._currentSeekPromise) return 'seek';
    if (this._videoElement.paused) return 'stop';
    if (this._videoElement.ended) return 'stop';

    return 'play';
  }

  async play(): Promise<void> {
    this._videoElement?.play();
  }

  async stop(): Promise<void> {
    this._videoElement?.pause();
    this._cancelSeek();
  }

  dispose(): void {
    this._cancelSeek();
    if (this._videoElement) {
      this._videoElement.pause();
      URL.revokeObjectURL(this._videoElement.src);
      this._videoElement.src = '';
    }
  }

  get currentTime() {
    return this._videoElement?.currentTime ?? 0;
  }

  _currentSeekPromise: Deferred<boolean> | null = null;

  async seek(time: number): Promise<boolean> {
    if (!this._videoElement) return false;

    this._cancelSeek();

    if (this._videoElement.currentTime == time) return true;

    const currentSeekPromise = new Deferred<boolean>();
    this._currentSeekPromise = currentSeekPromise;

    const seekSetup = new Disposer();
    seekSetup.addListener(this._videoElement, 'seeked', () => {
      currentSeekPromise.resolve(true);
    });

    this._videoElement.currentTime = time;

    try {
      return await currentSeekPromise;
    } finally {
      seekSetup.dispose();
      if (this._currentSeekPromise == currentSeekPromise) {
        this._currentSeekPromise = null;
      }
    }
  }

  private _cancelSeek() {
    this._currentSeekPromise?.resolve(false);
    this._currentSeekPromise = null;
  }
}
