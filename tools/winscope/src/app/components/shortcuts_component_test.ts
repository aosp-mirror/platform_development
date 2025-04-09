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
import {HttpClientModule} from '@angular/common/http';
import {TestBed} from '@angular/core/testing';
import {MatDialogModule} from '@angular/material/dialog';
import {MatIconModule} from '@angular/material/icon';
import {DOMTestHelper} from 'test/unit/dom_test_utils';
import {ShortcutsComponent} from './shortcuts_component';

describe('ShortcutsComponent', () => {
  let component: ShortcutsComponent;
  let dom: DOMTestHelper<ShortcutsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MatIconModule, HttpClientModule, MatDialogModule],
      declarations: [ShortcutsComponent],
    }).compileComponents();
    const fixture = TestBed.createComponent(ShortcutsComponent);
    component = fixture.componentInstance;
    dom = new DOMTestHelper(fixture, fixture.nativeElement);
    dom.detectChanges();
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('renders key shortcuts', () => {
    checkShortcuts('.key-shortcut', [
      ['Zoom in'],
      ['Zoom out'],
      ['Move slider left'],
      ['Move slider right'],
      ['Previous state'],
      ['Next state'],
    ]);
  });

  it('renders pointer shortcuts', () => {
    checkShortcuts('.pointer-shortcut', [
      ['Right click', 'bookmarks'],
      ['Vertical Scroll', 'Zoom'],
      ['Horizontal Scroll', 'Move slider'],
      ['Vertical Scroll', 'Zoom'],
    ]);
  });

  function checkShortcuts(
    shortcutsSelector: string,
    expectedContent: string[][],
  ) {
    const shortcuts = dom.findAll(shortcutsSelector);
    expect(shortcuts.length).toEqual(expectedContent.length);

    for (let i = 0; i < expectedContent.length; i++) {
      expectedContent[i].forEach((s) => {
        shortcuts[i].checkText(s);
      });
    }
  }
});
