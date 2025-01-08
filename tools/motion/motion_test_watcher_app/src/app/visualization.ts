import { checkArgument, checkNotNull } from '../utils/preconditions';
import { DataPoint, isNotFound } from './golden';
import { VisualTimeline } from './visual-timeline';

export interface Visualization {
  render(
    timeline: VisualTimeline,
    dataPoints: Array<DataPoint>,
    canvas: HTMLCanvasElement,
  ): void;
}

const PROBE_COLORS = [
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
  color: string = PROBE_COLORS[0];
  fillColor: string = lighten(this.color);

  constructor(
    public minValue: number,
    public maxValue: number,
  ) {
    checkArgument(
      minValue < maxValue,
      `minValue ${minValue} >= maxValue ${maxValue}`,
    );
  }

  render(
    timeline: VisualTimeline,
    dataPoints: Array<DataPoint>,
    canvas: HTMLCanvasElement,
  ) {
    const cw = canvas.width;
    const ch = 30;

    const ctx = checkNotNull(canvas.getContext('2d'));
    ctx.clearRect(0, 0, cw, canvas.height);

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

      const bottomLineWidth = 2;
      // solid bottom line
      ctx.fillStyle = color;
      ctx.arc(
        timeline.frameToPx(start),
        ch - bottomLineWidth / 2,
        5,
        0,
        2 * Math.PI,
      );
      ctx.arc(
        timeline.frameToPx(end - 1),
        ch - bottomLineWidth / 2,
        5,
        0,
        2 * Math.PI,
      );
      ctx.fill();

      ctx.save();
      try {
        ctx.fillStyle = this.fillColor;
        ctx.beginPath();
        ctx.moveTo(timeline.frameToPx(start), ch - bottomLineWidth);
        for (let i = start; i < end; i++) {
          const value = dataPoints.at(i);
          if (typeof value !== 'number') {
            continue;
          }
          ctx.lineTo(
            timeline.frameToPx(i),
            (1 - (value - min) / (max - min)) * ch,
          );
        }
        ctx.lineTo(timeline.frameToPx(end - 1), ch - bottomLineWidth);
        ctx.fill();
      } finally {
        ctx.restore();
      }

      ctx.beginPath();
      ctx.strokeStyle = color;
      ctx.lineWidth = bottomLineWidth;

      ctx.moveTo(timeline.frameToPx(start), ch - bottomLineWidth / 2);
      ctx.lineTo(timeline.frameToPx(end - 1), ch - bottomLineWidth / 2);

      ctx.stroke();
    }
  }
}

export class DataPointVisualization implements Visualization {
  color: string = PROBE_COLORS[0];

  render(
    timeline: VisualTimeline,
    dataPoints: Array<DataPoint>,
    canvas: HTMLCanvasElement,
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
        /* endAngle */ 2 * Math.PI,
      );
      ctx.fill();
    });
  }
}
