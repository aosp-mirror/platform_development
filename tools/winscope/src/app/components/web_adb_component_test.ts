/*
 * Copyright (C) 2022 The Android Open Source Project
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
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {MatIconModule} from '@angular/material/icon';
import {WebAdbComponent} from './web_adb_component';

describe('WebAdbComponent', () => {
  let fixture: ComponentFixture<WebAdbComponent>;
  let component: WebAdbComponent;
  let htmlElement: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MatIconModule],
      declarations: [WebAdbComponent],
    }).compileComponents();
    fixture = TestBed.createComponent(WebAdbComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('renders the info message', () => {
    fixture.detectChanges();
    expect(htmlElement.querySelector('.adb-info')?.innerHTML).toBe(
      'Add new device',
    );
    expect(htmlElement.querySelector('.adb-icon')?.innerHTML).toBe('info');
  });
});
