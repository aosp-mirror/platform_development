import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { MatListModule, MatSelectionListChange } from '@angular/material/list';

import { Golden } from '../golden';
import { MotionGoldenComponent } from '../motion-golden/motion-golden.component';
import { ScreenshotGoldenComponent } from '../screenshot-golden/screenshot-golden.component';

@Component({
  selector: 'app-test-overview',
  standalone: true,
  imports: [MatListModule, MotionGoldenComponent, ScreenshotGoldenComponent],
  templateUrl: './test-overview.component.html',
  styleUrl: './test-overview.component.scss',
})
export class TestOverviewComponent implements OnChanges {
  @Input() goldens: Golden[] = [];

  totalTestCount = 0;
  passingTestCount = 0;
  failingTestCount = 0;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['goldens']) {
      this.totalTestCount = this.goldens.length;
      this.failingTestCount = this.goldens.filter(
        (golden) => golden.result !== 'PASSED',
      ).length;
      this.passingTestCount = this.totalTestCount - this.failingTestCount;
    }
  }

  get selectedGolden() {
    return this.goldens.find((it) => it.id == this.selectedGoldenId);
  }

  selectedGoldenId: String | null = null;

  goldenSelected(ev: MatSelectionListChange) {
    const selection = ev.source.selectedOptions;
    this.selectedGoldenId = selection.hasValue()
      ? selection.selected[0].value
      : null;
  }
}
