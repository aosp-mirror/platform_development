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

import {Component, ViewChild} from '@angular/core';
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {MatButtonModule} from '@angular/material/button';
import {MatCardModule} from '@angular/material/card';
import {MatIconModule} from '@angular/material/icon';
import {MatSelectModule} from '@angular/material/select';
import {MatTooltipModule} from '@angular/material/tooltip';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {assertDefined} from 'common/assert_utils';
import {UnitTestUtils} from 'test/unit/utils';
import {MediaBasedTraceEntry} from 'trace/media_based_trace_entry';
import {ViewerMediaBasedComponent} from './viewer_media_based_component';

describe('ViewerMediaBasedComponent', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let component: TestHostComponent;
  let htmlElement: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        MatCardModule,
        MatTooltipModule,
        MatButtonModule,
        MatIconModule,
        MatSelectModule,
        BrowserAnimationsModule,
      ],
      declarations: [TestHostComponent, ViewerMediaBasedComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(TestHostComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;
    fixture.detectChanges();
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('renders title correctly', () => {
    const title = assertDefined(htmlElement.querySelector('.header'));
    expect(title.innerHTML).toContain('Screen recording');

    component.titles = ['Screenshot'];
    fixture.detectChanges();
    expect(title.innerHTML).toContain('Screenshot');
  });

  it('can be minimized and maximized', () => {
    const buttonMinimize = assertDefined(
      htmlElement.querySelector<HTMLElement>('.button-minimize'),
    );
    const videoContainer = assertDefined(
      htmlElement.querySelector<HTMLElement>('.video-container'),
    );
    expect(videoContainer.style.height).toEqual('');

    buttonMinimize.click();
    fixture.detectChanges();
    expect(videoContainer.style.height).toEqual('0px');

    buttonMinimize.click();
    fixture.detectChanges();
    expect(videoContainer.style.height).toEqual('');
  });

  it('forces minimized state', () => {
    component.forceMinimize = true;
    fixture.detectChanges();

    const buttonMinimize = assertDefined(
      htmlElement.querySelector<HTMLButtonElement>('.button-minimize'),
    );
    const videoContainer = assertDefined(
      htmlElement.querySelector<HTMLElement>('.video-container'),
    );
    expect(videoContainer.style.height).toEqual('0px');
    expect(buttonMinimize.disabled).toBeTrue();

    component.forceMinimize = false;
    fixture.detectChanges();
    expect(videoContainer.style.height).toEqual('');
    expect(buttonMinimize.disabled).toBeFalse();
  });

  it('shows video', async () => {
    const videoFile = await UnitTestUtils.getFixtureFile(
      'traces/elapsed_and_real_timestamp/screen_recording_metadata_v2.mp4',
    );
    component.currentTraceEntries = [new MediaBasedTraceEntry(1, videoFile)];
    fixture.detectChanges();
    const videoContainer = assertDefined(
      htmlElement.querySelector<HTMLElement>('.video-container'),
    );
    expect(videoContainer.querySelector('video')).toBeTruthy();
    expect(videoContainer.querySelector('img')).toBeNull();
  });

  it('shows screenshot image', () => {
    component.currentTraceEntries = [
      new MediaBasedTraceEntry(0, new Blob(), true),
    ];
    fixture.detectChanges();
    const videoContainer = assertDefined(
      htmlElement.querySelector<HTMLElement>('.video-container'),
    );
    expect(videoContainer.querySelector('img')).toBeTruthy();
    expect(videoContainer.querySelector('video')).toBeNull();
  });

  it('shows no frame message', () => {
    const videoContainer = assertDefined(
      htmlElement.querySelector<HTMLElement>('.video-container'),
    );
    expect(videoContainer.textContent).toContain('No frame to show');
  });

  it('selector changes entry shown', async () => {
    component.currentTraceEntries = [
      new MediaBasedTraceEntry(0, new Blob(), true),
      new MediaBasedTraceEntry(0, new Blob(), true),
    ];
    component.titles = ['Screenshot 1', 'Screenshot 2'];
    fixture.detectChanges();

    const screenComponent = assertDefined(component.screenComponent);

    let url = screenComponent.safeUrl;

    await openSelect();

    const options = document.querySelectorAll<HTMLElement>('mat-option');

    options.item(1).click();
    fixture.detectChanges();
    await fixture.whenStable();
    expect(screenComponent.safeUrl).not.toEqual(url);
    url = screenComponent.safeUrl;

    options.item(1).click();
    fixture.detectChanges();
    expect(screenComponent.safeUrl).toEqual(url);

    options.item(0).click();
    fixture.detectChanges();
    expect(screenComponent.safeUrl).not.toEqual(url);
    url = screenComponent.safeUrl;

    options.item(0).click();
    fixture.detectChanges();
    expect(screenComponent.safeUrl).toEqual(url);
  });

  async function openSelect() {
    const selectTrigger = assertDefined(
      htmlElement.querySelector<HTMLElement>('.mat-select-trigger'),
    );
    selectTrigger.click();
    fixture.detectChanges();
  }

  @Component({
    selector: 'host-component',
    template: `
      <viewer-media-based
        [currentTraceEntries]="currentTraceEntries"
        [titles]="titles"
        [forceMinimize]="forceMinimize"></viewer-media-based>
    `,
  })
  class TestHostComponent {
    currentTraceEntries: MediaBasedTraceEntry[] = [];
    titles: string[] = [];
    forceMinimize = false;

    @ViewChild(ViewerMediaBasedComponent)
    screenComponent: ViewerMediaBasedComponent | undefined;
  }
});
