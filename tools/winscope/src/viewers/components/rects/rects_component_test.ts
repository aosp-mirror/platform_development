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
import {ViewerEvents} from 'viewers/common/viewer_events';
import {RectsComponent} from 'viewers/components/rects/rects_component';
import {UiRect} from 'viewers/components/rects/types2d';
import {Canvas} from './canvas';
import {ColorType, ShadingMode} from './types3d';
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
    let groupId = 0;
    htmlElement.addEventListener(ViewerEvents.RectGroupIdChange, (event) => {
      groupId = (event as CustomEvent).detail.groupId;
    });
    const button = Array.from(container.querySelectorAll('button'))[1];
    button.click();
    fixture.detectChanges();
    checkButtons(
      ['Display 0', 'Display 1', 'Display 2'],
      ['secondary', 'primary', 'secondary'],
      '.display-name-buttons',
    );
    expect(groupId).toEqual(1);
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

  it('updates scene on separation slider change', () => {
    const inputRect = makeRectWithGroupId(0);
    component.rects = [inputRect, inputRect];
    const spy = spyOn(Canvas.prototype, 'draw').and.callThrough();
    fixture.detectChanges();
    updateSeparationSlider();

    expect(spy).toHaveBeenCalledTimes(2);
    const sceneBefore = assertDefined(spy.calls.first().args.at(0));
    const sceneAfter = assertDefined(spy.calls.mostRecent().args.at(0));

    expect(sceneBefore.rects[1].topLeft.z).toEqual(5);
    expect(sceneAfter.rects[1].topLeft.z).toEqual(0.3);
  });

  it('updates scene on rotation slider change', () => {
    const inputRect = makeRectWithGroupId(0);
    component.rects = [inputRect];
    const spy = spyOn(Canvas.prototype, 'draw').and.callThrough();
    fixture.detectChanges();
    updateRotationSlider();

    expect(spy).toHaveBeenCalledTimes(2);
    const sceneBefore = assertDefined(spy.calls.first().args.at(0));
    const sceneAfter = assertDefined(spy.calls.mostRecent().args.at(0));

    expect(sceneBefore.camera.rotationFactor).toEqual(1);
    expect(sceneAfter.camera.rotationFactor).toEqual(0.5);
  });

  it('updates scene on only visible mode change', () => {
    const inputRect = makeRectWithGroupId(0);
    const nonVisibleRect = makeRectWithGroupId(0, false);
    component.rects = [inputRect, nonVisibleRect];
    const spy = spyOn(Canvas.prototype, 'draw').and.callThrough();
    fixture.detectChanges();

    updateShowOnlyVisibleMode();

    expect(spy).toHaveBeenCalledTimes(2);
    const sceneBefore = assertDefined(spy.calls.first().args.at(0));
    const sceneAfter = assertDefined(spy.calls.mostRecent().args.at(0));

    expect(sceneBefore.rects.length).toEqual(2);
    expect(sceneAfter.rects.length).toEqual(1);
  });

  it('updates scene on shading mode change', () => {
    const inputRect = makeRectWithGroupId(0);
    component.rects = [inputRect];
    const spy = spyOn(Canvas.prototype, 'draw').and.callThrough();
    fixture.detectChanges();

    updateShadingMode(ShadingMode.GRADIENT, ShadingMode.WIRE_FRAME);
    updateShadingMode(ShadingMode.WIRE_FRAME, ShadingMode.OPACITY);

    expect(spy).toHaveBeenCalledTimes(3);
    const sceneGradient = assertDefined(spy.calls.first().args.at(0));
    const sceneWireFrame = assertDefined(spy.calls.argsFor(1).at(0));
    const sceneOpacity = assertDefined(spy.calls.mostRecent().args.at(0));

    expect(sceneGradient.rects[0].colorType).toEqual(ColorType.VISIBLE);
    expect(sceneGradient.rects[0].darkFactor).toEqual(1);

    expect(sceneWireFrame.rects[0].colorType).toEqual(ColorType.EMPTY);
    expect(sceneWireFrame.rects[0].darkFactor).toEqual(1);

    expect(sceneOpacity.rects[0].colorType).toEqual(
      ColorType.VISIBLE_WITH_OPACITY,
    );
    expect(sceneOpacity.rects[0].darkFactor).toEqual(0.5);
  });

  it('uses stored rects view settings', () => {
    fixture.detectChanges();

    updateSeparationSlider();
    updateShadingMode(ShadingMode.GRADIENT, ShadingMode.WIRE_FRAME);
    updateShowOnlyVisibleMode();

    const newFixture = TestBed.createComponent(TestHostComponent);
    newFixture.detectChanges();
    const newRectsComponent = assertDefined(
      newFixture.componentInstance.rectsComponent,
    );
    expect(newRectsComponent.getZSpacingFactor()).toEqual(0.06);
    expect(newRectsComponent.getShowOnlyVisibleMode()).toBeTrue();
    expect(newRectsComponent.getShadingMode()).toEqual(ShadingMode.WIRE_FRAME);
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

  it('draws mini rects with non-present group id', () => {
    fixture.detectChanges();
    const inputRect = makeRectWithGroupId(0);
    const miniRect = makeRectWithGroupId(2);
    component.rects = [inputRect];
    component.displays = [{displayId: 10, groupId: 0, name: 'Display 0'}];
    component.miniRects = [miniRect];
    const spy = spyOn(Canvas.prototype, 'draw').and.callThrough();
    fixture.detectChanges();
    expect(spy).toHaveBeenCalledTimes(2);
    expect(
      spy.calls
        .all()
        .forEach((call) => expect(call.args[0].rects.length).toEqual(1)),
    );
  });

  it('draws mini rects with default spacing, rotation and shading mode', () => {
    fixture.detectChanges();

    updateSeparationSlider();
    updateRotationSlider();
    updateShadingMode(ShadingMode.GRADIENT, ShadingMode.WIRE_FRAME);

    const inputRect = makeRectWithGroupId(0);
    component.rects = [inputRect, inputRect];
    component.displays = [{displayId: 10, groupId: 0, name: 'Display 0'}];
    component.miniRects = [inputRect, inputRect];
    const spy = spyOn(Canvas.prototype, 'draw').and.callThrough();
    fixture.detectChanges();
    expect(spy).toHaveBeenCalledTimes(2);

    const largeRectsScene = assertDefined(spy.calls.first().args.at(0));
    const miniRectsScene = assertDefined(spy.calls.mostRecent().args.at(0));

    expect(largeRectsScene.camera.rotationFactor).toEqual(0.5);
    expect(miniRectsScene.camera.rotationFactor).toEqual(1);

    expect(largeRectsScene.rects[0].colorType).toEqual(ColorType.EMPTY);
    expect(miniRectsScene.rects[0].colorType).toEqual(ColorType.VISIBLE);

    expect(largeRectsScene.rects[1].topLeft.z).toEqual(0.3);
    expect(miniRectsScene.rects[1].topLeft.z).toEqual(5);
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

  function findAndClickElement(selector: string) {
    const el = assertDefined(
      htmlElement.querySelector(selector),
    ) as HTMLElement;
    el.click();
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

  function updateSeparationSlider() {
    const rectsComponent = assertDefined(component.rectsComponent);
    expect(rectsComponent.getZSpacingFactor()).toEqual(1);
    rectsComponent.onSeparationSliderChange(0.06);
    fixture.detectChanges();
    expect(rectsComponent.getZSpacingFactor()).toEqual(0.06);
  }

  function updateRotationSlider() {
    const rectsComponent = assertDefined(component.rectsComponent);
    rectsComponent.onRotationSliderChange(0.5);
    fixture.detectChanges();
  }

  function updateShadingMode(before: ShadingMode, after: ShadingMode) {
    const rectsComponent = assertDefined(component.rectsComponent);
    expect(rectsComponent.getShadingMode()).toEqual(before);
    findAndClickElement('.right-btn-container button.shading-mode');
    expect(rectsComponent.getShadingMode()).toEqual(after);
  }

  function updateShowOnlyVisibleMode() {
    const rectsComponent = assertDefined(component.rectsComponent);
    expect(rectsComponent.getShowOnlyVisibleMode()).toBeFalse();
    findAndClickElement('.top-view-controls .show-only-visible  input');
    expect(rectsComponent.getShowOnlyVisibleMode()).toBeTrue();
  }

  function makeRectWithGroupId(groupId: number, isVisible = true): UiRect {
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
      .setIsVisible(isVisible)
      .setIsDisplay(false)
      .setId('test-id-1234')
      .setGroupId(groupId)
      .setIsVirtual(false)
      .setIsClickable(false)
      .setCornerRadius(0)
      .setDepth(0)
      .setOpacity(0.5)
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
        [displays]="displays"
        [miniRects]="miniRects"
        [shadingModes]="shadingModes"></rects-view>
    `,
  })
  class TestHostComponent {
    store = new PersistentStore();
    rects: UiRect[] = [];
    displays: DisplayIdentifier[] = [];
    miniRects: UiRect[] = [];
    isStackBased = false;
    shadingModes = [
      ShadingMode.GRADIENT,
      ShadingMode.WIRE_FRAME,
      ShadingMode.OPACITY,
    ];

    @ViewChild(RectsComponent)
    rectsComponent: RectsComponent | undefined;
  }
});
