export interface Visualization {
  render(
    svg: d3.Selection<SVGSVGElement, unknown, null, undefined>,
    data: DataPoint[],
    width: number,
    height: number
  ): void;
}

export interface DataPoint {
  x: number;
  actualValue?: number;
  expectedValue?: number;
}
