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
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {MatDividerModule} from '@angular/material/divider';
import {MatTooltipModule} from '@angular/material/tooltip';
import {LayerBuilder} from 'test/unit/layer_builder';
import {PropertyGroupsComponent} from './property_groups_component';
import {TransformMatrixComponent} from './transform_matrix_component';

describe('PropertyGroupsComponent', () => {
  let fixture: ComponentFixture<PropertyGroupsComponent>;
  let component: PropertyGroupsComponent;
  let htmlElement: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MatDividerModule, MatTooltipModule],
      declarations: [PropertyGroupsComponent, TransformMatrixComponent],
      schemas: [],
    }).compileComponents();
    fixture = TestBed.createComponent(PropertyGroupsComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('it renders verbose flags if available', async () => {
    const layer = new LayerBuilder().setFlags(3).build();
    component.item = layer;
    fixture.detectChanges();

    const flags = htmlElement.querySelector('.flags');
    expect(flags).toBeTruthy();
    expect(flags!.innerHTML).toMatch('Flags:.*HIDDEN|OPAQUE \\(0x3\\)');
  });

  it('it renders numeric flags if verbose flags not available', async () => {
    const layer = new LayerBuilder().setFlags(0).build();
    component.item = layer;
    fixture.detectChanges();

    const flags = htmlElement.querySelector('.flags');
    expect(flags).toBeTruthy();
    expect(flags!.innerHTML).toMatch('Flags:.*0');
  });
});
