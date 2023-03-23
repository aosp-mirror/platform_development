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
  Input,
  Output,
  SimpleChanges,
  ViewChild,
} from '@angular/core';
import {Color} from 'app/colors';
import {TimeRange} from 'app/timeline_data';
import {Timestamp} from 'trace/timestamp';
import {Trace, TraceEntry} from 'trace/trace';
import {TracePosition} from 'trace/trace_position';

@Component({
  selector: 'single-timeline',
  template: `
    <div class="single-timeline" #wrapper>
      <canvas #canvas></canvas>
    </div>
  `,
  styles: [
    `
      .single-timeline {
        height: 2rem;
        padding: 1rem 0;
      }
    `,
  ],
})
export class SingleTimelineComponent {
  @Input() color = '#AF5CF7';
  @Input() trace!: Trace<{}>;
  @Input() selectedEntry: TraceEntry<{}> | undefined = undefined;
  @Input() selectionRange!: TimeRange;

  @Output() onTracePositionUpdate = new EventEmitter<TracePosition>();

  @ViewChild('canvas', {static: false}) canvasRef!: ElementRef;
  @ViewChild('wrapper', {static: false}) wrapperRef!: ElementRef;

  hoveringEntry?: Timestamp;

  private viewInitialized = false;

  get canvas() {
    return this.canvasRef.nativeElement;
  }

  get ctx(): CanvasRenderingContext2D {
    const ctx = this.canvas.getContext('2d');

    if (ctx == null) {
      throw Error('Failed to get canvas context!');
    }

    return ctx;
  }

  ngOnInit() {
    if (!this.trace || !this.selectionRange) {
      throw Error('Not all required inputs have been set');
    }
  }

  ngAfterViewInit() {
    // TODO: Clear observer at some point
    new ResizeObserver(() => this.initializeCanvas()).observe(this.wrapperRef.nativeElement);
    this.initializeCanvas();
  }

  initializeCanvas() {
    // Reset any size before computing new size to avoid it interfering with size computations
    this.canvas.width = 0;
    this.canvas.height = 0;
    this.canvas.style.width = 'auto';
    this.canvas.style.height = 'auto';

    const computedStyle = getComputedStyle(this.wrapperRef.nativeElement);
    const width = this.wrapperRef.nativeElement.offsetWidth;
    const height =
      this.wrapperRef.nativeElement.offsetHeight -
      // tslint:disable-next-line:ban
      parseFloat(computedStyle.paddingTop) -
      // tslint:disable-next-line:ban
      parseFloat(computedStyle.paddingBottom);

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

    this.redraw();

    this.canvas.addEventListener('mousemove', (event: MouseEvent) => {
      this.handleMouseMove(event);
    });
    this.canvas.addEventListener('mousedown', (event: MouseEvent) => {
      this.handleMouseDown(event);
    });
    this.canvas.addEventListener('mouseout', (event: MouseEvent) => {
      this.handleMouseOut(event);
    });

    this.viewInitialized = true;
  }

  ngOnChanges(changes: SimpleChanges) {
    if (this.viewInitialized) {
      this.redraw();
    }
  }

  private handleMouseOut(e: MouseEvent) {
    if (this.hoveringEntry) {
      // If undefined there is no current hover effect so no need to clear
      this.redraw();
    }
    this.hoveringEntry = undefined;
  }

  getXScale(): number {
    return this.ctx.getTransform().m11;
  }

  getYScale(): number {
    return this.ctx.getTransform().m22;
  }

  private handleMouseMove(e: MouseEvent) {
    e.preventDefault();
    e.stopPropagation();
    const mouseX = e.offsetX * this.getXScale();
    const mouseY = e.offsetY * this.getYScale();

    this.updateCursor(mouseX, mouseY);
    this.drawEntryHover(mouseX, mouseY);
  }

  private drawEntryHover(mouseX: number, mouseY: number) {
    const currentHoverEntry = this.getEntryAt(mouseX, mouseY);
    if (this.hoveringEntry === currentHoverEntry) {
      return;
    }

    if (this.hoveringEntry) {
      // If null there is no current hover effect so no need to clear
      this.clearCanvas();
      this.drawTimeline();
    }

    this.hoveringEntry = currentHoverEntry;

    if (!this.hoveringEntry) {
      return;
    }

    this.defineEntryPath(this.hoveringEntry);

    this.ctx.globalAlpha = 1.0;
    this.ctx.strokeStyle = Color.ACTIVE_BORDER;
    this.ctx.lineWidth = 2;
    this.ctx.save();
    this.ctx.clip();
    this.ctx.lineWidth *= 2;
    this.ctx.fill();
    this.ctx.stroke();
    this.ctx.restore();
    this.ctx.stroke();

    this.ctx.restore();
  }

