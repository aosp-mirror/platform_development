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
import {CdkMenuModule} from '@angular/cdk/menu';
import {ChangeDetectionStrategy, Component, ViewChild} from '@angular/core';
import {ComponentFixture, fakeAsync, TestBed} from '@angular/core/testing';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {MatButtonModule} from '@angular/material/button';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatIconModule} from '@angular/material/icon';
import {MatInputModule} from '@angular/material/input';
import {MatSelectModule} from '@angular/material/select';
import {MatTooltipModule} from '@angular/material/tooltip';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {TimelineData} from 'app/timeline_data';
import {assertDefined} from 'common/assert_utils';
import {TimeRange, Timestamp} from 'common/time';
import {TimestampConverterUtils} from 'test/unit/timestamp_converter_utils';
import {TracesBuilder} from 'test/unit/traces_builder';
import {dragElement} from 'test/utils';
import {TracePosition} from 'trace/trace_position';
import {TraceType} from 'trace/trace_type';
import {MiniTimelineComponent} from './mini_timeline_component';
import {SliderComponent} from './slider_component';

describe('MiniTimelineComponent', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let component: TestHostComponent;
  let htmlElement: HTMLElement;
  let timelineData: TimelineData;

  const timestamp10 = TimestampConverterUtils.makeRealTimestamp(10n);
  const timestamp15 = TimestampConverterUtils.makeRealTimestamp(15n);
  const timestamp16 = TimestampConverterUtils.makeRealTimestamp(16n);
  const timestamp20 = TimestampConverterUtils.makeRealTimestamp(20n);
  const timestamp700 = TimestampConverterUtils.makeRealTimestamp(700n);
  const timestamp810 = TimestampConverterUtils.makeRealTimestamp(810n);
  const timestamp1000 = TimestampConverterUtils.makeRealTimestamp(1000n);
  const timestamp2000 = TimestampConverterUtils.makeRealTimestamp(2000n);
  const timestamp4000 = TimestampConverterUtils.makeRealTimestamp(4000n);

  const position800 = TracePosition.fromTimestamp(
    TimestampConverterUtils.makeRealTimestamp(800n),
  );

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
        CdkMenuModule,
      ],
      declarations: [TestHostComponent, MiniTimelineComponent, SliderComponent],
    })
      .overrideComponent(MiniTimelineComponent, {
        set: {changeDetection: ChangeDetectionStrategy.Default},
      })
      .compileComponents();
    fixture = TestBed.createComponent(TestHostComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;

    timelineData = new TimelineData();
    const traces = new TracesBuilder()
      .setTimestamps(TraceType.SURFACE_FLINGER, [timestamp10])
      .setTimestamps(TraceType.TRANSACTIONS, [timestamp10, timestamp20])
      .setTimestamps(TraceType.WINDOW_MANAGER, [timestamp20])
      .build();
    await timelineData.initialize(
      traces,
      undefined,
      TimestampConverterUtils.TIMESTAMP_CONVERTER,
    );
    component.timelineData = timelineData;
    expect(timelineData.getCurrentPosition()).toBeDefined();
    component.currentTracePosition = timelineData.getCurrentPosition()!;
    component.selectedTraces = [TraceType.SURFACE_FLINGER];
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('redraws on resize', () => {
    fixture.detectChanges();
    const miniTimelineComponent = assertDefined(
      component.miniTimelineComponent,
    );
    const spy = spyOn(assertDefined(miniTimelineComponent.drawer), 'draw');
    expect(spy).not.toHaveBeenCalled();

    miniTimelineComponent.onResize({} as Event);

    expect(spy).toHaveBeenCalled();
  });

  it('resets zoom on reset zoom button click', () => {
    const expectedZoomRange = new TimeRange(timestamp15, timestamp16);
    timelineData.setZoom(expectedZoomRange);

    let zoomRange = timelineData.getZoomRange();
    let fullRange = timelineData.getFullTimeRange();
    expect(zoomRange).toBe(expectedZoomRange);
    expect(fullRange.from).toBe(timestamp10);
    expect(fullRange.to).toBe(timestamp20);

    fixture.detectChanges();

    const zoomButton = assertDefined(
      htmlElement.querySelector('button#reset-zoom-btn'),
    ) as HTMLButtonElement;
    zoomButton.click();

    zoomRange = timelineData.getZoomRange();
    fullRange = timelineData.getFullTimeRange();
    expect(zoomRange).toBe(fullRange);
  });

  it('show zoom controls when zoomed out', () => {
    const zoomControlDiv = assertDefined(
      htmlElement.querySelector('.zoom-control'),
    );
    expect(window.getComputedStyle(zoomControlDiv).visibility).toBe('visible');

    const zoomButton = assertDefined(
      htmlElement.querySelector('button#reset-zoom-btn'),
    ) as HTMLButtonElement;
    expect(window.getComputedStyle(zoomButton).visibility).toBe('visible');
  });

  it('shows zoom controls when zoomed in', () => {
    const zoom = new TimeRange(timestamp15, timestamp16);
    timelineData.setZoom(zoom);

    fixture.detectChanges();

    const zoomControlDiv = assertDefined(
      htmlElement.querySelector('.zoom-control'),
    );
    expect(window.getComputedStyle(zoomControlDiv).visibility).toBe('visible');

    const zoomButton = assertDefined(
      htmlElement.querySelector('button#reset-zoom-btn'),
    ) as HTMLButtonElement;
    expect(window.getComputedStyle(zoomButton).visibility).toBe('visible');
  });

  it('loads with initial zoom', () => {
    const initialZoom = new TimeRange(timestamp15, timestamp16);
    component.initialZoom = initialZoom;
    fixture.detectChanges();
    const timelineData = assertDefined(component.timelineData);
    const zoomRange = timelineData.getZoomRange();
    expect(zoomRange.from).toEqual(initialZoom.from);
    expect(zoomRange.to).toEqual(initialZoom.to);
  });

  it('updates timelineData on zoom changed', () => {
    fixture.detectChanges();
    const zoom = new TimeRange(timestamp15, timestamp16);
    assertDefined(component.miniTimelineComponent).onZoomChanged(zoom);
    fixture.detectChanges();
    expect(timelineData.getZoomRange()).toBe(zoom);
  });

  it('creates an appropriately sized canvas', () => {
    fixture.detectChanges();
    const canvas = assertDefined(component.miniTimelineComponent).getCanvas();
    expect(canvas.width).toBeGreaterThan(100);
    expect(canvas.height).toBeGreaterThan(10);
  });

  it('getTracesToShow returns traces targeted by selectedTraces', () => {
    fixture.detectChanges();
    const selectedTraces = assertDefined(component.selectedTraces);
    const traces = assertDefined(
      component.miniTimelineComponent,
    ).getTracesToShow();
    const types: TraceType[] = [];
    traces.forEachTrace((trace) => {
      types.push(trace.type);
    });
    expect(types).toHaveSize(selectedTraces.length);
    for (const type of selectedTraces) {
      expect(types).toContain(type);
    }
  });

  it('getTracesToShow adds traces in correct order', () => {
    component.selectedTraces = [
      TraceType.WINDOW_MANAGER,
      TraceType.SURFACE_FLINGER,
      TraceType.TRANSACTIONS,
    ];
    fixture.detectChanges();
    const traces = assertDefined(component.miniTimelineComponent)
      .getTracesToShow()
      .mapTrace((trace, type) => trace.type);
    expect(traces).toEqual([
      TraceType.TRANSACTIONS,
      TraceType.WINDOW_MANAGER,
      TraceType.SURFACE_FLINGER,
    ]);
  });

  it('moving slider around updates zoom', fakeAsync(() => {
    fixture.detectChanges();
    const initialZoom = new TimeRange(timestamp15, timestamp16);
    assertDefined(component.miniTimelineComponent).onZoomChanged(initialZoom);
    fixture.detectChanges();

    const slider = assertDefined(htmlElement.querySelector('.slider .handle'));
    expect(window.getComputedStyle(slider).visibility).toEqual('visible');

    dragElement(fixture, slider, 100, 8);

    const finalZoom = timelineData.getZoomRange();
    expect(finalZoom).not.toBe(initialZoom);
  }));

  it('zoom button zooms onto cursor', () => {
    const timelineData = assertDefined(component.timelineData);
    const traces = new TracesBuilder()
      .setTimestamps(TraceType.SURFACE_FLINGER, [timestamp10])
      .setTimestamps(TraceType.WINDOW_MANAGER, [timestamp1000])
      .build();

    timelineData.initialize(
      traces,
      undefined,
      TimestampConverterUtils.TIMESTAMP_CONVERTER,
    );
    fixture.detectChanges();

    let initialZoom = new TimeRange(timestamp10, timestamp1000);
    assertDefined(component.miniTimelineComponent).onZoomChanged(initialZoom);

    timelineData.setPosition(position800);
    const cursorPos = position800.timestamp.getValueNs();

    fixture.detectChanges();

    const zoomButton = assertDefined(
      htmlElement.querySelector('#zoom-in-btn'),
    ) as HTMLButtonElement;

    for (let i = 0; i < 10; i++) {
      zoomButton.click();
      fixture.detectChanges();
      const finalZoom = timelineData.getZoomRange();
      checkZoomDifference(initialZoom, finalZoom);

      // center to get closer to cursor or stay on cursor
      const curCenter = finalZoom.from
        .add(finalZoom.to.getValueNs())
        .div(2n)
        .getValueNs();
      const prevCenter = initialZoom.from
        .add(initialZoom.to.getValueNs())
        .div(2n)
        .getValueNs();

      if (prevCenter === position800.timestamp.getValueNs()) {
        expect(curCenter).toBe(prevCenter);
      } else {
        expect(Math.abs(Number(curCenter - cursorPos))).toBeLessThan(
          Math.abs(Number(prevCenter - cursorPos)),
        );
      }

      initialZoom = finalZoom;
    }
  });

  it('can zoom out with the buttons', () => {
    fixture.detectChanges();
    const timelineData = assertDefined(component.timelineData);
    const traces = new TracesBuilder()
      .setTimestamps(TraceType.SURFACE_FLINGER, [timestamp10])
      .setTimestamps(TraceType.WINDOW_MANAGER, [timestamp1000])
      .build();

    timelineData.initialize(
      traces,
      undefined,
      TimestampConverterUtils.TIMESTAMP_CONVERTER,
    );

    let initialZoom = new TimeRange(timestamp700, timestamp810);
    assertDefined(component.miniTimelineComponent).onZoomChanged(initialZoom);

    timelineData.setPosition(position800);
    const cursorPos = position800.timestamp.getValueNs();

    fixture.detectChanges();

    const zoomButton = assertDefined(
      htmlElement.querySelector('#zoom-out-btn'),
    ) as HTMLButtonElement;

    for (let i = 0; i < 10; i++) {
      zoomButton.click();
      fixture.detectChanges();
      const finalZoom = timelineData.getZoomRange();
      checkZoomDifference(finalZoom, initialZoom);

      // center to get closer to cursor or stay on cursor unless we reach the edge
      const curCenter = finalZoom.from
        .add(finalZoom.to.getValueNs())
        .div(2n)
        .getValueNs();
      const prevCenter = initialZoom.from
        .add(initialZoom.to.getValueNs())
        .div(2n)
        .getValueNs();

      if (
        finalZoom.from.getValueNs() === timestamp10.getValueNs() ||
        finalZoom.to.getValueNs() === timestamp1000.getValueNs()
      ) {
        // No checks as cursor will stop being more centered
      } else if (prevCenter === cursorPos) {
        expect(curCenter).toBe(prevCenter);
      } else {
        expect(Math.abs(Number(curCenter - cursorPos))).toBeGreaterThan(
          Math.abs(Number(prevCenter - cursorPos)),
        );
      }

      initialZoom = finalZoom;
    }
  });

  it('can not zoom out past full range', () => {
    const timelineData = assertDefined(component.timelineData);
    const traces = new TracesBuilder()
      .setTimestamps(TraceType.SURFACE_FLINGER, [timestamp10])
      .setTimestamps(TraceType.WINDOW_MANAGER, [timestamp1000])
      .build();

    timelineData.initialize(
      traces,
      undefined,
      TimestampConverterUtils.TIMESTAMP_CONVERTER,
    );
    fixture.detectChanges();

    const initialZoom = new TimeRange(timestamp10, timestamp1000);
    assertDefined(component.miniTimelineComponent).onZoomChanged(initialZoom);

    timelineData.setPosition(position800);
    fixture.detectChanges();

    const zoomButton = assertDefined(
      htmlElement.querySelector('#zoom-out-btn'),
    ) as HTMLButtonElement;

    zoomButton.click();
    fixture.detectChanges();
    const finalZoom = timelineData.getZoomRange();

    expect(finalZoom.from.getValueNs()).toBe(initialZoom.from.getValueNs());
    expect(finalZoom.to.getValueNs()).toBe(initialZoom.to.getValueNs());
  });

  it('zooms in with scroll wheel', () => {
    const traces = new TracesBuilder()
      .setTimestamps(TraceType.SURFACE_FLINGER, [timestamp10])
      .setTimestamps(TraceType.WINDOW_MANAGER, [timestamp1000])
      .build();

    assertDefined(component.timelineData).initialize(
      traces,
      undefined,
      TimestampConverterUtils.TIMESTAMP_CONVERTER,
    );
    fixture.detectChanges();

    let initialZoom = new TimeRange(timestamp10, timestamp1000);
    const miniTimelineComponent = assertDefined(
      component.miniTimelineComponent,
    );
    miniTimelineComponent.onZoomChanged(initialZoom);

    fixture.detectChanges();

    for (let i = 0; i < 10; i++) {
      miniTimelineComponent.onScroll({
        deltaY: -200,
        deltaX: 0,
        x: 10, // scrolling on pos
        target: {id: 'mini-timeline-canvas', offsetLeft: 0},
      } as unknown as WheelEvent);

      fixture.detectChanges();
      const finalZoom = timelineData.getZoomRange();
      expect(finalZoom).not.toBe(initialZoom);
      expect(
        finalZoom.to.minus(finalZoom.from.getValueNs()).getValueNs(),
      ).toBeLessThan(
        Number(
          initialZoom.to.minus(initialZoom.from.getValueNs()).getValueNs(),
        ),
      );

      initialZoom = finalZoom;
    }
  });

  it('zooms out with scroll wheel', () => {
    fixture.detectChanges();
    const traces = new TracesBuilder()
      .setTimestamps(TraceType.SURFACE_FLINGER, [timestamp10])
      .setTimestamps(TraceType.WINDOW_MANAGER, [timestamp1000])
      .build();

    assertDefined(component.timelineData).initialize(
      traces,
      undefined,
      TimestampConverterUtils.TIMESTAMP_CONVERTER,
    );

    let initialZoom = new TimeRange(timestamp700, timestamp810);
    const miniTimelineComponent = assertDefined(
      component.miniTimelineComponent,
    );
    miniTimelineComponent.onZoomChanged(initialZoom);

    fixture.detectChanges();

    for (let i = 0; i < 10; i++) {
      miniTimelineComponent.onScroll({
        deltaY: 200,
        deltaX: 0,
        x: 10, // scrolling on pos
        target: {id: 'mini-timeline-canvas', offsetLeft: 0},
      } as unknown as WheelEvent);

      fixture.detectChanges();
      const finalZoom = timelineData.getZoomRange();
      expect(finalZoom).not.toBe(initialZoom);
      expect(
        finalZoom.to.minus(finalZoom.from.getValueNs()).getValueNs(),
      ).toBeGreaterThan(
        Number(
          initialZoom.to.minus(initialZoom.from.getValueNs()).getValueNs(),
        ),
      );

      initialZoom = finalZoom;
    }
  });

  it('cannot zoom out past full range', () => {
    fixture.detectChanges();
    const traces = new TracesBuilder()
      .setTimestamps(TraceType.SURFACE_FLINGER, [timestamp10])
      .setTimestamps(TraceType.WINDOW_MANAGER, [timestamp1000])
      .build();

    assertDefined(component.timelineData).initialize(
      traces,
      undefined,
      TimestampConverterUtils.TIMESTAMP_CONVERTER,
    );

    const initialZoom = new TimeRange(timestamp10, timestamp1000);
    const miniTimelineComponent = assertDefined(
      component.miniTimelineComponent,
    );
    miniTimelineComponent.onZoomChanged(initialZoom);

    miniTimelineComponent.onScroll({
      deltaY: 1000,
      deltaX: 0,
      x: 10, // scrolling on pos
      target: {id: 'mini-timeline-canvas', offsetLeft: 0},
    } as unknown as WheelEvent);

    fixture.detectChanges();

    const finalZoom = timelineData.getZoomRange();

    expect(finalZoom.from.getValueNs()).toBe(initialZoom.from.getValueNs());
    expect(finalZoom.to.getValueNs()).toBe(initialZoom.to.getValueNs());
  });

  it('applies expanded timeline scroll wheel event', () => {
    const traces = new TracesBuilder()
      .setTimestamps(TraceType.SURFACE_FLINGER, [timestamp10])
      .setTimestamps(TraceType.WINDOW_MANAGER, [timestamp1000])
      .build();

    assertDefined(component.timelineData).initialize(
      traces,
      undefined,
      TimestampConverterUtils.TIMESTAMP_CONVERTER,
    );

    const initialZoom = new TimeRange(timestamp10, timestamp1000);
    fixture.detectChanges();
    assertDefined(component.miniTimelineComponent).onZoomChanged(initialZoom);
    fixture.detectChanges();

    component.expandedTimelineScrollEvent = {
      deltaY: 1000,
      deltaX: 0,
      x: 10, // scrolling on pos
      target: component.miniTimelineComponent?.getCanvas(),
    } as unknown as WheelEvent;
    fixture.detectChanges();

    const finalZoom = timelineData.getZoomRange();

    expect(finalZoom.from.getValueNs()).toBe(initialZoom.from.getValueNs());
    expect(finalZoom.to.getValueNs()).toBe(initialZoom.to.getValueNs());
  });

  it('opens context menu', () => {
    fixture.detectChanges();
    expect(document.querySelector('.context-menu')).toBeFalsy();

    assertDefined(component.miniTimelineComponent)
      .getCanvas()
      .dispatchEvent(new MouseEvent('contextmenu'));
    fixture.detectChanges();

    const menu = assertDefined(document.querySelector('.context-menu'));
    const options = menu.querySelectorAll('.context-menu-item');
    expect(options.length).toEqual(2);
  });

  it('adds bookmark', () => {
    fixture.detectChanges();
    const miniTimelineComponent = assertDefined(
      component.miniTimelineComponent,
    );
    const spy = spyOn(miniTimelineComponent.onToggleBookmark, 'emit');

    miniTimelineComponent
      .getCanvas()
      .dispatchEvent(new MouseEvent('contextmenu'));
    fixture.detectChanges();

    const menu = assertDefined(document.querySelector('.context-menu'));
    const options = menu.querySelectorAll('.context-menu-item');
    expect(options.item(0).textContent).toContain('Add bookmark');
    (options.item(0) as HTMLElement).click();

    expect(spy).toHaveBeenCalledWith({
      range: new TimeRange(timestamp10, timestamp10),
      rangeContainsBookmark: false,
    });
  });

  it('removes bookmark', () => {
    component.bookmarks = [timestamp10];
    fixture.detectChanges();
    const miniTimelineComponent = assertDefined(
      component.miniTimelineComponent,
    );
    const spy = spyOn(miniTimelineComponent.onToggleBookmark, 'emit');

    miniTimelineComponent
      .getCanvas()
      .dispatchEvent(new MouseEvent('contextmenu'));
    fixture.detectChanges();

    const menu = assertDefined(document.querySelector('.context-menu'));
    const options = menu.querySelectorAll('.context-menu-item');
    expect(options.item(0).textContent).toContain('Remove bookmark');
    (options.item(0) as HTMLElement).click();

    expect(spy).toHaveBeenCalledWith({
      range: new TimeRange(timestamp10, timestamp10),
      rangeContainsBookmark: true,
    });
  });

  it('removes all bookmarks', () => {
    component.bookmarks = [timestamp10, timestamp1000];
    fixture.detectChanges();
    const miniTimelineComponent = assertDefined(
      component.miniTimelineComponent,
    );
    const spy = spyOn(miniTimelineComponent.onRemoveAllBookmarks, 'emit');

    miniTimelineComponent
      .getCanvas()
      .dispatchEvent(new MouseEvent('contextmenu'));
    fixture.detectChanges();

    const menu = assertDefined(document.querySelector('.context-menu'));
    const options = menu.querySelectorAll('.context-menu-item');
    expect(options.item(1).textContent).toContain('Remove all bookmarks');
    (options.item(1) as HTMLElement).click();

    expect(spy).toHaveBeenCalled();
  });

  it('zooms in/out on KeyW/KeyS press', () => {
    const traces = new TracesBuilder()
      .setTimestamps(TraceType.SURFACE_FLINGER, [
        timestamp1000,
        timestamp2000,
        timestamp4000,
      ])
      .build();

    assertDefined(component.timelineData).initialize(
      traces,
      undefined,
      TimestampConverterUtils.TIMESTAMP_CONVERTER,
    );

    const initialZoom = new TimeRange(timestamp1000, timestamp2000);
    component.initialZoom = initialZoom;
    fixture.detectChanges();

    document.dispatchEvent(new KeyboardEvent('keydown', {code: 'KeyW'}));
    fixture.detectChanges();

    const zoomedIn = timelineData.getZoomRange();
    checkZoomDifference(initialZoom, zoomedIn);

    document.dispatchEvent(new KeyboardEvent('keydown', {code: 'KeyS'}));
    fixture.detectChanges();

    const zoomedOut = timelineData.getZoomRange();
    checkZoomDifference(zoomedOut, zoomedIn);
  });

  it('moves right/left on KeyD/KeyA press', () => {
    const traces = new TracesBuilder()
      .setTimestamps(TraceType.SURFACE_FLINGER, [
        timestamp1000,
        timestamp2000,
        timestamp4000,
      ])
      .build();

    assertDefined(component.timelineData).initialize(
      traces,
      undefined,
      TimestampConverterUtils.TIMESTAMP_CONVERTER,
    );

    const initialZoom = new TimeRange(timestamp1000, timestamp2000);
    component.initialZoom = initialZoom;
    fixture.detectChanges();

    while (timelineData.getZoomRange().to !== timestamp4000) {
      document.dispatchEvent(new KeyboardEvent('keydown', {code: 'KeyD'}));
      fixture.detectChanges();
      const zoomRange = timelineData.getZoomRange();
      const increase =
        zoomRange.from.getValueNs() - initialZoom.from.getValueNs();
      expect(increase).toBeGreaterThan(0);
      expect(zoomRange.to.getValueNs()).toEqual(
        initialZoom.to.getValueNs() + increase,
      );
    }

    // cannot move past end of trace
    const finalZoom = timelineData.getZoomRange();
    document.dispatchEvent(new KeyboardEvent('keydown', {code: 'KeyD'}));
    fixture.detectChanges();
    expect(timelineData.getZoomRange()).toEqual(finalZoom);

    while (timelineData.getZoomRange().from !== timestamp1000) {
      document.dispatchEvent(new KeyboardEvent('keydown', {code: 'KeyA'}));
      fixture.detectChanges();
      const zoomRange = timelineData.getZoomRange();
      const decrease =
        finalZoom.from.getValueNs() - zoomRange.from.getValueNs();
      expect(decrease).toBeGreaterThan(0);
      expect(zoomRange.to.getValueNs()).toEqual(
        finalZoom.to.getValueNs() - decrease,
      );
    }

    // cannot move before start of trace
    document.dispatchEvent(new KeyboardEvent('keydown', {code: 'KeyA'}));
    fixture.detectChanges();
    expect(timelineData.getZoomRange()).toEqual(initialZoom);
  });

  function checkZoomDifference(
    biggerRange: TimeRange,
    smallerRange: TimeRange,
  ) {
    expect(biggerRange).not.toBe(smallerRange);
    expect(smallerRange.from.getValueNs()).toBeGreaterThanOrEqual(
      Number(biggerRange.from.getValueNs()),
    );
    expect(smallerRange.to.getValueNs()).toBeLessThanOrEqual(
      Number(biggerRange.to.getValueNs()),
    );
    expect(
      smallerRange.to.minus(smallerRange.from.getValueNs()).getValueNs(),
    ).toBeLessThan(
      Number(biggerRange.to.minus(biggerRange.from.getValueNs()).getValueNs()),
    );
  }

  @Component({
    selector: 'host-component',
    template: `
      <mini-timeline
        [timelineData]="timelineData"
        [currentTracePosition]="currentTracePosition"
        [selectedTraces]="selectedTraces"
        [initialZoom]="initialZoom"
        [expandedTimelineScrollEvent]="expandedTimelineScrollEvent"
        [bookmarks]="bookmarks"></mini-timeline>
    `,
  })
  class TestHostComponent {
    timelineData = new TimelineData();
    currentTracePosition: TracePosition | undefined;
    selectedTraces: TraceType[] = [];
    initialZoom: TimeRange | undefined;
    expandedTimelineScrollEvent: WheelEvent | undefined;
    bookmarks: Timestamp[] = [];

    @ViewChild(MiniTimelineComponent)
    miniTimelineComponent: MiniTimelineComponent | undefined;
  }
});
