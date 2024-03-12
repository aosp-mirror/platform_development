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
import {RealTimestamp} from 'common/time';
import {Transition} from 'flickerlib/common';
import {TraceBuilder} from 'test/unit/trace_builder';
import {waitToBeCalled} from 'test/utils';
import {TraceType} from 'trace/trace_type';
import {TransitionTimelineComponent} from './transition_timeline_component';

describe('TransitionTimelineComponent', () => {
  let fixture: ComponentFixture<TransitionTimelineComponent>;
  let component: TransitionTimelineComponent;
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
      declarations: [TransitionTimelineComponent],
    })
      .overrideComponent(TransitionTimelineComponent, {
        set: {changeDetection: ChangeDetectionStrategy.Default},
      })
      .compileComponents();
    fixture = TestBed.createComponent(TransitionTimelineComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('can draw non-overlapping transitions', async () => {
    component.trace = new TraceBuilder()
      .setType(TraceType.TRANSITION)
      .setEntries([
        {
          createTime: {unixNanos: 10n},
          finishTime: {unixNanos: 30n},
        } as Transition,
        {
          createTime: {unixNanos: 60n},
          finishTime: {unixNanos: 110n},
        } as Transition,
      ])
      .setTimestamps([new RealTimestamp(10n), new RealTimestamp(60n)])
      .build();
    component.selectionRange = {from: new RealTimestamp(10n), to: new RealTimestamp(110n)};

    const drawRectSpy = spyOn(component.canvasDrawer, 'drawRect');

    fixture.detectChanges();
    await fixture.whenRenderingDone();
    await waitToBeCalled(drawRectSpy, 2);

    const padding = 5;
    const oneRowTotalHeight = 30;
    const oneRowHeight = oneRowTotalHeight - padding;
    const width = component.canvasDrawer.getScaledCanvasWidth();

    expect(drawRectSpy).toHaveBeenCalledTimes(2);
    expect(drawRectSpy).toHaveBeenCalledWith(
      {
        x: 0,
        y: padding,
        w: Math.floor(width / 5),
        h: oneRowHeight,
      },
      component.color,
      1
    );
    expect(drawRectSpy).toHaveBeenCalledWith(
      {
        x: Math.floor(width / 2),
        y: padding,
        w: Math.floor(width / 2),
        h: oneRowHeight,
      },
      component.color,
      1
    );
  });

  it('can draw transitions zoomed in', async () => {
    component.trace = new TraceBuilder()
      .setType(TraceType.TRANSITION)
      .setEntries([
        {
          createTime: {unixNanos: 0n},
          finishTime: {unixNanos: 20n},
        } as Transition,
        {
          createTime: {unixNanos: 60n},
          finishTime: {unixNanos: 160n},
        } as Transition,
      ])
      .setTimestamps([new RealTimestamp(10n), new RealTimestamp(60n)])
      .build();
    component.selectionRange = {from: new RealTimestamp(10n), to: new RealTimestamp(110n)};

    const drawRectSpy = spyOn(component.canvasDrawer, 'drawRect');

    fixture.detectChanges();
    await fixture.whenRenderingDone();
    await waitToBeCalled(drawRectSpy, 2);

    const padding = 5;
    const oneRowTotalHeight = 30;
    const oneRowHeight = oneRowTotalHeight - padding;
    const width = component.canvasDrawer.getScaledCanvasWidth();

    expect(drawRectSpy).toHaveBeenCalledTimes(2);
    expect(drawRectSpy).toHaveBeenCalledWith(
      {
        x: -Math.floor(width / 10),
        y: padding,
        w: Math.floor(width / 5),
        h: oneRowHeight,
      },
      component.color,
      1
    );
    expect(drawRectSpy).toHaveBeenCalledWith(
      {
        x: Math.floor(width / 2),
        y: padding,
        w: Math.floor(width),
        h: oneRowHeight,
      },
      component.color,
      1
    );
  });

  it('can draw selected entry', async () => {
    const transition = {
      createTime: {unixNanos: 35n},
      finishTime: {unixNanos: 85n},
      aborted: true,
    } as Transition;
    component.trace = new TraceBuilder()
      .setType(TraceType.TRANSITION)
      .setEntries([transition])
      .setTimestamps([new RealTimestamp(35n)])
      .build();
    component.selectionRange = {from: new RealTimestamp(10n), to: new RealTimestamp(110n)};
    component.selectedEntry = component.trace.getEntry(0);

    const drawRectSpy = spyOn(component.canvasDrawer, 'drawRect');
    const drawRectBorderSpy = spyOn(component.canvasDrawer, 'drawRectBorder');

    const waitPromises = [waitToBeCalled(drawRectSpy, 1), waitToBeCalled(drawRectBorderSpy, 1)];

    fixture.detectChanges();
    await fixture.whenRenderingDone();

    await Promise.all(waitPromises);

    const padding = 5;
    const oneRowTotalHeight = 30;
    const oneRowHeight = oneRowTotalHeight - padding;
    const width = component.canvasDrawer.getScaledCanvasWidth();

    const expectedRect = {
      x: Math.floor((width * 1) / 4),
      y: padding,
      w: Math.floor(width / 2),
      h: oneRowHeight,
    };
    expect(drawRectSpy).toHaveBeenCalledTimes(2); // once drawn as a normal entry another time with rect border
    expect(drawRectSpy).toHaveBeenCalledWith(expectedRect, component.color, 0.25);

    expect(drawRectBorderSpy).toHaveBeenCalledTimes(1);
    expect(drawRectBorderSpy).toHaveBeenCalledWith(expectedRect);
  });

  it('can draw hovering entry', async () => {
    const transition = {
      createTime: {unixNanos: 35n},
      finishTime: {unixNanos: 85n},
      aborted: true,
    } as Transition;
    component.trace = new TraceBuilder()
      .setType(TraceType.TRANSITION)
      .setEntries([transition])
      .setTimestamps([new RealTimestamp(35n)])
      .build();
    component.selectionRange = {from: new RealTimestamp(10n), to: new RealTimestamp(110n)};

    const drawRectSpy = spyOn(component.canvasDrawer, 'drawRect');
    const drawRectBorderSpy = spyOn(component.canvasDrawer, 'drawRectBorder');

    fixture.detectChanges();
    await fixture.whenRenderingDone();

    const padding = 5;
    const oneRowTotalHeight = 30;
    const oneRowHeight = oneRowTotalHeight - padding;
    const width = component.canvasDrawer.getScaledCanvasWidth();

    component.handleMouseMove({
      offsetX: Math.floor(width / 2),
      offsetY: oneRowTotalHeight / 2,
      preventDefault: () => {},
      stopPropagation: () => {},
    } as MouseEvent);

    await waitToBeCalled(drawRectSpy, 1);
    await waitToBeCalled(drawRectBorderSpy, 1);

    const expectedRect = {
      x: Math.floor((width * 1) / 4),
      y: padding,
      w: Math.floor(width / 2),
      h: oneRowHeight,
    };
    expect(drawRectSpy).toHaveBeenCalledTimes(1);
    expect(drawRectSpy).toHaveBeenCalledWith(expectedRect, component.color, 0.25);

    expect(drawRectBorderSpy).toHaveBeenCalledTimes(1);
    expect(drawRectBorderSpy).toHaveBeenCalledWith(expectedRect);
  });

  it('can draw overlapping transitions (default)', async () => {
    component.trace = new TraceBuilder()
      .setType(TraceType.TRANSITION)
      .setEntries([
        {
          createTime: {unixNanos: 10n},
          finishTime: {unixNanos: 85n},
        } as Transition,
        {
          createTime: {unixNanos: 60n},
          finishTime: {unixNanos: 110n},
        } as Transition,
      ])
      .setTimestamps([new RealTimestamp(10n), new RealTimestamp(60n)])
      .build();
    component.selectionRange = {from: new RealTimestamp(10n), to: new RealTimestamp(110n)};

    const drawRectSpy = spyOn(component.canvasDrawer, 'drawRect');

    fixture.detectChanges();
    await fixture.whenRenderingDone();
    await waitToBeCalled(drawRectSpy, 2);

    const padding = 5;
    const rows = 2;
    const oneRowTotalHeight = (component.canvasDrawer.getScaledCanvasHeight() - 2 * padding) / rows;
    const oneRowHeight = oneRowTotalHeight - padding;
    const width = component.canvasDrawer.getScaledCanvasWidth();

    expect(drawRectSpy).toHaveBeenCalledTimes(2);
    expect(drawRectSpy).toHaveBeenCalledWith(
      {
        x: 0,
        y: padding,
        w: Math.floor((width * 3) / 4),
        h: oneRowHeight,
      },
      component.color,
      1
    );
    expect(drawRectSpy).toHaveBeenCalledWith(
      {
        x: Math.floor(width / 2),
        y: padding + oneRowTotalHeight,
        w: Math.floor(width / 2),
        h: oneRowHeight,
      },
      component.color,
      1
    );
  });

  it('can draw overlapping transitions (contained)', async () => {
    component.trace = new TraceBuilder()
      .setType(TraceType.TRANSITION)
      .setEntries([
        {
          createTime: {unixNanos: 10n},
          finishTime: {unixNanos: 85n},
        } as Transition,
        {
          createTime: {unixNanos: 35n},
          finishTime: {unixNanos: 60n},
        } as Transition,
      ])
      .setTimestamps([new RealTimestamp(10n), new RealTimestamp(35n)])
      .build();
    component.selectionRange = {from: new RealTimestamp(10n), to: new RealTimestamp(110n)};

    const drawRectSpy = spyOn(component.canvasDrawer, 'drawRect');

    fixture.detectChanges();
    await fixture.whenRenderingDone();
    await waitToBeCalled(drawRectSpy, 2);

    const padding = 5;
    const rows = 2;
    const oneRowTotalHeight = (component.canvasDrawer.getScaledCanvasHeight() - 2 * padding) / rows;
    const oneRowHeight = oneRowTotalHeight - padding;
    const width = component.canvasDrawer.getScaledCanvasWidth();

    expect(drawRectSpy).toHaveBeenCalledTimes(2);
    expect(drawRectSpy).toHaveBeenCalledWith(
      {
        x: 0,
        y: padding,
        w: Math.floor((width * 3) / 4),
        h: oneRowHeight,
      },
      component.color,
      1
    );
    expect(drawRectSpy).toHaveBeenCalledWith(
      {
        x: Math.floor(width / 4),
        y: padding + oneRowTotalHeight,
        w: Math.floor(width / 4),
        h: oneRowHeight,
      },
      component.color,
      1
    );
  });

  it('can draw aborted transitions', async () => {
    component.trace = new TraceBuilder()
      .setType(TraceType.TRANSITION)
      .setEntries([
        {
          createTime: {unixNanos: 35n},
          finishTime: {unixNanos: 85n},
          aborted: true,
        } as Transition,
      ])
      .setTimestamps([new RealTimestamp(35n)])
      .build();
    component.selectionRange = {from: new RealTimestamp(10n), to: new RealTimestamp(110n)};

    const drawRectSpy = spyOn(component.canvasDrawer, 'drawRect');

    fixture.detectChanges();
    await fixture.whenRenderingDone();
    await waitToBeCalled(drawRectSpy, 1);

    const padding = 5;
    const oneRowTotalHeight = 30;
    const oneRowHeight = oneRowTotalHeight - padding;
    const width = component.canvasDrawer.getScaledCanvasWidth();

    expect(drawRectSpy).toHaveBeenCalledTimes(1);
    expect(drawRectSpy).toHaveBeenCalledWith(
      {
        x: Math.floor((width * 1) / 4),
        y: padding,
        w: Math.floor(width / 2),
        h: oneRowHeight,
      },
      component.color,
      0.25
    );
  });

  it('does not render transition with min creation time', async () => {
    component.trace = new TraceBuilder()
      .setType(TraceType.TRANSITION)
      .setEntries([
        {
          createTime: {unixNanos: 10n, isMin: true},
          finishTime: {unixNanos: 30n},
        } as Transition,
      ])
      .setTimestamps([new RealTimestamp(10n)])
      .build();
    component.shouldNotRenderEntries.push(0);
    component.selectionRange = {from: new RealTimestamp(10n), to: new RealTimestamp(110n)};

    const drawRectSpy = spyOn(component.canvasDrawer, 'drawRect');

    fixture.detectChanges();
    await fixture.whenRenderingDone();

    expect(drawRectSpy).not.toHaveBeenCalled();
  });
});
