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

import {assertDefined} from 'common/assert_utils';
import {Rect} from 'common/rect';
import {CanvasDrawer} from './canvas_drawer';

describe('CanvasDrawer', () => {
  it('erases the canvas', async () => {
    const actualCanvas = createCanvas(100, 100);
    const expectedCanvas = createCanvas(100, 100);

    const canvasDrawer = new CanvasDrawer();
    canvasDrawer.setCanvas(actualCanvas);
    canvasDrawer.drawRect(new Rect(10, 10, 10, 10), '#333333', 1.0);

    expect(pixelsAllMatch(actualCanvas, expectedCanvas)).toBeFalse();

    canvasDrawer.clear();

    expect(pixelsAllMatch(actualCanvas, expectedCanvas)).toBeTrue();
  });

  it('can draw opaque rect', () => {
    const actualCanvas = createCanvas(100, 100);
    const expectedCanvas = createCanvas(100, 100);

    const canvasDrawer = new CanvasDrawer();
    canvasDrawer.setCanvas(actualCanvas);
    canvasDrawer.drawRect(new Rect(10, 10, 10, 10), '#333333', 1.0);

    const expectedCtx = assertDefined(expectedCanvas.getContext('2d'));
    expectedCtx.fillStyle = '#333333';
    expectedCtx.rect(10, 10, 10, 10);
    expectedCtx.fill();

    expect(pixelsAllMatch(actualCanvas, expectedCanvas)).toBeTrue();
  });

  it('can draw translucent rect', () => {
    const actualCanvas = createCanvas(100, 100);
    const expectedCanvas = createCanvas(100, 100);

    const canvasDrawer = new CanvasDrawer();
    canvasDrawer.setCanvas(actualCanvas);
    canvasDrawer.drawRect(new Rect(10, 10, 10, 10), '#333333', 0.5);

    const expectedCtx = assertDefined(expectedCanvas.getContext('2d'));
    expectedCtx.fillStyle = 'rgba(51,51,51,0.5)';
    expectedCtx.rect(10, 10, 10, 10);
    expectedCtx.fill();

    expect(pixelsAllMatch(actualCanvas, expectedCanvas)).toBeTrue();
  });

  it('can draw rect border', () => {
    const actualCanvas = createCanvas(100, 100);
    const expectedCanvas = createCanvas(100, 100);

    const canvasDrawer = new CanvasDrawer();
    canvasDrawer.setCanvas(actualCanvas);
    canvasDrawer.drawRectBorder(new Rect(10, 10, 10, 10));

    const expectedCtx = assertDefined(expectedCanvas.getContext('2d'));

    expectedCtx.rect(9, 9, 12, 3);
    expectedCtx.fill();
    expectedCtx.rect(9, 9, 3, 12);
    expectedCtx.fill();
    expectedCtx.rect(9, 18, 12, 3);
    expectedCtx.fill();
    expectedCtx.rect(18, 9, 3, 12);
    expectedCtx.fill();

    expect(pixelsAllMatch(actualCanvas, expectedCanvas)).toBeTrue();
  });

  it('can draw rect outside bounds', () => {
    const actualCanvas = createCanvas(100, 100);
    const expectedCanvas = createCanvas(100, 100);

    const canvasDrawer = new CanvasDrawer();
    canvasDrawer.setCanvas(actualCanvas);
    canvasDrawer.drawRect(new Rect(200, 200, 10, 10), '#333333', 1.0);
    canvasDrawer.drawRect(new Rect(95, 95, 50, 50), '#333333', 1.0);

    const expectedCtx = assertDefined(expectedCanvas.getContext('2d'));
    expectedCtx.fillStyle = '#333333';
    expectedCtx.rect(95, 95, 5, 5);
    expectedCtx.fill();

    expect(pixelsAllMatch(actualCanvas, expectedCanvas)).toBeTrue();
  });
});

function createCanvas(width: number, height: number): HTMLCanvasElement {
  const canvas = document.createElement('canvas') as HTMLCanvasElement;
  canvas.width = width;
  canvas.height = height;
  return canvas;
}

function pixelsAllMatch(
  canvasA: HTMLCanvasElement,
  canvasB: HTMLCanvasElement,
): boolean {
  if (canvasA.width !== canvasB.width || canvasA.height !== canvasB.height) {
    return false;
  }

  const imgA = assertDefined(canvasA.getContext('2d')).getImageData(
    0,
    0,
    canvasA.width,
    canvasA.height,
  ).data;
  const imgB = assertDefined(canvasB.getContext('2d')).getImageData(
    0,
    0,
    canvasB.width,
    canvasB.height,
  ).data;

  for (let i = 0; i < imgA.length; i++) {
    if (imgA[i] !== imgB[i]) {
      return false;
    }
  }

  return true;
}
