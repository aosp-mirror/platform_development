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
import {MatButtonModule} from '@angular/material/button';
import {MatCheckboxModule} from '@angular/material/checkbox';
import {MatDividerModule} from '@angular/material/divider';
import {MatRadioModule} from '@angular/material/radio';
import {MatSliderModule} from '@angular/material/slider';
import {MatTooltipModule} from '@angular/material/tooltip';
import {assertDefined} from 'common/assert_utils';
import {PersistentStore} from 'common/persistent_store';
import {DisplayIdentifier} from 'viewers/common/display_identifier';
import {RectsComponent} from 'viewers/components/rects/rects_component';
import {UiRect} from 'viewers/components/rects/types2d';
import {Canvas} from './canvas';
import {UiRectBuilder} from './ui_rect_builder';

describe('RectsComponent', () => {
  let component: TestHostComponent;
  let fixture: ComponentFixture<TestHostComponent>;
  let htmlElement: HTMLElement;

  beforeEach(async () => {
    localStorage.clear();

    await TestBed.configureTestingModule({
      imports: [
        CommonModule,
        MatCheckboxModule,
        MatDividerModule,
        MatSliderModule,
        MatRadioModule,
        MatButtonModule,
        MatTooltipModule,
      ],
      declarations: [TestHostComponent, RectsComponent],
      schemas: [CUSTOM_ELEMENTS_SCHEMA],
    }).compileComponents();

    fixture = TestBed.createComponent(TestHostComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;
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
    fixture.detectChanges();
    spyOn(Canvas.prototype, 'draw').and.callThrough();

    const inputRect = makeRectWithGroupId(0);

    expect(Canvas.prototype.draw).toHaveBeenCalledTimes(0);
    component.rects = [inputRect];
    fixture.detectChanges();
    expect(Canvas.prototype.draw).toHaveBeenCalledTimes(1);
    component.rects = [inputRect];
    fixture.detectChanges();
    expect(Canvas.prototype.draw).toHaveBeenCalledTimes(2);
  });

  it('draws scene when rotation slider changes', () => {
    fixture.detectChanges();
    spyOn(Canvas.prototype, 'draw').and.callThrough();
    const slider = assertDefined(htmlElement.querySelector('.slider-rotation'));

    expect(Canvas.prototype.draw).toHaveBeenCalledTimes(0);

    slider.dispatchEvent(new MouseEvent('mousedown'));
    expect(Canvas.prototype.draw).toHaveBeenCalledTimes(1);
  });

  it('draws scene when spacing slider changes', () => {
    fixture.detectChanges();
    spyOn(Canvas.prototype, 'draw').and.callThrough();
    const slider = assertDefined(htmlElement.querySelector('.slider-spacing'));

    expect(Canvas.prototype.draw).toHaveBeenCalledTimes(0);

    slider.dispatchEvent(new MouseEvent('mousedown'));
    expect(Canvas.prototype.draw).toHaveBeenCalledTimes(1);
  });

  it('unfocuses spacing slider on click', () => {
    fixture.detectChanges();
    const spacingSlider = assertDefined(
      htmlElement.querySelector('.slider-spacing'),
    );
    checkSliderUnfocusesOnClick(spacingSlider, 0.02);
  });

  it('unfocuses rotation slider on click', () => {
    fixture.detectChanges();
    const rotationSlider = assertDefined(
      htmlElement.querySelector('.slider-rotation'),
    );
    checkSliderUnfocusesOnClick(rotationSlider, 1);
  });

  it('draws display buttons', () => {
    component.displays = [
      {displayId: 0, groupId: 0, name: 'Display 0'},
      {displayId: 1, groupId: 1, name: 'Display 1'},
      {displayId: 2, groupId: 2, name: 'Display 2'},
    ];
    fixture.detectChanges();
    checkButtons(
      ['Display 0', 'Display 1', 'Display 2'],
      ['primary', 'secondary', 'secondary'],
      '.display-name-buttons',
    );
  });

  it('handles display button click', () => {
    component.displays = [
      {displayId: 0, groupId: 0, name: 'Display 0'},
      {displayId: 1, groupId: 1, name: 'Display 1'},
      {displayId: 2, groupId: 2, name: 'Display 2'},
    ];
    fixture.detectChanges();
    checkButtons(
      ['Display 0', 'Display 1', 'Display 2'],
      ['primary', 'secondary', 'secondary'],
      '.display-name-buttons',
    );

    const container = assertDefined(
      htmlElement.querySelector('.display-name-buttons'),
    );
    const button = Array.from(container.querySelectorAll('button'))[1];
    button.click();
    fixture.detectChanges();
    checkButtons(
      ['Display 0', 'Display 1', 'Display 2'],
      ['secondary', 'primary', 'secondary'],
      '.display-name-buttons',
    );
  });

  it('tracks selected display', () => {
    component.displays = [
      {displayId: 10, groupId: 0, name: 'Display 0'},
      {displayId: 20, groupId: 1, name: 'Display 1'},
    ];
    fixture.detectChanges();
    checkButtons(
      ['Display 0', 'Display 1'],
      ['primary', 'secondary'],
      '.display-name-buttons',
    );

    component.displays = [
      {displayId: 20, groupId: 2, name: 'Display 1'},
      {displayId: 10, groupId: 1, name: 'Display 0'},
    ];
    fixture.detectChanges();
    checkButtons(
      ['Display 1', 'Display 0'],
      ['secondary', 'primary'],
      '.display-name-buttons',
    );
  });

  it('uses stored rects view settings', () => {
    fixture.detectChanges();
    const rectsComponent = assertDefined(component.rectsComponent);
    expect(rectsComponent.getZSpacingFactor()).toEqual(1);
    rectsComponent.onSeparationSliderChange(0.06);
    fixture.detectChanges();
    expect(rectsComponent.getZSpacingFactor()).toEqual(0.06);

    expect(rectsComponent.getShowOnlyVisibleMode()).toBeFalse();
    findAndClickCheckbox('.top-view-controls .show-only-visible  input');
    expect(rectsComponent.getShowOnlyVisibleMode()).toBeTrue();

    const newFixture = TestBed.createComponent(TestHostComponent);
    newFixture.detectChanges();
    const newRectsComponent = assertDefined(
      newFixture.componentInstance.rectsComponent,
    );
    expect(newRectsComponent.getZSpacingFactor()).toEqual(0.06);
    expect(newRectsComponent.getShowOnlyVisibleMode()).toBeTrue();
  });

  it('defaults initial selection to first display with non-display rects and groupId 0', () => {
    const inputRect = makeRectWithGroupId(0);
    component.rects = [inputRect];
    component.displays = [
      {displayId: 10, groupId: 1, name: 'Display 0'},
      {displayId: 20, groupId: 0, name: 'Display 1'},
    ];
    fixture.detectChanges();

    checkButtons(
      ['Display 0', 'Display 1'],
      ['secondary', 'primary'],
      '.display-name-buttons',
    );
  });

  it('defaults initial selection to first display with non-display rects and groupId non-zero', () => {
    const inputRect = makeRectWithGroupId(1);
    component.rects = [inputRect];
    component.displays = [
      {displayId: 10, groupId: 0, name: 'Display 0'},
      {displayId: 20, groupId: 1, name: 'Display 1'},
    ];
    fixture.detectChanges();

    checkButtons(
      ['Display 0', 'Display 1'],
      ['secondary', 'primary'],
      '.display-name-buttons',
    );
  });

  function checkButtons(
    buttonValues: string[],
    buttonColors: string[],
    selector: string,
  ) {
    const displayButtonContainer = assertDefined(
      htmlElement.querySelector(selector),
    );
    const buttons = Array.from(
      displayButtonContainer.querySelectorAll('button'),
    );
    expect(buttons.map((it) => it.textContent?.trim())).toEqual(buttonValues);

    for (let i = 0; i < buttonValues.length; i++) {
      expect((buttons[i] as HTMLButtonElement).outerHTML).toContain(
        buttonColors[i],
      );
    }
  }

  function findAndClickCheckbox(selector: string) {
    const box = assertDefined(
      htmlElement.querySelector(selector),
    ) as HTMLInputElement;
    box.dispatchEvent(new Event('click'));
    fixture.detectChanges();
  }

  function findAndClickTab(index: number) {
    const tabs = Array.from(
      htmlElement.querySelectorAll('.grouping-tabs mat-tab'),
    );
    const tab = assertDefined(tabs[index]) as HTMLElement;
    tab.click();
    fixture.detectChanges();
  }

  function checkSliderUnfocusesOnClick(slider: Element, expectedValue: number) {
    const rectsComponent = assertDefined(component.rectsComponent);
    slider.dispatchEvent(new MouseEvent('mousedown'));
    expect(rectsComponent.getZSpacingFactor()).toEqual(expectedValue);
    htmlElement.dispatchEvent(
      new KeyboardEvent('keydown', {key: 'ArrowRight'}),
    );
    expect(rectsComponent.getZSpacingFactor()).toEqual(expectedValue);
    htmlElement.dispatchEvent(new KeyboardEvent('keydown', {key: 'ArrowLeft'}));
    expect(rectsComponent.getZSpacingFactor()).toEqual(expectedValue);
  }

  function makeRectWithGroupId(groupId: number): UiRect {
    return new UiRectBuilder()
      .setX(0)
      .setY(0)
      .setWidth(1)
      .setHeight(1)
      .setLabel('rectangle1')
      .setTransform({
        dsdx: 1,
        dsdy: 0,
        dtdx: 0,
        dtdy: 1,
        tx: 0,
        ty: 0,
      })
      .setIsVisible(true)
      .setIsDisplay(false)
      .setId('test-id-1234')
      .setGroupId(groupId)
      .setIsVirtual(false)
      .setIsClickable(false)
      .setCornerRadius(0)
      .setDepth(0)
      .build();
  }

  @Component({
    selector: 'host-component',
    template: `
      <rects-view
        title="TestRectsView"
        [store]="store"
        [rects]="rects"
        [isStackBased]="isStackBased"
        [displays]="displays"></rects-view>
    `,
  })
  class TestHostComponent {
    store = new PersistentStore();
    rects: UiRect[] = [];
    displays: DisplayIdentifier[] = [];
    isStackBased = false;

    @ViewChild(RectsComponent)
    rectsComponent: RectsComponent | undefined;
  }
});
