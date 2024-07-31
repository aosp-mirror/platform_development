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
import {NotificationType, UserNotification} from 'messaging/user_notification';
import {SnackBarComponent} from './snack_bar_component';

type Messages = string[];

@Injectable({providedIn: 'root'})
export class SnackBarOpener {
  private isOpen = false;
  private queue: Messages[] = [];

  constructor(
    @Inject(NgZone) private ngZone: NgZone,
    @Inject(MatSnackBar) private snackBar: MatSnackBar,
  ) {}

  onNotifications(notifications: UserNotification[]) {
    const messages = this.convertNotificationsToMessages(notifications);

    if (messages.length === 0) {
      return;
    }

    if (this.isOpen) {
      this.queue.push(messages);
      return;
    }

    this.displayMessages(messages);
  }

  private convertNotificationsToMessages(
    notifications: UserNotification[],
  ): Messages {
    const messages: Messages = [];

    const warnings = notifications.filter(
      (n) => n.getNotificationType() === NotificationType.WARNING,
    );
    const groups = this.groupNotificationsByDescriptor(warnings);

    for (const groupedWarnings of groups) {
      const CROP_THRESHOLD = 5;
      const countUsed = Math.min(groupedWarnings.length, CROP_THRESHOLD);
      const countCropped = groupedWarnings.length - countUsed;

      groupedWarnings.slice(0, countUsed).forEach((warning) => {
        messages.push(warning.getMessage());
      });

      if (countCropped > 0) {
        messages.push(
          `... (cropped ${countCropped} '${groupedWarnings[0].getDescriptor()}' message${
            countCropped > 1 ? 's' : ''
          })`,
        );
      }
    }

    return messages;
  }

  private groupNotificationsByDescriptor(
    warnings: UserNotification[],
  ): Set<UserNotification[]> {
    const groups = new Map<string, UserNotification[]>();

    warnings.forEach((warning) => {
      if (groups.get(warning.getDescriptor()) === undefined) {
        groups.set(warning.getDescriptor(), []);
      }
      assertDefined(groups.get(warning.getDescriptor())).push(warning);
    });

    return new Set(groups.values());
  }

  private displayMessages(messages: Messages) {
    this.ngZone.run(() => {
      // The snackbar needs to be opened within ngZone,
      // otherwise it will first display on the left and then will jump to the center
      this.isOpen = true;
      const ref = this.snackBar.openFromComponent(SnackBarComponent, {
        data: messages,
        duration: 10000,
      });
      ref.afterDismissed().subscribe(() => {
        this.isOpen = false;
        const next = this.queue.shift();
        if (next !== undefined) {
          this.displayMessages(next);
        }
      });
    });
  }
}
