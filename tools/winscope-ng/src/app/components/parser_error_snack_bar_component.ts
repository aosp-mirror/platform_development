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
import {Component, Inject} from "@angular/core";
import {MAT_SNACK_BAR_DATA, MatSnackBarRef} from "@angular/material/snack-bar";
import {TRACE_INFO} from "app/trace_info";
import {ParserError, ParserErrorType} from "parsers/parser_factory";

@Component({
  selector: "upload-snack-bar",
  template: `
    <div class="snack-bar-container">
      <p *ngFor="let message of messages" class="mat-body-1">
        {{message}}
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
    `
  ]
})

export class ParserErrorSnackBarComponent {
  messages: string[] = [];

  constructor(
    @Inject(MatSnackBarRef) public snackBarRef: MatSnackBarRef<ParserErrorSnackBarComponent>,
    @Inject(MAT_SNACK_BAR_DATA) errors: ParserError[]
  ) {
    this.messages = this.convertErrorsToMessages(errors);

    if (this.messages.length === 0) {
      this.snackBarRef.dismiss();
    }
  }

  private convertErrorsToMessages(errors: ParserError[]): string[] {
    const messages: string[] = [];
    const groups = this.groupErrorsByType(errors);

    for (const [type, groupedErrors] of groups) {
      const CROP_THRESHOLD = 5;
      const countUsed = Math.min(groupedErrors.length, CROP_THRESHOLD);
      const countCropped = groupedErrors.length - countUsed;

      groupedErrors.slice(0, countUsed).forEach(error => {
        messages.push(this.convertErrorToMessage(error));
      });

      if (countCropped > 0) {
        messages.push(this.makeCroppedMessage(type, countCropped));
      }
    }

    return messages;
  }

  private convertErrorToMessage(error: ParserError): string {
    let message = `${error.trace.name}: unknown error occurred`;
    if (error.type === ParserErrorType.OVERRIDE && error.traceType) {
      message = `${error.trace.name}:` +
        ` overridden by another trace of type ${TRACE_INFO[error.traceType].name}`;
    } else if (error.type === ParserErrorType.UNSUPPORTED_FORMAT) {
      message = `${error.trace.name}: unsupported file format`;
    }
    return message;
  }

  private makeCroppedMessage(type: ParserErrorType, count: number): string {
    switch(type) {
    case ParserErrorType.OVERRIDE:
      return `... (cropped ${count} overridden trace messages)`;
    case ParserErrorType.UNSUPPORTED_FORMAT:
      return `... (cropped ${count} unsupported file format messages)`;
    default:
      return `... (cropped ${count} unknown error messages)`;
    }
  }

  private groupErrorsByType(errors: ParserError[]): Map<ParserErrorType, ParserError[]> {
    const groups = new Map<ParserErrorType, ParserError[]>();

    errors.forEach(error => {
      if (groups.get(error.type) === undefined) {
        groups.set(error.type, []);
      }
      groups.get(error.type)!.push(error);
    });

    return groups;
  }
}
