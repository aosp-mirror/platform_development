/*
 * Copyright (C) 2023 The Android Open Source Project
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
  ElementRef,
  EventEmitter,
  HostListener,
  Input,
  Output,
  ViewChild,
} from '@angular/core';
import {assertDefined} from 'common/assert_utils';
import {Point} from 'common/geometry/point';
import {TimeRange} from 'common/time';
import {ComponentTimestampConverter} from 'common/timestamp_converter';
import {Trace, TraceEntry} from 'trace/trace';
import {TracePosition} from 'trace/trace_position';
import {CanvasDrawer} from './canvas_drawer';

export abstract class AbstractTimelineRowComponent<T extends {}> {
  abstract selectedEntry: TraceEntry<T> | undefined;
  abstract trace: Trace<{}> | undefined;

  @Input() color = '#AF5CF7';
  @Input() isActive = false;
  @Input() selectionRange: TimeRange | undefined;
  @Input() timestampConverter: ComponentTimestampConverter | undefined;

  @Output() readonly onScrollEvent = new EventEmitter<WheelEvent>();
  @Output() readonly onTraceClicked = new EventEmitter<Trace<object>>();
  @Output() readonly onTracePositionUpdate = new EventEmitter<TracePosition>();
  @Output() readonly onMouseXRatioUpdate = new EventEmitter<
    number | undefined
  >();

  @ViewChild('canvas', {static: false}) canvasRef: ElementRef | undefined;
  @ViewChild('wrapper', {static: false}) wrapperRef: ElementRef | undefined;

  canvasDrawer = new CanvasDrawer();
  protected viewInitialized = false;
  private observer = new ResizeObserver(() => this.initializeCanvas());

  getCanvas(): HTMLCanvasElement {
    return this.canvasRef?.nativeElement;
  }

  getBackgroundColor() {
    return this.isActive ? 'var(--selected-element-color)' : undefined;
  }

  ngAfterViewInit() {
    this.observer.observe(assertDefined(this.wrapperRef).nativeElement);
    this.initializeCanvas();
  }

  ngOnChanges() {
    if (this.viewInitialized) {
      this.redraw();
    }
  }

  ngOnDestroy() {
    this.observer.disconnect();
  }

  initializeCanvas() {
    const canvas = this.getCanvas();

    // Reset any size before computing new size to avoid it interfering with size computations
    canvas.width = 0;
    canvas.height = 0;
    canvas.style.width = 'auto';
    canvas.style.height = 'auto';

    const htmlElement = assertDefined(this.wrapperRef).nativeElement;

    const computedStyle = getComputedStyle(htmlElement);
    const width = htmlElement.offsetWidth;
    const height =
      htmlElement.offsetHeight -
      // tslint:disable-next-line:ban
      parseFloat(computedStyle.paddingTop) -
      // tslint:disable-next-line:ban
      parseFloat(computedStyle.paddingBottom);

    const HiPPIwidth = window.devicePixelRatio * width;
    const HiPPIheight = window.devicePixelRatio * height;

    canvas.width = HiPPIwidth;
    canvas.height = HiPPIheight;
    canvas.style.width = '100%';
    canvas.style.height = '100%';

    // ensure all drawing operations are scaled
    if (window.devicePixelRatio !== 1) {
      const context = assertDefined(canvas.getContext('2d'));
      context.scale(window.devicePixelRatio, window.devicePixelRatio);
    }

    this.canvasDrawer.setCanvas(this.getCanvas());
    this.redraw();

    canvas.addEventListener('mousemove', (event: MouseEvent) => {
      this.handleMouseMove(event);
    });
    canvas.addEventListener('mousedown', (event: MouseEvent) => {
      this.handleMouseDown(event);
    });
    canvas.addEventListener('mouseout', (event: MouseEvent) => {
      this.handleMouseOut(event);
    });

    this.viewInitialized = true;
  }

  handleMouseDown(e: MouseEvent) {
    e.preventDefault();
    e.stopPropagation();
    const mousePoint = {
      x: e.offsetX,
      y: e.offsetY,
    };

    const entry = this.getEntryAt(mousePoint);
    // TODO: This can probably get made better by getting the transition and checking both the end and start timestamps match
    if (entry && entry !== this.selectedEntry) {
      this.redraw();
      this.selectedEntry = entry;
      this.onTracePositionUpdate.emit(TracePosition.fromTraceEntry(entry));
    } else if (!entry && this.trace) {
      this.onTraceClicked.emit(this.trace);
    }
  }

  handleMouseMove(e: MouseEvent) {
    e.preventDefault();
    e.stopPropagation();
    const mousePoint = {
      x: e.offsetX,
      y: e.offsetY,
    };

    this.onHover(mousePoint);
  }

  @HostListener('wheel', ['$event'])
  updateScroll(event: WheelEvent) {
    this.onScrollEvent.emit(event);
  }

  onTimelineClick(event: MouseEvent) {
    if ((event.target as HTMLElement).id === 'canvas') {
      return;
    }
    this.onTraceClicked.emit(assertDefined(this.trace));
  }

  trackMousePos(event: MouseEvent) {
    const canvas = event.target as HTMLCanvasElement;
    this.onMouseXRatioUpdate.emit(event.offsetX / canvas.offsetWidth);
  }

  onMouseLeave(event: MouseEvent) {
    this.onMouseXRatioUpdate.emit(undefined);
  }

  protected redraw() {
    this.canvasDrawer.clear();
    this.drawTimeline();
  }

  abstract drawTimeline(): void;
  protected abstract getEntryAt(mousePoint: Point): TraceEntry<T> | undefined;
  protected abstract onHover(mousePoint: Point): void;
  protected abstract handleMouseOut(e: MouseEvent): void;
}
