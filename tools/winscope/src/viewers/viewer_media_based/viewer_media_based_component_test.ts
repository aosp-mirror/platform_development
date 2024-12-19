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
import {getFixtureFile} from 'test/unit/fixture_utils';
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
    const title = assertDefined(htmlElement.querySelector('.overlay-title'));
    expect(title.textContent).toEqual('Screen recording');

    component.titles = ['Screenshot'];
    fixture.detectChanges();
    expect(title.textContent).toEqual('Screenshot');

    component.titles = ['Screenshot.png'];
    fixture.detectChanges();
    expect(title.textContent).toEqual('Screenshot');

    component.titles = ['Screenshot.png (parent.zip)'];
    fixture.detectChanges();
    expect(title.textContent).toEqual('Screenshot');

    component.titles = ['Screenshot (parent.zip)'];
    fixture.detectChanges();
    expect(title.textContent).toEqual('Screenshot');
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
    const initialMaxWidth = getContainerMaxWidth();
    const videoFile = await getFixtureFile(
      'traces/elapsed_and_real_timestamp/screen_recording_metadata_v2.mp4',
    );
    component.currentTraceEntries = [new MediaBasedTraceEntry(1, videoFile)];
    fixture.detectChanges();
    await fixture.whenStable();
    const videoContainer = assertDefined(
      htmlElement.querySelector<HTMLElement>('.video-container'),
    );
    expect(videoContainer.querySelector('video')).toBeTruthy();
    expect(videoContainer.querySelector('img')).toBeNull();
    expect(getContainerMaxWidth()).not.toEqual(initialMaxWidth);
  });

  it('shows screenshot image', async () => {
    const initialMaxWidth = getContainerMaxWidth();
    const screenshotFile = await getFixtureFile('traces/screenshot_2.png');
    component.currentTraceEntries = [
      new MediaBasedTraceEntry(0, screenshotFile, true),
    ];
    fixture.detectChanges();
    await fixture.whenStable();

    const videoContainer = assertDefined(
      htmlElement.querySelector<HTMLElement>('.video-container'),
    );
    expect(videoContainer.querySelector('img')).toBeTruthy();
    expect(videoContainer.querySelector('video')).toBeNull();
    expect(getContainerMaxWidth()).not.toEqual(initialMaxWidth);
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

  it('video current time updated correctly on entry change', async () => {
    component.currentTraceEntries = [
      new MediaBasedTraceEntry(10, new Blob(), false),
      new MediaBasedTraceEntry(15, new Blob(), false),
    ];
    component.titles = ['Recording 1', 'Recording 2'];
    fixture.detectChanges();

    expect(
      htmlElement.querySelector<HTMLVideoElement>('video')?.currentTime,
    ).toEqual(10);

    await openSelect();
    const options = document.querySelectorAll<HTMLElement>('mat-option');

    options.item(1).click();
    fixture.detectChanges();
    expect(
      htmlElement.querySelector<HTMLVideoElement>('video')?.currentTime,
    ).toEqual(15);
  });

  it('does not update frame if trace entries do not change', () => {
    component.currentTraceEntries = [
      new MediaBasedTraceEntry(0, new Blob(), true),
    ];
    component.titles = ['Screenshot 1'];
    fixture.detectChanges();

    const screenComponent = assertDefined(component.screenComponent);
    const url = screenComponent.safeUrl;

    component.titles = ['Screenshot 1', 'Screenshot 2'];
    fixture.detectChanges();
    expect(screenComponent.safeUrl).toEqual(url);
  });

  it('updates max container size on window resize', async () => {
    const screenshotFile = await getFixtureFile('traces/screenshot.png');
    component.currentTraceEntries = [
      new MediaBasedTraceEntry(0, screenshotFile, true),
    ];
    fixture.detectChanges();
    await fixture.whenStable();

    const initialMaxWidth = getContainerMaxWidth();
    const newWindowHeight = window.innerHeight / 2;
    spyOnProperty(window, 'innerHeight').and.returnValue(newWindowHeight);
    resizeWindow();
    const maxWidthAfterNewWindowHeight = getContainerMaxWidth();
    expect(maxWidthAfterNewWindowHeight < initialMaxWidth).toBeTrue();

    const newWindowWidth = maxWidthAfterNewWindowHeight / 2;
    spyOnProperty(window, 'innerWidth').and.returnValue(newWindowWidth);
    resizeWindow();
    expect(getContainerMaxWidth() < maxWidthAfterNewWindowHeight).toBeTrue();
  });

  function getContainerMaxWidth(): number {
    const container = assertDefined(
      htmlElement.querySelector<HTMLElement>('.container'),
    );
    return Number(container.style.maxWidth.slice(0, -2));
  }

  async function openSelect() {
    const selectTrigger = assertDefined(
      htmlElement.querySelector<HTMLElement>('.mat-select-trigger'),
    );
    selectTrigger.click();
    fixture.detectChanges();
  }

  async function resizeWindow() {
    window.dispatchEvent(new Event('resize'));
    fixture.detectChanges();
    await fixture.whenStable();
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
