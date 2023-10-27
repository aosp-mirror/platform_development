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
import {RealTimestamp} from 'common/time';
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

    const drawRectSpy = spyOn(component.canvasDrawer, 'drawRect').and.callThrough();

    fixture.detectChanges();
    await fixture.whenRenderingDone();
    drawRectSpy.calls.reset();
    await waitToBeCalled(drawRectSpy, 4);

    const width = 32;
    const height = width;
    const alpha = 0.2;

    const canvasWidth = component.canvasDrawer.getScaledCanvasWidth() - width;

    expect(drawRectSpy).toHaveBeenCalledTimes(4);
    expect(drawRectSpy).toHaveBeenCalledWith({
      x: 0,
      y: 0,
      w: width,
      h: height,
      color: component.color,
      alpha,
    });
    expect(drawRectSpy).toHaveBeenCalledWith({
      x: Math.floor((canvasWidth * 2) / 100),
      y: 0,
      w: width,
      h: height,
      color: component.color,
      alpha,
    });
    expect(drawRectSpy).toHaveBeenCalledWith({
      x: Math.floor((canvasWidth * 5) / 100),
      y: 0,
      w: width,
      h: height,
      color: component.color,
      alpha,
    });
    expect(drawRectSpy).toHaveBeenCalledWith({
      x: Math.floor((canvasWidth * 60) / 100),
      y: 0,
      w: width,
      h: height,
      color: component.color,
      alpha,
    });
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
    expect(drawRectSpy).toHaveBeenCalledWith({
      x: Math.floor((canvasWidth * 10) / 25),
      y: 0,
      w: width,
      h: height,
      color: component.color,
      alpha,
    });
  });

  it('can draw hovering entry', async () => {
    setTraceAndSelectionRange(10n, 110n);

    fixture.detectChanges();
    await fixture.whenRenderingDone();

    const drawRectSpy = spyOn(component.canvasDrawer, 'drawRect').and.callThrough();
    const drawRectBorderSpy = spyOn(component.canvasDrawer, 'drawRectBorder').and.callThrough();

    const waitPromises = [waitToBeCalled(drawRectBorderSpy, 1), waitToBeCalled(drawRectSpy, 1)];

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
    expect(drawRectSpy).toHaveBeenCalledWith({
      x: 0,
      y: 0,
      w: 32,
      h: 32,
      color: component.color,
      alpha: 1.0,
    });

    expect(drawRectBorderSpy).toHaveBeenCalledTimes(1);
    expect(drawRectBorderSpy).toHaveBeenCalledWith(0, 0, 32, 32);
  });

  it('can select and draw first entry', async () => {
    setTraceAndSelectionRange(10n, 110n);

    fixture.detectChanges();
    await fixture.whenRenderingDone();

    await selectAndDrawCorrectEntry(1, 10n);
  });

  it('can select and draw correct entry based on mouse position', async () => {
    setTraceAndSelectionRange(10n, 110n);

    fixture.detectChanges();
    await fixture.whenRenderingDone();

    const secondEntryPos = Math.floor(component.canvasDrawer.getScaledCanvasWidth() * 0.02);
    await selectAndDrawCorrectEntry(secondEntryPos, 12n);
  });

  it('can select and draw correct entry based on mouse position when timeline zoomed in near start', async () => {
    setTraceAndSelectionRange(10n, 15n);

    fixture.detectChanges();
    await fixture.whenRenderingDone();

    const secondEntryPos = Math.floor(component.canvasDrawer.getScaledCanvasWidth() * 0.4);
    selectAndDrawCorrectEntry(secondEntryPos, 12n);
  });

  it('can select and draw correct entry based on mouse position when timeline zoomed in near end', async () => {
    setTraceAndSelectionRange(60n, 80n);

    fixture.detectChanges();
    await fixture.whenRenderingDone();

    const secondEntryPos = Math.floor(component.canvasDrawer.getScaledCanvasWidth() * 0.5);
    selectAndDrawCorrectEntry(secondEntryPos, 70n);
  });

  function setTraceAndSelectionRange(low: bigint, high: bigint) {
    component.trace = new TraceBuilder<{}>()
      .setType(TraceType.TRANSITION)
      .setEntries([{}, {}, {}, {}])
      .setTimestamps([
        new RealTimestamp(10n),
        new RealTimestamp(12n),
        new RealTimestamp(15n),
        new RealTimestamp(70n),
      ])
      .build();
    component.selectionRange = {from: new RealTimestamp(low), to: new RealTimestamp(high)};
  }

  async function selectAndDrawCorrectEntry(xPos: number, expectedTimestampNs: bigint) {
    const drawRectSpy = spyOn(component.canvasDrawer, 'drawRect').and.callThrough();
    const drawRectBorderSpy = spyOn(component.canvasDrawer, 'drawRectBorder').and.callThrough();

    const waitPromises = [waitToBeCalled(drawRectSpy, 1), waitToBeCalled(drawRectBorderSpy, 1)];

    await component.handleMouseDown({
      offsetX: xPos,
      offsetY: component.canvasDrawer.getScaledCanvasHeight() / 2,
      preventDefault: () => {},
      stopPropagation: () => {},
    } as MouseEvent);

    fixture.detectChanges();
    await fixture.whenRenderingDone();

    await Promise.all(waitPromises);

    expect(assertDefined(component.selectedEntry).getTimestamp().getValueNs()).toBe(
      expectedTimestampNs
    );
    expect(drawRectSpy).toHaveBeenCalledWith({
      x: xPos,
      y: 1,
      w: 30,
      h: 30,
      color: component.color,
      alpha: 1.0,
    });

    expect(drawRectBorderSpy).toHaveBeenCalledTimes(1);
    expect(drawRectBorderSpy).toHaveBeenCalledWith(xPos, 1, 30, 30);
  }
});
