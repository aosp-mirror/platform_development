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
import {Component} from '@angular/core';
import {ComponentFixture, ComponentFixtureAutoDetect, TestBed} from '@angular/core/testing';
import {MatDividerModule} from '@angular/material/divider';
import {MatTooltipModule} from '@angular/material/tooltip';
import {assertDefined} from 'common/assert_utils';
import {Color} from 'flickerlib/common';
import {LayerBuilder} from 'test/unit/layer_builder';
import {SurfaceFlingerPropertyGroupsComponent} from './surface_flinger_property_groups_component';
import {TransformMatrixComponent} from './transform_matrix_component';

describe('PropertyGroupsComponent', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let component: TestHostComponent;
  let htmlElement: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [{provide: ComponentFixtureAutoDetect, useValue: true}],
      imports: [MatDividerModule, MatTooltipModule],
      declarations: [
        TestHostComponent,
        SurfaceFlingerPropertyGroupsComponent,
        TransformMatrixComponent,
      ],
      schemas: [],
    }).compileComponents();
    fixture = TestBed.createComponent(TestHostComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;
    fixture.detectChanges();
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('renders verbose flags if available', () => {
    const layer = new LayerBuilder().setFlags(3).build();
    component.item = layer;
    fixture.detectChanges();

    const flags = assertDefined(htmlElement.querySelector('.flags'));
    expect(flags.innerHTML).toMatch('Flags:.*HIDDEN|OPAQUE \\(0x3\\)');
  });

  it('renders numeric flags if verbose flags not available', () => {
    const flags = assertDefined(htmlElement.querySelector('.flags'));
    expect(flags.innerHTML).toMatch('Flags:.*0');
  });

  it('displays calculated geometry', () => {
    const calculatedDiv = assertDefined(htmlElement.querySelector('.geometry .left-column'));
    expect(calculatedDiv.querySelector('transform-matrix')).toBeTruthy();
    expect(assertDefined(calculatedDiv.querySelector('.crop')).innerHTML).toContain('[empty]');
    expect(assertDefined(calculatedDiv.querySelector('.final-bounds')).innerHTML).toContain(
      '[empty]'
    );
  });

  it('displays requested geometry', () => {
    const layer = new LayerBuilder().setFlags(0).build();
    layer.proto = {
      requestedTransform: {
        dsdx: 0,
        dsdy: 0,
        dtdx: 0,
        dtdy: 0,
        type: 0,
      },
    };
    component.item = layer;
    fixture.detectChanges();
    const requestedDiv = assertDefined(htmlElement.querySelector('.geometry .right-column'));
    expect(requestedDiv.querySelector('transform-matrix')).toBeTruthy();
    expect(assertDefined(requestedDiv.querySelector('.crop')).innerHTML).toContain('[empty]');
  });

  it('displays buffer info', () => {
    const layer = new LayerBuilder().setFlags(0).build();
    layer.proto = {
      destinationFrame: {
        left: 0,
        top: 0,
        right: 0,
        bottom: 0,
      },
    };
    component.item = layer;
    fixture.detectChanges();

    const sizeDiv = htmlElement.querySelector('.buffer .size');
    expect(assertDefined(sizeDiv).innerHTML).toContain('[empty]');
    const currFrameDiv = htmlElement.querySelector('.buffer .frame-number');
    expect(assertDefined(currFrameDiv).innerHTML).toContain('0');
    const transformDiv = htmlElement.querySelector('.buffer .transform');
    expect(assertDefined(transformDiv).innerHTML).toContain('IDENTITY');
    const destFrameDiv = htmlElement.querySelector('.buffer .dest-frame');
    expect(assertDefined(destFrameDiv).innerHTML).toContain('left: 0, top: 0, right: 0, bottom: 0');
  });

  it('displays hierarchy info', () => {
    const zDiv = htmlElement.querySelector('.hierarchy-info .z-order');
    expect(assertDefined(zDiv).innerHTML).toContain('0');
    const relParentDiv = htmlElement.querySelector('.hierarchy-info .rel-parent');
    expect(assertDefined(relParentDiv).innerHTML).toContain('none');
  });

  it('displays simple calculated effects', () => {
    const calculatedDiv = assertDefined(htmlElement.querySelector('.effects .left-column'));
    expect(assertDefined(calculatedDiv.querySelector('.shadow')).innerHTML).toContain('1 px');
    expect(assertDefined(calculatedDiv.querySelector('.blur')).innerHTML).toContain('1 px');
    expect(assertDefined(calculatedDiv.querySelector('.corner-radius')).innerHTML).toContain(
      '1 px'
    );
  });

  it('displays simple requested effects', () => {
    const layer = new LayerBuilder().setFlags(0).build();
    layer.proto = {
      requestedShadowRadius: 1,
      requestedCornerRadius: 1,
    };
    component.item = layer;
    fixture.detectChanges();

    const calculatedDiv = assertDefined(htmlElement.querySelector('.effects .right-column'));
    expect(assertDefined(calculatedDiv.querySelector('.shadow')).innerHTML).toContain('1 px');
    expect(assertDefined(calculatedDiv.querySelector('.corner-radius')).innerHTML).toContain(
      '1 px'
    );
  });

  it('displays empty color and alpha value in effects', () => {
    const layer = new LayerBuilder().setFlags(0).build();
    layer.color.a = 1;
    component.item = layer;
    fixture.detectChanges();

    const colorDiv = htmlElement.querySelector('.color');
    expect(assertDefined(colorDiv).innerHTML).toContain('[empty], alpha: 1');
  });

  it('displays rgba color in effects', () => {
    const layer = new LayerBuilder().setFlags(0).setColor(new Color(0, 0, 0, 1)).build();
    component.item = layer;
    fixture.detectChanges();

    const colorDiv = htmlElement.querySelector('.color');
    expect(assertDefined(colorDiv).innerHTML).toContain('(0, 0, 0, 1)');
  });

  it('displays not set message if no inputs present', () => {
    const noInputsMessage = htmlElement.querySelector('.inputs .left-column');
    expect(assertDefined(noInputsMessage).innerHTML).toContain('not set');
  });

  it('displays input window info if available', () => {
    const layer = new LayerBuilder().setFlags(0).build();
    layer.proto = {
      inputWindowInfo: {
        focusable: false,
        inputTransform: {
          dsdx: 0,
          dsdy: 0,
          dtdx: 0,
          dtdy: 0,
          type: 0,
        },
        cropLayerId: 0,
        replaceTouchableRegionWithCrop: false,
      },
    };
    component.item = layer;
    fixture.detectChanges();

    expect(htmlElement.querySelector('.inputs .left-column transform-matrix')).toBeTruthy();

    const configDiv = assertDefined(htmlElement.querySelector('.inputs .right-column'));
    expect(assertDefined(configDiv.querySelector('.focusable')).innerHTML).toContain('false');
    expect(assertDefined(configDiv.querySelector('.crop-touch-region')).innerHTML).toContain(
      'none'
    );
    expect(assertDefined(configDiv.querySelector('.replace-touch-region')).innerHTML).toContain(
      'false'
    );
  });

  @Component({
    selector: 'host-component',
    template: ` <surface-flinger-property-groups [item]="item"></surface-flinger-property-groups> `,
  })
  class TestHostComponent {
    item = new LayerBuilder().setFlags(0).build();
  }
});
