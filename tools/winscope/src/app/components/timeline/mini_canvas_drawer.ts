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

import {TimeRange} from 'app/timeline_data';
import {TRACE_INFO} from 'app/trace_info';
import {Timestamp, TimestampType} from 'trace/timestamp';
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceType} from 'trace/trace_type';
import {Color} from '../../colors';
import {CanvasDrawer} from '../canvas/canvas_drawer';
import {CanvasMouseHandler} from '../canvas/canvas_mouse_handler';
import {DraggableCanvasObject} from '../canvas/draggable_canvas_object';
import {Segment} from './utils';

export class MiniCanvasDrawerInput {
  constructor(
    public fullRange: TimeRange,
    public selectedPosition: Timestamp,
    public selection: TimeRange,
    public traces: Traces
  ) {}

  transform(mapToRange: Segment): MiniCanvasDrawerData {
    const transformer = new Transformer(this.fullRange, mapToRange);
    return new MiniCanvasDrawerData(
      transformer.transform(this.selectedPosition),
      {
        from: transformer.transform(this.selection.from),
        to: transformer.transform(this.selection.to),
      },
      this.transformTracesTimestamps(transformer),
      transformer
    );
  }

  private transformTracesTimestamps(transformer: Transformer): Map<TraceType, number[]> {
    const transformedTraceSegments = new Map<TraceType, number[]>();

    this.traces.forEachTrace((trace) => {
      transformedTraceSegments.set(trace.type, this.transformTraceTimestamps(transformer, trace));
    });

    return transformedTraceSegments;
  }

  private transformTraceTimestamps(transformer: Transformer, trace: Trace<{}>): number[] {
    const result: number[] = [];

    trace.forEachTimestamp((timestamp) => {
      result.push(transformer.transform(timestamp));
    });

    return result;
  }
}

export class Transformer {
  private timestampType: TimestampType;

  private fromWidth: bigint;
  private targetWidth: number;

  private fromOffset: bigint;
  private toOffset: number;

  constructor(private fromRange: TimeRange, private toRange: Segment) {
    this.timestampType = fromRange.from.getType();

    this.fromWidth = this.fromRange.to.getValueNs() - this.fromRange.from.getValueNs();
    // Needs to be a whole number to be compatible with bigints
    this.targetWidth = Math.round(this.toRange.to - this.toRange.from);

    this.fromOffset = this.fromRange.from.getValueNs();
    // Needs to be a whole number to be compatible with bigints
    this.toOffset = Math.round(this.toRange.from);
  }

  transform(x: Timestamp): number {
    return (
      this.toOffset +
      (this.targetWidth * Number(x.getValueNs() - this.fromOffset)) / Number(this.fromWidth)
    );
  }

  untransform(x: number): Timestamp {
    x = Math.round(x);
    const valueNs =
      this.fromOffset + (BigInt(x - this.toOffset) * this.fromWidth) / BigInt(this.targetWidth);
    return new Timestamp(this.timestampType, valueNs);
  }
}

class MiniCanvasDrawerOutput {
  constructor(public selectedPosition: Timestamp, public selection: TimeRange) {}
}

class MiniCanvasDrawerData {
  constructor(
    public selectedPosition: number,
    public selection: Segment,
    public timelineEntries: Map<TraceType, number[]>,
    public transformer: Transformer
  ) {}

  toOutput(): MiniCanvasDrawerOutput {
    return new MiniCanvasDrawerOutput(this.transformer.untransform(this.selectedPosition), {
      from: this.transformer.untransform(this.selection.from),
      to: this.transformer.untransform(this.selection.to),
    });
  }
}

export class MiniCanvasDrawer implements CanvasDrawer {
  ctx: CanvasRenderingContext2D;
  handler: CanvasMouseHandler;

  private activePointer: DraggableCanvasObject;
  private leftFocusSectionSelector: DraggableCanvasObject;
  private rightFocusSectionSelector: DraggableCanvasObject;

