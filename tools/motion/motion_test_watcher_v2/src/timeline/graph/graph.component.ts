import { PreviewService } from './../../service/preview.service';
import {
  AfterViewInit,
  Component,
  Input,
  ViewChild,
  OnChanges,
  SimpleChanges,
  ElementRef,
} from '@angular/core';
import { MotionGoldenData, MotionGoldenFeature } from '../../model/golden';
import { Visualization, DataPoint } from './visualization';
import { LineGraphVisualization } from './line-graph-visualization';
import * as d3 from 'd3';
import { NgIf } from '@angular/common';

@Component({
  selector: 'app-graph',
  imports: [NgIf],
  templateUrl: './graph.component.html',
  styleUrl: './graph.component.css',
})
export class GraphComponent implements AfterViewInit, OnChanges {
  constructor(private previewService: PreviewService) {}

  @Input() expectedData: MotionGoldenData | undefined;
  @Input() actualData: MotionGoldenData | undefined;
  @Input() featureName: string | undefined;

  @ViewChild('chartContainer', { static: true })
  chartContainer!: ElementRef<HTMLDivElement>;

  private svg!: d3.Selection<SVGSVGElement, unknown, null, undefined>;
  private width!: number;
  private height!: number;
  private data: DataPoint[] = [];
  private visualization!: Visualization;

  ngAfterViewInit(): void {
    this.width = this.chartContainer.nativeElement.offsetWidth;
    this.height = this.chartContainer.nativeElement.offsetHeight;
    this.createChart();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (
      changes['actualData'] ||
      changes['expectedData'] ||
      changes['featureName']
    ) {
      this.updateData();
      this.createChart();
    }
  }

  private updateData(): void {
    this.data = [];
    const actualFeature =
      this.featureName && this.actualData
        ? this.actualData.features.find((f) => f.name === this.featureName)
        : undefined;
    const expectedFeature =
      this.featureName && this.expectedData
        ? this.expectedData.features.find((f) => f.name === this.featureName)
        : undefined;

    this.visualization = this.createVisualization(actualFeature);

    if (this.visualization instanceof LineGraphVisualization) {
      this.createLineChartData(actualFeature, expectedFeature);
    } else {
    }
  }

  private createLineChartData(
    actualFeature: MotionGoldenFeature | undefined,
    expectedFeature: MotionGoldenFeature | undefined
  ) {
    const combinedLength = Math.max(
      actualFeature?.data_points?.length || 0,
      expectedFeature?.data_points?.length || 0
    );

    for (let i = 0; i < combinedLength; i++) {
      const actualDataPoint =
        actualFeature?.data_points && actualFeature.data_points[i];
      const expectedDataPoint =
        expectedFeature?.data_points && expectedFeature.data_points[i];

      let x = -1;
      if (this.actualData?.frame_ids[i] === 'after') {
        x = (this.actualData?.frame_ids[i - 1] as number) + 50;
      } else if (this.actualData?.frame_ids[i] === 'before') {
        x = (this.actualData?.frame_ids[i + 1] as number) - 50;
      } else {
        x = this.actualData?.frame_ids[i] as number;
      }

      const newPoint: DataPoint = { x };
      if (actualDataPoint && typeof actualDataPoint === 'number') {
        newPoint.actualValue = actualDataPoint;
      }
      if (expectedDataPoint && typeof expectedDataPoint === 'number') {
        newPoint.expectedValue = expectedDataPoint;
      }

      this.data.push(newPoint);
    }
  }

  private createVisualization(
    actualFeature: MotionGoldenFeature | undefined,
    expectedFeature: MotionGoldenFeature | undefined = undefined
  ): Visualization {
    const name = actualFeature?.name;
    const type = actualFeature?.type;

    if (
      actualFeature &&
      ['float', 'int', 'dp', 'dpOffset', 'dpSize', 'offset'].includes(
        type || ''
      )
    ) {
      const numericValues =
        actualFeature.data_points?.filter(
          (it): it is number => typeof it === 'number'
        ) || [];
      const minValue = Math.min(...numericValues) ?? 0;
      let maxValue = Math.max(...numericValues) ?? 1;

      if (minValue === maxValue) {
        maxValue += 1;
      }

      return new LineGraphVisualization(
        minValue,
        maxValue,
        this.previewService
      );
    }
    return new LineGraphVisualization(0, 1, this.previewService);
  }

  private createChart(): void {
    this.chartContainer.nativeElement.innerHTML = '';
    this.svg = d3
      .select(this.chartContainer.nativeElement)
      .append('svg')
      .attr('width', this.width)
      .attr('height', this.height);
    this.visualization.render(this.svg, this.data, this.width, this.height);
  }
}
