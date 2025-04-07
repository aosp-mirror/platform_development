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
import {Component, Inject} from '@angular/core';
import {TestBed} from '@angular/core/testing';
import {MatButtonModule} from '@angular/material/button';
import {MatCheckboxModule} from '@angular/material/checkbox';
import {
  MatDialog,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import {MatIconModule} from '@angular/material/icon';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {DOMTestHelper} from 'test/unit/dom_test_utils';
import {
  WarningDialogComponent,
  WarningDialogData,
  WarningDialogResult,
} from './warning_dialog_component';

describe('WarningDialogComponent', () => {
  let component: TestHostComponent;
  let dom: DOMTestHelper<TestHostComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        MatIconModule,
        MatDialogModule,
        MatCheckboxModule,
        MatButtonModule,
        BrowserAnimationsModule,
      ],
      declarations: [TestHostComponent, WarningDialogComponent],
    }).compileComponents();
    const fixture = TestBed.createComponent(TestHostComponent);
    component = fixture.componentInstance;
    dom = new DOMTestHelper(fixture, fixture.nativeElement);
    dom.detectChanges();
  });

  it('can be created', () => {
    expect(dom.findInDocument('warning-dialog')).toBeUndefined();
    openAndReturnDialog();
  });

  it('renders warning message, action boxes and buttons', () => {
    const dialog = openAndReturnDialog();

    const content = dialog.get('.warning-content');
    content.get('.warning-message').checkText('test message');

    const actionBoxContainer = content.get('.warning-action-boxes');
    actionBoxContainer.checkText('option1');
    actionBoxContainer.checkText('option2');

    const actionButtonContainer = content.get('.warning-action-buttons');
    actionButtonContainer.checkText('action1');
    actionButtonContainer.checkText('action2');
    actionButtonContainer.checkText('close message');
  });

  it('provides action text and selected options as dialog result on close', async () => {
    const dialog = openAndReturnDialog();
    await dialog.clickAndWaitStable('.warning-action-buttons button');
    expect(component.dialogResult).toEqual({
      closeActionText: 'action1',
      selectedOptions: [],
    });
  });

  it('provides close text and selected options as dialog result on close', async () => {
    const dialog = openAndReturnDialog();
    await dialog.clickLastAndWaitStable('.warning-action-buttons button');
    expect(component.dialogResult).toEqual({
      closeActionText: 'close message',
      selectedOptions: [],
    });
  });

  it('updates selected options and provides selected options in dialog result', async () => {
    const dialog = openAndReturnDialog();
    const option = dialog.get('.warning-action-boxes mat-checkbox input');
    option.getHTMLElement<HTMLInputElement>().checked = true;
    option.click();
    await dialog.clickLastAndWaitStable('.warning-action-buttons button');
    expect(component.dialogResult).toEqual({
      closeActionText: 'close message',
      selectedOptions: ['option1'],
    });
  });

  function openAndReturnDialog(): DOMTestHelper<TestHostComponent> {
    dom.findAndClick('button');
    return dom.getInDocument('warning-dialog');
  }

  @Component({
    selector: 'host-component',
    template: `
      <button (click)="onClick()"></button>
    `,
  })
  class TestHostComponent {
    dialogRef: MatDialogRef<WarningDialogComponent> | undefined;
    dialogResult: WarningDialogResult | undefined;

    constructor(@Inject(MatDialog) public dialog: MatDialog) {}

    onClick() {
      const data: WarningDialogData = {
        message: 'test message',
        actions: ['action1', 'action2'],
        options: ['option1', 'option2'],
        closeText: 'close message',
      };
      this.dialogRef = this.dialog.open(WarningDialogComponent, {data});
      this.dialogRef
        .afterClosed()
        .subscribe(async (result: WarningDialogResult) => {
          this.dialogResult = result;
        });
    }
  }
});
