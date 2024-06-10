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

import {ComponentFixture, TestBed} from '@angular/core/testing';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {assertDefined} from 'common/assert_utils';
import {TraceType} from 'trace/trace_type';
import {VISIBLE_CHIP} from 'viewers/common/chip';
import {UserOptions} from 'viewers/common/user_options';
import {UserOptionsComponent} from './user_options_component';

describe('UserOptionsComponent', () => {
  let fixture: ComponentFixture<UserOptionsComponent>;
  let component: UserOptionsComponent;
  let htmlElement: HTMLElement;
  const testEventType = 'TestEventType';

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MatButtonModule, MatIconModule],
      declarations: [UserOptionsComponent],
    }).compileComponents();
    fixture = TestBed.createComponent(UserOptionsComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;
    component.userOptions = {
      option1: {
        name: 'option 1',
        enabled: false,
        isUnavailable: false,
      },
      optionWithChip: {
        name: 'option with chip',
        enabled: false,
        isUnavailable: false,
        chip: VISIBLE_CHIP,
      },
      optionWithIcon: {
        name: 'option with icon',
        enabled: false,
        isUnavailable: false,
        icon: 'visibility',
      },
    };
    component.eventType = testEventType;
    component.traceType = TraceType.SURFACE_FLINGER;
    fixture.detectChanges();
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('displays options', () => {
    const options = htmlElement.querySelectorAll('.user-option');
    expect(options.length).toEqual(3);

    expect(options.item(0).textContent).toContain('option 1');
    expect(options.item(0).querySelector('.user-option-chip')).toBeNull();
    expect(options.item(0).querySelector('.mat-icon')).toBeNull();

    expect(options.item(1).textContent).toContain('option with chip');
    expect(
      options.item(1).querySelector('.user-option-chip')?.textContent,
    ).toContain('V');
    expect(options.item(1).querySelector('.mat-icon')).toBeNull();

    expect(options.item(2).textContent).toContain('option with icon');
    expect(options.item(2).querySelector('.user-option-chip')).toBeNull();
    expect(options.item(2).querySelector('.mat-icon')?.textContent).toContain(
      'visibility',
    );
  });

  it('disables option if unavailable', () => {
    let option = assertDefined(htmlElement.querySelector('.user-option'));
    expect((option as HTMLButtonElement).disabled).toBeFalse();

    component.userOptions['option1'].isUnavailable = true;
    fixture.detectChanges();
    option = assertDefined(htmlElement.querySelector('.user-option'));
    expect((option as HTMLInputElement).disabled).toBeTrue();
  });

  it('emits event on user option change', () => {
    let options: UserOptions | undefined;
    htmlElement.addEventListener(testEventType, (event) => {
      options = (event as CustomEvent).detail.userOptions;
    });
    const logSpy = spyOn(component, 'logCallback');
    const option = assertDefined(
      htmlElement.querySelector('.user-option'),
    ) as HTMLInputElement;
    option.click();
    fixture.detectChanges();
    expect(assertDefined(options)['option1'].enabled).toBeTrue();
    expect(logSpy).toHaveBeenCalled();
  });
});
