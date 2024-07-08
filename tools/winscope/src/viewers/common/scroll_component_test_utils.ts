/*
 * Copyright (C) 2023 The Android Open Source Project
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
 * limitations under the License.d
 */

import {CdkVirtualScrollViewport} from '@angular/cdk/scrolling';
import {ComponentFixture} from '@angular/core/testing';
import {assertDefined} from 'common/assert_utils';
import {animationFrameScheduler} from 'rxjs';
import {ViewerProtologComponent} from 'viewers/viewer_protolog/viewer_protolog_component';
import {ViewerTransactionsComponent} from 'viewers/viewer_transactions/viewer_transactions_component';

type ScrollComponent = ViewerProtologComponent | ViewerTransactionsComponent;

export function executeScrollComponentTests(
  setUpTestEnvironment: () => Promise<
    [ComponentFixture<ScrollComponent>, HTMLElement, CdkVirtualScrollViewport]
  >,
) {
  describe('', () => {
    let fixture: ComponentFixture<any>;
    let htmlElement: HTMLElement;
    let viewport: CdkVirtualScrollViewport;

    beforeEach(async () => {
      [fixture, htmlElement, viewport] = await setUpTestEnvironment();
    });

    it('renders initial state', () => {
      const items = htmlElement.querySelectorAll('.entry');
      expect(items.length).toBe(20);
    });

    it('gets data length', () => {
      expect(viewport.getDataLength()).toBe(200);
    });

    it('should get the rendered range', () => {
      expect(viewport.getRenderedRange()).toEqual({start: 0, end: 20});
    });

    it('should scroll to index in large jumps', () => {
      expect(htmlElement.querySelector(`.entry[item-id="30"]`)).toBeFalsy();
      checkScrollToIndex(30);
      expect(htmlElement.querySelector(`.entry[item-id="70"]`)).toBeFalsy();
      checkScrollToIndex(70);
    });

    it('should update without jumps as the user scrolls down or up', () => {
      for (let i = 1; i < 50; i++) {
        checkScrollToIndex(i);
      }
      for (let i = 49; i >= 0; i--) {
        checkScrollToIndex(i);
      }
    });

    function checkScrollToIndex(i: number) {
      viewport.scrollToIndex(i);
      viewport.elementRef.nativeElement.dispatchEvent(new Event('scroll'));
      animationFrameScheduler.flush();
      fixture.detectChanges();
      assertDefined(htmlElement.querySelector(`.entry[item-id="${i}"]`));
    }
  });
}
