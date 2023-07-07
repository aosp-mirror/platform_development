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

import {FunctionUtils} from 'common/function_utils';
import {
  BuganizerAttachmentsDownloadEmitter,
  OnBuganizerAttachmentsDownloaded,
  OnBuganizerAttachmentsDownloadStart,
} from 'interfaces/buganizer_attachments_download_emitter';
import {Runnable} from 'interfaces/runnable';

export class AbtChromeExtensionProtocolStub
  implements BuganizerAttachmentsDownloadEmitter, Runnable
{
  onBuganizerAttachmentsDownloadStart: OnBuganizerAttachmentsDownloadStart =
    FunctionUtils.DO_NOTHING;
  onBuganizerAttachmentsDownloaded: OnBuganizerAttachmentsDownloaded =
    FunctionUtils.DO_NOTHING_ASYNC;

  setOnBuganizerAttachmentsDownloadStart(callback: OnBuganizerAttachmentsDownloadStart) {
    this.onBuganizerAttachmentsDownloadStart = callback;
  }

  setOnBuganizerAttachmentsDownloaded(callback: OnBuganizerAttachmentsDownloaded) {
    this.onBuganizerAttachmentsDownloaded = callback;
  }

  run() {
    // do nothing
  }
}
