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
import {TraceTreeNode} from 'trace/trace_tree_node';
import {ImeAdditionalProperties} from 'viewers/common/ime_additional_properties';
import {ImeUtils} from 'viewers/common/ime_utils';
import {PresenterInputMethod} from 'viewers/common/presenter_input_method';

export class PresenterInputMethodManagerService extends PresenterInputMethod {
  protected updateHierarchyTableProperties() {
    return {
      ...new ImManagerServiceTableProperties(
        this.entry?.obj?.inputMethodManagerService?.curMethodId,
        this.entry?.obj?.inputMethodManagerService?.curFocusedWindowName,
        this.entry?.obj?.inputMethodManagerService?.lastImeTargetWindowName,
        this.entry?.obj?.inputMethodManagerService?.inputShown ?? false
      ),
    };
  }

  protected override getAdditionalProperties(
    wmEntry: TraceTreeNode | undefined,
    sfEntry: TraceTreeNode | undefined
  ) {
    return new ImeAdditionalProperties(
      wmEntry ? ImeUtils.processWindowManagerTraceEntry(wmEntry) : undefined,
      undefined
    );
  }
}

class ImManagerServiceTableProperties {
  constructor(
    public inputMethodId: string | undefined,
    public curFocusedWindow: string | undefined,
    public lastImeTargetWindow: string | undefined,
    public inputShown: boolean
  ) {}
}
