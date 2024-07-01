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
import {DENYLIST_PROPERTIES} from 'parsers/window_manager/denylist_properties';
import {EAGER_PROPERTIES} from 'parsers/window_manager/eager_properties';
import {ProtoType} from 'parsers/window_manager/proto_type';
import {TamperedProtos} from 'parsers/window_manager/tampered_protos';
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
  private readonly LISTS = new Map<ProtoType, OperationLists>([
    [
      ProtoType.WindowManagerService,
      {
        common: [
          new SetFormatters(this.tamperedProtos.windowManagerServiceField),
          new TranslateIntDef(this.tamperedProtos.windowManagerServiceField),
        ],
        eager: [
          new AddDefaults(
            this.tamperedProtos.windowManagerServiceField,
            EAGER_PROPERTIES.get(ProtoType.WindowManagerService),
          ),
        ],
        lazy: [
          new AddDefaults(
            this.tamperedProtos.windowManagerServiceField,
            undefined,
            DENYLIST_PROPERTIES.get(ProtoType.WindowManagerService),
          ),
        ],
      },
    ],

    [
      ProtoType.RootWindowContainer,
      {
        common: [
          new SetFormatters(this.tamperedProtos.rootWindowContainerField),
          new TranslateIntDef(this.tamperedProtos.rootWindowContainerField),
        ],
        eager: [
          new AddDefaults(
            this.tamperedProtos.rootWindowContainerField,
            EAGER_PROPERTIES.get(ProtoType.RootWindowContainer),
          ),
        ],
        lazy: [
          new AddDefaults(
            this.tamperedProtos.rootWindowContainerField,
            undefined,
            DENYLIST_PROPERTIES.get(ProtoType.RootWindowContainer),
          ),
        ],
      },
    ],

    [
      ProtoType.WindowContainer,
      {
        common: [
          new SetFormatters(this.tamperedProtos.windowContainerField),
          new TranslateIntDef(this.tamperedProtos.windowContainerField),
        ],
        eager: [
          new AddDefaults(
            this.tamperedProtos.windowContainerField,
            EAGER_PROPERTIES.get(ProtoType.WindowContainer),
          ),
          new AddIsVisible(),
        ],
        lazy: [
          new AddDefaults(
            this.tamperedProtos.windowContainerField,
            undefined,
            DENYLIST_PROPERTIES.get(ProtoType.WindowContainer),
          ),
        ],
      },
    ],

    [
      ProtoType.DisplayContent,
      {
        common: [
          new SetFormatters(this.tamperedProtos.displayContentField),
          new TranslateIntDef(this.tamperedProtos.displayContentField),
        ],
        eager: [
          new AddDefaults(
            this.tamperedProtos.displayContentField,
            EAGER_PROPERTIES.get(ProtoType.DisplayContent),
          ),
        ],
        lazy: [
          new AddDefaults(
            this.tamperedProtos.displayContentField,
            undefined,
            DENYLIST_PROPERTIES.get(ProtoType.DisplayContent),
          ),
        ],
      },
    ],

    [
      ProtoType.DisplayArea,
      {
        common: [
          new SetFormatters(this.tamperedProtos.displayAreaField),
          new TranslateIntDef(this.tamperedProtos.displayAreaField),
        ],
        eager: [
          new AddDefaults(
            this.tamperedProtos.displayAreaField,
            EAGER_PROPERTIES.get(ProtoType.DisplayArea),
          ),
        ],
        lazy: [
          new AddDefaults(
            this.tamperedProtos.displayAreaField,
            undefined,
            DENYLIST_PROPERTIES.get(ProtoType.DisplayArea),
          ),
        ],
      },
    ],

    [
      ProtoType.Task,
      {
        common: [
          new SetFormatters(this.tamperedProtos.taskField),
          new TranslateIntDef(this.tamperedProtos.taskField),
        ],
        eager: [
          new AddDefaults(
            this.tamperedProtos.taskField,
            EAGER_PROPERTIES.get(ProtoType.Task),
          ),
        ],
        lazy: [
          new AddDefaults(
            this.tamperedProtos.taskField,
            undefined,
            DENYLIST_PROPERTIES.get(ProtoType.Task),
          ),
        ],
      },
    ],

    [
      ProtoType.Activity,
      {
        common: [
          new SetFormatters(this.tamperedProtos.activityField),
          new TranslateIntDef(this.tamperedProtos.activityField),
        ],
        eager: [
          new AddDefaults(
            this.tamperedProtos.activityField,
            EAGER_PROPERTIES.get(ProtoType.Activity),
          ),
          new AddIsVisible(),
        ],
        lazy: [
          new AddDefaults(
            this.tamperedProtos.activityField,
            undefined,
            DENYLIST_PROPERTIES.get(ProtoType.Activity),
          ),
        ],
      },
    ],

    [
      ProtoType.WindowToken,
      {
        common: [
          new SetFormatters(this.tamperedProtos.windowTokenField),
          new TranslateIntDef(this.tamperedProtos.windowTokenField),
        ],
        eager: [
          new AddDefaults(
            this.tamperedProtos.windowTokenField,
            EAGER_PROPERTIES.get(ProtoType.WindowToken),
          ),
        ],
        lazy: [
          new AddDefaults(
            this.tamperedProtos.windowTokenField,
            undefined,
            DENYLIST_PROPERTIES.get(ProtoType.WindowToken),
          ),
        ],
      },
    ],

    [
      ProtoType.WindowState,
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
            EAGER_PROPERTIES.get(ProtoType.WindowState),
          ),
          new AddWindowType(),
          new AddIsVisible(),
        ],
        lazy: [
          new AddDefaults(
            this.tamperedProtos.windowStateField,
            undefined,
            DENYLIST_PROPERTIES.get(ProtoType.WindowState),
          ),
        ],
      },
    ],

    [
      ProtoType.TaskFragment,
      {
        common: [
          new SetFormatters(this.tamperedProtos.taskFragmentField),
          new TranslateIntDef(this.tamperedProtos.taskFragmentField),
        ],
        eager: [
          new AddDefaults(
            this.tamperedProtos.taskFragmentField,
            EAGER_PROPERTIES.get(ProtoType.TaskFragment),
          ),
        ],
        lazy: [
          new AddDefaults(
            this.tamperedProtos.taskFragmentField,
            undefined,
            DENYLIST_PROPERTIES.get(ProtoType.TaskFragment),
          ),
        ],
      },
    ],
  ]);

  constructor(private readonly tamperedProtos: TamperedProtos) {}

  get(type: ProtoType): OperationLists {
    return assertDefined(this.LISTS.get(type));
  }
}
