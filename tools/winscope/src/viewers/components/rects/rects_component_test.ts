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
import {CUSTOM_ELEMENTS_SCHEMA} from '@angular/core';
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {MatCheckboxModule} from '@angular/material/checkbox';
import {MatDividerModule} from '@angular/material/divider';
import {MatRadioModule} from '@angular/material/radio';
import {MatSliderModule} from '@angular/material/slider';
import {Rectangle} from 'viewers/common/rectangle';
import {RectsComponent} from 'viewers/components/rects/rects_component';
import {Canvas} from './canvas';

describe('RectsComponent', () => {
  let component: RectsComponent;
  let fixture: ComponentFixture<RectsComponent>;
  let htmlElement: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CommonModule, MatCheckboxModule, MatDividerModule, MatSliderModule, MatRadioModule],
      declarations: [RectsComponent],
      schemas: [CUSTOM_ELEMENTS_SCHEMA],
    }).compileComponents();

    fixture = TestBed.createComponent(RectsComponent);
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
    const rectsCanvas = htmlElement.querySelector('.canvas-rects');
    expect(rectsCanvas).toBeTruthy();
  });

  it('draws scene when input data changes', async () => {
    spyOn(Canvas.prototype, 'draw').and.callThrough();

    const inputRect: Rectangle = {
      topLeft: {x: 0, y: 0},
      bottomRight: {x: 1, y: -1},
      label: 'rectangle1',
      transform: {
        matrix: {
          dsdx: 1,
          dsdy: 0,
          dtdx: 0,
          dtdy: 1,
          tx: 0,
          ty: 0,
        },
      },
      isVisible: true,
      isDisplay: false,
      ref: null,
      id: 'test-id-1234',
      displayId: 0,
      isVirtual: false,
      isClickable: false,
      cornerRadius: 0,
    };

    expect(Canvas.prototype.draw).toHaveBeenCalledTimes(0);
    component.rects = [inputRect];
    expect(Canvas.prototype.draw).toHaveBeenCalledTimes(1);
    component.rects = [inputRect];
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

    const displayButtonContainer = htmlElement.querySelector('.display-button-container');
    expect(displayButtonContainer).toBeTruthy();

    const buttons = Array.from(displayButtonContainer?.querySelectorAll('button') ?? []);
    expect(buttons.length).toBe(3);

    const buttonValues = buttons.map((it) => it.textContent?.trim());
    expect(buttonValues).toEqual(['0', '1', '2']);
  });
});
