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
  EventEmitter,
  HostListener,
  Input,
  Output,
  SimpleChanges,
  ViewChild,
} from '@angular/core';
import {TimelineData} from 'app/timeline_data';
import {assertDefined} from 'common/assert_utils';
import {TimeRange, Timestamp} from 'common/time';
import {TimeUtils} from 'common/time_utils';
import {Traces} from 'trace/traces';
import {TracePosition} from 'trace/trace_position';
import {TraceType, TraceTypeUtils} from 'trace/trace_type';
import {MiniTimelineDrawer} from './drawer/mini_timeline_drawer';
import {MiniTimelineDrawerImpl} from './drawer/mini_timeline_drawer_impl';
import {MiniTimelineDrawerInput} from './drawer/mini_timeline_drawer_input';
import {Transformer} from './transformer';

@Component({
  selector: 'mini-timeline',
  template: `
    <div id="mini-timeline-wrapper" #miniTimelineWrapper>
      <canvas #canvas id="mini-timeline-canvas"></canvas>
      <div class="zoom-control-wrapper">
        <div class="zoom-control">
          <div class="zoom-buttons">
            <button mat-icon-button id="reset-zoom-btn" (click)="resetZoom()">
              <mat-icon>refresh</mat-icon>
            </button>
            <button mat-icon-button id="zoom-in-btn" (click)="zoomIn()">
              <mat-icon>zoom_in</mat-icon>
            </button>
            <button mat-icon-button id="zoom-out-btn" (click)="zoomOut()">
              <mat-icon>zoom_out</mat-icon>
            </button>
          </div>
          <slider
            [fullRange]="timelineData.getFullTimeRange()"
            [zoomRange]="timelineData.getZoomRange()"
            [currentPosition]="timelineData.getCurrentPosition()"
            (onZoomChanged)="onZoomChanged($event)"></slider>
        </div>
      </div>
    </div>
  `,
  styles: [
    `
      #mini-timeline-wrapper {
        width: 100%;
        min-height: 5em;
        height: 100%;
      }
      .zoom-control-wrapper {
        margin-top: -25px;
        margin-left: -60px;
        padding-right: 30px;
      }
      .zoom-control {
        display: flex;
        align-items: center;
        justify-content: space-between;
        justify-items: center;
        gap: 23px;
      }
      .zoom-control slider {
        flex-grow: 1;
      }
    `,
  ],
})
export class MiniTimelineComponent {
  @Input() timelineData!: TimelineData;
  @Input() currentTracePosition!: TracePosition;
  @Input() selectedTraces!: TraceType[];

  @Output() readonly onTracePositionUpdate = new EventEmitter<TracePosition>();
  @Output() readonly onSeekTimestampUpdate = new EventEmitter<
    Timestamp | undefined
  >();

  @ViewChild('miniTimelineWrapper', {static: false})
  miniTimelineWrapper!: ElementRef;
  @ViewChild('canvas', {static: false}) canvasRef!: ElementRef;
  get canvas(): HTMLCanvasElement {
    return this.canvasRef.nativeElement;
  }

  drawer: MiniTimelineDrawer | undefined = undefined;

  ngAfterViewInit(): void {
    this.makeHiPPICanvas();

    const updateTimestampCallback = (timestamp: Timestamp) => {
      this.onSeekTimestampUpdate.emit(undefined);
      this.onTracePositionUpdate.emit(
        this.timelineData.makePositionFromActiveTrace(timestamp),
      );
    };

    this.drawer = new MiniTimelineDrawerImpl(
      this.canvas,
      () => this.getMiniCanvasDrawerInput(),
      (position) => this.onSeekTimestampUpdate.emit(position),
      updateTimestampCallback,
      updateTimestampCallback,
    );
    this.drawer.draw();
  }

  ngOnChanges(changes: SimpleChanges) {
    if (this.drawer !== undefined) {
      this.drawer.draw();
    }
  }

  isZoomed(): boolean {
    const fullRange = this.timelineData.getFullTimeRange();
    const zoomRange = this.timelineData.getZoomRange();
    return fullRange.from !== zoomRange.from || fullRange.to !== zoomRange.to;
  }

