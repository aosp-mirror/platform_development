/*
 * Copyright (C) 2023 The Android Open Source Project
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
import {DOMTestHelper} from 'test/unit/dom_test_utils';
import {AbstractHierarchyViewerComponentTest} from 'viewers/common/abstract_hierarchy_viewer_component_test';
import {TraceRectType} from 'viewers/components/rects/rect_spec';
import {SurfaceFlingerPropertyGroupsComponent} from 'viewers/components/surface_flinger_property_groups_component';
import {UiData} from './ui_data';
import {ViewerSurfaceFlingerComponent} from './viewer_surface_flinger_component';

@Component({
  selector: 'host-component',
  template:
    '<viewer-surface-flinger [inputData]="inputData"></viewer-surface-flinger>',
})
class TestHostComponent {
  inputData: UiData | undefined;
}

class ViewerSurfaceFlingerComponentTest extends AbstractHierarchyViewerComponentTest<TestHostComponent> {
  protected override readonly testRects = true;
  protected override readonly hierarchyTitle = 'HIERARCHY';
  protected override readonly propertiesTitle = 'PROTO DUMP';
  protected override readonly rectsTitle = 'LAYERS';

  protected override executeSpecializedTests() {
    describe('Specialized tests', () => {
      let dom: DOMTestHelper<TestHostComponent>;
      let component: TestHostComponent;

      beforeEach(async () => {
        [dom, component] = await this.setUpTestEnvironment();
      });

      it('creates property groups view', () => {
        expect(dom.find('.property-groups')).toBeDefined();
      });

      it('handles property groups section collapse/expand', () => {
        dom.checkSectionCollapseAndExpand('.property-groups', 'PROPERTIES');
      });

      it('handles rect type change', () => {
        let uiData = new UiData(undefined);
        uiData.rectSpec = {
          type: TraceRectType.LAYERS,
          icon: '',
          legend: [],
        };
        component.inputData = uiData;
        dom.detectChanges();

        dom.checkSectionCollapseAndExpand(
          '.rects-view',
          TraceRectType.LAYERS.toUpperCase(),
        );

        uiData = new UiData(undefined);
        uiData.rectSpec = {
          type: TraceRectType.INPUT_WINDOWS,
          icon: '',
          legend: [],
        };
        component.inputData = uiData;
        dom.detectChanges();
        dom.checkSectionCollapseAndExpand(
          '.rects-view',
          TraceRectType.INPUT_WINDOWS.toUpperCase(),
        );
      });
    });
  }

  protected async setUpTestEnvironment(): Promise<
    [DOMTestHelper<TestHostComponent>, TestHostComponent]
  > {
    return this.initializeTestEnvironment(TestHostComponent, [
      ViewerSurfaceFlingerComponent,
      SurfaceFlingerPropertyGroupsComponent,
    ]);
  }
}

describe('ViewerSurfaceFlingerComponent', () => {
  new ViewerSurfaceFlingerComponentTest().execute();
});
