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
import {assertDefined} from 'common/assert_utils';
import {UserNotificationListener} from 'messaging/user_notification_listener';
import {WinscopeError} from 'messaging/winscope_error';
import {SnackBarComponent} from './snack_bar_component';

@Injectable({providedIn: 'root'})
export class SnackBarOpener implements UserNotificationListener {
  constructor(
    @Inject(NgZone) private ngZone: NgZone,
    @Inject(MatSnackBar) private snackBar: MatSnackBar,
  ) {}

  onErrors(errors: WinscopeError[]) {
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

  private convertErrorsToMessages(errors: WinscopeError[]): string[] {
    const messages: string[] = [];
    const groups = this.groupErrorsByType(errors);

    for (const groupedErrors of groups) {
      const CROP_THRESHOLD = 5;
      const countUsed = Math.min(groupedErrors.length, CROP_THRESHOLD);
      const countCropped = groupedErrors.length - countUsed;

      groupedErrors.slice(0, countUsed).forEach((error) => {
        messages.push(error.getMessage());
      });

      if (countCropped > 0) {
        messages.push(
          `... (cropped ${countCropped} '${groupedErrors[0].getType()}' messages)`,
        );
      }
    }

    return messages;
  }

  private groupErrorsByType(errors: WinscopeError[]): Set<WinscopeError[]> {
    const groups = new Map<Function, WinscopeError[]>();

    errors.forEach((error) => {
      if (groups.get(error.constructor) === undefined) {
        groups.set(error.constructor, []);
      }
      assertDefined(groups.get(error.constructor)).push(error);
    });

    return new Set(groups.values());
  }
}
