/*
 * Copyright 2024 Google LLC
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

import { checkArgument, checkNotNull } from '../util/preconditions';
import { DataPoint, isNotFound } from './golden';
import { VisualTimeline } from './visual-timeline';

export interface Visualization {
  height: number;
  render(
    timeline: VisualTimeline,
    dataPoints: Array<DataPoint>,
    canvas: HTMLCanvasElement
  ): void;
}

export const PROBE_COLORS = [
  '#E51C23',
  '#9C27B0',
  '#5677FC',
  '#00BCD4',
  '#259B24',
  '#CDDC39',
  '#FFC107',
  '#795548',
  '#737373',
];

function lighten(hex: string): string {
  hex = hex.replace(/[^0-9A-F]/gi, '');
  var bigint = parseInt(hex, 16);
  var r = (bigint >> 16) & 255;
  var g = (bigint >> 8) & 255;
  var b = bigint & 255;

  return 'rgba(' + r + ',' + g + ',' + b + ',0.15)';
}

export class LineGraphVisualization implements Visualization {
  color: string;
  fillColor: string;

  height: number = 96;

  constructor(
    public minValue: number,
    public maxValue: number,
    color: string | null
  ) {
    checkArgument(
      minValue < maxValue,
      `minValue ${minValue} >= maxValue ${maxValue}`
    );

    this.color = color ?? PROBE_COLORS[0];
    this.fillColor = lighten(this.color);
  }

  render(
    timeline: VisualTimeline,
    dataPoints: Array<DataPoint>,
    canvas: HTMLCanvasElement
  ) {
    const cw = canvas.width;
    const ch = 90;

    const ctx = checkNotNull(canvas.getContext('2d'));
    ctx.clearRect(0, 0, cw, canvas.height);
    ctx.save();
    ctx.translate(0, 5);

    const min = this.minValue;
    const max = this.maxValue;
    const color = this.color;

    for (let i = 0; i < dataPoints.length; i++) {
      if (isNotFound(dataPoints[i])) {
        continue;
      }
      const start = i;

      while (i < dataPoints.length && !isNotFound(dataPoints[i])) {
        i++;
      }
      const end = i;

      const bottomLineWidth = 1;

      const mid = min <= 0 && max >= 0 ? 0 : min > 0 ? min : max;

      const midY = (1 - (mid - min) / (max - min)) * ch;

      ctx.save();
      try {
        ctx.fillStyle = this.fillColor;
        ctx.beginPath();
        ctx.moveTo(timeline.frameToPx(start), midY);
        for (let i = start; i < end; i++) {
          const value = dataPoints.at(i);
          if (typeof value !== 'number') {
            continue;
          }
          ctx.lineTo(
            timeline.frameToPx(i),
            (1 - (value - min) / (max - min)) * ch
          );
        }
        ctx.lineTo(timeline.frameToPx(end - 1), midY);
        ctx.fill();
      } finally {
        ctx.restore();
      }
      ctx.save();
      try {
        ctx.beginPath();
        ctx.strokeStyle = color;
        ctx.lineWidth = bottomLineWidth;

        ctx.moveTo(timeline.frameToPx(start), midY);
        ctx.lineTo(timeline.frameToPx(end - 1), midY);

        ctx.stroke();
      } finally {
        ctx.restore();
      }

      ctx.fillStyle = color;

      for (let i = start; i < end; i++) {
        const value = dataPoints.at(i);
        if (typeof value !== 'number') {
          continue;
        }
        ctx.beginPath();

        ctx.arc(
          timeline.frameToPx(i),
          (1 - (value - min) / (max - min)) * ch,
          2,
          0,
          2 * Math.PI
        );

        ctx.fill();
      }

      ctx.restore();
    }
  }
}

export class DataPointVisualization implements Visualization {
  color: string = PROBE_COLORS[0];
  height: number = 32;

  render(
    timeline: VisualTimeline,
    dataPoints: Array<DataPoint>,
    canvas: HTMLCanvasElement
  ) {
    const cw = canvas.width;

    const ctx = checkNotNull(canvas.getContext('2d'));
    ctx.clearRect(0, 0, cw, canvas.height);

    ctx.fillStyle = this.color;

    dataPoints.forEach((dataPoint, idx) => {
      if (isNotFound(dataPoint)) return;
      ctx.beginPath();

      ctx.arc(
        /* x */ timeline.frameToPx(idx),
        /* y */ 15,
        /* radius */ 5,
        /* startAngle */ 0,
        /* endAngle */ 2 * Math.PI
      );
      ctx.fill();
    });
  }
}
