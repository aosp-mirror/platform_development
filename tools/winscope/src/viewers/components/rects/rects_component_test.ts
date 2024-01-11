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
import {CommonModule} from '@angular/common';
import {Component, CUSTOM_ELEMENTS_SCHEMA, ViewChild} from '@angular/core';
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {MatCheckboxModule} from '@angular/material/checkbox';
import {MatDividerModule} from '@angular/material/divider';
import {MatRadioModule} from '@angular/material/radio';
import {MatSliderModule} from '@angular/material/slider';
import {assertDefined} from 'common/assert_utils';
import {PersistentStore} from 'common/persistent_store';
import {RectsComponent} from 'viewers/components/rects/rects_component';
import {UiRect} from 'viewers/components/rects/types2d';
import {Canvas} from './canvas';

describe('RectsComponent', () => {
  let component: TestHostComponent;
  let fixture: ComponentFixture<TestHostComponent>;
  let htmlElement: HTMLElement;

  beforeEach(async () => {
    localStorage.clear();

    await TestBed.configureTestingModule({
      imports: [CommonModule, MatCheckboxModule, MatDividerModule, MatSliderModule, MatRadioModule],
      declarations: [TestHostComponent, RectsComponent],
      schemas: [CUSTOM_ELEMENTS_SCHEMA],
    }).compileComponents();

    fixture = TestBed.createComponent(TestHostComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;
    fixture.detectChanges();
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('renders rotation slider', () => {
    const slider = htmlElement.querySelector('mat-slider.slider-rotation');
    expect(slider).toBeTruthy();
  });

  it('renders separation slider', () => {
    const slider = htmlElement.querySelector('mat-slider.slider-spacing');
    expect(slider).toBeTruthy();
  });

  it('renders canvas', () => {
    const rectsCanvas = htmlElement.querySelector('.large-rects-canvas');
    expect(rectsCanvas).toBeTruthy();
  });

  it('draws scene when input data changes', async () => {
    spyOn(Canvas.prototype, 'draw').and.callThrough();

    const inputRect: UiRect = {
      x: 0,
      y: 0,
      w: 1,
      h: 1,
      label: 'rectangle1',
      transform: {
        dsdx: 1,
        dsdy: 0,
        dtdx: 0,
        dtdy: 1,
        tx: 0,
        ty: 0,
      },
      isVisible: true,
      isDisplay: false,
      id: 'test-id-1234',
      displayId: 0,
      isVirtual: false,
      isClickable: false,
      cornerRadius: 0,
    };

    expect(Canvas.prototype.draw).toHaveBeenCalledTimes(0);
    component.rectsComponent.rects = [inputRect];
    expect(Canvas.prototype.draw).toHaveBeenCalledTimes(1);
    component.rectsComponent.rects = [inputRect];
    expect(Canvas.prototype.draw).toHaveBeenCalledTimes(2);
  });

  it('draws scene when rotation slider changes', () => {
    spyOn(Canvas.prototype, 'draw').and.callThrough();
    const slider = htmlElement.querySelector('.slider-rotation');

    expect(Canvas.prototype.draw).toHaveBeenCalledTimes(0);

    slider?.dispatchEvent(new MouseEvent('mousedown'));
    expect(Canvas.prototype.draw).toHaveBeenCalledTimes(1);
  });

  it('draws scene when spacing slider changes', () => {
    spyOn(Canvas.prototype, 'draw').and.callThrough();
    const slider = htmlElement.querySelector('.slider-spacing');

    expect(Canvas.prototype.draw).toHaveBeenCalledTimes(0);

    slider?.dispatchEvent(new MouseEvent('mousedown'));
    expect(Canvas.prototype.draw).toHaveBeenCalledTimes(1);
  });

  it('draws display buttons', () => {
    component.displayIds = [0, 1, 2];

    fixture.detectChanges();

    const displayButtonContainer = assertDefined(
      htmlElement.querySelector('.display-button-container')
    );

    const buttons = Array.from(assertDefined(displayButtonContainer.querySelectorAll('button')));
    expect(buttons.length).toBe(3);

    const buttonValues = buttons.map((it) => it.textContent?.trim());
    expect(buttonValues).toEqual(['0', '1', '2']);
  });

  it('uses stored rects view settings', () => {
    expect(component.rectsComponent.getZSpacingFactor()).toEqual(1);
    component.rectsComponent.onSeparationSliderChange(0.06);
    fixture.detectChanges();
    expect(component.rectsComponent.getZSpacingFactor()).toEqual(0.06);

    expect(component.rectsComponent.getShowVirtualMode()).toBeFalse();
    findAndClickCheckbox('.top-view-controls .show-virtual input');
    expect(component.rectsComponent.getShowVirtualMode()).toBeTrue();

    expect(component.rectsComponent.getShowOnlyVisibleMode()).toBeFalse();
    findAndClickCheckbox('.top-view-controls .show-only-visible  input');
    expect(component.rectsComponent.getShowOnlyVisibleMode()).toBeTrue();

    const newFixture = TestBed.createComponent(TestHostComponent);
    const newComponent = newFixture.componentInstance;
    newFixture.detectChanges();

    expect(newComponent.rectsComponent.getZSpacingFactor()).toEqual(0.06);
    expect(newComponent.rectsComponent.getShowVirtualMode()).toBeTrue();
    expect(newComponent.rectsComponent.getShowOnlyVisibleMode()).toBeTrue();
  });

  function findAndClickCheckbox(selector: string) {
    const box = assertDefined(htmlElement.querySelector(selector)) as HTMLInputElement;
    box.dispatchEvent(new Event('click'));
    fixture.detectChanges();
  }

  @Component({
    selector: 'host-component',
    template: `
      <rects-view
        title="TestRectsView"
        [store]="store"
        [rects]="rects"
        [displayIds]="displayIds"></rects-view>
    `,
  })
  class TestHostComponent {
    store = new PersistentStore();
    rects: UiRect[] = [];
    displayIds: number[] = [];

    @ViewChild(RectsComponent)
    rectsComponent!: RectsComponent;
  }
});
