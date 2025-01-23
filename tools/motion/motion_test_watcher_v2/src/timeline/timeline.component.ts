import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import {
  MotionGolden,
  MotionGoldenData,
  MotionGoldenFeature,
} from '../model/golden';
import { GoldensService } from '../service/goldens.service';
import { PreviewService } from '../service/preview.service';
import { NgIf } from '@angular/common';
import { forkJoin } from 'rxjs';
import { GraphComponent } from './graph/graph.component';
import { MatSnackBar } from '@angular/material/snack-bar';

@Component({
  selector: 'app-timeline',
  imports: [NgIf, GraphComponent],
  templateUrl: './timeline.component.html',
  styleUrl: './timeline.component.css',
})
export class TimelineComponent implements OnChanges {
  constructor(
    private goldenService: GoldensService,
    private snackBar: MatSnackBar,
    private preivewService: PreviewService
  ) {}

  @Input() selectedGolden: MotionGolden | null = null;

  actualData: MotionGoldenData | undefined;
  expectedData: MotionGoldenData | undefined;
  loading: boolean = false;
  featureCount = 0;
  selectedFeatureIdx: number = 0;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['selectedGolden']) {
      this.updatePage();
    }
  }

  updatePage() {
    if (!this.selectedGolden) return;
    this.loading = true;

    forkJoin([
      this.goldenService.getActualGoldenData(this.selectedGolden),
      this.goldenService.getExpectedGoldenData(this.selectedGolden),
    ]).subscribe({
      next: ([actualData, expectedData]) => {
        this.loading = false;
        this.expectedData = expectedData;
        this.actualData = actualData;
        this.preivewService.updateFrames(this.actualData.frame_ids);
        this.buildUi();
      },
      error: (err) => {
        this.loading = false;
        this.expectedData = undefined;
        this.actualData = undefined;
      },
    });
  }

  buildUi() {
    if (!this.selectedGolden) return;
    if (!this.actualData || !this.expectedData) return;
    this.processData(this.actualData);
    this.processData(this.expectedData);
    this.featureCount = this.actualData.features.length;
  }

  processData(data: MotionGoldenData) {
    const newFeatures: MotionGoldenFeature[] = [];

    data.features.forEach((feature) => {
      if (
        feature.data_points &&
        Array.isArray(feature.data_points) &&
        feature.data_points.length > 0
      ) {
        let firstValidDataPoint: any = null;
        for (const dataPoint of feature.data_points) {
          if (typeof dataPoint === 'object' && dataPoint !== null) {
            firstValidDataPoint = dataPoint;
            break;
          }
        }

        if (firstValidDataPoint) {
          const keys = Object.keys(firstValidDataPoint);
          keys.forEach((key) => {
            newFeatures.push({
              name: `${feature.name}.${key}`,
              type: feature.type,
              data_points: feature.data_points.map((point: any) =>
                point && typeof point === 'object' ? point[key] : undefined
              ),
            });
          });
        } else {
          newFeatures.push(feature);
        }
      } else {
        newFeatures.push(feature);
      }
    });

    data.features = newFeatures;
  }

  selectGraph(index: number) {
    this.selectedFeatureIdx = index;
  }

  onNext() {
    this.selectedFeatureIdx = (this.selectedFeatureIdx + 1) % this.featureCount;
  }

  onPrevious() {
    this.selectedFeatureIdx =
      (this.selectedFeatureIdx - 1 + this.featureCount) % this.featureCount;
  }

  updateGolden() {
    if (!this.selectedGolden) return;
    this.goldenService.updateGolden(this.selectedGolden).subscribe({
      next: () => {
        this.snackBar.open('Golden updated successfully!', 'Close', {
          duration: 3000,
          panelClass: 'success-snackbar',
        });
      },
      error: (err) => {
        console.error(err);
        this.snackBar.open(
          'Error updating golden. See console for details.',
          'Close',
          {
            duration: 5000,
            panelClass: 'error-snackbar',
          }
        );
      },
    });
  }

  getSelectedFeatureName(): string | undefined {
    if (this.actualData && this.selectedFeatureIdx !== undefined) {
      return this.actualData.features[this.selectedFeatureIdx]?.name;
    }
    return undefined;
  }

}
