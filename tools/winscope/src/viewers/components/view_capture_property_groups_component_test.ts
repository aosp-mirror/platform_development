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
import {
  ComponentFixture,
  ComponentFixtureAutoDetect,
  TestBed,
} from '@angular/core/testing';
import {MatDividerModule} from '@angular/material/divider';
import {MatTooltipModule} from '@angular/material/tooltip';
import {assertDefined} from 'common/assert_utils';
import {VcCuratedProperties} from 'viewers/common/curated_properties';
import {TransformMatrixComponent} from './transform_matrix_component';
import {ViewCapturePropertyGroupsComponent} from './view_capture_property_groups_component';

describe('ViewCapturePropertyGroupsComponent', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let component: TestHostComponent;
  let htmlElement: HTMLElement;

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
    fixture = TestBed.createComponent(TestHostComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;
    fixture.detectChanges();
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('displays view section', () => {
    const viewSection = assertDefined(htmlElement.querySelector('.view'));
    expect(
      assertDefined(viewSection.querySelector('.class-name')).innerHTML,
    ).toContain('test.package.name');
    expect(
      assertDefined(viewSection.querySelector('.hashcode')).innerHTML,
    ).toContain('12345678');
  });

  it('displays geometry coordinates section', () => {
    const coordinatesSection = assertDefined(
      htmlElement.querySelector('.geometry .coordinates'),
    );
    const left = assertDefined(coordinatesSection.querySelector('.left'));
    expect(left.innerHTML).toContain('Left:');
    expect(left.innerHTML).toContain('0');
    const top = assertDefined(coordinatesSection.querySelector('.top'));
    expect(top.innerHTML).toContain('Top:');
    expect(top.innerHTML).toContain('5');
    const elevation = assertDefined(
      coordinatesSection.querySelector('.elevation'),
    );
    expect(elevation.innerHTML).toContain('Elevation:');
    expect(elevation.innerHTML).toContain('2');
  });

  it('displays geometry size section', () => {
    const sizeSection = assertDefined(
      htmlElement.querySelector('.geometry .size'),
    );
    const height = assertDefined(sizeSection.querySelector('.height'));
    expect(height.innerHTML).toContain('Height:');
    expect(height.innerHTML).toContain('86');
    const width = assertDefined(sizeSection.querySelector('.width'));
    expect(width.innerHTML).toContain('Width:');
    expect(width.innerHTML).toContain('826');
  });

  it('displays geometry translation section', () => {
    const translationSection = assertDefined(
      htmlElement.querySelector('.geometry .translation'),
    );
    const translationx = assertDefined(
      translationSection.querySelector('.translationx'),
    );
    expect(translationx.innerHTML).toContain('Translation X:');
    expect(translationx.innerHTML).toContain('0');
    const translationy = assertDefined(
      translationSection.querySelector('.translationy'),
    );
    expect(translationy.innerHTML).toContain('Translation Y:');
    expect(translationy.innerHTML).toContain('0');
  });

  it('displays geometry scroll section', () => {
    const scrollSection = assertDefined(
      htmlElement.querySelector('.geometry .scroll'),
    );
    const scrollx = assertDefined(scrollSection.querySelector('.scrollx'));
    expect(scrollx.innerHTML).toContain('Scroll X:');
    expect(scrollx.innerHTML).toContain('1');
    const scrolly = assertDefined(scrollSection.querySelector('.scrolly'));
    expect(scrolly.innerHTML).toContain('Scroll Y:');
    expect(scrolly.innerHTML).toContain('1');
  });

  it('displays geometry scale section', () => {
    const scaleSection = assertDefined(
      htmlElement.querySelector('.geometry .scale'),
    );
    const scalex = assertDefined(scaleSection.querySelector('.scalex'));
    expect(scalex.innerHTML).toContain('Scale X:');
    expect(scalex.innerHTML).toContain('2');
    const scaley = assertDefined(scaleSection.querySelector('.scaley'));
    expect(scaley.innerHTML).toContain('Scale Y:');
    expect(scaley.innerHTML).toContain('2');
  });

  it('displays effects translation section', () => {
    const translationSection = assertDefined(
      htmlElement.querySelector('.effects .translation'),
    );
    const visibility = assertDefined(
      translationSection.querySelector('.visibility'),
    );
    expect(visibility.innerHTML).toContain('Visibility:');
    expect(visibility.innerHTML).toContain('0');
    const alpha = assertDefined(translationSection.querySelector('.alpha'));
    expect(alpha.innerHTML).toContain('Alpha:');
    expect(alpha.innerHTML).toContain('1');
    const willNotDraw = assertDefined(
      translationSection.querySelector('.will-not-draw'),
    );
    expect(willNotDraw.innerHTML).toContain('Will Not Draw:');
    expect(willNotDraw.innerHTML).toContain('true');
  });

  it('displays effects misc section', () => {
    const miscSection = assertDefined(
      htmlElement.querySelector('.effects .misc'),
    );
    const clipChildren = assertDefined(
      miscSection.querySelector('.clip-children'),
    );
    expect(clipChildren.innerHTML).toContain('Clip Children:');
    expect(clipChildren.innerHTML).toContain('false');
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
