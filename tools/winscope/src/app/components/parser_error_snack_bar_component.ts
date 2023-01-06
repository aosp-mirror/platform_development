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
import {Component, Inject, NgZone} from '@angular/core';
import {MatSnackBar, MatSnackBarRef, MAT_SNACK_BAR_DATA} from '@angular/material/snack-bar';
import {TRACE_INFO} from 'app/trace_info';
import {ParserError, ParserErrorType} from 'parsers/parser_factory';

@Component({
  selector: 'upload-snack-bar',
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
export class ParserErrorSnackBarComponent {
  constructor(
    @Inject(MatSnackBarRef) public snackBarRef: MatSnackBarRef<ParserErrorSnackBarComponent>,
    @Inject(MAT_SNACK_BAR_DATA) public messages: string[]
  ) {}

  static showIfNeeded(ngZone: NgZone, snackBar: MatSnackBar, errors: ParserError[]) {
    const messages = ParserErrorSnackBarComponent.convertErrorsToMessages(errors);

    if (messages.length === 0) {
      return;
    }

    ngZone.run(() => {
      // The snackbar needs to be opened within ngZone,
      // otherwise it will first display on the left and then will jump to the center
      snackBar.openFromComponent(ParserErrorSnackBarComponent, {
        data: messages,
        duration: 10000,
      });
    });
  }

  private static convertErrorsToMessages(errors: ParserError[]): string[] {
    const messages: string[] = [];
    const groups = ParserErrorSnackBarComponent.groupErrorsByType(errors);

    for (const [type, groupedErrors] of groups) {
      const CROP_THRESHOLD = 5;
      const countUsed = Math.min(groupedErrors.length, CROP_THRESHOLD);
      const countCropped = groupedErrors.length - countUsed;

      groupedErrors.slice(0, countUsed).forEach((error) => {
        messages.push(ParserErrorSnackBarComponent.convertErrorToMessage(error));
      });

      if (countCropped > 0) {
        messages.push(ParserErrorSnackBarComponent.makeCroppedMessage(type, countCropped));
      }
    }

    return messages;
  }

  private static convertErrorToMessage(error: ParserError): string {
    const fileName = error.trace !== undefined ? error.trace.name : '<no file name>';
    const traceTypeName =
      error.traceType !== undefined ? TRACE_INFO[error.traceType].name : '<unknown>';

    switch (error.type) {
      case ParserErrorType.NO_INPUT_FILES:
        return 'No input files';
      case ParserErrorType.UNSUPPORTED_FORMAT:
        return `${fileName}: unsupported file format`;
      case ParserErrorType.OVERRIDE: {
        return `${fileName}: overridden by another trace of type ${traceTypeName}`;
      }
      default:
        return `${fileName}: unknown error occurred`;
    }
  }

  private static makeCroppedMessage(type: ParserErrorType, count: number): string {
    switch (type) {
      case ParserErrorType.OVERRIDE:
        return `... (cropped ${count} overridden trace messages)`;
      case ParserErrorType.UNSUPPORTED_FORMAT:
        return `... (cropped ${count} unsupported file format messages)`;
      default:
        return `... (cropped ${count} unknown error messages)`;
    }
  }

  private static groupErrorsByType(errors: ParserError[]): Map<ParserErrorType, ParserError[]> {
    const groups = new Map<ParserErrorType, ParserError[]>();

    errors.forEach((error) => {
      if (groups.get(error.type) === undefined) {
        groups.set(error.type, []);
      }
      groups.get(error.type)!.push(error);
    });

    return groups;
  }
}
