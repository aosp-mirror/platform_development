import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { filter, finalize } from 'rxjs';
import { MatTooltipModule, TooltipPosition } from '@angular/material/tooltip';
import { MotionGolden } from '../golden';
import { GoldensService } from '../goldens.service';
import { RecordedMotion } from '../recorded-motion';
import { ProgressTracker } from '../../utils/progress';
import { VideoViewComponent } from '../video-view/video-view.component';
import { TimelineViewComponent } from '../timeline-view/timeline-view.component';
import { MatToolbarModule } from '@angular/material/toolbar';
import { VideoControlsComponent } from '../video-controls/video-controls.component';
import { MatCardModule } from '@angular/material/card';
import { DragDropModule } from '@angular/cdk/drag-drop';

@Component({
  selector: 'app-motion-golden',
  standalone: true,
  imports: [
    MatIconModule,
    MatButtonModule,
    MatToolbarModule,
    VideoControlsComponent,
    MatTooltipModule,
    VideoViewComponent,
    MatCardModule,
    TimelineViewComponent,
    DragDropModule,
  ],
  templateUrl: './motion-golden.component.html',
  styleUrl: './motion-golden.component.scss',
})
export class MotionGoldenComponent implements OnChanges {
  constructor(
    private goldenService: GoldensService,
    private snackBar: MatSnackBar,
    private progressTracker: ProgressTracker,
  ) {}

  @Input() golden!: MotionGolden;
  tooltipPosition: TooltipPosition = 'right';

  recordedMotion: RecordedMotion | undefined;

  private goldenChangeId = 0;
  ngOnChanges(changes: SimpleChanges): void {
    if (changes['golden']) {
      this.goldenChangeId++;
      if (this.golden) {
        this.fetchRecordedMotion(this.golden, this.goldenChangeId);
      } else {
        this.recordedMotion = undefined;
      }
    }
  }

  fetchRecordedMotion(golden: MotionGolden, goldenChangeId: number): void {
    this.progressTracker.beginProgress();
    this.goldenService
      .loadRecordedMotion(golden)
      .pipe(
        filter((_) => goldenChangeId === this.goldenChangeId),
        finalize(() => this.progressTracker.endProgress()),
      )
      .subscribe((recordedMotion) => (this.recordedMotion = recordedMotion));
  }

  updateGolden(): void {
    this.progressTracker.beginProgress();
    this.goldenService
      .updateGolden(this.golden)
      .pipe(finalize(() => this.progressTracker.endProgress()))
      .subscribe((_) => this.snackBar.open('updated'));
  }
}