  private getMiniCanvasDrawerInput() {
    return new MiniTimelineDrawerInput(
      this.timelineData.getFullTimeRange(),
      this.currentTracePosition.timestamp,
      this.timelineData.getSelectionTimeRange(),
      this.timelineData.getZoomRange(),
      this.getTracesToShow(),
      this.timelineData,
    );
  }

  getTracesToShow(): Traces {
    const traces = new Traces();
    this.selectedTraces
      .filter(
        (type) => this.timelineData.getTraces().getTrace(type) !== undefined,
      )
      .sort((a, b) => TraceTypeUtils.compareByDisplayOrder(b, a)) // reversed to ensure display is ordered top to bottom
      .forEach((type) => {
        traces.setTrace(
          type,
          assertDefined(this.timelineData.getTraces().getTrace(type)),
        );
      });
    return traces;
  }

  private makeHiPPICanvas() {
    // Reset any size before computing new size to avoid it interfering with size computations
    this.canvas.width = 0;
    this.canvas.height = 0;
    this.canvas.style.width = 'auto';
    this.canvas.style.height = 'auto';

    const width = this.miniTimelineWrapper.nativeElement.offsetWidth;
    const height = this.miniTimelineWrapper.nativeElement.offsetHeight;

    const HiPPIwidth = window.devicePixelRatio * width;
    const HiPPIheight = window.devicePixelRatio * height;

    this.canvas.width = HiPPIwidth;
    this.canvas.height = HiPPIheight;
    this.canvas.style.width = width + 'px';
    this.canvas.style.height = height + 'px';

    // ensure all drawing operations are scaled
    if (window.devicePixelRatio !== 1) {
      const context = this.canvas.getContext('2d')!;
      context.scale(window.devicePixelRatio, window.devicePixelRatio);
    }
  }

  @HostListener('window:resize', ['$event'])
  onResize(event: Event) {
    this.makeHiPPICanvas();
    this.drawer?.draw();
  }

  onZoomChanged(zoom: TimeRange) {
    this.timelineData.setZoom(zoom);
    this.timelineData.setSelectionTimeRange(zoom);
    this.drawer?.draw();
  }

  resetZoom() {
    this.onZoomChanged(this.timelineData.getFullTimeRange());
  }

  zoomIn(zoomOn?: Timestamp) {
    this.zoom({nominator: 3n, denominator: 4n}, zoomOn);
  }

  zoomOut(zoomOn?: Timestamp) {
    this.zoom({nominator: 5n, denominator: 4n}, zoomOn);
  }

