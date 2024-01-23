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
  root.lookupType('com.android.server.wm.WindowManagerTraceFileProto')
);

export const WindowManagerServiceField = assertDefined(
  WindowManagerTraceFileProto.fields['entry'].tamperedMessageType
).fields['windowManagerService'];

export const WindowManagerServiceType = assertDefined(
  WindowManagerServiceField.tamperedMessageType
);

export const RootWindowContainerField = WindowManagerServiceType.fields['rootWindowContainer'];
export const RootWindowContainerType = assertDefined(RootWindowContainerField.tamperedMessageType);

export const WindowContainerField = RootWindowContainerType.fields['windowContainer'];
export const WindowContainerType = assertDefined(WindowContainerField.tamperedMessageType);

export const WindowContainerChildField = WindowContainerType.fields['children'];
export const WindowContainerChildType = assertDefined(
  WindowContainerChildField.tamperedMessageType
);

export const DisplayContentField = WindowContainerChildType.fields['displayContent'];
export const DisplayContentType = assertDefined(DisplayContentField.tamperedMessageType);

export const DisplayAreaField = WindowContainerChildType.fields['displayArea'];
export const DisplayAreaType = assertDefined(DisplayAreaField.tamperedMessageType);

export const TaskField = WindowContainerChildType.fields['task'];
export const TaskType = assertDefined(TaskField.tamperedMessageType);

export const ActivityField = WindowContainerChildType.fields['activity'];
export const ActivityType = assertDefined(ActivityField.tamperedMessageType);

export const WindowTokenField = WindowContainerChildType.fields['windowToken'];
export const WindowTokenType = assertDefined(WindowTokenField.tamperedMessageType);

export const WindowStateField = WindowContainerChildType.fields['window'];
export const WindowStateType = assertDefined(WindowStateField.tamperedMessageType);

export const TaskFragmentField = WindowContainerChildType.fields['taskFragment'];
export const TaskFragmentType = assertDefined(TaskFragmentField.tamperedMessageType);