  private get pointerWidth() {
    return this.getHeight() / 6;
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

  get usableRange() {
    return {
      from: this.padding.left,
      to: this.getWidth() - this.padding.left - this.padding.right,
    };
  }

  get input() {
    return this.inputGetter().transform(this.usableRange);
  }

  constructor(
    public canvas: HTMLCanvasElement,
    private inputGetter: () => MiniCanvasDrawerInput,
    private onPointerPositionDragging: (pos: Timestamp) => void,
    private onPointerPositionChanged: (pos: Timestamp) => void,
    private onSelectionChanged: (selection: TimeRange) => void,
    private onUnhandledClick: (pos: Timestamp) => void
  ) {
    const ctx = canvas.getContext('2d');

    if (ctx === null) {
      throw Error('MiniTimeline canvas context was null!');
    }

    this.ctx = ctx;

    const onUnhandledClickInternal = (x: number, y: number) => {
      this.onUnhandledClick(this.input.transformer.untransform(x));
    };
    this.handler = new CanvasMouseHandler(this, 'pointer', onUnhandledClickInternal);

    this.activePointer = new DraggableCanvasObject(
      this,
      () => this.selectedPosition,
      (ctx: CanvasRenderingContext2D, position: number) => {
        const barWidth = 3;
        const triangleHeight = this.pointerWidth / 2;

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
        this.input.selectedPosition = x;
        this.onPointerPositionDragging(this.input.transformer.untransform(x));
      },
      (x) => {
        this.input.selectedPosition = x;
        this.onPointerPositionChanged(this.input.transformer.untransform(x));
      },
      () => this.usableRange
    );

    const focusSelectorDrawConfig = {
      fillStyle: Color.SELECTOR_COLOR,
      fill: true,
    };

    const onLeftSelectionChanged = (x: number) => {
      this.selection.from = x;
      this.onSelectionChanged({
        from: this.input.transformer.untransform(x),
        to: this.input.transformer.untransform(this.selection.to),
      });
    };
    const onRightSelectionChanged = (x: number) => {
      this.selection.to = x;
      this.onSelectionChanged({
        from: this.input.transformer.untransform(this.selection.from),
        to: this.input.transformer.untransform(x),
      });
    };

    const barWidth = 6;
    const selectorArrowWidth = this.innerHeight / 12;
    const selectorArrowHeight = selectorArrowWidth * 2;

    this.leftFocusSectionSelector = new DraggableCanvasObject(
      this,
      () => this.selection.from,
      (ctx: CanvasRenderingContext2D, position: number) => {
        ctx.beginPath();
        ctx.moveTo(position - barWidth, this.padding.top);
        ctx.lineTo(position, this.padding.top);
        ctx.lineTo(position + selectorArrowWidth, this.padding.top + selectorArrowWidth);
        ctx.lineTo(position, this.padding.top + selectorArrowHeight);
        ctx.lineTo(position, this.padding.top + this.innerHeight);
        ctx.lineTo(position - barWidth, this.padding.top + this.innerHeight);
        ctx.lineTo(position - barWidth, this.padding.top);
        ctx.closePath();
      },
      focusSelectorDrawConfig,
      onLeftSelectionChanged,
      onLeftSelectionChanged,
      () => {
        return {
          from: this.usableRange.from,
          to: this.rightFocusSectionSelector.position - selectorArrowWidth - barWidth,
        };
      }
    );

    this.rightFocusSectionSelector = new DraggableCanvasObject(
      this,
      () => this.selection.to,
      (ctx: CanvasRenderingContext2D, position: number) => {
        ctx.beginPath();
        ctx.moveTo(position + barWidth, this.padding.top);
        ctx.lineTo(position, this.padding.top);
        ctx.lineTo(position - selectorArrowWidth, this.padding.top + selectorArrowWidth);
        ctx.lineTo(position, this.padding.top + selectorArrowHeight);
        ctx.lineTo(position, this.padding.top + this.innerHeight);
        ctx.lineTo(position + barWidth, this.padding.top + this.innerHeight);
        ctx.closePath();
      },
      focusSelectorDrawConfig,
      onRightSelectionChanged,
      onRightSelectionChanged,
      () => {
        return {
          from: this.leftFocusSectionSelector.position + selectorArrowWidth + barWidth,
          to: this.usableRange.to,
        };
      }
    );
  }

  get selectedPosition() {
    return this.input.selectedPosition;
  }

  get selection() {
    return this.input.selection;
  }

  get timelineEntries() {
    return this.input.timelineEntries;
  }

  get padding() {
    return {
      top: Math.ceil(this.getHeight() / 5),
      bottom: Math.ceil(this.getHeight() / 5),
      left: Math.ceil(this.pointerWidth / 2),
      right: Math.ceil(this.pointerWidth / 2),
    };
  }

  get innerHeight() {
    return this.getHeight() - this.padding.top - this.padding.bottom;
  }

  draw() {
    this.ctx.clearRect(0, 0, this.getWidth(), this.getHeight());

    this.drawSelectionBackground();

    this.drawTraceLines();

    this.drawTimelineGuides();

    this.leftFocusSectionSelector.draw(this.ctx);
    this.rightFocusSectionSelector.draw(this.ctx);

    this.activePointer.draw(this.ctx);
  }

  private drawSelectionBackground() {
    const triangleHeight = this.innerHeight / 6;

    // Selection background
    this.ctx.globalAlpha = 0.8;
    this.ctx.fillStyle = Color.SELECTION_BACKGROUND;
    const width = this.selection.to - this.selection.from;
    this.ctx.fillRect(
      this.selection.from,
      this.padding.top + triangleHeight / 2,
      width,
      this.innerHeight - triangleHeight / 2
    );
    this.ctx.restore();
  }

  private drawTraceLines() {
    const lineHeight = this.innerHeight / 8;

    let fromTop = this.padding.top + (this.innerHeight * 2) / 3 - lineHeight;

    this.timelineEntries.forEach((entries, traceType) => {
      // TODO: Only if active or a selected trace
      for (const entry of entries) {
        this.ctx.globalAlpha = 0.7;
        this.ctx.fillStyle = TRACE_INFO[traceType].color;

        const width = 5;
        this.ctx.fillRect(entry - width / 2, fromTop, width, lineHeight);
        this.ctx.globalAlpha = 1.0;
      }

      fromTop -= (lineHeight * 4) / 3;
    });
  }

  private drawTimelineGuides() {
    const edgeBarHeight = (this.innerHeight * 1) / 2;
    const edgeBarWidth = 4;

    const boldBarHeight = (this.innerHeight * 1) / 5;
    const boldBarWidth = edgeBarWidth;

    const lightBarHeight = (this.innerHeight * 1) / 6;
    const lightBarWidth = 2;

    const minSpacing = lightBarWidth * 7;
    const barsInSetWidth = 9 * lightBarWidth + boldBarWidth;
    const barSets = Math.floor(
      (this.getWidth() - edgeBarWidth * 2 - minSpacing) / (barsInSetWidth + 10 * minSpacing)
    );
    const bars = barSets * 10;

    // Draw start bar
    this.ctx.fillStyle = Color.GUIDE_BAR;
    this.ctx.fillRect(
      0,
      this.padding.top + this.innerHeight - edgeBarHeight,
      edgeBarWidth,
      edgeBarHeight
    );

    // Draw end bar
    this.ctx.fillStyle = Color.GUIDE_BAR;
    this.ctx.fillRect(
      this.getWidth() - edgeBarWidth,
      this.padding.top + this.innerHeight - edgeBarHeight,
      edgeBarWidth,
      edgeBarHeight
    );

    const spacing = (this.getWidth() - barSets * barsInSetWidth - edgeBarWidth) / bars;
    let start = edgeBarWidth + spacing;
    for (let i = 1; i < bars; i++) {
      if (i % 10 === 0) {
        // Draw boldbar
        this.ctx.fillStyle = Color.GUIDE_BAR;
        this.ctx.fillRect(
          start,
          this.padding.top + this.innerHeight - boldBarHeight,
          boldBarWidth,
          boldBarHeight
        );
        start += boldBarWidth; // TODO: Shift a bit
      } else {
        // Draw lightbar
        this.ctx.fillStyle = Color.GUIDE_BAR_LIGHT;
        this.ctx.fillRect(
          start,
          this.padding.top + this.innerHeight - lightBarHeight,
          lightBarWidth,
          lightBarHeight
        );
        start += lightBarWidth;
      }
      start += spacing;
    }
  }
}
