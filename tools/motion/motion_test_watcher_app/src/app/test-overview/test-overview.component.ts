import {
  Component,
  Input,
  OnChanges,
  AfterViewInit,
  ViewChild,
  SimpleChanges,
} from '@angular/core';
import { MatListModule, MatSelectionListChange } from '@angular/material/list';

import { MotionGolden } from '../golden';
import { MotionGoldenComponent } from '../motion-golden/motion-golden.component';
import { MatDividerModule } from '@angular/material/divider';

import {
  MatPaginator,
  MatPaginatorModule,
  PageEvent,
} from '@angular/material/paginator';
import { MatSort, MatSortModule } from '@angular/material/sort';
import {
  MatTableDataSource,
  MatTable,
  MatTableModule,
} from '@angular/material/table';
import { MatRadioModule } from '@angular/material/radio';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-test-overview',
  standalone: true,
  imports: [
    MatListModule,
    MotionGoldenComponent,
    MatDividerModule,
    MatTableModule,
    MatRadioModule,
    MatSortModule,
    MatIconModule,
    MatButtonModule,
    MatPaginatorModule,
  ],
  templateUrl: './test-overview.component.html',
  styleUrl: './test-overview.component.scss',
})
export class TestOverviewComponent implements OnChanges, AfterViewInit {
  dataSource: MatTableDataSource<MotionGolden> = new MatTableDataSource();
  displayedColumns: string[] = [
    'select',
    'status',
    'golden',
    'testClassName',
    'testMethodName',
    'testTime',
  ];

  constructor() {
    this.dataSource.sortingDataAccessor = (data, sortHeaderIdstring) => {
      switch (sortHeaderIdstring) {
        case 'status':
          return data.result;
        case 'testClassName':
          return data.testClassName;
        case 'testMethodName':
          return data.testMethodName;
        case 'testTime':
          return Date.parse(data.testTime);
      }
      return 0;
    };
  }

  @Input() goldens: MotionGolden[] = [];

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
      this.dataSource.data = this.goldens;
    }
  }
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;
  get selectedGolden() {
    return this.goldens.find((it) => it.id == this.selectedGoldenId);
  }

  ngAfterViewInit() {
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;
  }

  get testStatusClass() {
    if (this.failingTestCount > 0) {
      return 'failing';
    } else if (this.passingTestCount === this.totalTestCount) {
      return 'passing';
    } else {
      return 'no-status';
    }
  }

  selectedGoldenId: string | null = null;

  goldenSelected(golden: MotionGolden) {
    this.selectedGoldenId =
      golden.id !== this.selectedGoldenId ? golden.id : null;
  }
}
