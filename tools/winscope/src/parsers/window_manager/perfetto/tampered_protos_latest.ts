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
import {TamperedMessageType} from 'parsers/tampered_message_type';
import {TamperedProtos} from 'parsers/window_manager/tampered_protos';
import root from 'protos/windowmanager/latest/json';

const Wrapper = TamperedMessageType.tamper(
  root.lookupType('perfetto.protos.Wrapper'),
);

const entryField = assertDefined(Wrapper.fields['windowmanagerTraceEntry']);

const windowManagerServiceField = assertDefined(entryField.tamperedMessageType)
  .fields['windowManagerService'];

const rootWindowContainerField = assertDefined(
  windowManagerServiceField.tamperedMessageType,
).fields['rootWindowContainer'];

const windowContainerField = assertDefined(
  rootWindowContainerField.tamperedMessageType,
).fields['windowContainer'];

const windowContainerChildField = assertDefined(
  windowContainerField.tamperedMessageType,
).fields['children'];

export const TAMPERED_PROTOS_LATEST: TamperedProtos = {
  entryField,

  windowManagerServiceField,

  rootWindowContainerField: assertDefined(
    windowManagerServiceField.tamperedMessageType,
  ).fields['rootWindowContainer'],

  windowContainerField: assertDefined(
    rootWindowContainerField.tamperedMessageType,
  ).fields['windowContainer'],

  windowContainerChildField: assertDefined(
    windowContainerField.tamperedMessageType,
  ).fields['children'],

  displayContentField: assertDefined(
    windowContainerChildField.tamperedMessageType,
  ).fields['displayContent'],

  displayAreaField: assertDefined(windowContainerChildField.tamperedMessageType)
    .fields['displayArea'],

  taskField: assertDefined(windowContainerChildField.tamperedMessageType)
    .fields['task'],

  activityField: assertDefined(windowContainerChildField.tamperedMessageType)
    .fields['activity'],

  windowTokenField: assertDefined(windowContainerChildField.tamperedMessageType)
    .fields['windowToken'],

  windowStateField: assertDefined(windowContainerChildField.tamperedMessageType)
    .fields['window'],

  taskFragmentField: assertDefined(
    windowContainerChildField.tamperedMessageType,
  ).fields['taskFragment'],
};
