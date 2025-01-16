import { Visualization, DataPoint } from './visualization';
import * as d3 from 'd3';
import { PreviewService } from '../../service/preview.service';

export class LineGraphVisualization implements Visualization {
  minValue: number;
  maxValue: number;
  viewSelectedCurrentFrame: number = 0;

  margin = { top: 20, right: 20, bottom: 30, left: 50 };
  chartWidth = 0;
  chartHeight = 0;
  xScale = d3.scaleLinear();
  yScale = d3.scaleLinear();

  constructor(
    minValue: number,
    maxValue: number,
    private previewService: PreviewService
  ) {
    this.minValue = minValue;
    this.maxValue = maxValue;
    this.previewService.currentFrameFromView$.subscribe((frame) => {
      if(this.viewSelectedCurrentFrame === frame) return;
      this.viewSelectedCurrentFrame = frame ? frame : 0;
      this.updateMarker();
    });
  }

  render(
    svg: d3.Selection<SVGSVGElement, unknown, null, undefined>,
    data: DataPoint[],
    width: number,
    height: number
  ): void {
    this.chartWidth = width - this.margin.left - this.margin.right;
    this.chartHeight = height - this.margin.top - this.margin.bottom;

    this.xScale = d3
      .scaleLinear()
      .domain(d3.extent(data, (d) => d.x) as [number, number])
      .range([0, this.chartWidth]);

    this.yScale = d3
      .scaleLinear()
      .domain([this.minValue, this.maxValue])
      .range([this.chartHeight, 0]);

    const xAxis = d3.axisBottom(this.xScale);
    const yAxis = d3.axisLeft(this.yScale);

    const actualLine = d3
      .line<DataPoint>()
      .x((d) => this.xScale(d.x))
      .y((d) => this.yScale(d.actualValue || 0));

    const expectedLine = d3
      .line<DataPoint>()
      .x((d) => this.xScale(d.x))
      .y((d) => this.yScale(d.expectedValue || 0));

    const g = svg
      .append('g')
      .attr('transform', `translate(${this.margin.left},${this.margin.top})`);

    g.append('g')
      .attr('class', 'x axis')
      .attr('transform', `translate(0,${this.chartHeight})`)
      .call(xAxis);

    g.append('g').attr('class', 'y axis').call(yAxis);

    g.append('path')
      .datum(data)
      .attr('fill', 'none')
      .attr('stroke', 'green')
      .attr('stroke-width', 1.5)
      .attr('d', expectedLine)

    g.append('path')
      .datum(data)
      .attr('fill', 'none')
      .attr('stroke', 'blue')
      .attr('stroke-width', 1.5)
      .attr('d', actualLine);

    const legend = g
      .append('g')
      .attr('class', 'legend')
      .attr(
        'transform',
        `translate(${this.chartWidth - 100}, ${this.chartHeight - 100})`
      );

    legend
      .append('line')
      .attr('x1', 0)
      .attr('y1', 0)
      .attr('x2', 20)
      .attr('y2', 0)
      .attr('stroke', 'blue')
      .attr('stroke-width', 1.5);

    legend
      .append('text')
      .attr('x', 25)
      .attr('y', 0)
      .text('Actual')
      .attr('alignment-baseline', 'middle');

    legend
      .append('line')
      .attr('x1', 0)
      .attr('y1', 20)
      .attr('x2', 20)
      .attr('y2', 20)
      .attr('stroke', 'green')
      .attr('stroke-width', 1.5);

    legend
      .append('text')
      .attr('x', 25)
      .attr('y', 20)
      .text('Expected')
      .attr('alignment-baseline', 'middle');

    g.append('line')
    .attr('class', 'currentFrameLine')
    .attr('x1', 0)
    .attr('y1', -100)
    .attr('x2', 0)
    .attr('y2', this.chartHeight)
    .attr('stroke', 'red')
    .attr('stroke-width', 1)
    .attr('stroke-linecap', 'butt')
    .attr('transform', `translate(${this.margin.left},${this.margin.top})`);
  }

  private updateMarker(): void {
    const svg = d3.select('g');
    if (!svg) return;

    svg.selectAll('.currentFrameLine').remove();
    const xPos = this.xScale(this.viewSelectedCurrentFrame);
    if (xPos >= 0 && xPos <= this.chartWidth) {
      svg
        .append('line')
        .attr('class', 'currentFrameLine')
        .attr('x1', xPos)
        .attr('y1', -100)
        .attr('x2', xPos)
        .attr('y2', this.chartHeight)
        .attr('stroke', 'red')
        .attr('stroke-width', 1)
        .attr('stroke-linecap', 'butt')
        .attr('transform', `translate(${this.margin.left},${this.margin.top})`);
    }
  }
}
