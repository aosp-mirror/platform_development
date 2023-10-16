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
import {CUSTOM_ELEMENTS_SCHEMA} from '@angular/core';
import {ComponentFixture, ComponentFixtureAutoDetect, TestBed} from '@angular/core/testing';
import {MatCardModule} from '@angular/material/card';
import {ViewerScreenRecordingComponent} from './viewer_screen_recording_component';

describe('ViewerScreenRecordingComponent', () => {
  let fixture: ComponentFixture<ViewerScreenRecordingComponent>;
  let component: ViewerScreenRecordingComponent;
  let htmlElement: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [{provide: ComponentFixtureAutoDetect, useValue: true}],
      imports: [MatCardModule],
      declarations: [ViewerScreenRecordingComponent],
      schemas: [CUSTOM_ELEMENTS_SCHEMA],
    }).compileComponents();

    fixture = TestBed.createComponent(ViewerScreenRecordingComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;

    fixture.detectChanges();
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('can be minimized and maximized', () => {
    const buttonMinimize = htmlElement.querySelector('.button-minimize');
    const videoContainer = htmlElement.querySelector('.video-container') as HTMLElement;
    expect(buttonMinimize).toBeTruthy();
    expect(videoContainer).toBeTruthy();
    expect(videoContainer!.style.height).toEqual('');

    buttonMinimize!.dispatchEvent(new Event('click'));
    fixture.detectChanges();
    expect(videoContainer!.style.height).toEqual('0px');

    buttonMinimize!.dispatchEvent(new Event('click'));
    fixture.detectChanges();
    expect(videoContainer!.style.height).toEqual('');
  });
});
