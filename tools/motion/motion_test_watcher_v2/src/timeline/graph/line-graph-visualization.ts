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
      if (this.viewSelectedCurrentFrame === frame) return;
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
      .attr('stroke', 'red')
      .attr('stroke-width', 1.5)
      .attr('d', expectedLine);

    g.selectAll('.dot-expected')
      .data(data.filter((d) => d.expectedValue !== undefined))
      .enter()
      .append('circle')
      .attr('class', 'dot-expected')
      .attr('cx', (d) => this.xScale(d.x))
      .attr('cy', (d) => this.yScale(d.expectedValue || 0))
      .attr('r', 3)
      .attr('fill', 'red');

    g.append('path')
      .datum(data)
      .attr('fill', 'none')
      .attr('stroke', 'blue')
      .attr('stroke-width', 1.5)
      .attr('d', actualLine);

    g.selectAll('.dot-actual')
      .data(data.filter((d) => d.actualValue !== undefined))
      .enter()
      .append('circle')
      .attr('class', 'dot-actual')
      .attr('cx', (d) => this.xScale(d.x))
      .attr('cy', (d) => this.yScale(d.actualValue || 0))
      .attr('r', 2)
      .attr('fill', 'blue');

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
      .attr('stroke', 'red')
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

    // Hover over

    const markerLine = g
      .append('line')
      .attr('class', 'marker-line')
      .attr('y1', 0)
      .attr('y2', this.chartHeight)
      .attr('stroke', 'lightblue')
      .attr('stroke-width', 1)
      .style('opacity', 0);

    const tooltip = g
      .append('g')
      .attr('class', 'tooltip')
      .style('display', 'none');

    const tooltipRect = tooltip
      .append('rect')
      .attr('fill', 'white')
      .attr('stroke', 'black')
      .attr('rx', 5);

    const tooltipText = tooltip.append('text').attr('fill', 'black');

    g.append('rect')
      .attr('class', 'overlay')
      .attr('width', this.chartWidth)
      .attr('height', this.chartHeight)
      .attr('fill', 'none')
      .attr('pointer-events', 'all')
      .on('mouseover', () => {
        markerLine.style('opacity', 1);
        tooltip.style('opacity', 1);
      })
      .on('mouseout', () => {
        markerLine.style('opacity', 0);
        tooltip.style('opacity', 0);
      })
      .on('mousemove', (event: MouseEvent) => {
        const xPos = d3.pointer(event, g.node())[0];
        const dataPoint = this.getDataPointAtX(xPos, data);
        if (dataPoint) {
          const snappedXPos = this.xScale(dataPoint.x);
          markerLine.attr('x1', snappedXPos).attr('x2', snappedXPos);

          tooltipText
            .text(`Actual: ${dataPoint.actualValue}`)
            .append('tspan')
            .attr('x', 0)
            .attr('dy', '1.2em')
            .text(`Expected: ${dataPoint.expectedValue}`);
          const textBBox = (tooltipText.node() as SVGTextElement).getBBox();
          tooltipRect
            .attr('x', textBBox.x - 5)
            .attr('y', textBBox.y - 5)
            .attr('width', textBBox.width + 10)
            .attr('height', textBBox.height + 10);

          let tooltipX = snappedXPos + 10;
          const tooltipWidth = textBBox.width + 10;
          if (tooltipX + tooltipWidth > this.chartWidth) {
            tooltipX = snappedXPos - tooltipWidth - 10;
          }

          tooltip.attr(
            'transform',
            `translate(${tooltipX},${this.yScale(dataPoint.actualValue || 0)})`
          );

          tooltip.style('display', 'block');
        } else {
          tooltip.style('display', 'none');
        }
      });
  }

  private getDataPointAtX(x: number, data: DataPoint[]): DataPoint | null {
    const xValue = this.xScale.invert(x);
    let closestDataPoint = null;
    let minDistance = Infinity;

    for (const dataPoint of data) {
      const distance = Math.abs(dataPoint.x - xValue);
      if (distance < minDistance) {
        minDistance = distance;
        closestDataPoint = dataPoint;
      }
    }
    return closestDataPoint;
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
