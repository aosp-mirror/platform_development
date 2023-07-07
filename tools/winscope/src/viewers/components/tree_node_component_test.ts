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
import {Component, NO_ERRORS_SCHEMA, ViewChild} from '@angular/core';
import {ComponentFixture, ComponentFixtureAutoDetect, TestBed} from '@angular/core/testing';
import {TreeNodeComponent} from './tree_node_component';

describe('TreeNodeComponent', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let component: TestHostComponent;
  let htmlElement: HTMLElement;

  beforeAll(async () => {
    await TestBed.configureTestingModule({
      providers: [{provide: ComponentFixtureAutoDetect, useValue: true}],
      declarations: [TreeNodeComponent, TestHostComponent],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(TestHostComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;
  });

  it('can be created', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  @Component({
    selector: 'host-component',
    template: `
      <tree-node
        [item]="item"
        [isCollapsed]="true"
        [isPinned]="false"
        [isInPinnedSection]="false"
        [hasChildren]="false"></tree-node>
    `,
  })
  class TestHostComponent {
    item = {
      simplifyNames: false,
      kind: 'entry',
      name: 'LayerTraceEntry',
      shortName: 'LTE',
      chips: [],
    };

    @ViewChild(TreeNodeComponent)
    treeNodeComponent!: TreeNodeComponent;
  }
});
