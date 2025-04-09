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

import {DOMTestHelper} from 'test/unit/dom_test_utils';
import {AbstractHierarchyViewerComponentTest} from 'viewers/common/abstract_hierarchy_viewer_component_test';
import {ViewerViewCaptureComponent} from './viewer_view_capture_component';

class ViewerViewCaptureComponentTest extends AbstractHierarchyViewerComponentTest<ViewerViewCaptureComponent> {
  protected override readonly testRects = true;
  protected override readonly hierarchyTitle = 'HIERARCHY';
  protected override readonly propertiesTitle = 'PROPERTIES';
  protected override readonly rectsTitle = 'SKETCH';

  protected async setUpTestEnvironment(): Promise<
    [DOMTestHelper<ViewerViewCaptureComponent>, ViewerViewCaptureComponent]
  > {
    return this.initializeTestEnvironment(ViewerViewCaptureComponent);
  }
}

describe('ViewerViewCaptureComponent', () => {
  new ViewerViewCaptureComponentTest().execute();
});
