/*
 * Copyright (C) 2024 The Android Open Source Project
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

import {SnackBarOpener} from 'app/components/snack_bar_opener';
import {UserNotification} from 'messaging/user_notification';

export class UserNotifier {
  static setSnackBarOpener(snackBarOpener: SnackBarOpener) {
    UserNotifier.snackBarOpener = snackBarOpener;
  }

  static add(notification: UserNotification): typeof UserNotifier {
    UserNotifier.notifications.push(notification);
    return UserNotifier;
  }

  static notify() {
    if (UserNotifier.notifications.length === 0) return;
    UserNotifier.snackBarOpener?.onNotifications(UserNotifier.notifications);
    UserNotifier.notifications = [];
  }

  private static notifications: UserNotification[] = [];
  private static snackBarOpener: SnackBarOpener | undefined;
}
