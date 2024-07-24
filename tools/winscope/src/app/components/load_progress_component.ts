/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {Component, Input} from '@angular/core';

@Component({
  selector: 'load-progress',
  template: `
    <div class="container-progress">
      <p class="mat-body-3">
        <mat-icon fontIcon="sync"> </mat-icon>
      </p>

      <mat-progress-bar *ngIf="progressPercentage === undefined" mode="indeterminate">
      </mat-progress-bar>
      <mat-progress-bar
        *ngIf="progressPercentage !== undefined"
        mode="determinate"
        [value]="progressPercentage">
      </mat-progress-bar>

      <p class="mat-body-1">{{ message }}</p>
    </div>
  `,
  styles: [
    `
      .container-progress {
        display: flex;
        height: 100%;
        flex-direction: column;
        justify-content: center;
        align-content: center;
        align-items: center;
      }
      p {
        opacity: 0.6;
      }
      mat-icon {
        font-size: 3rem;
        width: unset;
        height: unset;
      }
      mat-progress-bar {
        max-width: 250px;
      }
      mat-card-content {
        flex-grow: 1;
      }
    `,
  ],
})
export class LoadProgressComponent {
  @Input() progressPercentage?: number;
  @Input() message = 'Loading...';
  private static readonly MIN_UI_UPDATE_PERIOD_MS = 200;

  static canUpdateComponent(lastUpdateTimeMs: number | undefined): boolean {
    if (lastUpdateTimeMs === undefined) {
      return true;
    }
    // Limit the amount of UI updates, because the progress bar component
    // renders weird stuff when updated too frequently.
    // Also, this way we save some resources.
    return (
      Date.now() - lastUpdateTimeMs >=
      LoadProgressComponent.MIN_UI_UPDATE_PERIOD_MS
    );
  }
}
