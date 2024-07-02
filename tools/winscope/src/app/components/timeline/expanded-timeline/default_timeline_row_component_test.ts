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

import {DragDropModule} from '@angular/cdk/drag-drop';
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {MatButtonModule} from '@angular/material/button';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatIconModule} from '@angular/material/icon';
import {MatInputModule} from '@angular/material/input';
import {MatSelectModule} from '@angular/material/select';
import {MatTooltipModule} from '@angular/material/tooltip';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {assertDefined} from 'common/assert_utils';
import {Rect} from 'common/rect';
import {TimeRange} from 'common/time';
import {TimestampConverterUtils} from 'test/unit/timestamp_converter_utils';
import {TraceBuilder} from 'test/unit/trace_builder';
import {waitToBeCalled} from 'test/utils';
import {TraceType} from 'trace/trace_type';
import {DefaultTimelineRowComponent} from './default_timeline_row_component';

describe('DefaultTimelineRowComponent', () => {
  let fixture: ComponentFixture<DefaultTimelineRowComponent>;
  let component: DefaultTimelineRowComponent;
  let htmlElement: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        FormsModule,
        MatButtonModule,
        MatFormFieldModule,
        MatInputModule,
        MatIconModule,
        MatSelectModule,
        MatTooltipModule,
        ReactiveFormsModule,
        BrowserAnimationsModule,
        DragDropModule,
      ],
      declarations: [DefaultTimelineRowComponent],
    }).compileComponents();
    fixture = TestBed.createComponent(DefaultTimelineRowComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('can draw entries', async () => {
    setTraceAndSelectionRange(10n, 110n);

    const drawRectSpy = spyOn(
      component.canvasDrawer,
      'drawRect',
    ).and.callThrough();

    fixture.detectChanges();
    await fixture.whenRenderingDone();

    const rectHeight = component.canvasDrawer.getScaledCanvasHeight();
    const rectWidth = rectHeight;
    const alpha = 0.2;

    const canvasWidth =
      component.canvasDrawer.getScaledCanvasWidth() - rectWidth;

    expect(drawRectSpy).toHaveBeenCalledTimes(4);
    expect(drawRectSpy).toHaveBeenCalledWith(
      new Rect(0, 0, rectWidth, rectHeight),
      component.color,
      alpha,
    );
    expect(drawRectSpy).toHaveBeenCalledWith(
      new Rect(Math.floor((canvasWidth * 2) / 100), 0, rectWidth, rectHeight),
      component.color,
      alpha,
    );
    expect(drawRectSpy).toHaveBeenCalledWith(
      new Rect(Math.floor((canvasWidth * 5) / 100), 0, rectWidth, rectHeight),
      component.color,
      alpha,
    );
    expect(drawRectSpy).toHaveBeenCalledWith(
      new Rect(Math.floor((canvasWidth * 60) / 100), 0, rectWidth, rectHeight),
      component.color,
      alpha,
    );
  });

  it('can draw entries zoomed in', async () => {
    setTraceAndSelectionRange(60n, 85n);

    const drawRectSpy = spyOn(component.canvasDrawer, 'drawRect');

    fixture.detectChanges();
    await fixture.whenRenderingDone();

    const rectHeight = component.canvasDrawer.getScaledCanvasHeight();
    const rectWidth = rectHeight;
    const alpha = 0.2;

    const canvasWidth =
      component.canvasDrawer.getScaledCanvasWidth() - rectWidth;

    expect(drawRectSpy).toHaveBeenCalledTimes(1);
    expect(drawRectSpy).toHaveBeenCalledWith(
      new Rect(Math.floor((canvasWidth * 10) / 25), 0, rectWidth, rectHeight),
      component.color,
      alpha,
    );
  });

  it('can draw hovering entry', async () => {
    setTraceAndSelectionRange(10n, 110n);

    fixture.detectChanges();
    await fixture.whenRenderingDone();

    const drawRectSpy = spyOn(
      component.canvasDrawer,
      'drawRect',
    ).and.callThrough();
    const drawRectBorderSpy = spyOn(
      component.canvasDrawer,
      'drawRectBorder',
    ).and.callThrough();

    const waitPromises = [
      waitToBeCalled(drawRectBorderSpy, 1),
      waitToBeCalled(drawRectSpy, 1),
    ];

    const event = new MouseEvent('mousemove');
    spyOnProperty(event, 'offsetX').and.returnValue(5);
    spyOnProperty(event, 'offsetY').and.returnValue(
      component.canvasDrawer.getScaledCanvasHeight() / 2,
    );
    component.getCanvas().dispatchEvent(event);

    fixture.detectChanges();
    await fixture.whenRenderingDone();

    await Promise.all(waitPromises);

    const rectHeight = component.canvasDrawer.getScaledCanvasHeight();
    const rectWidth = rectHeight;

    expect(assertDefined(component.hoveringEntry).getValueNs()).toBe(10n);
    expect(drawRectSpy).toHaveBeenCalledTimes(1);
    expect(drawRectSpy).toHaveBeenCalledWith(
      new Rect(0, 0, rectWidth, rectHeight),
      component.color,
      1.0,
    );

    expect(drawRectBorderSpy).toHaveBeenCalledTimes(1);
    expect(drawRectBorderSpy).toHaveBeenCalledWith(
      new Rect(0, 0, rectWidth, rectHeight),
    );
  });

  it('can draw correct entry on click of first entry', async () => {
    setTraceAndSelectionRange(10n, 110n);

    fixture.detectChanges();
    await fixture.whenRenderingDone();

    // 9 rect draws - 4 entry rects present + 4 for redraw + 1 for selected entry
    await drawCorrectEntryOnClick(0, 10n, 9);
  });

  it('can draw correct entry on click of middle entry', async () => {
    setTraceAndSelectionRange(10n, 110n);

    fixture.detectChanges();
    await fixture.whenRenderingDone();

    const canvasWidth = Math.floor(
      component.canvasDrawer.getScaledCanvasWidth() -
        component.canvasDrawer.getScaledCanvasHeight(),
    );
    const entryPos = Math.floor((canvasWidth * 5) / 100);

    // 9 rect draws - 4 entry rects present + 4 for redraw + 1 for selected entry
    await drawCorrectEntryOnClick(entryPos, 15n, 9);
  });

  it('can draw correct entry on click when timeline zoomed in near start', async () => {
    setTraceAndSelectionRange(10n, 15n);

    fixture.detectChanges();
    await fixture.whenRenderingDone();

    const canvasWidth = Math.floor(
      component.canvasDrawer.getScaledCanvasWidth() -
        component.canvasDrawer.getScaledCanvasHeight(),
    );
    const entryPos = Math.floor((canvasWidth * 2) / 5);

    // 7 rect draws - 3 entry rects present + 3 for redraw + 1 for selected entry
    await drawCorrectEntryOnClick(entryPos, 12n, 7);
  });

  it('can draw correct entry on click when timeline zoomed in near end', async () => {
    setTraceAndSelectionRange(60n, 80n);

    fixture.detectChanges();
    await fixture.whenRenderingDone();

    const canvasWidth = Math.floor(
      component.canvasDrawer.getScaledCanvasWidth() -
        component.canvasDrawer.getScaledCanvasHeight(),
    );
    const entryPos = Math.floor((canvasWidth * 10) / 20);

    // 3 rect draws - 1 entry rects present + 1 for redraw + 1 for selected entry
    await drawCorrectEntryOnClick(entryPos, 70n, 3);
  });

  it('emits scroll event', async () => {
    setTraceAndSelectionRange(10n, 110n);
    fixture.detectChanges();
    await fixture.whenRenderingDone();

    const spy = spyOn(component.onScrollEvent, 'emit');
    htmlElement.dispatchEvent(new WheelEvent('wheel'));
    fixture.detectChanges();
    expect(spy).toHaveBeenCalled();
  });

  it('tracks mouse position', async () => {
    setTraceAndSelectionRange(10n, 110n);

    fixture.detectChanges();
    await fixture.whenRenderingDone();

    const spy = spyOn(component.onMouseXRatioUpdate, 'emit');
    const canvas = assertDefined(component.canvasRef).nativeElement;

    const mouseMoveEvent = new MouseEvent('mousemove');
    Object.defineProperty(mouseMoveEvent, 'target', {value: canvas});
    Object.defineProperty(mouseMoveEvent, 'offsetX', {value: 100});
    canvas.dispatchEvent(mouseMoveEvent);
    fixture.detectChanges();

    expect(spy).toHaveBeenCalledWith(100 / canvas.offsetWidth);

    const mouseLeaveEvent = new MouseEvent('mouseleave');
    canvas.dispatchEvent(mouseLeaveEvent);
    fixture.detectChanges();
    expect(spy).toHaveBeenCalledWith(undefined);
  });

  function setTraceAndSelectionRange(low: bigint, high: bigint) {
    component.trace = new TraceBuilder<{}>()
      .setType(TraceType.TRANSITION)
      .setEntries([{}, {}, {}, {}])
      .setTimestamps([
        TimestampConverterUtils.makeRealTimestamp(10n),
        TimestampConverterUtils.makeRealTimestamp(12n),
        TimestampConverterUtils.makeRealTimestamp(15n),
        TimestampConverterUtils.makeRealTimestamp(70n),
      ])
      .build();
    component.selectionRange = new TimeRange(
      TimestampConverterUtils.makeRealTimestamp(low),
      TimestampConverterUtils.makeRealTimestamp(high),
    );
    component.timestampConverter = TimestampConverterUtils.TIMESTAMP_CONVERTER;
  }

  async function drawCorrectEntryOnClick(
    xPos: number,
    expectedTimestampNs: bigint,
    rectSpyCalls: number,
  ) {
    const drawRectSpy = spyOn(component.canvasDrawer, 'drawRect');
    const drawRectBorderSpy = spyOn(component.canvasDrawer, 'drawRectBorder');

    const waitPromises = [
      waitToBeCalled(drawRectSpy, rectSpyCalls),
      waitToBeCalled(drawRectBorderSpy, 1),
    ];

    const event = new MouseEvent('mousedown');
    spyOnProperty(event, 'offsetX').and.returnValue(xPos + 1);
    spyOnProperty(event, 'offsetY').and.returnValue(
      component.canvasDrawer.getScaledCanvasHeight() / 2,
    );
    component.getCanvas().dispatchEvent(event);

    fixture.detectChanges();
    await fixture.whenRenderingDone();

    await Promise.all(waitPromises);

    expect(
      assertDefined(component.selectedEntry).getTimestamp().getValueNs(),
    ).toBe(expectedTimestampNs);

    const rectHeight = component.canvasDrawer.getScaledCanvasHeight() - 2;
    const rectWidth = rectHeight;

    const expectedRect = new Rect(xPos + 1, 1, rectWidth, rectHeight);

    expect(drawRectSpy).toHaveBeenCalledTimes(rectSpyCalls);
    expect(drawRectSpy).toHaveBeenCalledWith(
      expectedRect,
      component.color,
      1.0,
    );

    expect(drawRectBorderSpy).toHaveBeenCalledTimes(1);
    expect(drawRectBorderSpy).toHaveBeenCalledWith(expectedRect);
  }
});
