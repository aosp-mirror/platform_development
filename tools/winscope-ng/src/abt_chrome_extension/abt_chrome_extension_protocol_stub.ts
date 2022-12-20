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

import {
  OnBugAttachmentsDownloadStart,
  OnBugAttachmentsReceived,
  AbtChromeExtensionProtocolDependencyInversion
} from "./abt_chrome_extension_protocol_dependency_inversion";
import {FunctionUtils} from "../common/utils/function_utils";

export class AbtChromeExtensionProtocolStub implements AbtChromeExtensionProtocolDependencyInversion {
  onBugAttachmentsDownloadStart: OnBugAttachmentsDownloadStart = FunctionUtils.DO_NOTHING;
  onBugAttachmentsReceived: OnBugAttachmentsReceived = FunctionUtils.DO_NOTHING_ASYNC;

  setOnBugAttachmentsDownloadStart(callback: OnBugAttachmentsDownloadStart) {
    this.onBugAttachmentsDownloadStart = callback;
  }

  setOnBugAttachmentsReceived(callback: OnBugAttachmentsReceived) {
    this.onBugAttachmentsReceived = callback;
  }

  run() {
    // do nothing
  }
}
