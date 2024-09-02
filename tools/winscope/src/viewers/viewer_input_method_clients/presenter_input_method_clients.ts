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
import {PersistentStoreProxy} from 'common/persistent_store_proxy';
import {Store} from 'common/store';
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceType} from 'trace/trace_type';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {NotifyHierarchyViewCallbackType} from 'viewers/common/abstract_hierarchy_viewer_presenter';
import {AbstractPresenterInputMethod} from 'viewers/common/abstract_presenter_input_method';
import {VISIBLE_CHIP} from 'viewers/common/chip';
import {HierarchyPresenter} from 'viewers/common/hierarchy_presenter';
import {ImeUiData} from 'viewers/common/ime_ui_data';
import {UpdateSfSubtreeDisplayNames} from 'viewers/common/operations/update_sf_subtree_display_names';
import {TableProperties} from 'viewers/common/table_properties';
import {UserOptions} from 'viewers/common/user_options';
import {UpdateDisplayNames} from './operations/update_display_names';

export class PresenterInputMethodClients extends AbstractPresenterInputMethod {
  protected override hierarchyPresenter = new HierarchyPresenter(
    PersistentStoreProxy.new<UserOptions>(
      'ImeHierarchyOptions',
      {
        simplifyNames: {
          name: 'Simplify names',
          enabled: true,
        },
        showOnlyVisible: {
          name: 'Show only',
          chip: VISIBLE_CHIP,
          enabled: false,
        },
        flat: {
          name: 'Flat',
          enabled: false,
        },
      },
      this.storage,
    ),
    [],
    true,
    false,
    this.getHierarchyTreeNameStrategy,
    [
      [TraceType.SURFACE_FLINGER, [new UpdateSfSubtreeDisplayNames()]],
      [TraceType.INPUT_METHOD_CLIENTS, [new UpdateDisplayNames()]],
    ],
  );
  constructor(
    trace: Trace<HierarchyTreeNode>,
    traces: Traces,
    storage: Store,
    notifyViewCallback: NotifyHierarchyViewCallbackType<ImeUiData>,
  ) {
    super(trace, traces, storage, notifyViewCallback);
  }

  protected override getHierarchyTableProperties(): TableProperties {
    const client = this.hierarchyPresenter
      .getCurrentHierarchyTreesForTrace(this.imeTrace)
      ?.at(0)
      ?.getChildByName('client');
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
