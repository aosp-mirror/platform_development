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
  AfterViewChecked,
  AfterViewInit,
  Component,
  ElementRef,
  Input,
  NgZone,
  OnChanges,
  OnDestroy,
  QueryList,
  SimpleChanges,
  ViewChild,
  ViewChildren,
  ViewContainerRef,
} from '@angular/core';
import { checkNotNull } from '../../utils/preconditions';
import {
  CdkDrag,
  CdkDragRelease,
  CdkDragStart,
  DragRef,
  Point,
} from '@angular/cdk/drag-drop';
import { GraphComponent } from './graph/graph.component';
import { VisualTimeline } from '../visual-timeline';
import { Disposer } from '../../utils/disposer';
import { RecordedMotion } from '../recorded-motion';
import { VideoSource } from '../video-source';
import { VideoControlsComponent } from '../video-controls/video-controls.component';
import { Feature } from '../feature';

@Component({
  selector: 'app-timeline-view',
  standalone: true,
  imports: [GraphComponent, VideoControlsComponent, CdkDrag],

  templateUrl: './timeline-view.component.html',
  styleUrls: ['./timeline-view.component.scss'],
})
export class TimelineViewComponent
  implements AfterViewInit, AfterViewChecked, OnDestroy, OnChanges
{
  private _recordingInputDisposer = new Disposer(true);

  constructor(
    private readonly viewRef: ViewContainerRef,
    private zone: NgZone,
  ) {}

  ngOnDestroy(): void {
    this._recordingInputDisposer.dispose();
  }

  private _observer = new ResizeObserver(() => {
    this.zone.run(() => {
      this._updateCanvasSize();
    });
  });

  @ViewChildren(GraphComponent) graphs!: QueryList<GraphComponent>;

  @ViewChild('canvas')
  canvas!: ElementRef<HTMLCanvasElement>;

  @Input()
  recordedMotion: RecordedMotion | undefined;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['recordedMotion']) {
      this.onRecordedMotionChanged();
    }
  }

  get videoSource(): VideoSource | undefined {
    return this.recordedMotion?.videoSource;
  }
  features: Feature[] = [];

  onRecordedMotionChanged() {
    this._recordingInputDisposer.dispose();

    if (this.recordedMotion) {
      this.visualTimeline = new VisualTimeline(
        this.canvas?.nativeElement?.width ?? 1,
        this.recordedMotion.timeline,
      );

      function recursiveFeatures(feature: Feature): Feature[] {
        return [feature, ...feature.subFeatures.flatMap(it => recursiveFeatures(it))];
      }

      this.features = this.recordedMotion.features.flatMap((it) =>
        recursiveFeatures(it),
      );


      this._recordingInputDisposer.addListener(
        this.recordedMotion.videoSource,
        'timeupdate',
        () => this._updatePlayHead(),
      );
    } else {
      this.visualTimeline = undefined;
      this.features = [];
    }

    this._scheduleRender();
  }

  visualTimeline?: VisualTimeline;

  ngAfterViewInit() {
    this._observer.observe(this.viewRef.element.nativeElement);
    this.graphs.changes.subscribe((r) => {
      const width = this.canvas.nativeElement.width;
      this.graphs.forEach((graph) => graph.updateCanvasSize(width));
    });
    this._updateCanvasSize();
  }

  timeHandlePosition = { x: 0, y: 0 };

  _isPlaying = false;

  ngAfterViewChecked() {
    const isPlaying = this.videoSource?.state === 'play';
    if (isPlaying == this._isPlaying) return;

    this._isPlaying = isPlaying;
    if (isPlaying) {
      const self = this;
      function continuouslyUpdatePlayhead() {
        self._updatePlayHead();
        if (self._isPlaying) requestAnimationFrame(continuouslyUpdatePlayhead);
      }
      requestAnimationFrame(continuouslyUpdatePlayhead);
    }
  }

  private _updatePlayHead() {
    if (!this.visualTimeline || !this.videoSource) return;
    const playheadX = this.visualTimeline.timeToPxClamped(
      this.videoSource.currentTime,
    );
    if (isFinite(playheadX)) {
      this.timeHandlePosition = { x: playheadX, y: 0 };
    }
  }

  private _wasPlayingBeforeDrag = false;
  onDragTimeHandleStart(event: CdkDragStart) {
    this._wasPlayingBeforeDrag = this.videoSource?.state == 'play';
    if (this._wasPlayingBeforeDrag) {
      this.videoSource?.stop();
    }
  }

  computeTimeHandleSnap = (
    pos: Point,
    dragRef: DragRef,
    dimensions: ClientRect,
    pickupPositionInElement: Point,
  ) => {
    if (!this.visualTimeline) return { x: 0, y: 0 };

    const canvasBounds = this.canvas.nativeElement.getBoundingClientRect();

    let frame = this.visualTimeline.pxToFrame(pos.x - canvasBounds.x);

    if (frame === Number.NEGATIVE_INFINITY) frame = 0;
    else if (frame === Number.POSITIVE_INFINITY)
      frame = this.visualTimeline.timeline.frameCount;

    if (this.videoSource) {
      this.videoSource.seek(this.visualTimeline.timeline.frameToTime(frame));
    }

    return {
      x: canvasBounds.x + this.visualTimeline.frameToPx(frame) - 5,
      y: dimensions.y,
    };
  };

  onDragTimeHandleEnd(event: CdkDragRelease) {
    if (this._wasPlayingBeforeDrag) {
      this.videoSource?.play();
    }
  }

  private _updateCanvasSize() {
    const canvasElement = this.canvas.nativeElement;
    const parentElement = checkNotNull(this.canvas.nativeElement.parentElement);
    const height = parentElement.clientHeight;
    const width = parentElement.clientWidth;
    if (canvasElement.width == width && canvasElement.height == height) {
      return;
    }

    canvasElement.width = width;
    canvasElement.height = height;
    if (this.visualTimeline) {
      this.visualTimeline.width = width;
    }
    this._render();
    this._updatePlayHead();

    this.graphs.forEach((graph) => graph.updateCanvasSize(width));
  }

  private _scheduledRender?: number;
  private _scheduleRender() {
    if (this._scheduledRender) return;

    this._scheduledRender = requestAnimationFrame(() => {
      this._render();
      this._scheduledRender = undefined;
    });
  }

  private _render() {
    const ctx = checkNotNull(this.canvas.nativeElement.getContext('2d'));
    const { width, height } = ctx.canvas;

    const minMinorGap = 10;
    const minMajorGap = 50;

    ctx.clearRect(0, 0, width, height);
    if (!this.recordedMotion) return;

    const timeline = this.recordedMotion.timeline;
    const framesCount = timeline.frameCount;

    const maxMinorTicks = Math.min(
      Math.floor(width / minMinorGap),
      framesCount,
    );

    const minorGap = width / maxMinorTicks;

    ctx.beginPath();
    for (let x = 0.5 + minorGap; x <= width; x += minorGap) {
      // Adding the gap skips the initial line at 0
      const xr = Math.round(x);
      let nx;

      if (xr >= x) {
        nx = xr - 0.5;
      } else {
        nx = xr + 0.5;
      }
      ctx.moveTo(nx, 0);
      ctx.lineTo(nx, height - 20);
    }

    ctx.strokeStyle = '#EEEEEE';
    ctx.lineWidth = 1;
    ctx.stroke();

    const majorGap = Math.max(2, Math.floor(minMajorGap / minorGap)) * minorGap;

    ctx.strokeStyle = '#DDDDDD';
    ctx.fillStyle = '#222222';
    ctx.lineWidth = 2;

    ctx.beginPath();
    for (let x = majorGap; x < width; x += majorGap) {
      // Adding the gap skips the initial line at 0
      const xr = Math.round(x);

      ctx.moveTo(xr, 0);
      ctx.lineTo(xr, height - 15);

      const frameNo = Math.floor((x / width) * framesCount);
      const frameLabel = timeline.frameLabels[frameNo];

      ctx.textAlign = 'center';
      ctx.fillText(frameLabel, xr, height - 5);
    }

    // Always draw start
    ctx.moveTo(1, 0);
    ctx.lineTo(1, height - 15);

    // Always draw end
    ctx.moveTo(width - 1, 0);
    ctx.lineTo(width - 1, height - 15);

    ctx.stroke();
  }
}
