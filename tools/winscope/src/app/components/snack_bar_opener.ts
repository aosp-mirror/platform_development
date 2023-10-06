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

import {Inject, Injectable, NgZone} from '@angular/core';
import {MatSnackBar} from '@angular/material/snack-bar';
import {TRACE_INFO} from 'app/trace_info';
import {UserNotificationListener} from 'interfaces/user_notification_listener';
import {ParserError, ParserErrorType} from 'parsers/parser_factory';
import {SnackBarComponent} from './snack_bar_component';

@Injectable({providedIn: 'root'})
export class SnackBarOpener implements UserNotificationListener {
  constructor(
    @Inject(NgZone) private ngZone: NgZone,
    @Inject(MatSnackBar) private snackBar: MatSnackBar
  ) {}

  onParserErrors(errors: ParserError[]) {
    const messages = this.convertErrorsToMessages(errors);

    if (messages.length === 0) {
      return;
    }

    this.ngZone.run(() => {
      // The snackbar needs to be opened within ngZone,
      // otherwise it will first display on the left and then will jump to the center
      this.snackBar.openFromComponent(SnackBarComponent, {
        data: messages,
        duration: 10000,
      });
    });
  }

  private convertErrorsToMessages(errors: ParserError[]): string[] {
    const messages: string[] = [];
    const groups = this.groupErrorsByType(errors);

    for (const [type, groupedErrors] of groups) {
      const CROP_THRESHOLD = 5;
      const countUsed = Math.min(groupedErrors.length, CROP_THRESHOLD);
      const countCropped = groupedErrors.length - countUsed;

      groupedErrors.slice(0, countUsed).forEach((error) => {
        messages.push(this.convertErrorToMessage(error));
      });

      if (countCropped > 0) {
        messages.push(this.makeCroppedMessage(type, countCropped));
      }
    }

    return messages;
  }

  private convertErrorToMessage(error: ParserError): string {
    const fileName = error.trace !== undefined ? error.trace : '<no file name>';
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

  private makeCroppedMessage(type: ParserErrorType, count: number): string {
    switch (type) {
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

    errors.forEach((error) => {
      if (groups.get(error.type) === undefined) {
        groups.set(error.type, []);
      }
      groups.get(error.type)!.push(error);
    });

    return groups;
  }
}
