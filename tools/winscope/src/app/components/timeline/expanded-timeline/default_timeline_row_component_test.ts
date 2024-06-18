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
import {ChangeDetectionStrategy} from '@angular/core';
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
import {NO_TIMEZONE_OFFSET_FACTORY} from 'common/timestamp_factory';
import {TraceBuilder} from 'test/unit/trace_builder';
import {waitToBeCalled} from 'test/utils';
import {TraceType} from 'trace/trace_type';
import {DefaultTimelineRowComponent} from './default_timeline_row_component';

describe('DefaultTimelineRowComponent', () => {
  let fixture: ComponentFixture<DefaultTimelineRowComponent>;
  let component: DefaultTimelineRowComponent;

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
    })
      .overrideComponent(DefaultTimelineRowComponent, {
        set: {changeDetection: ChangeDetectionStrategy.Default},
      })
      .compileComponents();
    fixture = TestBed.createComponent(DefaultTimelineRowComponent);
    component = fixture.componentInstance;
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
    drawRectSpy.calls.reset();
    await waitToBeCalled(drawRectSpy, 4);

    const width = 32;
    const height = width;
    const alpha = 0.2;

    const canvasWidth = component.canvasDrawer.getScaledCanvasWidth() - width;

    expect(drawRectSpy).toHaveBeenCalledTimes(4);
    expect(drawRectSpy).toHaveBeenCalledWith(
      new Rect(0, 0, width, height),
      component.color,
      alpha,
    );
    expect(drawRectSpy).toHaveBeenCalledWith(
      new Rect(Math.floor((canvasWidth * 2) / 100), 0, width, height),
      component.color,
      alpha,
    );
    expect(drawRectSpy).toHaveBeenCalledWith(
      new Rect(Math.floor((canvasWidth * 5) / 100), 0, width, height),
      component.color,
      alpha,
    );
    expect(drawRectSpy).toHaveBeenCalledWith(
      new Rect(Math.floor((canvasWidth * 60) / 100), 0, width, height),
      component.color,
      alpha,
    );
  });

  it('can draw entries zoomed in', async () => {
    setTraceAndSelectionRange(60n, 85n);

    const drawRectSpy = spyOn(component.canvasDrawer, 'drawRect');

    fixture.detectChanges();
    await fixture.whenRenderingDone();
    drawRectSpy.calls.reset();
    await waitToBeCalled(drawRectSpy, 1);

    const width = 32;
    const height = width;
    const alpha = 0.2;

    const canvasWidth = component.canvasDrawer.getScaledCanvasWidth() - width;

    expect(drawRectSpy).toHaveBeenCalledTimes(1);
    expect(drawRectSpy).toHaveBeenCalledWith(
      new Rect(Math.floor((canvasWidth * 10) / 25), 0, width, height),
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

    component.handleMouseMove({
      offsetX: 5,
      offsetY: component.canvasDrawer.getScaledCanvasHeight() / 2,
      preventDefault: () => {},
      stopPropagation: () => {},
    } as MouseEvent);

    fixture.detectChanges();
    await fixture.whenRenderingDone();

    await Promise.all(waitPromises);

    expect(assertDefined(component.hoveringEntry).getValueNs()).toBe(10n);
    expect(drawRectSpy).toHaveBeenCalledTimes(1);
    expect(drawRectSpy).toHaveBeenCalledWith(
      new Rect(0, 0, 32, 32),
      component.color,
      1.0,
    );

    expect(drawRectBorderSpy).toHaveBeenCalledTimes(1);
    expect(drawRectBorderSpy).toHaveBeenCalledWith(new Rect(0, 0, 32, 32));
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
      component.canvasDrawer.getScaledCanvasWidth() - 32,
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
      component.canvasDrawer.getScaledCanvasWidth() - 32,
    );
    const entryPos = Math.floor((canvasWidth * 2) / 5);

    // 5 rect draws - 2 entry rects present + 2 for redraw + 1 for selected entry
    await drawCorrectEntryOnClick(entryPos, 12n, 5);
  });

  it('can draw correct entry on click when timeline zoomed in near end', async () => {
    setTraceAndSelectionRange(60n, 80n);

    fixture.detectChanges();
    await fixture.whenRenderingDone();

    const canvasWidth = Math.floor(
      component.canvasDrawer.getScaledCanvasWidth() - 32,
    );
    const entryPos = Math.floor((canvasWidth * 10) / 20);

    // 3 rect draws - 1 entry rects present + 1 for redraw + 1 for selected entry
    await drawCorrectEntryOnClick(entryPos, 70n, 3);
  });

  function setTraceAndSelectionRange(low: bigint, high: bigint) {
    component.trace = new TraceBuilder<{}>()
      .setType(TraceType.TRANSITION)
      .setEntries([{}, {}, {}, {}])
      .setTimestamps([
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(10n),
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(12n),
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(15n),
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(70n),
      ])
      .build();
    component.selectionRange = {
      from: NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(low),
      to: NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(high),
    };
  }

  async function drawCorrectEntryOnClick(
    xPos: number,
    expectedTimestampNs: bigint,
    rectSpyCalls: number,
  ) {
    const drawRectSpy = spyOn(
      component.canvasDrawer,
      'drawRect',
    ).and.callThrough();
    const drawRectBorderSpy = spyOn(
      component.canvasDrawer,
      'drawRectBorder',
    ).and.callThrough();

    const waitPromises = [
      waitToBeCalled(drawRectSpy, rectSpyCalls),
      waitToBeCalled(drawRectBorderSpy, 1),
    ];

    await component.handleMouseDown({
      offsetX: xPos + 1,
      offsetY: component.canvasDrawer.getScaledCanvasHeight() / 2,
      preventDefault: () => {},
      stopPropagation: () => {},
    } as MouseEvent);

    fixture.detectChanges();
    await fixture.whenRenderingDone();

    await Promise.all(waitPromises);

    expect(
      assertDefined(component.selectedEntry).getTimestamp().getValueNs(),
    ).toBe(expectedTimestampNs);

    const expectedRect = new Rect(xPos + 1, 1, 30, 30);

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
