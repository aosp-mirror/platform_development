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

import {Rect} from 'common/geometry_utils';

export class CanvasDrawer {
  private canvas!: HTMLCanvasElement;
  private ctx!: CanvasRenderingContext2D;

  setCanvas(canvas: HTMLCanvasElement) {
    this.canvas = canvas;
    const ctx = this.canvas.getContext('2d');
    if (ctx === null) {
      throw new Error("Couldn't get context from canvas");
    }
    this.ctx = ctx;
  }

  drawRect(rect: Rect, color: string, alpha: number) {
    const rgbColor = this.hexToRgb(color);
    if (rgbColor === undefined) {
      throw new Error('Failed to parse provided hex color');
    }
    const {r, g, b} = rgbColor;

    this.defineRectPath(rect);
    this.ctx.fillStyle = `rgba(${r},${g},${b},${alpha})`;
    this.ctx.fill();

    this.ctx.restore();
  }

  drawRectBorder(rect: Rect) {
    this.defineRectPath(rect);
    this.highlightPath();
    this.ctx.restore();
  }

  clear() {
    this.ctx.clearRect(0, 0, this.getScaledCanvasWidth(), this.getScaledCanvasHeight());
  }

  getScaledCanvasWidth() {
    return Math.floor(this.canvas.width / this.getXScale());
  }

  getScaledCanvasHeight() {
    return Math.floor(this.canvas.height / this.getYScale());
  }

  getXScale(): number {
    return this.ctx.getTransform().m11;
  }

  getYScale(): number {
    return this.ctx.getTransform().m22;
  }

  private highlightPath() {
    this.ctx.globalAlpha = 1.0;
    this.ctx.lineWidth = 2;
    this.ctx.save();
    this.ctx.clip();
    this.ctx.lineWidth *= 2;
    this.ctx.stroke();
    this.ctx.restore();
    this.ctx.stroke();
  }

  private defineRectPath(rect: Rect) {
    this.ctx.beginPath();
    this.ctx.moveTo(rect.x, rect.y);
    this.ctx.lineTo(rect.x + rect.w, rect.y);
    this.ctx.lineTo(rect.x + rect.w, rect.y + rect.h);
    this.ctx.lineTo(rect.x, rect.y + rect.h);
    this.ctx.lineTo(rect.x, rect.y);
    this.ctx.closePath();
  }

  private hexToRgb(hex: string): {r: number; g: number; b: number} | undefined {
    // Expand shorthand form (e.g. "03F") to full form (e.g. "0033FF")
    const shorthandRegex = /^#?([a-f\d])([a-f\d])([a-f\d])$/i;
    hex = hex.replace(shorthandRegex, (m, r, g, b) => {
      return r + r + g + g + b + b;
    });

    const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
    return result
      ? {
          // tslint:disable-next-line:ban
          r: parseInt(result[1], 16),
          // tslint:disable-next-line:ban
          g: parseInt(result[2], 16),
          // tslint:disable-next-line:ban
          b: parseInt(result[3], 16),
        }
      : undefined;
  }
}
