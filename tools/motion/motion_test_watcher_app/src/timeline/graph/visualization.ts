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

export const COLORS = {
  gray: 'rgb(99, 99, 99)',
  green: 'rgb(76, 199, 45)',
  blue: 'rgb(98, 32, 221)',
  red: 'rgb(240, 88, 88)'
};
