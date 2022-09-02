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
import { Component, Inject } from "@angular/core";
import { MatSnackBarRef, MAT_SNACK_BAR_DATA } from "@angular/material/snack-bar";
import { TRACE_INFO } from "app/trace_info";
import { ParserError, ParserErrorType } from "parsers/parser_factory";

@Component({
  selector: "upload-snack-bar",
  template: `
    <div class="flex">
      <div class="message-wrapper">
        <p class="data" *ngFor="let msg of errorMessages">{{msg}}</p>
      </div>
      <button class="icon-button close" (click)="snackBarRef.dismiss()">
        <mat-icon class="icon-button close-snackbar">close</mat-icon>
      </button>
    </div>
  `,
})

export class ParserErrorSnackBarComponent {
  errorMessages: string[] = [];
  constructor(
    @Inject(MatSnackBarRef) public snackBarRef: MatSnackBarRef<ParserErrorSnackBarComponent>,
    @Inject(MAT_SNACK_BAR_DATA) errors: ParserError[]
  ) {
    errors.forEach(error => {
      this.errorMessages.push(this.getErrorString(error));
    });
  }

  private getErrorString(error: ParserError) {
    let errorString = "unknown error occurred";
    if (error.type === ParserErrorType.ALREADY_LOADED && error.traceType) {
      errorString = `Cannot load ${error.trace.name}; Already loaded a trace of type ${TRACE_INFO[error.traceType].name}`;
    } else if (error.type === ParserErrorType.UNSUPPORTED_FORMAT) {
      errorString =  `Cannot load ${error.trace.name}; File format is unsupported`;
    }
    return errorString;
  }
}
