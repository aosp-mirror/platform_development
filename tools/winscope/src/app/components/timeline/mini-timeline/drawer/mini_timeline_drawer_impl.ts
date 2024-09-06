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

import {Color} from 'app/colors';
import {Segment} from 'app/components/timeline/segment';
import {TimelineUtils} from 'app/components/timeline/timeline_utils';
import {Point} from 'common/geometry/point';
import {MouseEventButton} from 'common/mouse_event_button';
import {Padding} from 'common/padding';
import {Timestamp} from 'common/time';
import {Trace} from 'trace/trace';
import {TRACE_INFO} from 'trace/trace_info';
import {CanvasMouseHandler} from './canvas_mouse_handler';
import {CanvasMouseHandlerImpl} from './canvas_mouse_handler_impl';
import {DraggableCanvasObject} from './draggable_canvas_object';
import {DraggableCanvasObjectImpl} from './draggable_canvas_object_impl';
import {
  MiniCanvasDrawerData,
  TimelineTrace,
  TimelineTraces,
} from './mini_canvas_drawer_data';
import {MiniTimelineDrawer} from './mini_timeline_drawer';
import {MiniTimelineDrawerInput} from './mini_timeline_drawer_input';

/**
 * Mini timeline drawer implementation
 * @docs-private
 */
export class MiniTimelineDrawerImpl implements MiniTimelineDrawer {
  ctx: CanvasRenderingContext2D;
  handler: CanvasMouseHandler;
  private activePointer: DraggableCanvasObject;
  private lastMousePoint: Point | undefined;
  private static readonly MARKER_CLICK_REGION_WIDTH = 2;
  private static readonly TRACE_ENTRY_ALPHA = 0.7;

  constructor(
    public canvas: HTMLCanvasElement,
    private inputGetter: () => MiniTimelineDrawerInput,
    private onPointerPositionDragging: (pos: Timestamp) => void,
    private onPointerPositionChanged: (pos: Timestamp) => void,
    private onUnhandledClick: (
      pos: Timestamp,
      trace: Trace<object> | undefined,
    ) => void,
  ) {
    const ctx = canvas.getContext('2d');

    if (ctx === null) {
      throw new Error('MiniTimeline canvas context was null!');
    }

    this.ctx = ctx;

    const onUnhandledClickInternal = async (
      mousePoint: Point,
      button: number,
      trace: Trace<object> | undefined,
    ) => {
      if (button === MouseEventButton.SECONDARY) {
        return;
      }
      let pointX = mousePoint.x;

      if (mousePoint.y < this.getMarkerHeight()) {
        pointX =
          this.getInput().bookmarks.find((bm) => {
            const diff = mousePoint.x - bm;
            return diff > 0 && diff < this.getMarkerMaxWidth();
          }) ?? mousePoint.x;
      }

      this.onUnhandledClick(
        this.getInput().transformer.untransform(pointX),
        trace,
      );
    };
    this.handler = new CanvasMouseHandlerImpl(
      this,
      'pointer',
      onUnhandledClickInternal,
    );

    this.activePointer = new DraggableCanvasObjectImpl(
      this,
      () => this.getSelectedPosition(),
      (ctx: CanvasRenderingContext2D, position: number) => {
        const barWidth = 3;
        const triangleHeight = this.getMarkerHeight();

        ctx.beginPath();
        ctx.moveTo(position - triangleHeight, 0);
        ctx.lineTo(position + triangleHeight, 0);
        ctx.lineTo(position + barWidth / 2, triangleHeight);
        ctx.lineTo(position + barWidth / 2, this.getHeight());
        ctx.lineTo(position - barWidth / 2, this.getHeight());
        ctx.lineTo(position - barWidth / 2, triangleHeight);
        ctx.closePath();
      },
      {
        fillStyle: Color.ACTIVE_POINTER,
        fill: true,
      },
      (x) => {
        const input = this.getInput();
        input.selectedPosition = x;
        this.onPointerPositionDragging(input.transformer.untransform(x));
      },
      (x) => {
        const input = this.getInput();
        input.selectedPosition = x;
        this.onPointerPositionChanged(input.transformer.untransform(x));
      },
      () => this.getUsableRange(),
    );
  }

  getXScale() {
    return this.ctx.getTransform().m11;
  }

  getYScale() {
    return this.ctx.getTransform().m22;
  }

  getWidth() {
    return this.canvas.width / this.getXScale();
  }

  getHeight() {
    return this.canvas.height / this.getYScale();
  }

  getUsableRange() {
    const padding = this.getPadding();
    return {
      from: padding.left,
      to: this.getWidth() - padding.left - padding.right,
    };
  }

  getInput(): MiniCanvasDrawerData {
    return this.inputGetter().transform(this.getUsableRange());
  }

  getClickRange(clickPos: Point) {
    const markerHeight = this.getMarkerHeight();
    if (clickPos.y > markerHeight) {
      return {
        from: clickPos.x - MiniTimelineDrawerImpl.MARKER_CLICK_REGION_WIDTH,
        to: clickPos.x + MiniTimelineDrawerImpl.MARKER_CLICK_REGION_WIDTH,
      };
    }
    const markerMaxWidth = this.getMarkerMaxWidth();
    return {
      from: clickPos.x - markerMaxWidth,
      to: clickPos.x + markerMaxWidth,
    };
  }

