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
import {Rect} from 'common/geometry/rect';
import {CanvasDrawer} from './canvas_drawer';

describe('CanvasDrawer', () => {
  let actualCanvas: HTMLCanvasElement;
  let expectedCanvas: HTMLCanvasElement;
  let canvasDrawer: CanvasDrawer;

  const testRect = new Rect(10, 10, 10, 10);
  const hexColor = '#333333';
  const expectedRgbaColor = 'rgba(51,51,51,1)';
  const expectedTransparentColor = 'rgba(51,51,51,0)';

  beforeEach(() => {
    actualCanvas = createCanvas(100, 100);
    expectedCanvas = createCanvas(100, 100);
    canvasDrawer = new CanvasDrawer();
    canvasDrawer.setCanvas(actualCanvas);
  });

  it('erases the canvas', async () => {
    canvasDrawer.drawRect(testRect, hexColor, 1.0);
    expect(pixelsAllMatch(actualCanvas, expectedCanvas)).toBeFalse();

    canvasDrawer.clear();
    expect(pixelsAllMatch(actualCanvas, expectedCanvas)).toBeTrue();
  });

  it('can draw opaque rect', () => {
    canvasDrawer.drawRect(testRect, hexColor, 1.0);

    const expectedCtx = assertDefined(expectedCanvas.getContext('2d'));
    expectedCtx.fillStyle = hexColor;
    expectedCtx.rect(testRect.x, testRect.y, testRect.w, testRect.h);
    expectedCtx.fill();

    expect(pixelsAllMatch(actualCanvas, expectedCanvas)).toBeTrue();
  });

  it('can draw translucent rect', () => {
    canvasDrawer.drawRect(testRect, hexColor, 0.5);

    const expectedCtx = assertDefined(expectedCanvas.getContext('2d'));
    expectedCtx.fillStyle = 'rgba(51,51,51,0.5)';
    expectedCtx.rect(testRect.x, testRect.y, testRect.w, testRect.h);
    expectedCtx.fill();

    expect(pixelsAllMatch(actualCanvas, expectedCanvas)).toBeTrue();
  });

  it('can draw rect with start gradient and full ellipsis', () => {
    const testGradientRect = new Rect(50, 10, 50, 10);
    canvasDrawer.drawRect(testGradientRect, hexColor, 1, true, false);

    const expectedCtx = assertDefined(expectedCanvas.getContext('2d'));
    fillGradient(expectedCtx, testGradientRect, 0.5, true, false);
    addEllipsis(expectedCtx, testGradientRect, true);

    expect(pixelsAllMatch(actualCanvas, expectedCanvas)).toBeTrue();
  });

  it('can draw rect with partial start ellipsis', () => {
    canvasDrawer.drawRect(testRect, hexColor, 1, true, false);

    const expectedCtx = assertDefined(expectedCanvas.getContext('2d'));
    expectedCtx.fillStyle = hexColor;
    expectedCtx.rect(testRect.x, testRect.y, testRect.w, testRect.h);
    expectedCtx.fill();

    addEllipsis(expectedCtx, testRect, true);

    expect(pixelsAllMatch(actualCanvas, expectedCanvas)).toBeTrue();
  });

  it('can draw rect with end gradient and full ellipsis', () => {
    const testGradientRect = new Rect(50, 10, 50, 10);
    canvasDrawer.drawRect(testGradientRect, hexColor, 1, false, true);

    const expectedCtx = assertDefined(expectedCanvas.getContext('2d'));
    fillGradient(expectedCtx, testGradientRect, 0.5, false, true);
    addEllipsis(expectedCtx, testGradientRect, false);

    expect(pixelsAllMatch(actualCanvas, expectedCanvas)).toBeTrue();
  });

  it('can draw rect with partial end ellipsis', () => {
    canvasDrawer.drawRect(testRect, hexColor, 1, false, true);

    const expectedCtx = assertDefined(expectedCanvas.getContext('2d'));
    fillGradient(expectedCtx, testRect, 1, false, true);
    addEllipsis(expectedCtx, testRect, false);

    expect(pixelsAllMatch(actualCanvas, expectedCanvas)).toBeTrue();
  });

  it('can draw rect border', () => {
    canvasDrawer.drawRectBorder(testRect);

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
    canvasDrawer.drawRect(new Rect(200, 200, 10, 10), hexColor, 1.0);
    canvasDrawer.drawRect(new Rect(95, 95, 50, 50), hexColor, 1.0);

    const expectedCtx = assertDefined(expectedCanvas.getContext('2d'));
    expectedCtx.fillStyle = hexColor;
    expectedCtx.rect(95, 95, 5, 5);
    expectedCtx.fill();

    expect(pixelsAllMatch(actualCanvas, expectedCanvas)).toBeTrue();
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

  function fillGradient(
    ctx: CanvasRenderingContext2D,
    testGradientRect: Rect,
    gradientRatio: number,
    startGradient: boolean,
    endGradient: boolean,
  ) {
    const gradient = ctx.createLinearGradient(
      testGradientRect.x,
      0,
      testGradientRect.x + testGradientRect.w,
      0,
    );
    gradient.addColorStop(
      0,
      startGradient ? expectedTransparentColor : expectedRgbaColor,
    );
    gradient.addColorStop(
      1,
      endGradient ? expectedTransparentColor : expectedRgbaColor,
    );
    gradient.addColorStop(gradientRatio, expectedRgbaColor);
    gradient.addColorStop(1 - gradientRatio, expectedRgbaColor);
    ctx.fillStyle = gradient;
    ctx.rect(
      testGradientRect.x,
      testGradientRect.y,
      testGradientRect.w,
      testGradientRect.h,
    );
    ctx.fill();
  }

  function addEllipsis(
    ctx: CanvasRenderingContext2D,
    testGradientRect: Rect,
    forwards: boolean,
  ) {
    ctx.fillStyle = 'black';
    const centerY = testGradientRect.y + testGradientRect.h / 2;
    const xLim = forwards
      ? testGradientRect.x + testGradientRect.w
      : testGradientRect.x;
    let centerX = forwards
      ? testGradientRect.x + 5
      : testGradientRect.x + testGradientRect.w - 5;
    let i = 0;
    const radius = 2;
    while (i < 3) {
      if (forwards && centerX + radius >= xLim) {
        break;
      }
      if (!forwards && centerX + radius <= xLim) {
        break;
      }
      ctx.beginPath();
      ctx.arc(centerX, centerY, radius, 0, 2 * Math.PI);
      ctx.fill();
      centerX = forwards ? centerX + 7 : centerX - 7;
      i++;
    }
  }
});
