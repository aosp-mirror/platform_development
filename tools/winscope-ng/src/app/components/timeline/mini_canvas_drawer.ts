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

import { TRACE_INFO } from "app/trace_info";
import { Color } from "../../colors";
import { TraceType } from "common/trace/trace_type";
import { CanvasDrawer } from "../canvas/canvas_drawer";
import { DraggableCanvasObject } from "../canvas/draggable_canvas_object";
import { CanvasMouseHandler } from "../canvas/canvas_mouse_handler";
import { BigIntSegment, Segment, TimelineData } from "./utils";

export class MiniCanvasDrawerInput {
  constructor(
    public fullRange: BigIntSegment,
    public selectedPosition: bigint,
    public selection: BigIntSegment,
    public timelineEntries: TimelineData,
  ) {}

  public transform(mapToRange: Segment): MiniCanvasDrawerData {
    const transformer = new Transformer(this.fullRange, mapToRange);
    return new MiniCanvasDrawerData(
      transformer.transform(this.selectedPosition),
      {
        from: transformer.transform(this.selection.from),
        to: transformer.transform(this.selection.to),
      },
      this.computeTransformedTraceSegments(transformer),
      transformer
    );
  }

  private computeTransformedTraceSegments(transformer: Transformer):  Map<TraceType, number[]> {
    const transformedTraceSegments = new Map<TraceType, number[]>();

    this.timelineEntries.forEach((entries, traceType) => {
      transformedTraceSegments.set(
        traceType,
        entries.map((entry) => transformer.transform(entry))
      );
    });

    return transformedTraceSegments;
  }
}

export class Transformer {
  private fromWidth: bigint;
  private targetWidth: number;

  private fromOffset: bigint;
  private toOffset: number;

  constructor(private fromRange: BigIntSegment, private toRange: Segment) {
    this.fromWidth = this.fromRange.to - this.fromRange.from;
    // Needs to be a whole number to be compatible with bigints
    this.targetWidth = Math.round(this.toRange.to - this.toRange.from);

    this.fromOffset = this.fromRange.from;
    // Needs to be a whole number to be compatible with bigints
    this.toOffset = Math.round(this.toRange.from);
  }

  public transform(x: bigint): number {
    return this.toOffset + this.targetWidth * Number(x - this.fromOffset) / Number(this.fromWidth);
  }

  public untransform(x: number): bigint {
    x = Math.round(x);
    return this.fromOffset + BigInt((x - this.toOffset)) * this.fromWidth / BigInt(this.targetWidth);
  }
}

class MiniCanvasDrawerOutput {
  constructor(
    public selectedPosition: bigint,
    public selection: BigIntSegment,
  ) {}
}

class MiniCanvasDrawerData {
  constructor(
    public selectedPosition: number,
    public selection: Segment,
    public timelineEntries: Map<TraceType, number[]>,
    public transformer: Transformer
  ) {}

  public toOutput(): MiniCanvasDrawerOutput {
    return new MiniCanvasDrawerOutput(
      this.transformer.untransform(this.selectedPosition),
      {
        from: this.transformer.untransform(this.selection.from),
        to: this.transformer.untransform(this.selection.to),
      }
    );
  }
}

export class MiniCanvasDrawer implements CanvasDrawer {

  public ctx: CanvasRenderingContext2D;
  public handler: CanvasMouseHandler;

  private activePointer: DraggableCanvasObject;
  private leftFocusSectionSelector: DraggableCanvasObject;
  private rightFocusSectionSelector: DraggableCanvasObject;

  private get pointerWidth() {
    return this.getScaledCanvasHeight() / 6;
  }

  public getXScale() {
    return this.ctx.getTransform().m11;
  }

  public getYScale() {
    return this.ctx.getTransform().m22;
  }

  getScaledCanvasWidth() {
    return this.canvas.width / this.getXScale();
  }

  getScaledCanvasHeight() {
    return this.canvas.height / this.getYScale();
  }

  get usableRange() {
    return {
      from: this.padding.left,
      to: this.getScaledCanvasWidth() - this.padding.left - this.padding.right
    };
  }

  get input() {
    return this.inputGetter().transform(this.usableRange);
  }

