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
import {TimestampUtils} from 'common/timestamp_utils';
import {Analytics} from 'logging/analytics';
import {Traces} from 'trace/traces';
import {TracePosition} from 'trace/trace_position';
import {TraceType, TraceTypeUtils} from 'trace/trace_type';
import {MiniTimelineDrawer} from './drawer/mini_timeline_drawer';
import {MiniTimelineDrawerImpl} from './drawer/mini_timeline_drawer_impl';
import {MiniTimelineDrawerInput} from './drawer/mini_timeline_drawer_input';
import {MIN_SLIDER_WIDTH} from './slider_component';
import {Transformer} from './transformer';

@Component({
  selector: 'mini-timeline',
  template: `
    <div class="mini-timeline-outer-wrapper">
      <div class="zoom-buttons">
        <button mat-icon-button id="zoom-in-btn" (click)="zoomIn()">
          <mat-icon>zoom_in</mat-icon>
        </button>
        <button mat-icon-button id="zoom-out-btn" (click)="zoomOut()">
          <mat-icon>zoom_out</mat-icon>
        </button>
        <button mat-icon-button id="reset-zoom-btn" (click)="resetZoom()">
          <mat-icon>refresh</mat-icon>
        </button>
      </div>
      <div id="mini-timeline-wrapper" #miniTimelineWrapper>
        <canvas
          #canvas
          id="mini-timeline-canvas"
          (mousemove)="trackMousePos($event)"
          (contextmenu)="recordClickPosition($event)"
          [cdkContextMenuTriggerFor]="timeline_context_menu"
          #menuTrigger = "cdkContextMenuTriggerFor"
          ></canvas>
        <div class="zoom-control">
          <slider
            [fullRange]="timelineData.getFullTimeRange()"
            [zoomRange]="timelineData.getZoomRange()"
            [currentPosition]="timelineData.getCurrentPosition()"
            [timestampConverter]="timelineData.getTimestampConverter()"
            (onZoomChanged)="onZoomChanged($event)"></slider>
        </div>
      </div>
    </div>

    <ng-template #timeline_context_menu>
      <div class="context-menu" cdkMenu #timelineMenu="cdkMenu">
        <div class="context-menu-item-container">
          <span class="context-menu-item" (click)="toggleBookmark()" cdkMenuItem> {{getToggleBookmarkText()}} </span>
          <span class="context-menu-item" (click)="removeAllBookmarks()" cdkMenuItem>Remove all bookmarks</span>
        </div>
      </div>
    </ng-template>
  `,
  styles: [
    `
      .mini-timeline-outer-wrapper {
        display: inline-flex;
        width: 100%;
        min-height: 5em;
        height: 100%;
      }
      .zoom-buttons {
        width: fit-content;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        background-color: var(--drawer-color);
      }
      .zoom-buttons button {
        width: fit-content;
      }
      #mini-timeline-wrapper {
        width: 100%;
        min-height: 5em;
        height: 100%;
      }
      .zoom-control {
        padding-right: ${MIN_SLIDER_WIDTH / 2}px;
        margin-top: -10px;
      }
      .zoom-control slider {
        flex-grow: 1;
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
  @Input() bookmarks: Timestamp[] = [];

  @Output() readonly onTracePositionUpdate = new EventEmitter<TracePosition>();
  @Output() readonly onSeekTimestampUpdate = new EventEmitter<
    Timestamp | undefined
  >();
  @Output() readonly onRemoveAllBookmarks = new EventEmitter<void>();
  @Output() readonly onToggleBookmark = new EventEmitter<{
    range: TimeRange;
    rangeContainsBookmark: boolean;
  }>();

  @ViewChild('miniTimelineWrapper', {static: false})
  miniTimelineWrapper: ElementRef | undefined;
  @ViewChild('canvas', {static: false}) canvasRef: ElementRef | undefined;

  getCanvas(): HTMLCanvasElement {
    return assertDefined(this.canvasRef).nativeElement;
  }

  drawer: MiniTimelineDrawer | undefined = undefined;
  private lastMousePosX: number | undefined;
  private lastMoves: WheelEvent[] = [];
  private lastRightClickTimeRange: TimeRange | undefined;

  recordClickPosition(event: MouseEvent) {
    event.preventDefault();
    event.stopPropagation();
    const lastRightClickPos = {x: event.offsetX, y: event.offsetY};
    const drawer = assertDefined(this.drawer);
    const clickRange = drawer.getClickRange(lastRightClickPos);
    const zoomRange = assertDefined(this.timelineData).getZoomRange();
    const usableRange = drawer.getUsableRange();
    const transformer = new Transformer(
      zoomRange,
      usableRange,
      assertDefined(this.timelineData?.getTimestampConverter()),
    );
    this.lastRightClickTimeRange = new TimeRange(
      transformer.untransform(clickRange.from),
      transformer.untransform(clickRange.to),
    );
  }

  private static readonly SLIDER_HORIZONTAL_STEP = 30;
  private static readonly SENSITIVITY_FACTOR = 5;

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
      if (changes['bookmarks'] && Object.keys(changes).length === 1) {
        this.drawer.drawBookmarks();
      } else {
        this.drawer.draw();
      }
    }
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

  trackMousePos(event: MouseEvent) {
    this.lastMousePosX = event.offsetX;
  }

  @HostListener('document:keydown', ['$event'])
  async handleKeyboardEvent(event: KeyboardEvent) {
    if (event.code === 'KeyA') {
      this.updateSliderPosition(-MiniTimelineComponent.SLIDER_HORIZONTAL_STEP);
    }
    if (event.code === 'KeyD') {
      this.updateSliderPosition(MiniTimelineComponent.SLIDER_HORIZONTAL_STEP);
    }

    if (event.code !== 'KeyW' && event.code !== 'KeyS') {
      return;
    }

    const timelineData = assertDefined(this.timelineData);
    const zoomRange = timelineData.getZoomRange();

    let zoomTo = this.currentTracePosition?.timestamp;
    if (this.lastMousePosX !== undefined) {
      zoomTo = new Transformer(
        zoomRange,
        assertDefined(this.drawer).getUsableRange(),
        assertDefined(timelineData.getTimestampConverter()),
      ).untransform(this.lastMousePosX);
    }
    event.code === 'KeyW' ? this.zoomIn(zoomTo) : this.zoomOut(zoomTo);
  }

  onZoomChanged(zoom: TimeRange) {
    const timelineData = assertDefined(this.timelineData);
    timelineData.setZoom(zoom);
    timelineData.setSelectionTimeRange(zoom);
    this.drawer?.draw();
  }

  resetZoom() {
    Analytics.Navigation.logZoom('reset', 'timeline');
    this.onZoomChanged(assertDefined(this.timelineData).getFullTimeRange());
  }

  zoomIn(zoomOn?: Timestamp) {
    Analytics.Navigation.logZoom(this.getZoomSource(zoomOn), 'timeline', 'in');
    this.zoom({nominator: 6n, denominator: 7n}, zoomOn);
  }

  zoomOut(zoomOn?: Timestamp) {
    Analytics.Navigation.logZoom(this.getZoomSource(zoomOn), 'timeline', 'out');
    this.zoom({nominator: 8n, denominator: 7n}, zoomOn);
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

    this.onZoomChanged(new TimeRange(newFrom, newTo));
  }

  @HostListener('wheel', ['$event'])
  onScroll(event: WheelEvent) {
    const moveDirection = this.getMoveDirection(event);

    if (
      (event.target as HTMLElement)?.id === 'mini-timeline-canvas' &&
      event.deltaY !== 0 &&
      moveDirection === 'y'
    ) {
      this.updateZoomByScrollEvent(event);
    }

    if (event.deltaX !== 0 && moveDirection === 'x') {
      this.updateHorizontalScroll(event);
    }
  }

  toggleBookmark() {
    if (!this.lastRightClickTimeRange) {
      return;
    }
    this.onToggleBookmark.emit({
      range: this.lastRightClickTimeRange,
      rangeContainsBookmark: this.bookmarks.some((bookmark) => {
        return assertDefined(this.lastRightClickTimeRange).containsTimestamp(
          bookmark,
        );
      }),
    });
  }

  getToggleBookmarkText() {
    if (!this.lastRightClickTimeRange) {
      return 'Add/remove bookmark';
    }

    const rangeContainsBookmark = this.bookmarks.some((bookmark) => {
      return assertDefined(this.lastRightClickTimeRange).containsTimestamp(
        bookmark,
      );
    });
    if (rangeContainsBookmark) {
      return 'Remove bookmark';
    }

    return 'Add bookmark';
  }

  removeAllBookmarks() {
    this.onRemoveAllBookmarks.emit();
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
      this.bookmarks,
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
    const scrollAmount =
      event.deltaX / MiniTimelineComponent.SENSITIVITY_FACTOR;
    this.updateSliderPosition(scrollAmount);
  }

  private updateSliderPosition(step: number) {
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
      .untransform(usableRange.from + step)
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

    this.onZoomChanged(new TimeRange(newFrom, newTo));
  }
}
