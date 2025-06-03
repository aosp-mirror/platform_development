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
import {
  trigger,
  state,
  style,
  animate,
  transition
} from '@angular/animations';

import { DialogContentComponent } from '../dialog/dialog.component';
import { MatButton } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
@Component({
  selector: 'app-root',
  imports: [
    MatToolbarModule,
    TestListComponent,
    PreviewComponent,
    TimelineComponent,
    NgIf,
    MatButton,
    MatProgressSpinnerModule
],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css',
  animations: [
    trigger('sidebarMenuAnimation', [
      state('void', style({
        width: '0',
        'min-width': '0',
        marginRight: '0',
        opacity: 0,
        paddingLeft: '0',
        paddingRight: '0',
        overflow: 'hidden'
      })),
      transition(':leave', [
        style({
          width: '*',
          'min-width': '25%',
          marginRight: '*',
          opacity: 1,
          paddingLeft: '*',
          paddingRight: '*'
        }),
        animate('300ms ease-in')
      ]),
      transition(':enter', [
        style({
          width: '0',
          'min-width': '0',
          marginRight: '0',
          opacity: 0,
          paddingLeft: '0',
          paddingRight: '0'
        }),
        animate('300ms ease-out', style({
          width: '*',
          'min-width': '25%',
          marginRight: '*',
          opacity: 1,
          paddingLeft: '*',
          paddingRight: '*'
        }))
      ])
    ])
  ]
})
export class AppComponent implements DoCheck, OnInit {
  constructor(
    private goldenService: GoldensService,
    private progressTracker: ProgressTracker,
    public dialog: MatDialog
    ) {}

  isNullOrEmpty(obj : any) : Boolean {
    return (obj == null || obj.length == 0)
  }

  openDialog(): void {
    const dialogRef = this.dialog.open(DialogContentComponent);

    dialogRef.afterClosed().subscribe(invocationID => {
      if (invocationID) {
        this.showLoaderBar()
        this.goldenService.refreshGoldens(true)
        this.goldenService.getTestArtifacts(invocationID)
        .pipe(finalize(() => this.hideLoaderBar()))
        .subscribe({
          next : (fetchedTestNames) => {
            if (this.isNullOrEmpty(fetchedTestNames)) {
              fetchedTestNames = []
              console.log("No artifacts found")
              alert("No artifacts found")
            }
            this.goldens = []
            this.selectedGolden = null
            this.testNames = fetchedTestNames
          },
          error : (err) => {
            this.testNames = []
            this.goldens = []
            this.selectedGolden = null
            this.showErrorAlert(err)
          }
        })
      }
    });
  }

  showProgress = false;
  showLoader = false;
  goldens: MotionGolden[] = [];
  testNames: String[] = [];
  selectedTest: String | null = null;
  selectedGolden: MotionGolden | null = null;
  showTestList: boolean = true;

  toggleTestListVisibility() {
    this.showTestList = !this.showTestList;
  }

  showErrorAlert(err: Error) {
    alert(`Some error occurred ${err.message}`)
  }
  showLoaderBar() : void {
    this.showLoader = true;
  }

  hideLoaderBar() : void {
    this.showLoader = false;
  }


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

   setSelectedTest(testName: String): void {
    this.selectedTest = testName;
    this.showLoaderBar();
    this.goldenService.getTestArtifactsForTestName(testName).pipe(finalize(() => this.hideLoaderBar())).subscribe({
      next: (fetchedGolden) => {
        this.selectedGolden = fetchedGolden
      },
      error: (err) => {
        this.goldens = [];
        this.selectedGolden = null;
        this.showErrorAlert(err)
      }
    })
  }
}
