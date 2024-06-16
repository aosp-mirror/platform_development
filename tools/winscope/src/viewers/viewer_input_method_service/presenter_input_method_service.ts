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
import {AbstractPresenterInputMethod} from 'viewers/common/abstract_presenter_input_method';

export class PresenterInputMethodService extends AbstractPresenterInputMethod {
  protected getHierarchyTableProperties() {
    const inputMethodService = this.hierarchyPresenter
      .getCurrentHierarchyTreesForTrace(this.imeTrace)
      ?.at(0)
      ?.getChildByName('inputMethodService');
    const windowVisible =
      inputMethodService?.getEagerPropertyByName('windowVisible')?.getValue() ??
      false;
    const decorViewVisible =
      inputMethodService
        ?.getEagerPropertyByName('decorViewVisible')
        ?.getValue() ?? false;
    const packageName = inputMethodService
      ?.getEagerPropertyByName('inputEditorInfo')
      ?.getChildByName('packageName')
      ?.formattedValue();

    return {
      ...new ImServiceTableProperties(
        windowVisible,
        decorViewVisible,
        packageName,
      ),
    };
  }
}

class ImServiceTableProperties {
  constructor(
    public windowVisible: boolean,
    public decorViewVisible: boolean,
    public packageName: string | undefined,
  ) {}
}
