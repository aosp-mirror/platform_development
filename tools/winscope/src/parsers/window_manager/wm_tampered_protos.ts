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
import root from 'protos/windowmanager/latest/json';

export const WindowManagerTraceFileProto = TamperedMessageType.tamper(
  root.lookupType('com.android.server.wm.WindowManagerTraceFileProto'),
);

export const WindowManagerServiceField = assertDefined(
  WindowManagerTraceFileProto.fields['entry'].tamperedMessageType,
).fields['windowManagerService'];

export const RootWindowContainerField = assertDefined(
  WindowManagerServiceField.tamperedMessageType,
).fields['rootWindowContainer'];

export const WindowContainerField = assertDefined(
  RootWindowContainerField.tamperedMessageType,
).fields['windowContainer'];

export const WindowContainerChildField = assertDefined(
  WindowContainerField.tamperedMessageType,
).fields['children'];

export const DisplayContentField = assertDefined(
  WindowContainerChildField.tamperedMessageType,
).fields['displayContent'];

export const DisplayAreaField = assertDefined(
  WindowContainerChildField.tamperedMessageType,
).fields['displayArea'];

export const TaskField = assertDefined(
  WindowContainerChildField.tamperedMessageType,
).fields['task'];

export const ActivityField = assertDefined(
  WindowContainerChildField.tamperedMessageType,
).fields['activity'];

export const WindowTokenField = assertDefined(
  WindowContainerChildField.tamperedMessageType,
).fields['windowToken'];

export const WindowStateField = assertDefined(
  WindowContainerChildField.tamperedMessageType,
).fields['window'];

export const TaskFragmentField = assertDefined(
  WindowContainerChildField.tamperedMessageType,
).fields['taskFragment'];