  private clearCanvas() {
    // Clear canvas
    this.ctx.clearRect(0, 0, this.getScaledCanvasWidth(), this.getScaledCanvasHeight());
  }

  private getEntryAt(mouseX: number, mouseY: number): Timestamp | undefined {
    // TODO: This can be optimized if it's laggy
    for (let i = 0; i < this.trace.lengthEntries; ++i) {
      const timestamp = this.trace.getEntry(i).getTimestamp();
      this.defineEntryPath(timestamp);
      if (this.ctx.isPointInPath(mouseX, mouseY)) {
        this.canvas.style.cursor = 'pointer';
        return timestamp;
      }
    }
    return undefined;
  }

  private updateCursor(mouseX: number, mouseY: number) {
    if (this.getEntryAt(mouseX, mouseY)) {
      this.canvas.style.cursor = 'pointer';
    }
    this.canvas.style.cursor = 'auto';
  }

  private handleMouseDown(e: MouseEvent) {
    e.preventDefault();
    e.stopPropagation();
    const mouseX = e.offsetX * this.getXScale();
    const mouseY = e.offsetY * this.getYScale();

    const clickedTimestamp = this.getEntryAt(mouseX, mouseY);

    if (
      clickedTimestamp &&
      clickedTimestamp.getValueNs() !== this.selectedEntry?.getTimestamp().getValueNs()
    ) {
      this.redraw();
      const entry = this.trace.findClosestEntry(clickedTimestamp);
      if (entry) {
        this.selectedEntry = entry;
        this.onTracePositionUpdate.emit(TracePosition.fromTraceEntry(entry));
      }
    }
  }

  getScaledCanvasWidth() {
    return Math.floor(this.canvas.width / this.getXScale());
  }

  getScaledCanvasHeight() {
    return Math.floor(this.canvas.height / this.getYScale());
  }

  get entryWidth() {
    return this.getScaledCanvasHeight();
  }

  get availableWidth() {
    return Math.floor(this.getScaledCanvasWidth() - this.entryWidth);
  }

  private defineEntryPath(entry: Timestamp, padding = 0) {
    const start = this.selectionRange.from.getValueNs();
    const end = this.selectionRange.to.getValueNs();

    const xPos = Number(
      (BigInt(this.availableWidth) * (entry.getValueNs() - start)) / (end - start)
    );

    rect(
      this.ctx,
      xPos + padding,
      padding,
      this.entryWidth - 2 * padding,
      this.entryWidth - 2 * padding
    );
  }

  private redraw() {
    this.clearCanvas();
    this.drawTimeline();
  }

  private drawTimeline() {
    this.trace.forEachTimestamp((entry) => {
      this.drawEntry(entry);
    });
    this.drawSelectedEntry();
  }

  private drawEntry(entry: Timestamp) {
    this.ctx.globalAlpha = 0.2;

    this.defineEntryPath(entry);
    this.ctx.fillStyle = this.color;
    this.ctx.fill();

    this.ctx.restore();
  }

  private drawSelectedEntry() {
    if (this.selectedEntry === undefined) {
      return;
    }

    this.ctx.globalAlpha = 1.0;
    this.defineEntryPath(this.selectedEntry.getTimestamp(), 1);
    this.ctx.fillStyle = this.color;
    this.ctx.strokeStyle = Color.ACTIVE_BORDER;
    this.ctx.lineWidth = 3;
    this.ctx.stroke();
    this.ctx.fill();

    this.ctx.restore();
  }
}

function rect(ctx: CanvasRenderingContext2D, x: number, y: number, w: number, h: number) {
  ctx.beginPath();
  ctx.moveTo(x, y);
  ctx.lineTo(x + w, y);
  ctx.lineTo(x + w, y + h);
  ctx.lineTo(x, y + h);
  ctx.lineTo(x, y);
  ctx.closePath();
}
