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

import {DOMTestHelper} from 'test/unit/dom_test_utils';
import {AbstractHierarchyViewerComponentTest} from 'viewers/common/abstract_hierarchy_viewer_component_test';
import {ImeAdditionalPropertiesComponent} from './ime_additional_properties_component';
import {ViewerInputMethodComponent} from './viewer_input_method_component';

class ViewerInputMethodComponentTest extends AbstractHierarchyViewerComponentTest<ViewerInputMethodComponent> {
  protected override readonly testRects = false;
  protected override readonly hierarchyTitle = 'HIERARCHY';
  protected override readonly propertiesTitle = 'PROPERTIES';

  protected override executeSpecializedTests() {
    describe('Specialized tests', () => {
      let dom: DOMTestHelper<ViewerInputMethodComponent>;
      let component: ViewerInputMethodComponent;

      beforeEach(async () => {
        [dom, component] = await this.setUpTestEnvironment();
      });

      it('creates additional properties view', () => {
        expect(dom.find('.ime-additional-properties')).toBeDefined();
      });

      it('handles ime additional properties section collapse/expand', () => {
        dom.checkSectionCollapseAndExpand(
          '.ime-additional-properties',
          'WM & SF PROPERTIES',
        );
      });
    });
  }

  protected async setUpTestEnvironment(): Promise<
    [DOMTestHelper<ViewerInputMethodComponent>, ViewerInputMethodComponent]
  > {
    return this.initializeTestEnvironment(ViewerInputMethodComponent, [
      ImeAdditionalPropertiesComponent,
    ]);
  }
}

describe('ViewerInputMethodComponent', () => {
  new ViewerInputMethodComponentTest().execute();
});
