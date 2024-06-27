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

import {assertDefined} from 'common/assert_utils';
import {AddDefaults} from 'parsers/operations/add_defaults';
import {SetFormatters} from 'parsers/operations/set_formatters';
import {TranslateIntDef} from 'parsers/operations/translate_intdef';
import {WM_DENYLIST_PROPERTIES} from 'parsers/window_manager/wm_denylist_properties';
import {WM_EAGER_PROPERTIES} from 'parsers/window_manager/wm_eager_properties';
import {WmProtoType} from 'parsers/window_manager/wm_proto_type';
import {WmTamperedProtos} from 'parsers/window_manager/wm_tampered_protos';
import {RECT_FORMATTER} from 'trace/tree_node/formatters';
import {Operation} from 'trace/tree_node/operations/operation';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {AddIsVisible} from './add_is_visible';
import {AddWindowType} from './add_window_type';

export interface OperationLists {
  common: Array<Operation<PropertyTreeNode>>;
  eager: Array<Operation<PropertyTreeNode>>;
  lazy: Array<Operation<PropertyTreeNode>>;
}

export class WmOperationLists {
  private readonly LISTS = new Map<WmProtoType, OperationLists>([
    [
      WmProtoType.WindowManagerService,
      {
        common: [
          new SetFormatters(this.tamperedProtos.windowManagerServiceField),
          new TranslateIntDef(this.tamperedProtos.windowManagerServiceField),
        ],
        eager: [
          new AddDefaults(
            this.tamperedProtos.windowManagerServiceField,
            WM_EAGER_PROPERTIES.get(WmProtoType.WindowManagerService),
          ),
        ],
        lazy: [
          new AddDefaults(
            this.tamperedProtos.windowManagerServiceField,
            undefined,
            WM_DENYLIST_PROPERTIES.get(WmProtoType.WindowManagerService),
          ),
        ],
      },
    ],

    [
      WmProtoType.RootWindowContainer,
      {
        common: [
          new SetFormatters(this.tamperedProtos.rootWindowContainerField),
          new TranslateIntDef(this.tamperedProtos.rootWindowContainerField),
        ],
        eager: [
          new AddDefaults(
            this.tamperedProtos.rootWindowContainerField,
            WM_EAGER_PROPERTIES.get(WmProtoType.RootWindowContainer),
          ),
        ],
        lazy: [
          new AddDefaults(
            this.tamperedProtos.rootWindowContainerField,
            undefined,
            WM_DENYLIST_PROPERTIES.get(WmProtoType.RootWindowContainer),
          ),
        ],
      },
    ],

    [
      WmProtoType.WindowContainer,
      {
        common: [
          new SetFormatters(this.tamperedProtos.windowContainerField),
          new TranslateIntDef(this.tamperedProtos.windowContainerField),
        ],
        eager: [
          new AddDefaults(
            this.tamperedProtos.windowContainerField,
            WM_EAGER_PROPERTIES.get(WmProtoType.WindowContainer),
          ),
          new AddIsVisible(),
        ],
        lazy: [
          new AddDefaults(
            this.tamperedProtos.windowContainerField,
            undefined,
            WM_DENYLIST_PROPERTIES.get(WmProtoType.WindowContainer),
          ),
        ],
      },
    ],

    [
      WmProtoType.DisplayContent,
      {
        common: [
          new SetFormatters(this.tamperedProtos.displayContentField),
          new TranslateIntDef(this.tamperedProtos.displayContentField),
        ],
        eager: [
          new AddDefaults(
            this.tamperedProtos.displayContentField,
            WM_EAGER_PROPERTIES.get(WmProtoType.DisplayContent),
          ),
        ],
        lazy: [
          new AddDefaults(
            this.tamperedProtos.displayContentField,
            undefined,
            WM_DENYLIST_PROPERTIES.get(WmProtoType.DisplayContent),
          ),
        ],
      },
    ],

    [
      WmProtoType.DisplayArea,
      {
        common: [
          new SetFormatters(this.tamperedProtos.displayAreaField),
          new TranslateIntDef(this.tamperedProtos.displayAreaField),
        ],
        eager: [
          new AddDefaults(
            this.tamperedProtos.displayAreaField,
            WM_EAGER_PROPERTIES.get(WmProtoType.DisplayArea),
          ),
        ],
        lazy: [
          new AddDefaults(
            this.tamperedProtos.displayAreaField,
            undefined,
            WM_DENYLIST_PROPERTIES.get(WmProtoType.DisplayArea),
          ),
        ],
      },
    ],

    [
      WmProtoType.Task,
      {
        common: [
          new SetFormatters(this.tamperedProtos.taskField),
          new TranslateIntDef(this.tamperedProtos.taskField),
        ],
        eager: [
          new AddDefaults(
            this.tamperedProtos.taskField,
            WM_EAGER_PROPERTIES.get(WmProtoType.Task),
          ),
        ],
        lazy: [
          new AddDefaults(
            this.tamperedProtos.taskField,
            undefined,
            WM_DENYLIST_PROPERTIES.get(WmProtoType.Task),
          ),
        ],
      },
    ],

    [
      WmProtoType.Activity,
      {
        common: [
          new SetFormatters(this.tamperedProtos.activityField),
          new TranslateIntDef(this.tamperedProtos.activityField),
        ],
        eager: [
          new AddDefaults(
            this.tamperedProtos.activityField,
            WM_EAGER_PROPERTIES.get(WmProtoType.Activity),
          ),
          new AddIsVisible(),
        ],
        lazy: [
          new AddDefaults(
            this.tamperedProtos.activityField,
            undefined,
            WM_DENYLIST_PROPERTIES.get(WmProtoType.Activity),
          ),
        ],
      },
    ],

    [
      WmProtoType.WindowToken,
      {
        common: [
          new SetFormatters(this.tamperedProtos.windowTokenField),
          new TranslateIntDef(this.tamperedProtos.windowTokenField),
        ],
        eager: [
          new AddDefaults(
            this.tamperedProtos.windowTokenField,
            WM_EAGER_PROPERTIES.get(WmProtoType.WindowToken),
          ),
        ],
        lazy: [
          new AddDefaults(
            this.tamperedProtos.windowTokenField,
            undefined,
            WM_DENYLIST_PROPERTIES.get(WmProtoType.WindowToken),
          ),
        ],
      },
    ],

    [
      WmProtoType.WindowState,
      {
        common: [
          new SetFormatters(
            this.tamperedProtos.windowStateField,
            new Map([
              ['containingFrame', RECT_FORMATTER],
              ['parentFrame', RECT_FORMATTER],
            ]),
          ),
          new TranslateIntDef(this.tamperedProtos.windowStateField),
        ],
        eager: [
          new AddDefaults(
            this.tamperedProtos.windowStateField,
            WM_EAGER_PROPERTIES.get(WmProtoType.WindowState),
          ),
          new AddWindowType(),
          new AddIsVisible(),
        ],
        lazy: [
          new AddDefaults(
            this.tamperedProtos.windowStateField,
            undefined,
            WM_DENYLIST_PROPERTIES.get(WmProtoType.WindowState),
          ),
        ],
      },
    ],

    [
      WmProtoType.TaskFragment,
      {
        common: [
          new SetFormatters(this.tamperedProtos.taskFragmentField),
          new TranslateIntDef(this.tamperedProtos.taskFragmentField),
        ],
        eager: [
          new AddDefaults(
            this.tamperedProtos.taskFragmentField,
            WM_EAGER_PROPERTIES.get(WmProtoType.TaskFragment),
          ),
        ],
        lazy: [
          new AddDefaults(
            this.tamperedProtos.taskFragmentField,
            undefined,
            WM_DENYLIST_PROPERTIES.get(WmProtoType.TaskFragment),
          ),
        ],
      },
    ],
  ]);

  constructor(private readonly tamperedProtos: WmTamperedProtos) {}

  get(type: WmProtoType): OperationLists {
    return assertDefined(this.LISTS.get(type));
  }
}
