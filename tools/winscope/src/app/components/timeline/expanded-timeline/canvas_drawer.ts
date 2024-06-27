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

import {TimelineUtils} from 'app/components/timeline/timeline_utils';
import {assertDefined} from 'common/assert_utils';
import {Rect} from 'common/rect';

export class CanvasDrawer {
  private canvas: HTMLCanvasElement | undefined;
  private ctx: CanvasRenderingContext2D | undefined;

  setCanvas(canvas: HTMLCanvasElement) {
    this.canvas = canvas;
    const ctx = this.canvas.getContext('2d');
    if (ctx === null) {
      throw new Error("Couldn't get context from canvas");
    }
    this.ctx = ctx;
  }

  drawRect(
    rect: Rect,
    hexColor: string,
    alpha: number,
    withGradientStart = false,
    withGradientEnd = false,
  ) {
    if (!this.ctx) {
      throw new Error('Canvas not set');
    }

    const rgbColor = TimelineUtils.convertHexToRgb(hexColor);
    if (rgbColor === undefined) {
      throw new Error('Failed to parse provided hex color');
    }
    const {r, g, b} = rgbColor;
    const rgbaColor = `rgba(${r},${g},${b},${alpha})`;
    const transparentColor = `rgba(${r},${g},${b},${0})`;

    this.defineRectPath(rect, this.ctx);
    if (withGradientStart || withGradientEnd) {
      const gradient = this.ctx.createLinearGradient(
        rect.x,
        0,
        rect.x + rect.w,
        0,
      );
      const gradientRatio = Math.max(0, Math.min(25 / rect.w, 1));
      gradient.addColorStop(
        0,
        withGradientStart ? transparentColor : rgbaColor,
      );
      gradient.addColorStop(1, withGradientEnd ? transparentColor : rgbaColor);
      gradient.addColorStop(gradientRatio, rgbaColor);
      gradient.addColorStop(1 - gradientRatio, rgbaColor);
      this.ctx.fillStyle = gradient;
    } else {
      this.ctx.fillStyle = rgbaColor;
    }
    this.ctx.fill();

    this.ctx.fillStyle = 'black';
    const centerY = rect.y + rect.h / 2;
    const marginOffset = 5;
    if (withGradientStart) {
      this.drawEllipsis(centerY, rect.x + marginOffset, true, rect.x + rect.w);
    }

    if (withGradientEnd) {
      this.drawEllipsis(centerY, rect.x + rect.w - marginOffset, false, rect.x);
    }

    this.ctx.restore();
  }

  private drawEllipsis(
    centerY: number,
    startX: number,
    forwards: boolean,
    xLim: number,
  ) {
    if (!this.ctx) {
      return;
    }
    let centerX = startX;
    let i = 0;
    const radius = 2;
    while (i < 3) {
      if (forwards && centerX + radius >= xLim) {
        break;
      }
      if (!forwards && centerX + radius <= xLim) {
        break;
      }
      this.ctx.beginPath();
      this.ctx.arc(centerX, centerY, radius, 0, 2 * Math.PI);
      this.ctx.fill();
      centerX = forwards ? centerX + 7 : centerX - 7;
      i++;
    }
  }

  drawRectBorder(rect: Rect) {
    if (!this.ctx) {
      throw new Error('Canvas not set');
    }
    this.defineRectPath(rect, this.ctx);
    this.highlightPath(this.ctx);
    this.ctx.restore();
  }

  clear() {
    assertDefined(this.ctx).clearRect(
      0,
      0,
      this.getScaledCanvasWidth(),
      this.getScaledCanvasHeight(),
    );
  }

  getScaledCanvasWidth() {
    return Math.floor(assertDefined(this.canvas).width / this.getXScale());
  }

  getScaledCanvasHeight() {
    return Math.floor(assertDefined(this.canvas).height / this.getYScale());
  }

  getXScale(): number {
    return assertDefined(this.ctx).getTransform().m11;
  }

  getYScale(): number {
    return assertDefined(this.ctx).getTransform().m22;
  }

  private highlightPath(ctx: CanvasRenderingContext2D) {
    ctx.globalAlpha = 1.0;
    ctx.lineWidth = 2;
    ctx.save();
    ctx.clip();
    ctx.lineWidth *= 2;
    ctx.stroke();
    ctx.restore();
    ctx.stroke();
  }

  private defineRectPath(rect: Rect, ctx: CanvasRenderingContext2D) {
    ctx.beginPath();
    ctx.moveTo(rect.x, rect.y);
    ctx.lineTo(rect.x + rect.w, rect.y);
    ctx.lineTo(rect.x + rect.w, rect.y + rect.h);
    ctx.lineTo(rect.x, rect.y + rect.h);
    ctx.lineTo(rect.x, rect.y);
    ctx.closePath();
  }
}
