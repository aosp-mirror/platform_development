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
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {MatButtonModule} from '@angular/material/button';
import {MatCheckboxModule} from '@angular/material/checkbox';
import {
  MatDialog,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import {MatIconModule} from '@angular/material/icon';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {assertDefined} from 'common/assert_utils';
import {
  WarningDialogComponent,
  WarningDialogData,
  WarningDialogResult,
} from './warning_dialog_component';

describe('WarningDialogComponent', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let component: TestHostComponent;
  let htmlElement: HTMLElement;

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
    fixture = TestBed.createComponent(TestHostComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;
    fixture.detectChanges();
  });

  it('can be created', () => {
    expect(document.querySelector('warning-dialog')).toBeNull();
    openAndReturnDialog();
  });

  it('renders warning message, action boxes and buttons', () => {
    const dialog = openAndReturnDialog();

    const content = assertDefined(dialog.querySelector('.warning-content'));
    expect(content.querySelector('.warning-message')?.textContent).toContain(
      'test message',
    );

    const actionBoxContainer = assertDefined(
      content.querySelector('.warning-action-boxes'),
    );
    expect(actionBoxContainer.textContent).toContain('option1');
    expect(actionBoxContainer.textContent).toContain('option2');

    const actionButtonContainer = assertDefined(
      content.querySelector('.warning-action-buttons'),
    );
    expect(actionButtonContainer.textContent).toContain('action1');
    expect(actionButtonContainer.textContent).toContain('action2');
    expect(actionButtonContainer.textContent).toContain('close message');
  });

  it('provides action text and selected options as dialog result on close', async () => {
    const dialog = openAndReturnDialog();

    const actionButton = assertDefined(
      dialog.querySelector('.warning-action-buttons button'),
    ) as HTMLElement;
    actionButton.click();
    fixture.detectChanges();
    await fixture.whenStable();

    expect(component.dialogResult).toEqual({
      closeActionText: 'action1',
      selectedOptions: [],
    });
  });

  it('provides close text and selected options as dialog result on close', async () => {
    const dialog = openAndReturnDialog();

    const buttons = assertDefined(
      dialog.querySelectorAll('.warning-action-buttons button'),
    );
    (buttons.item(buttons.length - 1) as HTMLElement).click();
    fixture.detectChanges();
    await fixture.whenStable();

    expect(component.dialogResult).toEqual({
      closeActionText: 'close message',
      selectedOptions: [],
    });
  });

  it('updates selected options and provides selected options in dialog result', async () => {
    const dialog = openAndReturnDialog();

    const option = assertDefined(
      dialog.querySelector('.warning-action-boxes mat-checkbox input'),
    ) as HTMLInputElement;
    option.checked = true;
    option.click();
    fixture.detectChanges();

    const buttons = assertDefined(
      dialog.querySelectorAll('.warning-action-buttons button'),
    );
    (buttons.item(buttons.length - 1) as HTMLElement).click();
    fixture.detectChanges();
    await fixture.whenStable();

    expect(component.dialogResult).toEqual({
      closeActionText: 'close message',
      selectedOptions: ['option1'],
    });
  });

  function openAndReturnDialog(): HTMLElement {
    htmlElement.querySelector('button')?.click();
    fixture.detectChanges();
    return assertDefined(
      document.querySelector('warning-dialog'),
    ) as HTMLElement;
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
