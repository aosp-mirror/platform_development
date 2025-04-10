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

import {TestBed} from '@angular/core/testing';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {assertDefined} from 'common/assert_utils';
import {DOMTestHelper} from 'test/unit/dom_test_utils';
import {CollapsibleSections} from 'viewers/common/collapsible_sections';
import {CollapsibleSectionType} from 'viewers/common/collapsible_section_type';
import {CollapsedSectionsComponent} from './collapsed_sections_component';

describe('CollapsedSectionsComponent', () => {
  let component: CollapsedSectionsComponent;
  let dom: DOMTestHelper<CollapsedSectionsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MatButtonModule, MatIconModule],
      declarations: [CollapsedSectionsComponent],
    }).compileComponents();
    const fixture = TestBed.createComponent(CollapsedSectionsComponent);
    component = fixture.componentInstance;
    dom = new DOMTestHelper(fixture, fixture.nativeElement);
    component.sections = new CollapsibleSections([
      {
        type: CollapsibleSectionType.RECTS,
        label: 'rects',
        isCollapsed: false,
      },
      {
        type: CollapsibleSectionType.HIERARCHY,
        label: 'hierarchy',
        isCollapsed: true,
      },
      {
        type: CollapsibleSectionType.PROPERTIES,
        label: 'properties',
        isCollapsed: false,
      },
    ]);
    dom.detectChanges();
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('displays only collapsed sections', () => {
    let sections = dom.findAll('.collapsed-section');
    expect(sections.length).toEqual(1);
    sections[0].checkText('HIERARCHY');
    expect(sections[0].find('button')).toBeDefined();

    assertDefined(component.sections).onCollapseStateChange(
      CollapsibleSectionType.RECTS,
      true,
    );
    dom.detectChanges();
    sections = dom.findAll('.collapsed-section');
    expect(sections.length).toEqual(2);
    sections[0].checkText('RECTS');
    expect(sections[0].find('button')).toBeDefined();
    sections[1].checkText('HIERARCHY');
    expect(sections[1].find('button')).toBeDefined();
  });

  it('emits sectionChange event', () => {
    const spy = spyOn(component.sectionChange, 'emit');
    dom.findAndClick('.collapsed-section button');
    expect(spy).toHaveBeenCalledOnceWith(CollapsibleSectionType.HIERARCHY);
  });
});
