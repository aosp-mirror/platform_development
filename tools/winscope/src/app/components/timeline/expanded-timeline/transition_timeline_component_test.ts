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
import {assertDefined} from 'common/assert_utils';
import {Rect} from 'common/geometry/rect';
import {TimestampConverterUtils} from 'common/time/test_utils';
import {TimeRange, Timestamp} from 'common/time/time';
import {PropertyTreeBuilder} from 'test/unit/property_tree_builder';
import {TraceBuilder} from 'test/unit/trace_builder';
import {waitToBeCalled} from 'test/utils';
import {TraceType} from 'trace/trace_type';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {TransitionTimelineComponent} from './transition_timeline_component';

describe('TransitionTimelineComponent', () => {
  let fixture: ComponentFixture<TransitionTimelineComponent>;
  let component: TransitionTimelineComponent;
  let htmlElement: HTMLElement;

  const time0 = TimestampConverterUtils.makeRealTimestamp(0n);
  const time5 = TimestampConverterUtils.makeRealTimestamp(5n);
  const time10 = TimestampConverterUtils.makeRealTimestamp(10n);
  const time20 = TimestampConverterUtils.makeRealTimestamp(20n);
  const time30 = TimestampConverterUtils.makeRealTimestamp(30n);
  const time35 = TimestampConverterUtils.makeRealTimestamp(35n);
  const time60 = TimestampConverterUtils.makeRealTimestamp(60n);
  const time85 = TimestampConverterUtils.makeRealTimestamp(85n);
  const time110 = TimestampConverterUtils.makeRealTimestamp(110n);
  const time120 = TimestampConverterUtils.makeRealTimestamp(120n);
  const time160 = TimestampConverterUtils.makeRealTimestamp(160n);

  const range10to110 = new TimeRange(time10, time110);
  const range0to160 = new TimeRange(time0, time160);

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
    component.timestampConverter = TimestampConverterUtils.TIMESTAMP_CONVERTER;
    component.fullRange = range0to160;
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('can draw non-overlapping transitions', async () => {
    const drawRectSpy = spyOn(component.canvasDrawer, 'drawRect');

    const transitions = [
      makeTransition(time10, time30),
      makeTransition(time60, time110),
    ];
    await setTraceAndSelectionRange(transitions, [time10, time60]);

    const padding = 5;
    const oneRowTotalHeight = 30;
    const oneRowHeight = oneRowTotalHeight - padding;
    const width = component.canvasDrawer.getScaledCanvasWidth();

    expect(drawRectSpy).toHaveBeenCalledTimes(2);
    expect(drawRectSpy).toHaveBeenCalledWith(
      new Rect(0, padding, Math.floor(width / 5), oneRowHeight),
      component.color,
      1,
      false,
      false,
    );
    expect(drawRectSpy).toHaveBeenCalledWith(
      new Rect(
        Math.floor(width / 2),
        padding,
        Math.floor(width / 2),
        oneRowHeight,
      ),
      component.color,
      1,
      false,
      false,
    );
  });

  it('can draw transitions zoomed in', async () => {
    const drawRectSpy = spyOn(component.canvasDrawer, 'drawRect');

    const transitions = [
      makeTransition(time10, time20), // drawn
      makeTransition(time60, time160), // drawn
      makeTransition(time120, time160), // not drawn - starts after selection range
      makeTransition(time0, time5), // not drawn - finishes before selection range
      makeTransition(time5, undefined), // not drawn - starts before selection range with unknown finish time
    ];
    await setTraceAndSelectionRange(transitions, [
      time10,
      time60,
      time120,
      time0,
      time5,
    ]);

    const padding = 5;
    const oneRowTotalHeight =
      (component.canvasDrawer.getScaledCanvasHeight() - 2 * padding) / 3;
    const oneRowHeight = oneRowTotalHeight - padding;
    const width = component.canvasDrawer.getScaledCanvasWidth();

    expect(drawRectSpy).toHaveBeenCalledTimes(2); // does not draw final transition
    expect(drawRectSpy).toHaveBeenCalledWith(
      new Rect(0, padding, Math.floor(width / 10), oneRowHeight),
      component.color,
      1,
      false,
      false,
    );
    expect(drawRectSpy).toHaveBeenCalledWith(
      new Rect(Math.floor(width / 2), padding, Math.floor(width), oneRowHeight),
      component.color,
      1,
      false,
      false,
    );
  });

  it('can draw selected entry', async () => {
    const drawRectSpy = spyOn(component.canvasDrawer, 'drawRect');
    const drawRectBorderSpy = spyOn(component.canvasDrawer, 'drawRectBorder');
    const waitPromises = [
      waitToBeCalled(drawRectSpy, 1),
      waitToBeCalled(drawRectBorderSpy, 1),
    ];
    await setDefaultTraceAndSelectionRange(true);
    await Promise.all(waitPromises);

    const expectedRect = getExpectedBorderedRect();
    expect(drawRectSpy).toHaveBeenCalledOnceWith(
      expectedRect,
      component.color,
      1,
      false,
      false,
    );
    expect(drawRectBorderSpy).toHaveBeenCalledTimes(1);
    expect(drawRectBorderSpy).toHaveBeenCalledWith(expectedRect);
  });

  it('can draw hovering entry', async () => {
    const drawRectSpy = spyOn(component.canvasDrawer, 'drawRect');
    await setDefaultTraceAndSelectionRange();
    const expectedRect = getExpectedBorderedRect();

    expect(drawRectSpy).toHaveBeenCalledOnceWith(
      expectedRect,
      component.color,
      1,
      false,
      false,
    );

    const drawRectBorderSpy = spyOn(
      component.canvasDrawer,
      'drawRectBorder',
    ).and.callThrough();

    await dispatchMousemoveEvent();
    expect(drawRectBorderSpy).toHaveBeenCalledOnceWith(expectedRect);

    drawRectSpy.calls.reset();
    drawRectBorderSpy.calls.reset();

    await dispatchMousemoveEvent();
    expect(drawRectSpy).toHaveBeenCalledOnceWith(
      expectedRect,
      component.color,
      1,
      false,
      false,
    );
    expect(drawRectBorderSpy).toHaveBeenCalledOnceWith(expectedRect);
  });

  it('redraws timeline to clear hover entry after mouse out', async () => {
    await setDefaultTraceAndSelectionRange();
    const drawRectSpy = spyOn(component.canvasDrawer, 'drawRect');

    const mouseoutEvent = new MouseEvent('mouseout');
    component.getCanvas().dispatchEvent(mouseoutEvent);
    fixture.detectChanges();
    await fixture.whenRenderingDone();
    expect(drawRectSpy).not.toHaveBeenCalled();

    await dispatchMousemoveEvent();
    component.getCanvas().dispatchEvent(mouseoutEvent);
    fixture.detectChanges();
    await fixture.whenRenderingDone();

    expect(drawRectSpy).toHaveBeenCalledOnceWith(
      getExpectedBorderedRect(),
      component.color,
      1,
      false,
      false,
    );
  });

  it('can draw overlapping transitions (default)', async () => {
    const drawRectSpy = spyOn(component.canvasDrawer, 'drawRect');
    const transitions = [
      makeTransition(time10, time85),
      makeTransition(time60, time110),
    ];
    await setTraceAndSelectionRange(transitions, [time10, time60]);

    const padding = 5;
    const rows = 2;
    const oneRowTotalHeight =
      (component.canvasDrawer.getScaledCanvasHeight() - 2 * padding) / rows;
    const oneRowHeight = oneRowTotalHeight - padding;
    const width = component.canvasDrawer.getScaledCanvasWidth();

    expect(drawRectSpy).toHaveBeenCalledTimes(2);
    expect(drawRectSpy).toHaveBeenCalledWith(
      new Rect(0, padding, Math.floor((width * 3) / 4), oneRowHeight),
      component.color,
      1,
      false,
      false,
    );
    expect(drawRectSpy).toHaveBeenCalledWith(
      new Rect(
        Math.floor(width / 2),
        padding + oneRowTotalHeight,
        Math.floor(width / 2),
        oneRowHeight,
      ),
      component.color,
      1,
      false,
      false,
    );
  });

  it('can draw overlapping transitions (contained)', async () => {
    const drawRectSpy = spyOn(component.canvasDrawer, 'drawRect');
    const transitions = [
      makeTransition(time10, time85),
      makeTransition(time35, time60),
    ];
    await setTraceAndSelectionRange(transitions, [time10, time35]);

    const padding = 5;
    const rows = 2;
    const oneRowTotalHeight =
      (component.canvasDrawer.getScaledCanvasHeight() - 2 * padding) / rows;
    const oneRowHeight = oneRowTotalHeight - padding;
    const width = component.canvasDrawer.getScaledCanvasWidth();

    expect(drawRectSpy).toHaveBeenCalledTimes(2);
    expect(drawRectSpy).toHaveBeenCalledWith(
      new Rect(0, padding, Math.floor((width * 3) / 4), oneRowHeight),
      component.color,
      1,
      false,
      false,
    );
    expect(drawRectSpy).toHaveBeenCalledWith(
      new Rect(
        Math.floor(width / 4),
        padding + oneRowTotalHeight,
        Math.floor(width / 4),
        oneRowHeight,
      ),
      component.color,
      1,
      false,
      false,
    );
  });

  it('can draw aborted transitions', async () => {
    const drawRectSpy = spyOn(component.canvasDrawer, 'drawRect');
    const transitions = [makeTransition(time35, undefined, time85)];
    await setTraceAndSelectionRange(transitions, [time35]);

    const padding = 5;
    const oneRowTotalHeight = 30;
    const oneRowHeight = oneRowTotalHeight - padding;
    const width = component.canvasDrawer.getScaledCanvasWidth();

    expect(drawRectSpy).toHaveBeenCalledTimes(1);
    expect(drawRectSpy).toHaveBeenCalledWith(
      new Rect(
        Math.floor((width * 1) / 4),
        padding,
        Math.floor(width / 2),
        oneRowHeight,
      ),
      component.color,
      0.25,
      false,
      false,
    );
  });

  it('can draw transition with unknown start time', async () => {
    const drawRectSpy = spyOn(component.canvasDrawer, 'drawRect');
    const transitions = [makeTransition(undefined, time85)];
    await setTraceAndSelectionRange(transitions, [time0]);

    const padding = 5;
    const oneRowTotalHeight = 30;
    const oneRowHeight = oneRowTotalHeight - padding;

    expect(drawRectSpy).toHaveBeenCalledTimes(1);
    expect(drawRectSpy).toHaveBeenCalledWith(
      new Rect(
        Math.floor((component.canvasDrawer.getScaledCanvasWidth() * 74) / 100),
        padding,
        oneRowHeight,
        oneRowHeight,
      ),
      component.color,
      1,
      true,
      false,
    );
  });

  it('can draw transition with unknown end time', async () => {
    const drawRectSpy = spyOn(component.canvasDrawer, 'drawRect');
    const transitions = [makeTransition(time35, undefined)];
    await setTraceAndSelectionRange(transitions, [time35]);

    const padding = 5;
    const oneRowTotalHeight = 30;
    const oneRowHeight = oneRowTotalHeight - padding;

    expect(drawRectSpy).toHaveBeenCalledTimes(1);
    expect(drawRectSpy).toHaveBeenCalledWith(
      new Rect(
        Math.floor((component.canvasDrawer.getScaledCanvasWidth() * 1) / 4),
        padding,
        oneRowHeight,
        oneRowHeight,
      ),
      component.color,
      1,
      false,
      true,
    );
  });

  it('does not render transition with create time but no dispatch time', async () => {
    const drawRectSpy = spyOn(component.canvasDrawer, 'drawRect');
    const transitions = [makeTransition(undefined, time85, undefined, time10)];
    await setTraceAndSelectionRange(transitions, [time10]);
    expect(drawRectSpy).not.toHaveBeenCalled();
  });

  it('handles missing trace entries', async () => {
    const transition0 = makeTransition(time10, time30);
    const transition1 = makeTransition(time60, time110);

    component.trace = new TraceBuilder<PropertyTreeNode>()
      .setType(TraceType.TRANSITION)
      .setEntries([transition0, transition1])
      .setTimestamps([time10, time20])
      .build();
    component.transitionEntries = [transition0, undefined];
    component.selectionRange = range10to110;

    const drawRectSpy = spyOn(component.canvasDrawer, 'drawRect');

    fixture.detectChanges();
    await fixture.whenRenderingDone();

    expect(drawRectSpy).toHaveBeenCalledTimes(1);
  });

  it('emits scroll event', async () => {
    await setDefaultTraceAndSelectionRange();

    const spy = spyOn(component.onScrollEvent, 'emit');
    htmlElement.dispatchEvent(new WheelEvent('wheel'));
    fixture.detectChanges();
    expect(spy).toHaveBeenCalled();
  });

  it('tracks mouse position', async () => {
    await setDefaultTraceAndSelectionRange();

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

  async function setDefaultTraceAndSelectionRange(setSelectedEntry = false) {
    const transitions = [makeTransition(time35, time85)];
    component.trace = new TraceBuilder<PropertyTreeNode>()
      .setType(TraceType.TRANSITION)
      .setEntries(transitions)
      .setTimestamps([time35])
      .build();
    component.transitionEntries = transitions;
    component.selectionRange = range10to110;
    if (setSelectedEntry) component.selectedEntry = component.trace.getEntry(0);
    fixture.detectChanges();
    await fixture.whenRenderingDone();
  }

  function makeTransition(
    dispatchTime: Timestamp | undefined,
    finishTime: Timestamp | undefined,
    abortTime?: Timestamp,
    createTime?: Timestamp,
  ): PropertyTreeNode {
    const shellDataChildren = [];
    if (dispatchTime !== undefined) {
      shellDataChildren.push({name: 'dispatchTimeNs', value: dispatchTime});
    }
    if (dispatchTime !== undefined) {
      shellDataChildren.push({name: 'abortTimeNs', value: abortTime});
    }

    const wmDataChildren = [];
    if (finishTime !== undefined) {
      wmDataChildren.push({name: 'finishTimeNs', value: finishTime});
    }
    if (createTime !== undefined) {
      wmDataChildren.push({name: 'createTimeNs', value: createTime});
    }

    return new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('TransitionsTraceEntry')
      .setName('transition')
      .setChildren([
        {
          name: 'wmData',
          children: wmDataChildren,
        },
        {
          name: 'shellData',
          children: shellDataChildren,
        },
        {name: 'aborted', value: abortTime !== undefined},
      ])
      .build();
  }

  async function setTraceAndSelectionRange(
    transitions: PropertyTreeNode[],
    timestamps: Timestamp[],
    range = range10to110,
  ) {
    component.trace = new TraceBuilder<PropertyTreeNode>()
      .setType(TraceType.TRANSITION)
      .setEntries(transitions)
      .setTimestamps(timestamps)
      .build();
    component.transitionEntries = transitions;
    component.selectionRange = range;
    fixture.detectChanges();
    await fixture.whenRenderingDone();
  }

  function getExpectedBorderedRect(): Rect {
    const padding = 5;
    const oneRowTotalHeight = 30;
    const oneRowHeight = oneRowTotalHeight - padding;
    const width = component.canvasDrawer.getScaledCanvasWidth();
    return new Rect(
      Math.floor((width * 1) / 4),
      padding,
      Math.floor(width / 2),
      oneRowHeight,
    );
  }

  async function dispatchMousemoveEvent() {
    const mousemoveEvent = new MouseEvent('mousemove');
    spyOnProperty(mousemoveEvent, 'offsetX').and.returnValue(
      Math.floor(component.canvasDrawer.getScaledCanvasWidth() / 2),
    );
    spyOnProperty(mousemoveEvent, 'offsetY').and.returnValue(25 / 2);
    component.getCanvas().dispatchEvent(mousemoveEvent);
    fixture.detectChanges();
    await fixture.whenRenderingDone();
  }
});
