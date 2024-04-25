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
import {Color} from 'app/colors';
import {TimelineData} from 'app/timeline_data';
import {assertDefined} from 'common/assert_utils';
import {PersistentStore} from 'common/persistent_store';
import {TimeRange, Timestamp} from 'common/time';
import {TimestampUtils} from 'common/timestamp_utils';
import {Analytics} from 'logging/analytics';
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
          <div class="zoom-buttons" [style.background-color]="getZoomButtonsBackgroundColor()">
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
            [timestampConverter]="timelineData.getTimestampConverter()"
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
        margin-left: -80px;
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
      .zoom-buttons {
        z-index: 20;
        position: relative;
        left: 120px;
        display: flex;
      }
      .zoom-buttons .mat-icon-button {
        width: 28px;
      }
    `,
  ],
})
export class MiniTimelineComponent {
  @Input() timelineData: TimelineData | undefined;
  @Input() currentTracePosition: TracePosition | undefined;
  @Input() selectedTraces: TraceType[] | undefined;
  @Input() initialZoom: TimeRange | undefined;
  @Input() expandedTimelineScrollEvent: WheelEvent | undefined;
  @Input() store: PersistentStore | undefined;

  @Output() readonly onTracePositionUpdate = new EventEmitter<TracePosition>();
  @Output() readonly onSeekTimestampUpdate = new EventEmitter<
    Timestamp | undefined
  >();

  @ViewChild('miniTimelineWrapper', {static: false})
  miniTimelineWrapper: ElementRef | undefined;
  @ViewChild('canvas', {static: false}) canvasRef: ElementRef | undefined;

  getCanvas(): HTMLCanvasElement {
    return assertDefined(this.canvasRef).nativeElement;
  }

  drawer: MiniTimelineDrawer | undefined = undefined;
  private lastMoves: WheelEvent[] = [];

  ngAfterViewInit(): void {
    this.makeHiPPICanvas();

    const updateTimestampCallback = (timestamp: Timestamp) => {
      this.onSeekTimestampUpdate.emit(undefined);
      this.onTracePositionUpdate.emit(
        assertDefined(this.timelineData).makePositionFromActiveTrace(timestamp),
      );
    };

    this.drawer = new MiniTimelineDrawerImpl(
      this.getCanvas(),
      () => this.getMiniCanvasDrawerInput(),
      (position) => this.onSeekTimestampUpdate.emit(position),
      updateTimestampCallback,
      updateTimestampCallback,
    );

    if (this.initialZoom !== undefined) {
      this.onZoomChanged(this.initialZoom);
    } else {
      this.resetZoom();
    }
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['expandedTimelineScrollEvent']?.currentValue !== undefined) {
      const event = changes['expandedTimelineScrollEvent'].currentValue;
      const moveDirection = this.getMoveDirection(event);

      if (event.deltaY !== 0 && moveDirection === 'y') {
        this.updateZoomByScrollEvent(event);
      }

      if (event.deltaX !== 0 && moveDirection === 'x') {
        this.updateHorizontalScroll(event);
      }
    } else if (this.drawer !== undefined) {
      this.drawer.draw();
    }
  }

  getZoomButtonsBackgroundColor(): string {
    return this.store?.get('dark-mode') === 'true'
      ? Color.APP_BACKGROUND_DARK_MODE
      : Color.APP_BACKGROUND_LIGHT_MODE;
  }

  getTracesToShow(): Traces {
    const traces = new Traces();
    const timelineData = assertDefined(this.timelineData);
    assertDefined(this.selectedTraces)
      .filter((type) => timelineData.getTraces().getTrace(type) !== undefined)
      .sort((a, b) => TraceTypeUtils.compareByDisplayOrder(b, a)) // reversed to ensure display is ordered top to bottom
      .forEach((type) => {
        traces.setTrace(
          type,
          assertDefined(timelineData.getTraces().getTrace(type)),
        );
      });
    return traces;
  }

  @HostListener('window:resize', ['$event'])
  onResize(event: Event) {
    this.makeHiPPICanvas();
    this.drawer?.draw();
  }

  onZoomChanged(zoom: TimeRange) {
    const timelineData = assertDefined(this.timelineData);
    timelineData.setZoom(zoom);
    timelineData.setSelectionTimeRange(zoom);
    this.drawer?.draw();
  }

  resetZoom() {
    Analytics.Navigation.logZoom('reset');
    this.onZoomChanged(assertDefined(this.timelineData).getFullTimeRange());
  }

  zoomIn(zoomOn?: Timestamp) {
    Analytics.Navigation.logZoom(this.getZoomSource(zoomOn), 'in');
    this.zoom({nominator: 3n, denominator: 4n}, zoomOn);
  }

  zoomOut(zoomOn?: Timestamp) {
    Analytics.Navigation.logZoom(this.getZoomSource(zoomOn), 'out');
    this.zoom({nominator: 5n, denominator: 4n}, zoomOn);
  }

  zoom(
    zoomRatio: {nominator: bigint; denominator: bigint},
    zoomOn?: Timestamp,
  ) {
    const timelineData = assertDefined(this.timelineData);
    const fullRange = timelineData.getFullTimeRange();
    const currentZoomRange = timelineData.getZoomRange();
    const currentZoomWidth = currentZoomRange.to.minus(
      currentZoomRange.from.getValueNs(),
    );
    const zoomToWidth = currentZoomWidth
      .times(zoomRatio.nominator)
      .div(zoomRatio.denominator);

    const cursorPosition = timelineData.getCurrentPosition()?.timestamp;
    const currentMiddle = currentZoomRange.from
      .add(currentZoomRange.to.getValueNs())
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

      newFrom = currentZoomRange.from.add(leftAdjustment.getValueNs());
      newTo = currentZoomRange.to.minus(rightAdjustment.getValueNs());
      const newMiddle = newFrom.add(newTo.getValueNs()).div(2n);

      if (
        (zoomTowards.getValueNs() <= currentMiddle.getValueNs() &&
          newMiddle.getValueNs() < zoomTowards.getValueNs()) ||
        (zoomTowards.getValueNs() >= currentMiddle.getValueNs() &&
          newMiddle.getValueNs() > zoomTowards.getValueNs())
      ) {
        // Moved past middle, so ensure cursor is in the middle
        newFrom = zoomTowards.minus(zoomToWidth.div(2n).getValueNs());
        newTo = zoomTowards.add(zoomToWidth.div(2n).getValueNs());
      }
    } else {
      newFrom = zoomOn.minus(zoomToWidth.div(2n).getValueNs());
      newTo = zoomOn.add(zoomToWidth.div(2n).getValueNs());
    }

    if (newFrom.getValueNs() < fullRange.from.getValueNs()) {
      newTo = TimestampUtils.min(
        fullRange.to,
        newTo.add(fullRange.from.minus(newFrom.getValueNs()).getValueNs()),
      );
      newFrom = fullRange.from;
    }

    if (newTo.getValueNs() > fullRange.to.getValueNs()) {
      newFrom = TimestampUtils.max(
        fullRange.from,
        newFrom.minus(newTo.minus(fullRange.to.getValueNs()).getValueNs()),
      );
      newTo = fullRange.to;
    }

    this.onZoomChanged({
      from: newFrom,
      to: newTo,
    });
  }

  @HostListener('wheel', ['$event'])
  onScroll(event: WheelEvent) {
    const moveDirection = this.getMoveDirection(event);

    if (
      (event.target as any)?.id === 'mini-timeline-canvas' &&
      event.deltaY !== 0 &&
      moveDirection === 'y'
    ) {
      this.updateZoomByScrollEvent(event);
    }

    if (event.deltaX !== 0 && moveDirection === 'x') {
      this.updateHorizontalScroll(event);
    }
  }

  private getZoomSource(zoomOn?: Timestamp): 'scroll' | 'button' {
    if (zoomOn === undefined) {
      return 'button';
    }

    return 'scroll';
  }

  private getMiniCanvasDrawerInput() {
    const timelineData = assertDefined(this.timelineData);
    return new MiniTimelineDrawerInput(
      timelineData.getFullTimeRange(),
      assertDefined(this.currentTracePosition).timestamp,
      timelineData.getSelectionTimeRange(),
      timelineData.getZoomRange(),
      this.getTracesToShow(),
      timelineData,
    );
  }

  private makeHiPPICanvas() {
    // Reset any size before computing new size to avoid it interfering with size computations
    const canvas = this.getCanvas();
    canvas.width = 0;
    canvas.height = 0;
    canvas.style.width = 'auto';
    canvas.style.height = 'auto';

    const miniTimelineWrapper = assertDefined(this.miniTimelineWrapper);
    const width = miniTimelineWrapper.nativeElement.offsetWidth;
    const height = miniTimelineWrapper.nativeElement.offsetHeight;

    const HiPPIwidth = window.devicePixelRatio * width;
    const HiPPIheight = window.devicePixelRatio * height;

    canvas.width = HiPPIwidth;
    canvas.height = HiPPIheight;
    canvas.style.width = width + 'px';
    canvas.style.height = height + 'px';

    // ensure all drawing operations are scaled
    if (window.devicePixelRatio !== 1) {
      const context = canvas.getContext('2d')!;
      context.scale(window.devicePixelRatio, window.devicePixelRatio);
    }
  }

  // -1 for x direction, 1 for y direction
  private getMoveDirection(event: WheelEvent): string {
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

    if (Math.abs(yMoveAmount) > Math.abs(xMoveAmount)) {
      return 'y';
    } else {
      return 'x';
    }
  }

  private updateZoomByScrollEvent(event: WheelEvent) {
    const timelineData = assertDefined(this.timelineData);
    const canvas = event.target as HTMLCanvasElement;
    const xPosInCanvas = event.x - canvas.offsetLeft;
    const zoomRange = timelineData.getZoomRange();

    const zoomTo = new Transformer(
      zoomRange,
      assertDefined(this.drawer).getUsableRange(),
      assertDefined(timelineData.getTimestampConverter()),
    ).untransform(xPosInCanvas);

    if (event.deltaY < 0) {
      this.zoomIn(zoomTo);
    } else {
      this.zoomOut(zoomTo);
    }
  }

  private updateHorizontalScroll(event: WheelEvent) {
    const scrollAmount = event.deltaX;
    const timelineData = assertDefined(this.timelineData);
    const fullRange = timelineData.getFullTimeRange();
    const zoomRange = timelineData.getZoomRange();

    const usableRange = assertDefined(this.drawer).getUsableRange();
    const transformer = new Transformer(
      zoomRange,
      usableRange,
      assertDefined(timelineData.getTimestampConverter()),
    );
    const shiftAmount = transformer
      .untransform(usableRange.from + scrollAmount)
      .minus(zoomRange.from.getValueNs());
    let newFrom = zoomRange.from.add(shiftAmount.getValueNs());
    let newTo = zoomRange.to.add(shiftAmount.getValueNs());

    if (newFrom.getValueNs() < fullRange.from.getValueNs()) {
      newTo = newTo.add(
        fullRange.from.minus(newFrom.getValueNs()).getValueNs(),
      );
      newFrom = fullRange.from;
    }

    if (newTo.getValueNs() > fullRange.to.getValueNs()) {
      newFrom = newFrom.minus(
        newTo.minus(fullRange.to.getValueNs()).getValueNs(),
      );
      newTo = fullRange.to;
    }

    this.onZoomChanged({
      from: newFrom,
      to: newTo,
    });
  }
}
