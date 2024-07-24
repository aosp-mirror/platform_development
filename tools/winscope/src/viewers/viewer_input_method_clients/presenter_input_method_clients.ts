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
import {PresenterInputMethod} from 'viewers/common/presenter_input_method';

export class PresenterInputMethodClients extends PresenterInputMethod {
  protected updateHierarchyTableProperties() {
    const client = this.entry?.getChildByName('client');
    const curId = client
      ?.getEagerPropertyByName('inputMethodManager')
      ?.getChildByName('curId')
      ?.formattedValue();
    const packageName = client
      ?.getEagerPropertyByName('editorInfo')
      ?.getChildByName('packageName')
      ?.formattedValue();
    return {
      ...new ImClientsTableProperties(curId, packageName),
    };
  }
}

class ImClientsTableProperties {
  constructor(
    public inputMethodId: string | undefined,
    public packageName: string | undefined,
  ) {}
}
