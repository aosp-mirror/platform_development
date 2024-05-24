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
import root from 'protos/viewcapture/latest/json';

export const ExportedData = TamperedMessageType.tamper(
  root.lookupType('com.android.app.viewcapture.data.ExportedData'),
);

export const NodeField = assertDefined(
  ExportedData.fields['windowData'].tamperedMessageType?.fields['frameData']
    .tamperedMessageType,
).fields['node'];