  getSelectedPosition() {
    return this.getInput().selectedPosition;
  }

  getBookmarks(): number[] {
    return this.getInput().bookmarks;
  }

  async getTimelineTraces(): Promise<TimelineTraces> {
    return await this.getInput().getTimelineTraces();
  }

  getPadding(): Padding {
    const height = this.getHeight();
    const pointerWidth = this.getPointerWidth();
    return {
      top: Math.ceil(height / 10),
      bottom: Math.ceil(height / 10),
      left: Math.ceil(pointerWidth / 2),
      right: Math.ceil(pointerWidth / 2),
    };
  }

  getInnerHeight() {
    const padding = this.getPadding();
    return this.getHeight() - padding.top - padding.bottom;
  }

  async draw() {
    this.ctx.clearRect(0, 0, this.getWidth(), this.getHeight());
    await this.drawTraceLines();
    this.drawBookmarks();
    this.activePointer.draw(this.ctx);
    this.drawHoverCursor();
  }

  async updateHover(mousePoint: Point | undefined) {
    this.lastMousePoint = mousePoint;
    await this.draw();
  }

  async getTraceClicked(mousePoint: Point): Promise<Trace<object> | undefined> {
    const timelineTraces = await this.getTimelineTraces();
    const innerHeight = this.getInnerHeight();
    const lineHeight = this.getLineHeight(timelineTraces, innerHeight);
    let fromTop = this.getPadding().top + innerHeight - lineHeight;

    for (const trace of timelineTraces.keys()) {
      if (
        this.pointWithinTimeline(mousePoint.y, fromTop, fromTop + lineHeight)
      ) {
        return trace;
      }
      fromTop -= this.fromTopStep(lineHeight);
    }

    return undefined;
  }

  private getPointerWidth() {
    return this.getHeight() / 6;
  }

  private getMarkerMaxWidth() {
    return (this.getPointerWidth() * 2) / 3;
  }

  private getMarkerHeight() {
    return this.getPointerWidth() / 2;
  }

  private async drawTraceLines() {
    const timelineTraces = await this.getTimelineTraces();
    const innerHeight = this.getInnerHeight();
    const lineHeight = this.getLineHeight(timelineTraces, innerHeight);
    let fromTop = this.getPadding().top + innerHeight - lineHeight;

    timelineTraces.forEach((timelineTrace, trace) => {
      if (this.inputGetter().timelineData.getActiveTrace() === trace) {
        this.fillActiveTimelineBackground(fromTop, lineHeight);
      } else if (
        this.lastMousePoint?.y &&
        this.pointWithinTimeline(this.lastMousePoint?.y, fromTop, lineHeight)
      ) {
        this.fillHoverTimelineBackground(fromTop, lineHeight);
      }

      this.drawTraceEntries(trace, timelineTrace, fromTop, lineHeight);

      fromTop -= this.fromTopStep(lineHeight);
    });
  }

  private drawTraceEntries(
    trace: Trace<object>,
    timelineTrace: TimelineTrace,
    fromTop: number,
    lineHeight: number,
  ) {
    this.ctx.globalAlpha = MiniTimelineDrawerImpl.TRACE_ENTRY_ALPHA;
    this.ctx.fillStyle = TRACE_INFO[trace.type].color;
    this.ctx.strokeStyle = 'blue';

    for (const entry of timelineTrace.points) {
      const width = 5;
      this.ctx.fillRect(entry - width / 2, fromTop, width, lineHeight);
    }

    for (const entry of timelineTrace.segments) {
      this.drawTransitionEntry(
        entry,
        fromTop,
        TRACE_INFO[trace.type].color,
        lineHeight,
      );
    }

    this.ctx.fillStyle = Color.ACTIVE_POINTER;
    if (timelineTrace.activePoint) {
      const entry = timelineTrace.activePoint;
      const width = 5;
      this.ctx.fillRect(entry - width / 2, fromTop, width, lineHeight);
    }

    if (timelineTrace.activeSegment) {
      this.drawTransitionEntry(
        timelineTrace.activeSegment,
        fromTop,
        Color.ACTIVE_POINTER,
        lineHeight,
      );
    }

    this.ctx.globalAlpha = 1.0;
  }