  zoom(
    zoomRatio: {nominator: bigint; denominator: bigint},
    zoomOn?: Timestamp,
  ) {
    const fullRange = this.timelineData.getFullTimeRange();
    const currentZoomRange = this.timelineData.getZoomRange();
    const currentZoomWidth = currentZoomRange.to.minus(currentZoomRange.from);
    const zoomToWidth = currentZoomWidth
      .times(zoomRatio.nominator)
      .div(zoomRatio.denominator);

    const cursorPosition = this.timelineData.getCurrentPosition()?.timestamp;
    const currentMiddle = currentZoomRange.from
      .plus(currentZoomRange.to)
      .div(2n);

    let newFrom: Timestamp;
    let newTo: Timestamp;
    if (zoomOn === undefined) {
      let zoomTowards = currentMiddle;
      if (cursorPosition !== undefined && cursorPosition.in(currentZoomRange)) {
        zoomTowards = cursorPosition;
      }

      let leftAdjustment;
      let rightAdjustment;
      if (zoomTowards.getValueNs() < currentMiddle.getValueNs()) {
        leftAdjustment = currentZoomWidth.times(0n);
        rightAdjustment = currentZoomWidth
          .times(zoomRatio.denominator - zoomRatio.nominator)
          .div(zoomRatio.denominator);
      } else {
        leftAdjustment = currentZoomWidth
          .times(zoomRatio.denominator - zoomRatio.nominator)
          .div(zoomRatio.denominator);
        rightAdjustment = currentZoomWidth.times(0n);
      }

      newFrom = currentZoomRange.from.plus(leftAdjustment);
      newTo = currentZoomRange.to.minus(rightAdjustment);
      const newMiddle = newFrom.plus(newTo).div(2n);

      if (
        (zoomTowards.getValueNs() <= currentMiddle.getValueNs() &&
          newMiddle.getValueNs() < zoomTowards.getValueNs()) ||
        (zoomTowards.getValueNs() >= currentMiddle.getValueNs() &&
          newMiddle.getValueNs() > zoomTowards.getValueNs())
      ) {
        // Moved past middle, so ensure cursor is in the middle
        newFrom = zoomTowards.minus(zoomToWidth.div(2n));
        newTo = zoomTowards.plus(zoomToWidth.div(2n));
      }
    } else {
      newFrom = zoomOn.minus(zoomToWidth.div(2n));
      newTo = zoomOn.plus(zoomToWidth.div(2n));
    }

    if (newFrom.getValueNs() < fullRange.from.getValueNs()) {
      newTo = TimeUtils.min(
        fullRange.to,
        newTo.plus(fullRange.from.minus(newFrom)),
      );
      newFrom = fullRange.from;
    }

    if (newTo.getValueNs() > fullRange.to.getValueNs()) {
      newFrom = TimeUtils.max(
        fullRange.from,
        newFrom.minus(newTo.minus(fullRange.to)),
      );
      newTo = fullRange.to;
    }

    this.onZoomChanged({
      from: newFrom,
      to: newTo,
    });
  }

  // -1 for x direction, 1 for y direction
  private lastMoves: WheelEvent[] = [];
  @HostListener('wheel', ['$event'])
  onScroll(event: WheelEvent) {
    this.lastMoves.push(event);
    setTimeout(() => this.lastMoves.shift(), 1000);

    const xMoveAmount = this.lastMoves.reduce(
      (accumulator, it) => accumulator + it.deltaX,
      0,
    );
    const yMoveAmount = this.lastMoves.reduce(
      (accumulator, it) => accumulator + it.deltaY,
      0,
    );

    let moveDirection: 'x' | 'y';
    if (Math.abs(yMoveAmount) > Math.abs(xMoveAmount)) {
      moveDirection = 'y';
    } else {
      moveDirection = 'x';
    }

    if (
      (event.target as any)?.id === 'mini-timeline-canvas' &&
      event.deltaY !== 0 &&
      moveDirection === 'y'
    ) {
      // Zooming
      const canvas = event.target as HTMLCanvasElement;
      const xPosInCanvas = event.x - canvas.offsetLeft;
      const zoomRange = this.timelineData.getZoomRange();

      const zoomTo = new Transformer(
        zoomRange,
        assertDefined(this.drawer).usableRange,
      ).untransform(xPosInCanvas);

      if (event.deltaY < 0) {
        this.zoomIn(zoomTo);
      } else {
        this.zoomOut(zoomTo);
      }
    }

    if (event.deltaX !== 0 && moveDirection === 'x') {
      // Horizontal scrolling
      const scrollAmount = event.deltaX;
      const fullRange = this.timelineData.getFullTimeRange();
      const zoomRange = this.timelineData.getZoomRange();

      const usableRange = assertDefined(this.drawer).usableRange;
      const transformer = new Transformer(zoomRange, usableRange);
      const shiftAmount = transformer
        .untransform(usableRange.from + scrollAmount)
        .minus(zoomRange.from);
      let newFrom = zoomRange.from.plus(shiftAmount);
      let newTo = zoomRange.to.plus(shiftAmount);

      if (newFrom.getValueNs() < fullRange.from.getValueNs()) {
        newTo = newTo.plus(fullRange.from.minus(newFrom));
        newFrom = fullRange.from;
      }

      if (newTo.getValueNs() > fullRange.to.getValueNs()) {
        newFrom = newFrom.minus(newTo.minus(fullRange.to));
        newTo = fullRange.to;
      }

      this.onZoomChanged({
        from: newFrom,
        to: newTo,
      });
    }
  }
}
