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
import {Rect} from 'common/rect';
import {NO_TIMEZONE_OFFSET_FACTORY} from 'common/timestamp_factory';
import {PropertyTreeBuilder} from 'test/unit/property_tree_builder';
import {TraceBuilder} from 'test/unit/trace_builder';
import {waitToBeCalled} from 'test/utils';
import {TraceType} from 'trace/trace_type';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {TransitionTimelineComponent} from './transition_timeline_component';

describe('TransitionTimelineComponent', () => {
  let fixture: ComponentFixture<TransitionTimelineComponent>;
  let component: TransitionTimelineComponent;
  const time0 = NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(0n);
  const time10 = NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(10n);
  const time20 = NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(20n);
  const time30 = NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(30n);
  const time35 = NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(35n);
  const time60 = NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(60n);
  const time85 = NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(85n);
  const time110 = NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(110n);
  const time160 = NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(160n);

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
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('can draw non-overlapping transitions', async () => {
    component.trace = new TraceBuilder<PropertyTreeNode>()
      .setType(TraceType.TRANSITION)
      .setEntries([
        new PropertyTreeBuilder()
          .setIsRoot(true)
          .setRootId('TransitionsTraceEntry')
          .setName('transition')
          .setChildren([
            {
              name: 'wmData',
              children: [{name: 'finishTimeNs', value: time30}],
            },
            {
              name: 'shellData',
              children: [{name: 'dispatchTimeNs', value: time10}],
            },
            {name: 'aborted', value: false},
          ])
          .build(),

        new PropertyTreeBuilder()
          .setIsRoot(true)
          .setRootId('TransitionsTraceEntry')
          .setName('transition')
          .setChildren([
            {
              name: 'wmData',
              children: [{name: 'finishTimeNs', value: time110}],
            },
            {
              name: 'shellData',
              children: [{name: 'dispatchTimeNs', value: time60}],
            },
            {name: 'aborted', value: false},
          ])
          .build(),
      ])
      .setTimestamps([
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(10n),
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(60n),
      ])
      .build();
    component.selectionRange = {
      from: NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(10n),
      to: NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(110n),
    };

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
      new Rect(0, padding, Math.floor(width / 5), oneRowHeight),
      component.color,
      1,
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
    );
  });

  it('can draw transitions zoomed in', async () => {
    component.trace = new TraceBuilder<PropertyTreeNode>()
      .setType(TraceType.TRANSITION)
      .setEntries([
        new PropertyTreeBuilder()
          .setIsRoot(true)
          .setRootId('TransitionsTraceEntry')
          .setName('transition')
          .setChildren([
            {
              name: 'wmData',
              children: [{name: 'finishTimeNs', value: time20}],
            },
            {
              name: 'shellData',
              children: [{name: 'dispatchTimeNs', value: time0}],
            },
            {name: 'aborted', value: false},
          ])
          .build(),
        new PropertyTreeBuilder()
          .setIsRoot(true)
          .setRootId('TransitionsTraceEntry')
          .setName('transition')
          .setChildren([
            {
              name: 'wmData',
              children: [{name: 'finishTimeNs', value: time160}],
            },
            {
              name: 'shellData',
              children: [{name: 'dispatchTimeNs', value: time60}],
            },
            {name: 'aborted', value: false},
          ])
          .build(),
      ])
      .setTimestamps([time10, time60])
      .build();
    component.selectionRange = {
      from: time10,
      to: time110,
    };

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
      new Rect(0, padding, Math.floor(width / 10), oneRowHeight),
      component.color,
      1,
    );
    expect(drawRectSpy).toHaveBeenCalledWith(
      new Rect(Math.floor(width / 2), padding, Math.floor(width), oneRowHeight),
      component.color,
      1,
    );
  });

  it('can draw selected entry', async () => {
    const transition = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('TransitionsTraceEntry')
      .setName('transition')
      .setChildren([
        {
          name: 'wmData',
          children: [{name: 'finishTimeNs', value: time85}],
        },
        {
          name: 'shellData',
          children: [{name: 'dispatchTimeNs', value: time35}],
        },
        {name: 'aborted', value: false},
      ])
      .build();
    component.trace = new TraceBuilder<PropertyTreeNode>()
      .setType(TraceType.TRANSITION)
      .setEntries([transition])
      .setTimestamps([time35])
      .build();
    component.selectionRange = {
      from: time10,
      to: time110,
    };
    component.selectedEntry = component.trace.getEntry(0);

    const drawRectSpy = spyOn(component.canvasDrawer, 'drawRect');
    const drawRectBorderSpy = spyOn(component.canvasDrawer, 'drawRectBorder');
    const waitPromises = [
      waitToBeCalled(drawRectSpy, 1),
      waitToBeCalled(drawRectBorderSpy, 1),
    ];

    fixture.detectChanges();
    await fixture.whenRenderingDone();
    await Promise.all(waitPromises);

    const padding = 5;
    const oneRowTotalHeight = 30;
    const oneRowHeight = oneRowTotalHeight - padding;
    const width = component.canvasDrawer.getScaledCanvasWidth();
    const expectedRect = new Rect(
      Math.floor((width * 1) / 4),
      padding,
      Math.floor(width / 2),
      oneRowHeight,
    );
    expect(drawRectSpy).toHaveBeenCalledTimes(2); // once drawn as a normal entry another time with rect border
    expect(drawRectSpy).toHaveBeenCalledWith(expectedRect, component.color, 1);
    expect(drawRectBorderSpy).toHaveBeenCalledTimes(1);
    expect(drawRectBorderSpy).toHaveBeenCalledWith(expectedRect);
  });

  it('can draw hovering entry', async () => {
    const transition = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('TransitionsTraceEntry')
      .setName('transition')
      .setChildren([
        {
          name: 'wmData',
          children: [{name: 'finishTimeNs', value: time85}],
        },
        {
          name: 'shellData',
          children: [{name: 'dispatchTimeNs', value: time35}],
        },
        {name: 'aborted', value: false},
      ])
      .build();
    component.trace = new TraceBuilder<PropertyTreeNode>()
      .setType(TraceType.TRANSITION)
      .setEntries([transition])
      .setTimestamps([time35])
      .build();
    component.selectionRange = {
      from: time10,
      to: time110,
    };

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
    const expectedRect = new Rect(
      Math.floor((width * 1) / 4),
      padding,
      Math.floor(width / 2),
      oneRowHeight,
    );
    expect(drawRectSpy).toHaveBeenCalledTimes(1);
    expect(drawRectSpy).toHaveBeenCalledWith(expectedRect, component.color, 1);
    expect(drawRectBorderSpy).toHaveBeenCalledTimes(1);
    expect(drawRectBorderSpy).toHaveBeenCalledWith(expectedRect);
  });

  it('can draw overlapping transitions (default)', async () => {
    component.trace = new TraceBuilder<PropertyTreeNode>()
      .setType(TraceType.TRANSITION)
      .setEntries([
        new PropertyTreeBuilder()
          .setIsRoot(true)
          .setRootId('TransitionsTraceEntry')
          .setName('transition')
          .setChildren([
            {
              name: 'wmData',
              children: [{name: 'finishTimeNs', value: time85}],
            },
            {
              name: 'shellData',
              children: [{name: 'dispatchTimeNs', value: time10}],
            },
            {name: 'aborted', value: false},
          ])
          .build(),
        new PropertyTreeBuilder()
          .setIsRoot(true)
          .setRootId('TransitionsTraceEntry')
          .setName('transition')
          .setChildren([
            {
              name: 'wmData',
              children: [{name: 'finishTimeNs', value: time110}],
            },
            {
              name: 'shellData',
              children: [{name: 'dispatchTimeNs', value: time60}],
            },
            {name: 'aborted', value: false},
          ])
          .build(),
      ])
      .setTimestamps([time10, time60])
      .build();
    component.selectionRange = {
      from: time10,
      to: time110,
    };

    const drawRectSpy = spyOn(component.canvasDrawer, 'drawRect');

    fixture.detectChanges();
    await fixture.whenRenderingDone();
    await waitToBeCalled(drawRectSpy, 2);

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
    );
  });

  it('can draw overlapping transitions (contained)', async () => {
    component.trace = new TraceBuilder<PropertyTreeNode>()
      .setType(TraceType.TRANSITION)
      .setEntries([
        new PropertyTreeBuilder()
          .setIsRoot(true)
          .setRootId('TransitionsTraceEntry')
          .setName('transition')
          .setChildren([
            {
              name: 'wmData',
              children: [{name: 'finishTimeNs', value: time85}],
            },
            {
              name: 'shellData',
              children: [{name: 'dispatchTimeNs', value: time10}],
            },
            {name: 'aborted', value: false},
          ])
          .build(),
        new PropertyTreeBuilder()
          .setIsRoot(true)
          .setRootId('TransitionsTraceEntry')
          .setName('transition')
          .setChildren([
            {
              name: 'wmData',
              children: [{name: 'finishTimeNs', value: time60}],
            },
            {
              name: 'shellData',
              children: [{name: 'dispatchTimeNs', value: time35}],
            },
            {name: 'aborted', value: false},
          ])
          .build(),
      ])
      .setTimestamps([time10, time35])
      .build();
    component.selectionRange = {
      from: time10,
      to: time110,
    };

    const drawRectSpy = spyOn(component.canvasDrawer, 'drawRect');

    fixture.detectChanges();
    await fixture.whenRenderingDone();
    await waitToBeCalled(drawRectSpy, 2);

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
    );
  });

  it('can draw aborted transitions', async () => {
    component.trace = new TraceBuilder<PropertyTreeNode>()
      .setType(TraceType.TRANSITION)
      .setEntries([
        new PropertyTreeBuilder()
          .setIsRoot(true)
          .setRootId('TransitionsTraceEntry')
          .setName('transition')
          .setChildren([
            {
              name: 'wmData',
              value: null,
            },
            {
              name: 'shellData',
              children: [
                {name: 'dispatchTimeNs', value: time35},
                {name: 'abortTimeNs', value: time85},
              ],
            },
            {name: 'aborted', value: true},
          ])
          .build(),
      ])
      .setTimestamps([time35])
      .build();
    component.selectionRange = {
      from: time10,
      to: time110,
    };

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
      new Rect(
        Math.floor((width * 1) / 4),
        padding,
        Math.floor(width / 2),
        oneRowHeight,
      ),
      component.color,
      0.25,
    );
  });

  it('does not render transition with create time but no dispatch time', async () => {
    component.trace = new TraceBuilder<PropertyTreeNode>()
      .setType(TraceType.TRANSITION)
      .setEntries([
        new PropertyTreeBuilder()
          .setIsRoot(true)
          .setRootId('TransitionsTraceEntry')
          .setName('transition')
          .setChildren([
            {
              name: 'wmData',
              children: [
                {name: 'createTimeNs', value: time10},
                {name: 'finishTimeNs', value: time85},
              ],
            },
            {name: 'aborted', value: false},
          ])
          .build(),
      ])
      .setTimestamps([time10])
      .build();
    component.selectionRange = {
      from: time10,
      to: time110,
    };

    const drawRectSpy = spyOn(component.canvasDrawer, 'drawRect');

    fixture.detectChanges();
    await fixture.whenRenderingDone();

    expect(drawRectSpy).not.toHaveBeenCalled();
  });
});
