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
  BuganizerAttachmentsDownloaded,
  BuganizerAttachmentsDownloadStart,
  WinscopeEvent,
  WinscopeEventType,
} from 'messaging/winscope_event';
import {
  EmitEvent,
  WinscopeEventEmitter,
} from 'messaging/winscope_event_emitter';
import {WinscopeEventListener} from 'messaging/winscope_event_listener';
import {
  MessageType,
  OpenBuganizerResponse,
  OpenRequest,
  WebCommandMessage,
} from './messages';

export class AbtChromeExtensionProtocol
  implements WinscopeEventEmitter, WinscopeEventListener
{
  static readonly ABT_EXTENSION_ID = 'mbbaofdfoekifkfpgehgffcpagbbjkmj';

  private emitEvent: EmitEvent = FunctionUtils.DO_NOTHING_ASYNC;

  setEmitEvent(callback: EmitEvent) {
    this.emitEvent = callback;
  }

  async onWinscopeEvent(event: WinscopeEvent) {
    await event.visit(WinscopeEventType.APP_INITIALIZED, async () => {
      const urlParams = new URLSearchParams(window.location.search);
      if (urlParams.get('source') !== 'openFromExtension' || !chrome) {
        return;
      }

      await this.emitEvent(new BuganizerAttachmentsDownloadStart());

      const openRequestMessage: OpenRequest = {
        action: MessageType.OPEN_REQUEST,
      };

      chrome.runtime.sendMessage(
        AbtChromeExtensionProtocol.ABT_EXTENSION_ID,
        openRequestMessage,
        async (message) => await this.onMessageReceived(message),
      );
    });
  }

  private async onMessageReceived(message: WebCommandMessage) {
    if (this.isOpenBuganizerResponseMessage(message)) {
      await this.onOpenBuganizerResponseMessageReceived(message);
    } else {
      console.warn(
        'ABT chrome extension protocol received unexpected message:',
        message,
      );
    }
  }

  private async onOpenBuganizerResponseMessageReceived(
    message: OpenBuganizerResponse,
  ) {
    console.log(
      'ABT chrome extension protocol received OpenBuganizerResponse message:',
      message,
    );

    if (message.attachments.length === 0) {
      console.warn('ABT chrome extension protocol received no attachments');
    }

    const filesBlobPromises = message.attachments.map(async (attachment) => {
      const fileQueryResponse = await fetch(attachment.objectUrl);
      const blob = await fileQueryResponse.blob();

      // Note: the received blob's media type is wrong. It is always set to "image/png".
      // Context: http://google3/javascript/closure/html/safeurl.js?g=0&l=256&rcl=273756987
      // Cloning the blob clears the media type.
      const file = new File([blob], attachment.name);

      return file;
    });

    const files = await Promise.all(filesBlobPromises);
    await this.emitEvent(new BuganizerAttachmentsDownloaded(files));
  }

  private isOpenBuganizerResponseMessage(
    message: WebCommandMessage,
  ): message is OpenBuganizerResponse {
    return message.action === MessageType.OPEN_BUGANIZER_RESPONSE;
  }
}