  private drawTransitionEntry(
    entry: Segment,
    fromTop: number,
    hexColor: string,
    lineHeight: number,
  ) {
    const width = Math.max(entry.to - entry.from, 3);

    if (!(entry.unknownStart || entry.unknownEnd)) {
      this.ctx.globalAlpha = MiniTimelineDrawerImpl.TRACE_ENTRY_ALPHA;
      this.ctx.fillStyle = hexColor;
      this.ctx.fillRect(entry.from, fromTop, width, lineHeight);
      return;
    }

    const rgbColor = TimelineUtils.convertHexToRgb(hexColor);
    if (rgbColor === undefined) {
      throw new Error('Failed to parse provided hex color');
    }
    const {r, g, b} = rgbColor;
    const rgbaColor = `rgba(${r},${g},${b},${MiniTimelineDrawerImpl.TRACE_ENTRY_ALPHA})`;
    const transparentColor = `rgba(${r},${g},${b},${0})`;

    const gradientWidthOutsideEntry = 12;
    const gradientWidthInsideEntry = Math.min(6, width);

    const startGradientx0 = entry.from - gradientWidthOutsideEntry;
    const endGradientx1 = entry.to + gradientWidthOutsideEntry;

    const start = entry.unknownStart ? startGradientx0 : entry.from;
    const end = entry.unknownEnd ? endGradientx1 : entry.to;

    const gradient = this.ctx.createLinearGradient(start, 0, end, 0);
    const gradientRatio = Math.max(
      0,
      Math.min(
        (gradientWidthOutsideEntry + gradientWidthInsideEntry) / (end - start),
        1,
      ),
    );
    gradient.addColorStop(0, entry.unknownStart ? transparentColor : rgbaColor);
    gradient.addColorStop(1, entry.unknownEnd ? transparentColor : rgbaColor);
    gradient.addColorStop(gradientRatio, rgbaColor);
    gradient.addColorStop(1 - gradientRatio, rgbaColor);
    this.ctx.fillStyle = gradient;

    this.ctx.globalAlpha = 1;
    this.ctx.fillRect(start, fromTop, end - start, lineHeight);

    if (entry.unknownStart) {
      this.drawEllipsis(entry.from - 8.5, fromTop, lineHeight);
    }
    if (entry.unknownEnd) {
      this.drawEllipsis(entry.from + width + 1.5, fromTop, lineHeight);
    }
  }

  private getEllipsisColor() {
    return this.inputGetter().isDarkMode ? 'white' : 'black';
  }

  private drawEllipsis(start: number, fromTop: number, lineHeight: number) {
    this.ctx.fillStyle = this.getEllipsisColor();
    let center = start;
    for (let i = 0; i < 3; i++) {
      this.ctx.beginPath();
      this.ctx.arc(center, fromTop + lineHeight / 2, 1, 0, 2 * Math.PI);
      this.ctx.fill();
      center += 3.5;
    }
  }

  private drawHoverCursor() {
    if (!this.lastMousePoint) {
      return;
    }
    const hoverWidth = 2;
    this.ctx.beginPath();
    this.ctx.moveTo(this.lastMousePoint.x - hoverWidth / 2, 0);
    this.ctx.lineTo(this.lastMousePoint.x + hoverWidth / 2, 0);
    this.ctx.lineTo(this.lastMousePoint.x + hoverWidth / 2, this.getHeight());
    this.ctx.lineTo(this.lastMousePoint.x - hoverWidth / 2, this.getHeight());
    this.ctx.closePath();

    this.ctx.globalAlpha = 0.4;
    this.ctx.fillStyle = Color.ACTIVE_POINTER;
    this.ctx.fill();
    this.ctx.globalAlpha = 1.0;
  }

  private drawBookmarks() {
    this.getBookmarks().forEach((position) => {
      const flagWidth = this.getMarkerMaxWidth();
      const flagHeight = this.getMarkerHeight();
      const barWidth = 2;

      this.ctx.beginPath();
      this.ctx.moveTo(position - barWidth / 2, 0);
      this.ctx.lineTo(position + flagWidth, 0);
      this.ctx.lineTo(position + (flagWidth * 5) / 6, flagHeight / 2);
      this.ctx.lineTo(position + flagWidth, flagHeight);
      this.ctx.lineTo(position + barWidth / 2, flagHeight);
      this.ctx.lineTo(position + barWidth / 2, this.getHeight());
      this.ctx.lineTo(position - barWidth / 2, this.getHeight());
      this.ctx.closePath();

      this.ctx.fillStyle = Color.BOOKMARK;
      this.ctx.fill();
    });
  }

  private fromTopStep(lineHeight: number): number {
    return (lineHeight * 4) / 3;
  }

  private fillActiveTimelineBackground(fromTop: number, lineHeight: number) {
    this.ctx.globalAlpha = 1.0;
    this.ctx.fillStyle = getComputedStyle(this.canvas).getPropertyValue(
      '--selected-element-color',
    );
    this.ctx.fillRect(0, fromTop, this.getUsableRange().to, lineHeight);
  }

  private fillHoverTimelineBackground(fromTop: number, lineHeight: number) {
    this.ctx.globalAlpha = 1.0;
    this.ctx.fillStyle = getComputedStyle(this.canvas).getPropertyValue(
      '--hover-element-color',
    );
    this.ctx.fillRect(0, fromTop, this.getUsableRange().to, lineHeight);
  }

  private getLineHeight(
    timelineTraces: TimelineTraces,
    innerHeight: number,
  ): number {
    return innerHeight / (Math.max(timelineTraces.size - 10, 0) + 12);
  }

  private pointWithinTimeline(
    point: number,
    from: number,
    to: number,
  ): boolean {
    return point > from && point <= from + to;
  }
}
