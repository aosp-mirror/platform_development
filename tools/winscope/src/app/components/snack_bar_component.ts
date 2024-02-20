/*
 * Copyright (C) 2023 The Android Open Source Project
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

import {Component, Inject} from '@angular/core';
import {MatSnackBarRef, MAT_SNACK_BAR_DATA} from '@angular/material/snack-bar';

@Component({
  selector: 'snack-bar',
  template: `
    <div class="snack-bar-container">
      <p *ngFor="let message of messages" class="mat-body-1">
        {{ message }}
      </p>
      <button color="primary" mat-button class="snack-bar-action" (click)="snackBarRef.dismiss()">
        Close
      </button>
    </div>
  `,
  styles: [
    `
      .snack-bar-container {
        display: flex;
        flex-direction: column;
      }
      .snack-bar-action {
        margin-left: 12px;
      }
    `,
  ],
})
export class SnackBarComponent {
  constructor(
    @Inject(MatSnackBarRef)
    public snackBarRef: MatSnackBarRef<SnackBarComponent>,
    @Inject(MAT_SNACK_BAR_DATA) public messages: string[],
  ) {}
}
