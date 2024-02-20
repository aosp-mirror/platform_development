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
import {
  ComponentFixture,
  ComponentFixtureAutoDetect,
  TestBed,
} from '@angular/core/testing';
import {MatCardModule} from '@angular/material/card';
import {assertDefined} from 'common/assert_utils';
import {UnitTestUtils} from 'test/unit/utils';
import {ScreenRecordingTraceEntry} from 'trace/screen_recording';
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

  it('renders title correctly', () => {
    const title = assertDefined(
      htmlElement.querySelector('.header'),
    ) as HTMLElement;
    expect(title.innerHTML).toContain('Screen recording');

    component.title = 'Screenshot';
    fixture.detectChanges();
    expect(title.innerHTML).toContain('Screenshot');
  });

  it('can be minimized and maximized', () => {
    const buttonMinimize = assertDefined(
      htmlElement.querySelector('.button-minimize'),
    ) as HTMLButtonElement;
    const videoContainer = assertDefined(
      htmlElement.querySelector('.video-container'),
    ) as HTMLElement;
    expect(videoContainer.style.height).toEqual('');

    buttonMinimize.click();
    fixture.detectChanges();
    expect(videoContainer.style.height).toEqual('0px');

    buttonMinimize.click();
    fixture.detectChanges();
    expect(videoContainer.style.height).toEqual('');
  });

  it('shows video', async () => {
    const videoFile = await UnitTestUtils.getFixtureFile(
      'traces/elapsed_and_real_timestamp/screen_recording_metadata_v2.mp4',
    );
    component.currentTraceEntry = new ScreenRecordingTraceEntry(1, videoFile);
    fixture.detectChanges();
    const videoContainer = assertDefined(
      htmlElement.querySelector('.video-container'),
    ) as HTMLElement;
    expect(videoContainer.querySelector('video')).toBeTruthy();
    expect(videoContainer.querySelector('img')).toBeNull();
  });

  it('shows screenshot image', () => {
    component.currentTraceEntry = new ScreenRecordingTraceEntry(
      0,
      new Blob(),
      true,
    );
    fixture.detectChanges();
    const videoContainer = assertDefined(
      htmlElement.querySelector('.video-container'),
    ) as HTMLElement;
    expect(videoContainer.querySelector('img')).toBeTruthy();
    expect(videoContainer.querySelector('video')).toBeNull();
  });

  it('shows no frame message', () => {
    const videoContainer = assertDefined(
      htmlElement.querySelector('.video-container'),
    ) as HTMLElement;
    expect(videoContainer.innerHTML).toContain(
      'No screen recording frame to show',
    );
  });
});
