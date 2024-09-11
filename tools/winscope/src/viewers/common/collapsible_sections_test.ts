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

import {CollapsibleSections} from './collapsible_sections';
import {CollapsibleSectionType} from './collapsible_section_type';

describe('AddDiffsPropertiesTree', () => {
  let collapsibleSections: CollapsibleSections;

  beforeEach(() => {
    const sections = [
      {
        type: CollapsibleSectionType.RECTS,
        label: 'LAYERS',
        isCollapsed: false,
      },
      {
        type: CollapsibleSectionType.HIERARCHY,
        label: 'HIERARCHY',
        isCollapsed: true,
      },
      {
        type: CollapsibleSectionType.PROPERTIES,
        label: 'PROTO DUMP',
        isCollapsed: false,
      },
    ];
    collapsibleSections = new CollapsibleSections(sections);
  });

  it('returns true if all sections expanded', () => {
    expect(collapsibleSections.areAllSectionsExpanded()).toBeFalse();
    collapsibleSections.onCollapseStateChange(
      CollapsibleSectionType.HIERARCHY,
      false,
    );
    expect(collapsibleSections.areAllSectionsExpanded()).toBeTrue();
  });

  it('returns collapsed sections', () => {
    expect(collapsibleSections.getCollapsedSections()).toEqual([
      {
        type: CollapsibleSectionType.HIERARCHY,
        label: 'HIERARCHY',
        isCollapsed: true,
      },
    ]);
  });

  it('returns section of given type if available', () => {
    expect(
      collapsibleSections.getSection(CollapsibleSectionType.HIERARCHY),
    ).toEqual({
      type: CollapsibleSectionType.HIERARCHY,
      label: 'HIERARCHY',
      isCollapsed: true,
    });
    expect(
      collapsibleSections.getSection(
        CollapsibleSectionType.IME_ADDITIONAL_PROPERTIES,
      ),
    ).toBeUndefined();
  });

  it('returns collapse state of given section type', () => {
    expect(
      collapsibleSections.isSectionCollapsed(CollapsibleSectionType.HIERARCHY),
    ).toBeTrue();
    expect(
      collapsibleSections.isSectionCollapsed(CollapsibleSectionType.RECTS),
    ).toBeFalse();

    // robust to given section type not present
    expect(
      collapsibleSections.isSectionCollapsed(
        CollapsibleSectionType.IME_ADDITIONAL_PROPERTIES,
      ),
    ).toBeFalse();
  });

  it('changes collapse state of given section type', () => {
    expect(
      collapsibleSections.isSectionCollapsed(CollapsibleSectionType.HIERARCHY),
    ).toBeTrue();

    collapsibleSections.onCollapseStateChange(
      CollapsibleSectionType.HIERARCHY,
      false,
    );
    expect(
      collapsibleSections.isSectionCollapsed(CollapsibleSectionType.HIERARCHY),
    ).toBeFalse();

    collapsibleSections.onCollapseStateChange(
      CollapsibleSectionType.HIERARCHY,
      true,
    );
    expect(
      collapsibleSections.isSectionCollapsed(CollapsibleSectionType.HIERARCHY),
    ).toBeTrue();

    // no change in state
    collapsibleSections.onCollapseStateChange(
      CollapsibleSectionType.HIERARCHY,
      true,
    );
    expect(
      collapsibleSections.isSectionCollapsed(CollapsibleSectionType.HIERARCHY),
    ).toBeTrue();

    // robust to given section type not present
    expect(() => {
      collapsibleSections.onCollapseStateChange(
        CollapsibleSectionType.IME_ADDITIONAL_PROPERTIES,
        true,
      );
    }).not.toThrowError();
  });
});
