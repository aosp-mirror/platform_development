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

import {Timestamp} from 'common/time';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {AbstractPresenterInputMethod} from 'viewers/common/abstract_presenter_input_method';
import {ImeAdditionalProperties} from 'viewers/common/ime_additional_properties';
import {ImeUtils} from 'viewers/common/ime_utils';

export class PresenterInputMethodManagerService extends AbstractPresenterInputMethod {
  protected getHierarchyTableProperties() {
    const inputMethodManagerService = this.hierarchyPresenter
      .getCurrentHierarchyTreesForTrace(this.imeTrace)
      ?.at(0)
      ?.getChildByName('inputMethodManagerService');

    const curMethodId = inputMethodManagerService
      ?.getEagerPropertyByName('curMethodId')
      ?.formattedValue();
    const curFocusedWindowName = inputMethodManagerService
      ?.getEagerPropertyByName('curFocusedWindowName')
      ?.formattedValue();
    const lastImeTargetWindowName = inputMethodManagerService
      ?.getEagerPropertyByName('lastImeTargetWindowName')
      ?.formattedValue();
    const inputShown =
      inputMethodManagerService
        ?.getEagerPropertyByName('inputShown')
        ?.getValue() ?? false;

    return {
      ...new ImManagerServiceTableProperties(
        curMethodId,
        curFocusedWindowName,
        lastImeTargetWindowName,
        inputShown,
      ),
    };
  }

  protected override async getAdditionalProperties(
    wmEntry: HierarchyTreeNode | undefined,
    sfEntry: HierarchyTreeNode | undefined,
    wmEntryTimestamp: Timestamp | undefined,
    sfEntryTimestamp: Timestamp | undefined,
  ): Promise<ImeAdditionalProperties> {
    return new ImeAdditionalProperties(
      wmEntry
        ? ImeUtils.processWindowManagerTraceEntry(wmEntry, wmEntryTimestamp)
        : undefined,
      undefined,
    );
  }
}

class ImManagerServiceTableProperties {
  constructor(
    public inputMethodId: string | undefined,
    public curFocusedWindow: string | undefined,
    public lastImeTargetWindow: string | undefined,
    public inputShown: boolean,
  ) {}
}
