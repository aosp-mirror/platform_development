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
import {TestBed} from '@angular/core/testing';
import {MatButtonModule} from '@angular/material/button';
import {MatCardModule} from '@angular/material/card';
import {MatIconModule} from '@angular/material/icon';
import {MatSelectModule} from '@angular/material/select';
import {MatTooltipModule} from '@angular/material/tooltip';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {assertDefined} from 'common/assert_utils';
import {DOMTestHelper} from 'test/unit/dom_test_utils';
import {getFixtureFile} from 'test/unit/fixture_utils';
import {MediaBasedTraceEntry} from 'trace/media_based_trace_entry';
import {ViewerEvents} from 'viewers/common/viewer_events';
import {ViewerMediaBasedComponent} from './viewer_media_based_component';

describe('ViewerMediaBasedComponent', () => {
  let component: TestHostComponent;
  let dom: DOMTestHelper<TestHostComponent>;

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
    const fixture = TestBed.createComponent(TestHostComponent);
    component = fixture.componentInstance;
    dom = new DOMTestHelper(fixture, fixture.nativeElement);
    dom.detectChanges();
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('renders title correctly', () => {
    const title = dom.get('.overlay-title');
    title.checkTextExact('Screen recording');

    component.titles = ['Screenshot'];
    dom.detectChanges();
    title.checkTextExact('Screenshot');

    component.titles = ['Screenshot.png'];
    dom.detectChanges();
    title.checkTextExact('Screenshot');

    component.titles = ['Screenshot.png (parent.zip)'];
    dom.detectChanges();
    title.checkTextExact('Screenshot');

    component.titles = ['Screenshot (parent.zip)'];
    dom.detectChanges();
    title.checkTextExact('Screenshot');
  });

  it('can be minimized and maximized', () => {
    const buttonMinimize = dom.get('.button-minimize');
    const videoContainer = dom.get('.video-container').getHTMLElement();
    expect(videoContainer.style.height).toEqual('');

    buttonMinimize.click();
    expect(videoContainer.style.height).toEqual('0px');

    buttonMinimize.click();
    expect(videoContainer.style.height).toEqual('');
  });

  it('forces minimized state', () => {
    component.forceMinimize = true;
    dom.detectChanges();

    const buttonMinimize = dom.get('.button-minimize');
    const videoContainer = dom.get('.video-container').getHTMLElement();
    expect(videoContainer.style.height).toEqual('0px');
    buttonMinimize.checkDisabled(true);

    component.forceMinimize = false;
    dom.detectChanges();
    expect(videoContainer.style.height).toEqual('');
    buttonMinimize.checkDisabled(false);
  });

  it('shows video', async () => {
    const initialMaxWidth = getContainerMaxWidth();
    const videoFile = await getFixtureFile(
      'traces/elapsed_and_real_timestamp/screen_recording_metadata_v2.mp4',
    );
    component.currentTraceEntries = [new MediaBasedTraceEntry(1, videoFile)];
    await dom.detectChangesAndWaitStable();
    const videoContainer = dom.get('.video-container');
    expect(videoContainer.find('video')).toBeDefined();
    expect(videoContainer.find('img')).toBeUndefined();
    expect(getContainerMaxWidth()).not.toEqual(initialMaxWidth);
  });

  it('shows screenshot image', async () => {
    const initialMaxWidth = getContainerMaxWidth();
    const screenshotFile = await getFixtureFile(
      'traces/screenshot/screenshot_2.png',
    );
    component.currentTraceEntries = [
      new MediaBasedTraceEntry(0, screenshotFile, true),
    ];
    await dom.detectChangesAndWaitStable();

    const videoContainer = dom.get('.video-container');
    expect(videoContainer.find('img')).toBeDefined();
    expect(videoContainer.find('video')).toBeUndefined();
    expect(getContainerMaxWidth()).not.toEqual(initialMaxWidth);
  });

  it('shows no frame message', () => {
    dom.get('.video-container').checkTextExact('No frame to show.');
  });

  it('selector changes entry shown', () => {
    component.currentTraceEntries = [
      new MediaBasedTraceEntry(0, new Blob(), true),
      new MediaBasedTraceEntry(0, new Blob(), true),
    ];
    component.titles = ['Screenshot 1', 'Screenshot 2'];
    dom.detectChanges();

    const screenComponent = assertDefined(component.screenComponent);
    let url = screenComponent.safeUrl;

    dom.openMatSelect();
    const options = dom.getMatSelectPanel().findAll('mat-option');

    options[1].click();
    expect(screenComponent.safeUrl).not.toEqual(url);
    url = screenComponent.safeUrl;

    options[1].click();
    expect(screenComponent.safeUrl).toEqual(url);

    options[0].click();
    expect(screenComponent.safeUrl).not.toEqual(url);
    url = screenComponent.safeUrl;

    options[0].click();
    expect(screenComponent.safeUrl).toEqual(url);
  });

  it('video current time updated correctly on entry change', () => {
    component.currentTraceEntries = [
      new MediaBasedTraceEntry(10, new Blob(), false),
      new MediaBasedTraceEntry(15, new Blob(), false),
    ];
    component.titles = ['Recording 1', 'Recording 2'];
    dom.detectChanges();

    expect(
      dom.get('video').getHTMLElement<HTMLVideoElement>().currentTime,
    ).toEqual(10);

    dom.openMatSelect();
    const options = dom.getMatSelectPanel().findAll('mat-option');

    options[1].click();
    expect(
      dom.get('video').getHTMLElement<HTMLVideoElement>().currentTime,
    ).toEqual(15);
  });

  it('does not update frame if trace entries do not change', () => {
    component.currentTraceEntries = [
      new MediaBasedTraceEntry(0, new Blob(), true),
    ];
    component.titles = ['Screenshot 1'];
    dom.detectChanges();

    const screenComponent = assertDefined(component.screenComponent);
    const url = screenComponent.safeUrl;

    component.titles = ['Screenshot 1', 'Screenshot 2'];
    dom.detectChanges();
    expect(screenComponent.safeUrl).toEqual(url);
  });

  it('updates max container size on window resize', async () => {
    const screenshotFile = await getFixtureFile(
      'traces/screenshot/screenshot.png',
    );
    component.currentTraceEntries = [
      new MediaBasedTraceEntry(0, screenshotFile, true),
    ];
    await dom.detectChangesAndWaitStable();

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

  it('emits event on double click', () => {
    let index: number | undefined;
    dom.addEventListener(ViewerEvents.OverlayDblClick, (event) => {
      index = (event as CustomEvent).detail;
    });
    expect(dom.find('.info-icon')).toBeUndefined();
    const container = dom.get('.container');
    container.doubleClick();
    expect(index).toBeUndefined();

    assertDefined(component.screenComponent).enableDoubleClick = true;
    dom.detectChanges();
    expect(dom.find('.info-icon')).toBeDefined();
    container.doubleClick();
    expect(index).toEqual(0);
  });

  function getContainerMaxWidth(): number {
    const container = dom.get('.container').getHTMLElement();
    return Number(container.style.maxWidth.slice(0, -2));
  }

  async function resizeWindow() {
    window.dispatchEvent(new Event('resize'));
    await dom.detectChangesAndWaitStable();
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
