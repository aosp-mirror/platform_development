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
import {ComponentFixture, fakeAsync, TestBed} from '@angular/core/testing';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {MatButtonModule} from '@angular/material/button';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatIconModule} from '@angular/material/icon';
import {MatInputModule} from '@angular/material/input';
import {MatSelectModule} from '@angular/material/select';
import {MatTooltipModule} from '@angular/material/tooltip';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {assertDefined} from 'common/assert_utils';
import {RealTimestamp, TimeRange} from 'common/time';
import {dragElement} from 'test/utils';
import {TracePosition} from 'trace/trace_position';
import {MIN_SLIDER_WIDTH, SliderComponent} from './slider_component';

describe('SliderComponent', () => {
  let fixture: ComponentFixture<SliderComponent>;
  let component: SliderComponent;
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
      declarations: [SliderComponent],
    })
      .overrideComponent(SliderComponent, {
        set: {changeDetection: ChangeDetectionStrategy.Default},
      })
      .compileComponents();
    fixture = TestBed.createComponent(SliderComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;

    component.fullRange = {
      from: new RealTimestamp(100n),
      to: new RealTimestamp(200n),
    };
    component.zoomRange = {
      from: new RealTimestamp(125n),
      to: new RealTimestamp(175n),
    };
    component.currentPosition = TracePosition.fromTimestamp(new RealTimestamp(150n));

    fixture.detectChanges();
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('reposition properly on zoom', () => {
    fixture.detectChanges();
    component.ngOnChanges({
      zoomRange: {
        firstChange: true,
        isFirstChange: () => true,
        previousValue: undefined,
        currentValue: component.zoomRange,
      },
    });
    fixture.detectChanges();

    const sliderWitdth = component.sliderBox.nativeElement.offsetWidth;
    expect(component.sliderWidth).toBe(sliderWitdth / 2);
    expect(component.dragPosition.x).toBe(sliderWitdth / 4);
  });

  it('has min width', () => {
    component.fullRange = {
      from: new RealTimestamp(100n),
      to: new RealTimestamp(200n),
    };
    component.zoomRange = {
      from: new RealTimestamp(125n),
      to: new RealTimestamp(126n),
    };

    fixture.detectChanges();
    component.ngOnChanges({
      zoomRange: {
        firstChange: true,
        isFirstChange: () => true,
        previousValue: undefined,
        currentValue: component.zoomRange,
      },
    });
    fixture.detectChanges();

    const sliderWidth = component.sliderBox.nativeElement.offsetWidth;
    expect(component.sliderWidth).toBe(MIN_SLIDER_WIDTH);
    expect(component.dragPosition.x).toBe(sliderWidth / 4 - MIN_SLIDER_WIDTH / 2);
  });

  it('repositions slider on resize', () => {
    const slider = assertDefined(htmlElement.querySelector('.slider'));
    const cursor = assertDefined(htmlElement.querySelector('.cursor'));

    fixture.detectChanges();

    const initialSliderXPos = slider.getBoundingClientRect().left;
    const initialCursorXPos = cursor.getBoundingClientRect().left;

    spyOnProperty(component.sliderBox.nativeElement, 'offsetWidth', 'get').and.returnValue(100);
    expect(component.sliderBox.nativeElement.offsetWidth).toBe(100);

    htmlElement.style.width = '587px';
    component.onResize({} as Event);
    fixture.detectChanges();

    expect(initialSliderXPos).not.toBe(slider.getBoundingClientRect().left);
    expect(initialCursorXPos).not.toBe(cursor.getBoundingClientRect().left);
  });

  it('draws current position cursor', () => {
    fixture.detectChanges();
    component.ngOnChanges({
      currentPosition: {
        firstChange: true,
        isFirstChange: () => true,
        previousValue: undefined,
        currentValue: component.currentPosition,
      },
    });
    fixture.detectChanges();

    const sliderBox = assertDefined(htmlElement.querySelector('#timeline-slider-box'));
    const cursor = assertDefined(htmlElement.querySelector('.cursor'));
    const sliderBoxRect = sliderBox.getBoundingClientRect();
    expect(cursor.getBoundingClientRect().left).toBe(
      (sliderBoxRect.left + sliderBoxRect.right) / 2
    );
  });

  it('moving slider around updates zoom', fakeAsync(async () => {
    fixture.detectChanges();

    const initialZoom = assertDefined(component.zoomRange);

    let lastZoomUpdate: TimeRange | undefined = undefined;
    const zoomChangedSpy = spyOn(component.onZoomChanged, 'emit').and.callFake((zoom) => {
      lastZoomUpdate = zoom;
    });

    const slider = htmlElement.querySelector('.slider .handle');
    expect(slider).toBeTruthy();
    expect(window.getComputedStyle(assertDefined(slider)).visibility).toBe('visible');

    dragElement(fixture, assertDefined(slider), 100, 8);

    expect(zoomChangedSpy).toHaveBeenCalled();

    const finalZoom = assertDefined<TimeRange>(lastZoomUpdate);
    expect(finalZoom.from).not.toBe(initialZoom.from);
    expect(finalZoom.to).not.toBe(initialZoom.to);
    expect(finalZoom.to.minus(finalZoom.from).getValueNs()).toBe(
      initialZoom.to.minus(initialZoom.from).getValueNs()
    );
  }));

  it('moving slider left pointer around updates zoom', fakeAsync(async () => {
    fixture.detectChanges();

    const initialZoom = assertDefined(component.zoomRange);

    let lastZoomUpdate: TimeRange | undefined = undefined;
    const zoomChangedSpy = spyOn(component.onZoomChanged, 'emit').and.callFake((zoom) => {
      lastZoomUpdate = zoom;
    });

    const leftCropper = htmlElement.querySelector('.slider .cropper.left');
    expect(leftCropper).toBeTruthy();
    expect(window.getComputedStyle(assertDefined(leftCropper)).visibility).toBe('visible');

    dragElement(fixture, assertDefined(leftCropper), 5, 0);

    expect(zoomChangedSpy).toHaveBeenCalled();

    const finalZoom = assertDefined<TimeRange>(lastZoomUpdate);
    expect(finalZoom.from).not.toBe(initialZoom.from);
    expect(finalZoom.to).toBe(initialZoom.to);
  }));

  it('moving slider right pointer around updates zoom', fakeAsync(async () => {
    fixture.detectChanges();

    const initialZoom = assertDefined(component.zoomRange);

    let lastZoomUpdate: TimeRange | undefined = undefined;
    const zoomChangedSpy = spyOn(component.onZoomChanged, 'emit').and.callFake((zoom) => {
      lastZoomUpdate = zoom;
    });

    const rightCropper = htmlElement.querySelector('.slider .cropper.right');
    expect(rightCropper).toBeTruthy();
    expect(window.getComputedStyle(assertDefined(rightCropper)).visibility).toBe('visible');

    dragElement(fixture, assertDefined(rightCropper), 5, 0);

    expect(zoomChangedSpy).toHaveBeenCalled();

    const finalZoom = assertDefined<TimeRange>(lastZoomUpdate);
    expect(finalZoom.from).toBe(initialZoom.from);
    expect(finalZoom.to).not.toBe(initialZoom.to);
  }));

  it('cannot slide left cropper past edges', fakeAsync(() => {
    component.zoomRange = component.fullRange;
    fixture.detectChanges();

    const initialZoom = assertDefined(component.zoomRange);

    let lastZoomUpdate: TimeRange | undefined = undefined;
    const zoomChangedSpy = spyOn(component.onZoomChanged, 'emit').and.callFake((zoom) => {
      lastZoomUpdate = zoom;
    });

    const leftCropper = htmlElement.querySelector('.slider .cropper.left');
    expect(leftCropper).toBeTruthy();
    expect(window.getComputedStyle(assertDefined(leftCropper)).visibility).toBe('visible');

    dragElement(fixture, assertDefined(leftCropper), -5, 0);

    expect(zoomChangedSpy).toHaveBeenCalled();

    const finalZoom = assertDefined<TimeRange>(lastZoomUpdate);
    expect(finalZoom.from.getValueNs()).toBe(initialZoom.from.getValueNs());
    expect(finalZoom.to.getValueNs()).toBe(initialZoom.to.getValueNs());
  }));

  it('cannot slide right cropper past edges', fakeAsync(() => {
    component.zoomRange = component.fullRange;
    fixture.detectChanges();

    const initialZoom = assertDefined(component.zoomRange);

    let lastZoomUpdate: TimeRange | undefined = undefined;
    const zoomChangedSpy = spyOn(component.onZoomChanged, 'emit').and.callFake((zoom) => {
      lastZoomUpdate = zoom;
    });

    const rightCropper = htmlElement.querySelector('.slider .cropper.right');
    expect(rightCropper).toBeTruthy();
    expect(window.getComputedStyle(assertDefined(rightCropper)).visibility).toBe('visible');

    dragElement(fixture, assertDefined(rightCropper), 5, 0);

    expect(zoomChangedSpy).toHaveBeenCalled();

    const finalZoom = assertDefined<TimeRange>(lastZoomUpdate);
    expect(finalZoom.from.getValueNs()).toBe(initialZoom.from.getValueNs());
    expect(finalZoom.to.getValueNs()).toBe(initialZoom.to.getValueNs());
  }));

  it('cannot slide left cropper past right cropper', fakeAsync(() => {
    component.zoomRange = {
      from: new RealTimestamp(125n),
      to: new RealTimestamp(125n),
    };
    fixture.detectChanges();

    const initialZoom = assertDefined(component.zoomRange);

    let lastZoomUpdate: TimeRange | undefined = undefined;
    const zoomChangedSpy = spyOn(component.onZoomChanged, 'emit').and.callFake((zoom) => {
      lastZoomUpdate = zoom;
    });

    const leftCropper = htmlElement.querySelector('.slider .cropper.left');
    expect(leftCropper).toBeTruthy();
    expect(window.getComputedStyle(assertDefined(leftCropper)).visibility).toBe('visible');

    dragElement(fixture, assertDefined(leftCropper), 100, 0);

    expect(zoomChangedSpy).toHaveBeenCalled();

    const finalZoom = assertDefined<TimeRange>(lastZoomUpdate);
    expect(finalZoom.from.getValueNs()).toBe(initialZoom.from.getValueNs());
    expect(finalZoom.to.getValueNs()).toBe(initialZoom.to.getValueNs());
  }));

  it('cannot slide right cropper past left cropper', fakeAsync(() => {
    component.zoomRange = {
      from: new RealTimestamp(125n),
      to: new RealTimestamp(125n),
    };
    fixture.detectChanges();

    const initialZoom = assertDefined(component.zoomRange);

    let lastZoomUpdate: TimeRange | undefined = undefined;
    const zoomChangedSpy = spyOn(component.onZoomChanged, 'emit').and.callFake((zoom) => {
      lastZoomUpdate = zoom;
    });

    const rightCropper = htmlElement.querySelector('.slider .cropper.right');
    expect(rightCropper).toBeTruthy();
    expect(window.getComputedStyle(assertDefined(rightCropper)).visibility).toBe('visible');

    dragElement(fixture, assertDefined(rightCropper), -100, 0);

    expect(zoomChangedSpy).toHaveBeenCalled();

    const finalZoom = assertDefined<TimeRange>(lastZoomUpdate);
    expect(finalZoom.from.getValueNs()).toBe(initialZoom.from.getValueNs());
    expect(finalZoom.to.getValueNs()).toBe(initialZoom.to.getValueNs());
  }));

  it('cannot move slider past edges', fakeAsync(() => {
    component.zoomRange = component.fullRange;
    fixture.detectChanges();

    const initialZoom = assertDefined(component.zoomRange);

    let lastZoomUpdate: TimeRange | undefined = undefined;
    const zoomChangedSpy = spyOn(component.onZoomChanged, 'emit').and.callFake((zoom) => {
      lastZoomUpdate = zoom;
    });

    const slider = htmlElement.querySelector('.slider .handle');
    expect(slider).toBeTruthy();
    expect(window.getComputedStyle(assertDefined(slider)).visibility).toBe('visible');

    dragElement(fixture, assertDefined(slider), 100, 8);

    expect(zoomChangedSpy).toHaveBeenCalled();

    const finalZoom = assertDefined<TimeRange>(lastZoomUpdate);
    expect(finalZoom.from.getValueNs()).toBe(initialZoom.from.getValueNs());
    expect(finalZoom.to.getValueNs()).toBe(initialZoom.to.getValueNs());
  }));
});
