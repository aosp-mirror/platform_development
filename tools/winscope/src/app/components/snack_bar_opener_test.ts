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

import {Overlay} from '@angular/cdk/overlay';
import {TestBed} from '@angular/core/testing';
import {MatSnackBar, MatSnackBarRef} from '@angular/material/snack-bar';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {
  FailedToInitializeTimelineData,
  NoValidFiles,
} from 'messaging/user_warnings';
import {waitToBeCalled} from 'test/utils';
import {SnackBarComponent} from './snack_bar_component';
import {SnackBarOpener} from './snack_bar_opener';

describe('SnackBarOpener', () => {
  let snackBarOpener: SnackBarOpener;
  let snackBar: MatSnackBar;
  let openSpy: jasmine.Spy<jasmine.Func>;
  const filesNotif = new NoValidFiles();
  const filesMessage = filesNotif.getMessage();

  const timelineNotif = new FailedToInitializeTimelineData();
  const timelineMessage = timelineNotif.getMessage();

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [SnackBarOpener, MatSnackBar, Overlay],
      imports: [BrowserAnimationsModule],
      declarations: [SnackBarComponent],
    });
    snackBarOpener = TestBed.inject(SnackBarOpener);
    snackBar = TestBed.inject(MatSnackBar);
    openSpy = createOpenComponentSpy();
  });

  it('shows notifications in snackbar', () => {
    snackBarOpener.onNotifications([filesNotif]);
    const expectedMessages = [filesMessage];
    expect(openSpy).toHaveBeenCalledWith(SnackBarComponent, {
      data: expectedMessages,
      duration: 5000,
    });
  });

  it('handles empty notifications', () => {
    snackBarOpener.onNotifications([]);
    expect(openSpy).not.toHaveBeenCalled();
  });

  it('does not crop notifications below threshold', () => {
    snackBarOpener.onNotifications(Array.from({length: 5}, () => filesNotif));
    const expectedMessages = Array.from({length: 5}, () => filesMessage);
    expect(openSpy).toHaveBeenCalledWith(SnackBarComponent, {
      data: expectedMessages,
      duration: 25000,
    });
  });

  it('crops notifications if one above threshold', () => {
    const notifications = Array.from({length: 6}, () => filesNotif);
    snackBarOpener.onNotifications(notifications);

    const expectedMessages = Array.from({length: 5}, () => filesMessage);
    expectedMessages.push("... (cropped 1 'no valid files' message)");
    expect(openSpy).toHaveBeenCalledWith(SnackBarComponent, {
      data: expectedMessages,
      duration: 30000,
    });
  });

  it('crops notifications if more than one above threshold', () => {
    const notifications = Array.from({length: 7}, () => filesNotif);
    snackBarOpener.onNotifications(notifications);

    const expectedMessages = Array.from({length: 5}, () => filesMessage);
    expectedMessages.push("... (cropped 2 'no valid files' messages)");
    expect(openSpy).toHaveBeenCalledWith(SnackBarComponent, {
      data: expectedMessages,
      duration: 30000,
    });
  });

  it('does not apply crop if total notifications above threshold but not in same group', () => {
    const notifications = Array.from({length: 5}, () => filesNotif).concat([
      timelineNotif,
    ]);
    snackBarOpener.onNotifications(notifications);

    const expectedMessages = Array.from({length: 5}, () => filesMessage).concat(
      [timelineMessage],
    );
    expect(openSpy).toHaveBeenCalledWith(SnackBarComponent, {
      data: expectedMessages,
      duration: 30000,
    });
  });

  it('only applies crop to notifications in same group', () => {
    const notifications = Array.from({length: 5}, () => filesNotif).concat([
      timelineNotif,
      filesNotif,
    ]);
    snackBarOpener.onNotifications(notifications);

    const expectedMessages = Array.from({length: 5}, () => filesMessage).concat(
      ["... (cropped 1 'no valid files' message)", timelineMessage],
    );
    expect(openSpy).toHaveBeenCalledWith(SnackBarComponent, {
      data: expectedMessages,
      duration: 35000,
    });
  });

  it('queues notifications', async () => {
    snackBarOpener.onNotifications([filesNotif]);
    expect(openSpy).toHaveBeenCalledOnceWith(SnackBarComponent, {
      data: [filesMessage],
      duration: 5000,
    });
    const ref: MatSnackBarRef<SnackBarComponent> =
      openSpy.calls.mostRecent().returnValue;

    openSpy.calls.reset();
    snackBarOpener.onNotifications([filesNotif]);
    expect(openSpy).not.toHaveBeenCalled();

    const afterDismissedSpy = jasmine.createSpy('test', () => {});
    ref.afterDismissed().subscribe(afterDismissedSpy);
    ref.dismiss();
    await waitToBeCalled(afterDismissedSpy);
    expect(openSpy).toHaveBeenCalledOnceWith(SnackBarComponent, {
      data: [filesMessage],
      duration: 5000,
    });
  });

  function createOpenComponentSpy(): jasmine.Spy<jasmine.Func> {
    return spyOn(snackBar, 'openFromComponent').and.callThrough();
  }
});
