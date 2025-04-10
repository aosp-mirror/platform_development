/*
 * Copyright (C) 2024 The Android Open Source Project
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
import {ComponentFixtureAutoDetect, TestBed} from '@angular/core/testing';
import {MatDividerModule} from '@angular/material/divider';
import {MatTooltipModule} from '@angular/material/tooltip';
import {DOMTestHelper} from 'test/unit/dom_test_utils';
import {VcCuratedProperties} from 'viewers/common/curated_properties';
import {TransformMatrixComponent} from './transform_matrix_component';
import {ViewCapturePropertyGroupsComponent} from './view_capture_property_groups_component';

describe('ViewCapturePropertyGroupsComponent', () => {
  let component: TestHostComponent;
  let dom: DOMTestHelper<TestHostComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [{provide: ComponentFixtureAutoDetect, useValue: true}],
      imports: [MatDividerModule, MatTooltipModule],
      declarations: [
        TestHostComponent,
        ViewCapturePropertyGroupsComponent,
        TransformMatrixComponent,
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(TestHostComponent);
    component = fixture.componentInstance;
    dom = new DOMTestHelper(fixture, fixture.nativeElement);
    dom.detectChanges();
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('displays view section', () => {
    const section = dom.get('.view');
    section.get('.class-name').checkText('test.package.name');
    section.get('.hashcode').checkText('12345678');
  });

  it('displays geometry coordinates section', () => {
    const section = dom.get('.geometry .coordinates');
    section.get('.left').checkTextExact('Left:  0');
    section.get('.top').checkTextExact('Top:  5');
    section.get('.elevation').checkTextExact('Elevation:  2');
  });

  it('displays geometry size section', () => {
    const section = dom.get('.geometry .size');
    section.get('.height').checkTextExact('Height:  86');
    section.get('.width').checkTextExact('Width:  826');
  });

  it('displays geometry translation section', () => {
    const section = dom.get('.geometry .translation');
    section.get('.translationx').checkTextExact('Translation X:  0');
    section.get('.translationy').checkTextExact('Translation Y:  0');
  });

  it('displays geometry scroll section', () => {
    const section = dom.get('.geometry .scroll');
    section.get('.scrollx').checkTextExact('Scroll X:  1');
    section.get('.scrolly').checkTextExact('Scroll Y:  1');
  });

  it('displays geometry scale section', () => {
    const section = dom.get('.geometry .scale');
    section.get('.scalex').checkTextExact('Scale X:  2');
    section.get('.scaley').checkTextExact('Scale Y:  2');
  });

  it('displays effects translation section', () => {
    const section = dom.get('.effects .translation');
    section.get('.visibility').checkTextExact('Visibility:  0');
    section.get('.alpha').checkTextExact('Alpha:  1');
    section.get('.will-not-draw').checkTextExact('Will Not Draw:  true');
  });

  it('displays effects misc section', () => {
    const section = dom.get('.effects .misc');
    section.get('.clip-children').checkTextExact('Clip Children:  false');
  });

  @Component({
    selector: 'host-component',
    template: `
      <view-capture-property-groups [properties]="properties"></view-capture-property-groups>
    `,
  })
  class TestHostComponent {
    properties: VcCuratedProperties = {
      className: 'test.package.name',
      hashcode: '12345678',
      left: '0',
      top: '5',
      elevation: '2',
      height: '86',
      width: '826',
      translationX: '0',
      translationY: '0',
      scrollX: '1',
      scrollY: '1',
      scaleX: '2',
      scaleY: '2',
      visibility: '0',
      alpha: '1',
      willNotDraw: 'true',
      clipChildren: 'false',
    };
  }
});
