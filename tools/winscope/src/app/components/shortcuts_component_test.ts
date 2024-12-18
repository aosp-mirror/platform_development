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
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {MatDialogModule} from '@angular/material/dialog';
import {MatIconModule} from '@angular/material/icon';
import {ShortcutsComponent} from './shortcuts_component';

describe('ShortcutsComponent', () => {
  let fixture: ComponentFixture<ShortcutsComponent>;
  let component: ShortcutsComponent;
  let htmlElement: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MatIconModule, HttpClientModule, MatDialogModule],
      declarations: [ShortcutsComponent],
    }).compileComponents();
    fixture = TestBed.createComponent(ShortcutsComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;
    fixture.detectChanges();
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('renders key shortcuts', () => {
    checkShortcuts('.key-shortcut', [
      ['zoom in'],
      ['zoom out'],
      ['slider left'],
      ['slider right'],
      ['previous'],
      ['next'],
    ]);
  });

  it('renders pointer shortcuts', () => {
    checkShortcuts('.pointer-shortcut', [
      ['right click', 'bookmarks'],
      ['vertical scroll', 'zoom'],
      ['horizontal scroll', 'move slider'],
      ['vertical scroll', 'zoom'],
    ]);
  });

  function checkShortcuts(
    shortcutsSelector: string,
    expectedContent: string[][],
  ) {
    const shortcuts = htmlElement.querySelectorAll(shortcutsSelector);
    expect(shortcuts.length).toEqual(expectedContent.length);

    for (let i = 0; i < expectedContent.length; i++) {
      expectedContent[i].forEach((s) => {
        expect(shortcuts.item(i).textContent?.toLowerCase()).toContain(s);
      });
    }
  }
});
