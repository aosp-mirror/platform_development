import { Component } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatToolbarModule } from '@angular/material/toolbar';
import { RouterOutlet } from '@angular/router';
import { finalize } from 'rxjs';

import { Golden } from './golden';
import { GoldensService } from './goldens.service';
import { TestOverviewComponent } from './test-overview/test-overview.component';

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
    MatProgressBarModule,
  ],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class AppComponent {
  constructor(private goldenService: GoldensService) {}

  _progressTracker = 0;

  goldens: Golden[] = [];

  get showProgress() {
    return this._progressTracker > 0;
  }

  ngOnInit(): void {
    this.fetchGoldens();
  }

  fetchGoldens(): void {
    this._progressTracker++;
    this.goldenService
      .getGoldens()
      .pipe(finalize(() => this._progressTracker--))
      .subscribe((goldens) => (this.goldens = goldens));
  }

  refreshGoldens(clear: boolean): void {
    this._progressTracker++;
    this.goldenService
      .refreshGoldens(clear)
      .pipe(finalize(() => this._progressTracker--))
      .subscribe((goldens) => (this.goldens = goldens));
  }
}