  constructor(
    public canvas: HTMLCanvasElement,
    private inputGetter: () => MiniCanvasDrawerInput,
    private onPointerPositionDragging: (pos: bigint) => void,
    private onPointerPositionChanged: (pos: bigint) => void,
    private onSelectionChanged: (selection: BigIntSegment) => void,
    private onUnhandledClick: (pos: bigint) => void,
  ) {
    const ctx = canvas.getContext("2d");

    if (ctx === null) {
      throw Error("MiniTimeline canvas context was null!");
    }

    this.ctx = ctx;

    const onUnhandledClickInternal = (x: number, y: number) => {
      this.onUnhandledClick(this.input.transformer.untransform(x));
    };
    this.handler = new CanvasMouseHandler(this, "pointer", onUnhandledClickInternal);

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
        ctx.lineTo(position + barWidth / 2, this.getScaledCanvasHeight());
        ctx.lineTo(position - barWidth / 2, this.getScaledCanvasHeight());
        ctx.lineTo(position - barWidth / 2, triangleHeight);
        ctx.closePath();
      },
      {
        fillStyle: Color.ACTIVE_POINTER,
        fill: true
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
        to: this.input.transformer.untransform(this.selection.to)
      });
    };
    const onRightSelectionChanged = (x: number) => {
      this.selection.to = x;
      this.onSelectionChanged({
        from: this.input.transformer.untransform(this.selection.from),
        to: this.input.transformer.untransform(x)
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
          to: this.rightFocusSectionSelector.position - selectorArrowWidth - barWidth
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
          to: this.usableRange.to
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
      top: Math.ceil(this.getScaledCanvasHeight() / 5),
      bottom: Math.ceil(this.getScaledCanvasHeight() / 5),
      left: Math.ceil(this.pointerWidth / 2),
      right: Math.ceil(this.pointerWidth / 2),
    };
  }

  get innerHeight() {
    return this.getScaledCanvasHeight() - this.padding.top - this.padding.bottom;
  }

  public draw() {
    this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);

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
    this.ctx.fillRect(this.selection.from, this.padding.top + triangleHeight / 2, width, this.innerHeight - triangleHeight / 2);
    this.ctx.restore();
  }

  private drawTraceLines() {
    const lineHeight = this.innerHeight / 8;

    let fromTop = this.padding.top + this.innerHeight * 2 / 3 - lineHeight;

    this.timelineEntries.forEach((entries, traceType) => {
      // TODO: Only if active or a selected trace
      for (const entry of entries) {
        this.ctx.globalAlpha = 0.7;
        this.ctx.fillStyle = TRACE_INFO[traceType].color;

        const width = 5;
        this.ctx.fillRect(entry - width / 2, fromTop, width, lineHeight);
        this.ctx.globalAlpha = 1.0;
      }

      fromTop -= lineHeight * 4/3;
    });
  }

  private drawTimelineGuides() {
    const edgeBarHeight = this.innerHeight * 1 / 2;
    const edgeBarWidth = 4;

    const boldBarHeight = this.innerHeight * 1 / 5;
    const boldBarWidth = edgeBarWidth;

    const lightBarHeight = this.innerHeight * 1 / 6;
    const lightBarWidth = 2;

    const minSpacing = lightBarWidth * 7;
    const barsInSetWidth = (9 * lightBarWidth + boldBarWidth);
    const barSets = Math.floor((this.getScaledCanvasWidth() - edgeBarWidth * 2 - minSpacing) / (barsInSetWidth + 10 * minSpacing));
    const bars = barSets * 10;

    // Draw start bar
    this.ctx.fillStyle = Color.GUIDE_BAR;
    this.ctx.fillRect(0, this.padding.top + this.innerHeight - edgeBarHeight, edgeBarWidth, edgeBarHeight);

    // Draw end bar
    this.ctx.fillStyle = Color.GUIDE_BAR;
    this.ctx.fillRect(this.getScaledCanvasWidth() - edgeBarWidth, this.padding.top + this.innerHeight - edgeBarHeight, edgeBarWidth, edgeBarHeight);

    const spacing = (this.getScaledCanvasWidth() - barSets * barsInSetWidth - edgeBarWidth) / (bars);
    let start = edgeBarWidth + spacing;
    for (let i = 1; i < bars; i++) {
      if (i % 10 == 0) {
        // Draw boldbar
        this.ctx.fillStyle = Color.GUIDE_BAR;
        this.ctx.fillRect(start, this.padding.top + this.innerHeight - boldBarHeight, boldBarWidth, boldBarHeight);
        start += boldBarWidth; // TODO: Shift a bit
      } else {
        // Draw lightbar
        this.ctx.fillStyle = Color.GUIDE_BAR_LIGHT;
        this.ctx.fillRect(start, this.padding.top + this.innerHeight - lightBarHeight, lightBarWidth, lightBarHeight);
        start += lightBarWidth;
      }
      start += spacing;
    }
  }
}
