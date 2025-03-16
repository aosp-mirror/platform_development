import { ProgressTracker } from './../util/progress';
import { GoldensService } from './../service/goldens.service';
import { Component, DoCheck, OnInit } from '@angular/core';
import { MatToolbarModule } from '@angular/material/toolbar';
import { TestListComponent } from '../test-list/test-list.component';
import { PreviewComponent } from '../preview/preview.component';
import { TimelineComponent } from '../timeline/timeline.component';
import { MotionGolden } from '../model/golden';
import { finalize } from 'rxjs';
import { NgIf } from '@angular/common';

@Component({
  selector: 'app-root',
  imports: [
    MatToolbarModule,
    TestListComponent,
    PreviewComponent,
    TimelineComponent,
    NgIf,
  ],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css',
})
export class AppComponent implements DoCheck, OnInit {
  constructor(
    private goldenService: GoldensService,
    private progressTracker: ProgressTracker
  ) {}

  showProgress = false;
  goldens: MotionGolden[] = [];
  selectedGolden: MotionGolden | null = null;

  ngDoCheck(): void {
    this.showProgress = this.progressTracker.isActive;
  }

  ngOnInit(): void {
    this.fetchGoldens();
  }

  fetchGoldens(): void {
    this.progressTracker.beginProgress;
    this.goldenService
      .getGoldens()
      .pipe(finalize(() => this.progressTracker.endProgress))
      .subscribe((goldens) => (this.goldens = goldens));
  }

  refreshGoldens(clear: boolean): void {
    this.progressTracker.beginProgress();
    this.goldenService
      .refreshGoldens(clear)
      .pipe(finalize(() => this.progressTracker.endProgress))
      .subscribe((goldens) => (this.goldens = goldens));
  }

  setSelectedGolden(golden: MotionGolden): void {
    this.selectedGolden = golden;
  }
}
