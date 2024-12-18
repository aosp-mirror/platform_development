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
import {TimestampConverterUtils} from 'common/time/test_utils';
import {TimeRange, Timestamp} from 'common/time/time';
import {TracesBuilder} from 'test/unit/traces_builder';
import {dragElement} from 'test/utils';
import {Trace} from 'trace/trace';
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
  const timestamp1750 = TimestampConverterUtils.makeRealTimestamp(1750n);
  const timestamp2000 = TimestampConverterUtils.makeRealTimestamp(2000n);
  const timestamp3000 = TimestampConverterUtils.makeRealTimestamp(3000n);
  const timestamp4000 = TimestampConverterUtils.makeRealTimestamp(4000n);

  const position800 = TracePosition.fromTimestamp(
    TimestampConverterUtils.makeRealTimestamp(800n),
  );

  const traces = new TracesBuilder()
    .setTimestamps(TraceType.SURFACE_FLINGER, [timestamp10])
    .setTimestamps(TraceType.TRANSACTIONS, [timestamp10, timestamp20])
    .setTimestamps(TraceType.WINDOW_MANAGER, [timestamp20])
    .build();
  const traceSf = assertDefined(traces.getTrace(TraceType.SURFACE_FLINGER));
  const traceWm = assertDefined(traces.getTrace(TraceType.WINDOW_MANAGER));
  const traceTransactions = assertDefined(
    traces.getTrace(TraceType.TRANSACTIONS),
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
    await timelineData.initialize(
      traces,
      undefined,
      TimestampConverterUtils.TIMESTAMP_CONVERTER,
    );
    component.timelineData = timelineData;
    expect(timelineData.getCurrentPosition()).toBeDefined();
    component.currentTracePosition = timelineData.getCurrentPosition()!;
    component.selectedTraces = [traceSf];
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

    window.dispatchEvent(new Event('resize'));
    fixture.detectChanges();

    expect(spy).toHaveBeenCalled();
  });

  it('resets zoom to full time range on reset button click if initial zoom unavailable', () => {
    const expectedZoomRange = new TimeRange(timestamp15, timestamp16);
    timelineData.setZoom(expectedZoomRange);
    const fullRange = timelineData.getFullTimeRange();
    const zoomRange = timelineData.getZoomRange();
    expect(zoomRange).toEqual(expectedZoomRange);
    expect(zoomRange).not.toEqual(fullRange);
    fixture.detectChanges();

    const zoomButton = assertDefined(
      htmlElement.querySelector('button#reset-zoom-btn'),
    ) as HTMLButtonElement;
    zoomButton.click();
    fixture.detectChanges();

    expect(timelineData.getZoomRange()).toEqual(fullRange);
  });

  it('resets zoom to initial zoom on reset button click if available', () => {
    const initialZoom = new TimeRange(timestamp15, timestamp16);
    component.initialZoom = initialZoom;
    fixture.detectChanges();
    expect(timelineData.getZoomRange()).toEqual(initialZoom);

    const newZoom = new TimeRange(timestamp10, timestamp16);
    timelineData.setZoom(newZoom);
    expect(timelineData.getZoomRange()).toEqual(newZoom);
    fixture.detectChanges();

    const zoomButton = assertDefined(
      htmlElement.querySelector('button#reset-zoom-btn'),
    ) as HTMLButtonElement;
    zoomButton.click();
    fixture.detectChanges();

    expect(timelineData.getZoomRange()).toEqual(initialZoom);
    expect(timelineData.getFullTimeRange()).not.toEqual(initialZoom);
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
    const selectedTracesTypes = selectedTraces.map((trace) => trace.type);

    const tracesToShow = assertDefined(
      component.miniTimelineComponent,
    ).getTracesToShow();
    const tracesToShowTypes = tracesToShow.map((trace) => trace.type);

    expect(new Set(tracesToShowTypes)).toEqual(new Set(selectedTracesTypes));
  });

  it('getTracesToShow adds traces in correct order', () => {
    component.selectedTraces = [traceWm, traceSf, traceTransactions];
    fixture.detectChanges();
    const tracesToShowTypes = assertDefined(component.miniTimelineComponent)
      .getTracesToShow()
      .map((trace) => trace.type);
    expect(tracesToShowTypes).toEqual([
      TraceType.TRANSACTIONS,
      TraceType.WINDOW_MANAGER,
      TraceType.SURFACE_FLINGER,
    ]);
  });

  it('updates zoom when slider moved', fakeAsync(() => {
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

  it('zooms in/out with buttons', () => {
    initializeTraces();

    const initialZoom = new TimeRange(timestamp700, timestamp810);
    const miniTimelineComponent = assertDefined(
      component.miniTimelineComponent,
    );
    miniTimelineComponent.onZoomChanged(initialZoom);
    miniTimelineComponent.currentTracePosition = position800;

    fixture.detectChanges();

    const zoomInButton = assertDefined(
      htmlElement.querySelector('#zoom-in-btn'),
    ) as HTMLButtonElement;
    const zoomOutButton = assertDefined(
      htmlElement.querySelector('#zoom-out-btn'),
    ) as HTMLButtonElement;

    zoomInButton.click();
    fixture.detectChanges();
    const zoomedIn = timelineData.getZoomRange();
    checkZoomDifference(initialZoom, zoomedIn);

    zoomOutButton.click();
    fixture.detectChanges();
    const zoomedOut = timelineData.getZoomRange();
    checkZoomDifference(zoomedOut, zoomedIn);
  });

  it('cannot zoom out past full range', () => {
    initializeTraces();

    const initialZoom = new TimeRange(timestamp10, timestamp1000);
    assertDefined(component.miniTimelineComponent).onZoomChanged(initialZoom);

    timelineData.setPosition(position800);
    fixture.detectChanges();

    const zoomButton = assertDefined(
      htmlElement.querySelector('#zoom-out-btn'),
    ) as HTMLButtonElement;

    zoomButton.click();
    fixture.detectChanges();

    let finalZoom = timelineData.getZoomRange();
    expect(finalZoom.from.getValueNs()).toBe(initialZoom.from.getValueNs());
    expect(finalZoom.to.getValueNs()).toBe(initialZoom.to.getValueNs());

    zoomOutByScrollWheel();

    finalZoom = timelineData.getZoomRange();
    expect(finalZoom.from.getValueNs()).toBe(initialZoom.from.getValueNs());
    expect(finalZoom.to.getValueNs()).toBe(initialZoom.to.getValueNs());
  });

  it('zooms in/out with scroll wheel', () => {
    initializeTraces();

    let initialZoom = new TimeRange(timestamp10, timestamp1000);
    const miniTimelineComponent = assertDefined(
      component.miniTimelineComponent,
    );
    miniTimelineComponent.onZoomChanged(initialZoom);

    fixture.detectChanges();

    for (let i = 0; i < 10; i++) {
      zoomInByScrollWheel();

      const finalZoom = timelineData.getZoomRange();
      checkZoomDifference(initialZoom, finalZoom);
      initialZoom = finalZoom;
    }

    for (let i = 0; i < 9; i++) {
      zoomOutByScrollWheel();

      const finalZoom = timelineData.getZoomRange();
      checkZoomDifference(finalZoom, initialZoom);
      initialZoom = finalZoom;
    }
  });

  it('applies expanded timeline scroll wheel event', () => {
    initializeTraces();

    const initialZoom = new TimeRange(timestamp10, timestamp1000);
    fixture.detectChanges();
    assertDefined(component.miniTimelineComponent).onZoomChanged(initialZoom);
    fixture.detectChanges();

    component.expandedTimelineScrollEvent = {
      deltaY: -200,
      deltaX: 0,
      x: 10, // scrolling on pos
      target: component.miniTimelineComponent?.getCanvas(),
    } as unknown as WheelEvent;
    fixture.detectChanges();

    const finalZoom = timelineData.getZoomRange();
    checkZoomDifference(initialZoom, finalZoom);
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
    initializeTracesForWASDZoom();

    const initialZoom = new TimeRange(timestamp1000, timestamp2000);
    component.initialZoom = initialZoom;
    fixture.detectChanges();

    zoomInByKeyW();
    const zoomedIn = timelineData.getZoomRange();
    checkZoomDifference(initialZoom, zoomedIn);

    zoomOutByKeyS();
    const zoomedOut = timelineData.getZoomRange();
    checkZoomDifference(zoomedOut, zoomedIn);
  });

  it('moves right/left on KeyD/KeyA press', () => {
    initializeTracesForWASDZoom();

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

  it('zooms in/out on mouse position if within current range', () => {
    initializeTracesForWASDZoom();
    const initialZoom = new TimeRange(timestamp1000, timestamp4000);
    component.initialZoom = initialZoom;
    component.currentTracePosition = TracePosition.fromTimestamp(timestamp2000);
    fixture.detectChanges();

    const miniTimelineComponent = assertDefined(
      component.miniTimelineComponent,
    );
    const canvas = miniTimelineComponent.getCanvas();
    const drawer = assertDefined(miniTimelineComponent.drawer);
    const usableRange = drawer.getUsableRange();

    const mouseMoveEvent = new MouseEvent('mousemove');
    Object.defineProperty(mouseMoveEvent, 'target', {value: canvas});
    Object.defineProperty(mouseMoveEvent, 'offsetX', {
      value:
        (usableRange.to - usableRange.from) * 0.25 + drawer.getPadding().left,
    });
    canvas.dispatchEvent(mouseMoveEvent);
    fixture.detectChanges();

    const fullRangeQuarterTimestamp = timestamp1750;
    checkZoomOnTimestamp(
      fullRangeQuarterTimestamp,
      1n,
      4n,
      zoomInByKeyW,
      zoomOutByKeyS,
    );
    checkZoomOnTimestamp(
      fullRangeQuarterTimestamp,
      1n,
      4n,
      zoomInByScrollWheel,
      zoomOutByScrollWheel,
    );
  });

  it('zooms in/out on current position if within current range and mouse position not available', () => {
    initializeTracesForWASDZoom();
    const initialZoom = new TimeRange(timestamp1000, timestamp4000);
    component.initialZoom = initialZoom;
    component.currentTracePosition = TracePosition.fromTimestamp(timestamp1750);
    fixture.detectChanges();

    const fullRangeQuarterTimestamp = timestamp1750;
    checkZoomOnTimestamp(
      fullRangeQuarterTimestamp,
      1n,
      4n,
      zoomInByKeyW,
      zoomOutByKeyS,
    );
    checkZoomOnTimestamp(
      fullRangeQuarterTimestamp,
      1n,
      4n,
      zoomInByScrollWheel,
      zoomOutByScrollWheel,
    );

    const zoomInButton = assertDefined(
      htmlElement.querySelector('#zoom-in-btn'),
    ) as HTMLButtonElement;
    const zoomOutButton = assertDefined(
      htmlElement.querySelector('#zoom-out-btn'),
    ) as HTMLButtonElement;

    checkZoomOnTimestamp(
      fullRangeQuarterTimestamp,
      1n,
      4n,
      () => {
        zoomInButton.click();
        fixture.detectChanges();
      },
      () => {
        zoomOutButton.click();
        fixture.detectChanges();
      },
    );
  });

  it('zooms in/out on current position after mouse leaves canvas', () => {
    initializeTracesForWASDZoom();
    const initialZoom = new TimeRange(timestamp1000, timestamp4000);
    component.initialZoom = initialZoom;
    component.currentTracePosition = TracePosition.fromTimestamp(timestamp1750);
    fixture.detectChanges();

    const miniTimelineComponent = assertDefined(
      component.miniTimelineComponent,
    );
    const canvas = miniTimelineComponent.getCanvas();
    const drawer = assertDefined(miniTimelineComponent.drawer);
    const usableRange = drawer.getUsableRange();

    const mouseMoveEvent = new MouseEvent('mousemove');
    Object.defineProperty(mouseMoveEvent, 'target', {value: canvas});
    Object.defineProperty(mouseMoveEvent, 'offsetX', {
      value: (usableRange.to - usableRange.from) * 0.5,
    });
    canvas.dispatchEvent(mouseMoveEvent);
    fixture.detectChanges();
    canvas.dispatchEvent(new MouseEvent('mouseleave'));
    fixture.detectChanges();

    const fullRangeQuarterTimestamp = timestamp1750;
    checkZoomOnTimestamp(
      fullRangeQuarterTimestamp,
      1n,
      4n,
      zoomInByKeyW,
      zoomOutByKeyS,
    );
    checkZoomOnTimestamp(
      fullRangeQuarterTimestamp,
      1n,
      4n,
      zoomInByScrollWheel,
      zoomOutByScrollWheel,
    );

    const zoomInButton = assertDefined(
      htmlElement.querySelector('#zoom-in-btn'),
    ) as HTMLButtonElement;
    const zoomOutButton = assertDefined(
      htmlElement.querySelector('#zoom-out-btn'),
    ) as HTMLButtonElement;

    checkZoomOnTimestamp(
      fullRangeQuarterTimestamp,
      1n,
      4n,
      () => {
        zoomInButton.click();
        fixture.detectChanges();
      },
      () => {
        zoomOutButton.click();
        fixture.detectChanges();
      },
    );
  });

  it('zooms in/out on middle of slider bar if current position out of range and mouse position not available', () => {
    initializeTracesForWASDZoom();
    const initialZoom = new TimeRange(timestamp2000, timestamp4000);
    component.initialZoom = initialZoom;
    component.currentTracePosition = TracePosition.fromTimestamp(timestamp1750);
    fixture.detectChanges();

    const fullRangeMiddleTimestamp = timestamp3000;
    checkZoomOnTimestamp(
      fullRangeMiddleTimestamp,
      1n,
      2n,
      zoomInByKeyW,
      zoomOutByKeyS,
    );
    checkZoomOnTimestamp(
      fullRangeMiddleTimestamp,
      1n,
      2n,
      zoomInByScrollWheel,
      zoomOutByScrollWheel,
    );

    const zoomInButton = assertDefined(
      htmlElement.querySelector('#zoom-in-btn'),
    ) as HTMLButtonElement;
    const zoomOutButton = assertDefined(
      htmlElement.querySelector('#zoom-out-btn'),
    ) as HTMLButtonElement;

    checkZoomOnTimestamp(
      fullRangeMiddleTimestamp,
      1n,
      2n,
      () => {
        zoomInButton.click();
        fixture.detectChanges();
      },
      () => {
        zoomOutButton.click();
        fixture.detectChanges();
      },
    );
  });

  it('zooms in/out on mouse position from expanded timeline', () => {
    initializeTracesForWASDZoom();
    const initialZoom = new TimeRange(timestamp1000, timestamp4000);
    component.initialZoom = initialZoom;
    component.currentTracePosition = TracePosition.fromTimestamp(timestamp2000);
    fixture.detectChanges();

    component.expandedTimelineMouseXRatio = 0.25;
    fixture.detectChanges();

    const fullRangeQuarterTimestamp = timestamp1750;
    checkZoomOnTimestamp(
      fullRangeQuarterTimestamp,
      1n,
      4n,
      zoomInByKeyW,
      zoomOutByKeyS,
    );
    checkZoomOnTimestamp(
      fullRangeQuarterTimestamp,
      1n,
      4n,
      zoomInByScrollWheel,
      zoomOutByScrollWheel,
    );
  });

  function initializeTraces() {
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
  }

  function initializeTracesForWASDZoom() {
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
  }

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
  }

  function zoomInByKeyW() {
    document.dispatchEvent(new KeyboardEvent('keydown', {code: 'KeyW'}));
    fixture.detectChanges();
  }

  function zoomOutByKeyS() {
    document.dispatchEvent(new KeyboardEvent('keydown', {code: 'KeyS'}));
    fixture.detectChanges();
  }

  function zoomInByScrollWheel() {
    assertDefined(component.miniTimelineComponent).onScroll({
      deltaY: -200,
      deltaX: 0,
      x: 10, // scrolling on pos
      target: {id: 'mini-timeline-canvas', offsetLeft: 0},
    } as unknown as WheelEvent);
    fixture.detectChanges();
  }

  function zoomOutByScrollWheel() {
    assertDefined(component.miniTimelineComponent).onScroll({
      deltaY: 200,
      deltaX: 0,
      x: 10, // scrolling on pos
      target: {id: 'mini-timeline-canvas', offsetLeft: 0},
    } as unknown as WheelEvent);
    fixture.detectChanges();
  }

  function checkZoomOnTimestamp(
    zoomOnTimestamp: Timestamp,
    ratioNom: bigint,
    ratioDenom: bigint,
    zoomInAction: () => void,
    zoomOutAction: () => void,
  ) {
    let currentZoom = timelineData.getZoomRange();
    for (let i = 0; i < 5; i++) {
      zoomInAction();

      const zoomedIn = timelineData.getZoomRange();
      checkZoomDifference(currentZoom, zoomedIn);
      currentZoom = zoomedIn;

      const zoomedInTimestamp = zoomedIn.from.add(
        (zoomedIn.to.minus(zoomedIn.from.getValueNs()).getValueNs() *
          ratioNom) /
          ratioDenom,
      );
      expect(
        Math.abs(Number(zoomedInTimestamp.minus(zoomOnTimestamp.getValueNs()))),
      ).toBeLessThanOrEqual(5);
    }
    for (let i = 0; i < 4; i++) {
      zoomOutAction();

      const zoomedOut = timelineData.getZoomRange();
      checkZoomDifference(zoomedOut, currentZoom);
      currentZoom = zoomedOut;

      const zoomedOutTimestamp = zoomedOut.from.add(
        (zoomedOut.to.minus(zoomedOut.from.getValueNs()).getValueNs() *
          ratioNom) /
          ratioDenom,
      );
      expect(
        Math.abs(
          Number(zoomedOutTimestamp.minus(zoomOnTimestamp.getValueNs())),
        ),
      ).toBeLessThanOrEqual(5);
    }
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
        [expandedTimelineMouseXRatio]="expandedTimelineMouseXRatio"
        [bookmarks]="bookmarks"></mini-timeline>
    `,
  })
  class TestHostComponent {
    timelineData = new TimelineData();
    currentTracePosition: TracePosition | undefined;
    selectedTraces: Array<Trace<object>> = [];
    initialZoom: TimeRange | undefined;
    expandedTimelineScrollEvent: WheelEvent | undefined;
    expandedTimelineMouseXRatio: number | undefined;
    bookmarks: Timestamp[] = [];

    @ViewChild(MiniTimelineComponent)
    miniTimelineComponent: MiniTimelineComponent | undefined;
  }
});
