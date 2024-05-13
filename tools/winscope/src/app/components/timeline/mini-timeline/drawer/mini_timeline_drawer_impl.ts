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
import {Point} from 'common/geometry_types';
import {MouseEventButton} from 'common/mouse_event_button';
import {Padding} from 'common/padding';
import {Timestamp} from 'common/time';
import {TRACE_INFO} from 'trace/trace_info';
import {CanvasMouseHandler} from './canvas_mouse_handler';
import {CanvasMouseHandlerImpl} from './canvas_mouse_handler_impl';
import {DraggableCanvasObject} from './draggable_canvas_object';
import {DraggableCanvasObjectImpl} from './draggable_canvas_object_impl';
import {MiniCanvasDrawerData, TimelineEntries} from './mini_canvas_drawer_data';
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
  private static readonly MARKER_CLICK_REGION_WIDTH = 2;

  constructor(
    public canvas: HTMLCanvasElement,
    private inputGetter: () => MiniTimelineDrawerInput,
    private onPointerPositionDragging: (pos: Timestamp) => void,
    private onPointerPositionChanged: (pos: Timestamp) => void,
    private onUnhandledClick: (pos: Timestamp) => void,
  ) {
    const ctx = canvas.getContext('2d');

    if (ctx === null) {
      throw Error('MiniTimeline canvas context was null!');
    }

    this.ctx = ctx;

    const onUnhandledClickInternal = async (
      mousePoint: Point,
      button: number,
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

      this.onUnhandledClick(this.getInput().transformer.untransform(pointX));
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

  drawBookmarks() {
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

  getBookmarks(): number[] {
    return this.getInput().bookmarks;
  }

  async getTimelineEntries(): Promise<TimelineEntries> {
    return await this.getInput().getTimelineEntries();
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
    const timelineEntries = await this.getTimelineEntries();
    const innerHeight = this.getInnerHeight();
    const lineHeight =
      innerHeight / (Math.max(timelineEntries.size - 10, 0) + 12);

    let fromTop = this.getPadding().top + innerHeight - lineHeight;

    timelineEntries.forEach((entries, traceType) => {
      this.ctx.globalAlpha = 0.7;
      this.ctx.fillStyle = TRACE_INFO[traceType].color;
      this.ctx.strokeStyle = 'blue';

      for (const entry of entries.points) {
        const width = 5;
        this.ctx.fillRect(entry - width / 2, fromTop, width, lineHeight);
      }

      for (const entry of entries.segments) {
        const width = Math.max(entry.to - entry.from, 3);
        this.ctx.fillRect(entry.from, fromTop, width, lineHeight);
      }

      this.ctx.fillStyle = Color.ACTIVE_POINTER;
      if (entries.activePoint) {
        const entry = entries.activePoint;
        const width = 5;
        this.ctx.fillRect(entry - width / 2, fromTop, width, lineHeight);
      }

      if (entries.activeSegment) {
        const entry = entries.activeSegment;
        const width = Math.max(entry.to - entry.from, 3);
        this.ctx.fillRect(entry.from, fromTop, width, lineHeight);
      }

      this.ctx.globalAlpha = 1.0;

      fromTop -= (lineHeight * 4) / 3;
    });
  }
}
