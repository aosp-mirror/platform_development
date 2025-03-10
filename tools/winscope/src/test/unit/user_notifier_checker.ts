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

import {UserNotifier} from 'common/user_notifier';
import {UserNotification} from 'messaging/user_notification';

export class UserNotifierChecker {
  private userNotifierAdd: jasmine.Spy<jasmine.Func>;
  private userNotifierNotify: jasmine.Spy<jasmine.Func>;

  constructor() {
    this.userNotifierAdd = spyOn(UserNotifier, 'add').and.callThrough();
    this.userNotifierNotify = spyOn(UserNotifier, 'notify');
  }

  expectNone() {
    expect(this.userNotifierAdd).not.toHaveBeenCalled();
  }

  expectAdded(notifications: UserNotification[]) {
    this.checkAdded(notifications);
    expect(this.userNotifierNotify).not.toHaveBeenCalled();
  }

  expectNotified(notifications: UserNotification[]) {
    this.checkAdded(notifications);
    expect(this.userNotifierNotify).toHaveBeenCalled();
  }

  reset() {
    this.userNotifierAdd.calls.reset();
    this.userNotifierNotify.calls.reset();
  }

  private checkAdded(notifications: UserNotification[]) {
    expect(this.userNotifierAdd).toHaveBeenCalledTimes(notifications.length);
    notifications.forEach((n) =>
      expect(this.userNotifierAdd).toHaveBeenCalledWith(n),
    );
  }
}
