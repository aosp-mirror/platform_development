import { Component, DoCheck } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatToolbarModule } from '@angular/material/toolbar';
import { RouterOutlet } from '@angular/router';
import { finalize } from 'rxjs';

import { MotionGolden } from './golden';
import { GoldensService } from './goldens.service';
import { TestOverviewComponent } from './test-overview/test-overview.component';
import { ProgressTracker } from '../utils/progress';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    RouterOutlet,
    MatToolbarModule,
    TestOverviewComponent,
    MatIconModule,
    MatDividerModule,
    MatButtonModule,
    MatDividerModule,
    MatProgressBarModule,
  ],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class AppComponent implements DoCheck {
  constructor(
    private goldenService: GoldensService,
    private progressTracker: ProgressTracker,
  ) {}

  ngDoCheck() {
    this.showProgress = this.progressTracker.isActive;
  }

  goldens: MotionGolden[] = [];

  showProgress = false;

  ngOnInit(): void {
    this.fetchGoldens();
  }

  fetchGoldens(): void {
    this.progressTracker.beginProgress();
    this.goldenService
      .getGoldens()
      .pipe(finalize(() => this.progressTracker.endProgress()))
      .subscribe((goldens) => (this.goldens = goldens));
  }

  refreshGoldens(clear: boolean): void {
    this.progressTracker.beginProgress();
    this.goldenService
      .refreshGoldens(clear)
      .pipe(finalize(() => this.progressTracker.endProgress()))
      .subscribe((goldens) => (this.goldens = goldens));
  }
}
