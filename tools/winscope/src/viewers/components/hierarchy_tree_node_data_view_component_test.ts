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
import {
  ComponentFixture,
  ComponentFixtureAutoDetect,
  TestBed,
} from '@angular/core/testing';
import {MatTooltipModule} from '@angular/material/tooltip';
import {HierarchyTreeNodeDataViewComponent} from './hierarchy_tree_node_data_view_component';

describe('HierarchyTreeNodeDataViewComponent', () => {
  let fixture: ComponentFixture<HierarchyTreeNodeDataViewComponent>;
  let component: HierarchyTreeNodeDataViewComponent;

  beforeAll(async () => {
    await TestBed.configureTestingModule({
      providers: [{provide: ComponentFixtureAutoDetect, useValue: true}],
      declarations: [HierarchyTreeNodeDataViewComponent],
      imports: [MatTooltipModule],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(HierarchyTreeNodeDataViewComponent);
    component = fixture.componentInstance;
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });
});
