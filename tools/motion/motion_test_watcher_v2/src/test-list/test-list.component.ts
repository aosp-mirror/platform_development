import { CommonModule, NgFor, NgIf } from '@angular/common';
import {
  Component,
  EventEmitter,
  Input,
  OnChanges,
  Output,
  SimpleChanges,
} from '@angular/core';
import { MotionGolden } from '../model/golden';
import { MatIconModule } from '@angular/material/icon';
import { MatExpansionModule } from '@angular/material/expansion';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-test-list',
  imports: [
    CommonModule,
    NgIf,
    NgFor,
    FormsModule,
    MatIconModule,
    MatExpansionModule,
  ],
  templateUrl: './test-list.component.html',
  styleUrl: './test-list.component.css',
})
export class TestListComponent implements OnChanges {
  @Input() goldens: MotionGolden[] = [];
  @Output() refreshRequest = new EventEmitter<boolean>();
  @Output() selectedGoldenChange = new EventEmitter<MotionGolden>();
  selectedGolden: MotionGolden | null = null;

  filterStatus: 'all' | 'pass' | 'fail' = 'all';

  totalTestCount = 0;
  passingTestCount = 0;
  failingTestCount = 0;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['goldens']) {
      this.totalTestCount = this.goldens.length;
      this.failingTestCount = this.goldens.filter(
        (golden) => golden.result !== 'PASSED'
      ).length;
      this.passingTestCount = this.totalTestCount - this.failingTestCount;
    }
  }

  triggerRefresh(clear: boolean): void {
    this.refreshRequest.emit(clear);
  }

  panelOpened(golden: MotionGolden): void {
    this.selectedGolden = golden;
    this.selectedGoldenChange.emit(golden);
  }

  get filteredGoldens(): MotionGolden[] {
    if (this.filterStatus === 'all') {
      return this.goldens;
    } else if (this.filterStatus === 'pass') {
      return this.goldens.filter((golden) => golden.result === 'PASSED');
    } else {
      return this.goldens.filter((golden) => golden.result !== 'PASSED');
    }
  }

  getResultClass(golden: MotionGolden): string {
    const result = golden.result.trim().toUpperCase();
    if (result === 'MISSING_REFERENCE') {
      return 'border-l-4 border-yellow-500';
    } else if (result === 'FAILED') {
      return 'border-l-4 border-red-500';
    } else if (result === 'PASSED') {
      return 'border-l-4 border-green-500';
    } else {
      return '';
    }
  }
}
