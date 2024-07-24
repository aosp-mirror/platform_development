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

import {ElementRef, EventEmitter} from '@angular/core';
import {assertDefined} from 'common/assert_utils';
import {Point} from 'common/geometry_types';
import {TraceEntry} from 'trace/trace';
import {TracePosition} from 'trace/trace_position';
import {CanvasDrawer} from './canvas_drawer';

export abstract class AbstractTimelineRowComponent<T extends {}> {
  abstract selectedEntry: TraceEntry<T> | undefined;
  abstract onTracePositionUpdate: EventEmitter<TracePosition>;
  abstract wrapperRef: ElementRef | undefined;
  abstract canvasRef: ElementRef | undefined;

  canvasDrawer = new CanvasDrawer();
  protected viewInitialized = false;
  private _observer = new ResizeObserver(() => this.initializeCanvas());

  getCanvas(): HTMLCanvasElement {
    return this.canvasRef?.nativeElement;
  }

  async ngAfterViewInit() {
    this._observer.observe(assertDefined(this.wrapperRef).nativeElement);
    await this.initializeCanvas();
  }

  ngOnChanges() {
    if (this.viewInitialized) {
      this.redraw();
    }
  }

  ngOnDestroy() {
    this._observer.disconnect();
  }

  async initializeCanvas() {
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
    canvas.style.width = width + 'px';
    canvas.style.height = height + 'px';

    // ensure all drawing operations are scaled
    if (window.devicePixelRatio !== 1) {
      const context = canvas.getContext('2d')!;
      context.scale(window.devicePixelRatio, window.devicePixelRatio);
    }

    this.canvasDrawer.setCanvas(this.getCanvas());
    await this.redraw();

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

  async handleMouseDown(e: MouseEvent) {
    e.preventDefault();
    e.stopPropagation();
    const mousePoint = {
      x: e.offsetX,
      y: e.offsetY,
    };

    const transitionEntry = await this.getEntryAt(mousePoint);
    // TODO: This can probably get made better by getting the transition and checking both the end and start timestamps match
    if (transitionEntry && transitionEntry !== this.selectedEntry) {
      this.redraw();
      this.selectedEntry = transitionEntry;
      this.onTracePositionUpdate.emit(
        TracePosition.fromTraceEntry(transitionEntry),
      );
    }
  }

  handleMouseMove(e: MouseEvent) {
    e.preventDefault();
    e.stopPropagation();
    const mousePoint = {
      x: e.offsetX,
      y: e.offsetY,
    };

    this.updateCursor(mousePoint);
    this.onHover(mousePoint);
  }

  protected async updateCursor(mousePoint: Point) {
    if (this.getEntryAt(mousePoint) !== undefined) {
      this.getCanvas().style.cursor = 'pointer';
    } else {
      this.getCanvas().style.cursor = 'auto';
    }
  }

  protected async redraw() {
    this.canvasDrawer.clear();
    await this.drawTimeline();
  }

  abstract drawTimeline(): Promise<void>;
  protected abstract getEntryAt(
    mousePoint: Point,
  ): Promise<TraceEntry<T> | undefined>;
  protected abstract onHover(mousePoint: Point): void;
  protected abstract handleMouseOut(e: MouseEvent): void;
}
