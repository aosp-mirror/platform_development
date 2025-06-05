/*
 * Copyright (C) 2025 The Android Open Source Project
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

import {Clipboard, ClipboardModule} from '@angular/cdk/clipboard';
import {TestBed} from '@angular/core/testing';
import {MatButtonModule} from '@angular/material/button';
import {MatSnackBarRef, MAT_SNACK_BAR_DATA} from '@angular/material/snack-bar';
import {DOMTestHelper} from 'test/unit/dom_test_utils';
import {SnackBarComponent} from './snack_bar_component';

describe('SnackBarComponent', () => {
  const messages = ['test message 1', 'test message 2'];
  let component: SnackBarComponent;
  let dom: DOMTestHelper<SnackBarComponent>;
  let mockCopyText: jasmine.Spy;
  let mockSnackbarRef: jasmine.SpyObj<MatSnackBarRef<SnackBarComponent>>;

  beforeEach(async () => {
    mockCopyText = jasmine.createSpy();
    mockSnackbarRef = jasmine.createSpyObj('snackBarRef', ['dismiss']);
    await TestBed.configureTestingModule({
      providers: [
        {provide: MatSnackBarRef, useValue: mockSnackbarRef},
        {provide: MAT_SNACK_BAR_DATA, useValue: messages},
        {provide: Clipboard, useValue: {copy: mockCopyText}},
      ],
      imports: [ClipboardModule, MatButtonModule],
      declarations: [SnackBarComponent],
    }).compileComponents();
    const fixture = TestBed.createComponent(SnackBarComponent);
    component = fixture.componentInstance;
    dom = new DOMTestHelper(fixture, fixture.nativeElement);
    dom.detectChanges();
  });

  it('shows all messages', () => {
    const messageElements = dom.findAll('.message');
    expect(messageElements.length).toEqual(2);
    messageElements[0].checkTextExact(messages[0]);
    messageElements[1].checkTextExact(messages[1]);
  });

  it('dismisses snackbar on close button click', () => {
    dom.findAndClick('.snack-bar-actions .close-button');
    expect(mockSnackbarRef.dismiss).toHaveBeenCalledTimes(1);
  });

  it('copies messages on copy button click', () => {
    dom.findAndClick('.snack-bar-actions .copy-button');
    expect(mockCopyText).toHaveBeenCalledOnceWith(messages.join('\n\n'));
  });
});
