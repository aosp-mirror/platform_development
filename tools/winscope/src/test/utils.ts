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
 * limitations under the License.
 */

import {ComponentFixture, flush} from '@angular/core/testing';

export function dispatchMouseEvent(
  source: Node,
  type: string,
  screenX: number,
  screenY: number,
  clientX: number,
  clientY: number,
) {
  const event = document.createEvent('MouseEvent');

  event.initMouseEvent(
    type,
    true /* canBubble */,
    false /* cancelable */,
    window /* view */,
    0 /* detail */,
    screenX /* screenX */,
    screenY /* screenY */,
    clientX /* clientX */,
    clientY /* clientY */,
    false /* ctrlKey */,
    false /* altKey */,
    false /* shiftKey */,
    false /* metaKey */,
    0 /* button */,
    null /* relatedTarget */,
  );
  Object.defineProperty(event, 'buttons', {get: () => 1});

  source.dispatchEvent(event);
}

export function dragElement<T>(
  fixture: ComponentFixture<T>,
  target: Element,
  x: number,
  y: number,
) {
  const {left, top} = target.getBoundingClientRect();

  dispatchMouseEvent(target, 'mousedown', left, top, 0, 0);
  fixture.detectChanges();
  flush();
  dispatchMouseEvent(document, 'mousemove', left + 1, top + 0, 1, y);
  fixture.detectChanges();
  flush();
  dispatchMouseEvent(document, 'mousemove', left + x, top + y, x, y);
  fixture.detectChanges();
  flush();
  dispatchMouseEvent(document, 'mouseup', left + x, top + y, x, y);
  fixture.detectChanges();

  flush();
}

export async function waitToBeCalled(
  spy: jasmine.Spy,
  times: number = 1,
  timeout = 10000,
) {
  return new Promise<void>((resolve, reject) => {
    let called = 0;
    spy.and.callThrough().and.callFake(() => {
      called++;
      if (called === times) {
        resolve();
      }
    });

    setTimeout(
      () => reject(`not called ${times} times within ${timeout}ms`),
      timeout,
    );
  });
}
