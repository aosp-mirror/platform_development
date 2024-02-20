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
import {TimestampType} from 'common/time';
import {
  RemoteToolBugreportReceived,
  RemoteToolTimestampReceived,
  WinscopeEvent,
  WinscopeEventType,
} from 'messaging/winscope_event';
import {
  EmitEvent,
  WinscopeEventEmitter,
} from 'messaging/winscope_event_emitter';
import {WinscopeEventListener} from 'messaging/winscope_event_listener';
import {
  Message,
  MessageBugReport,
  MessagePong,
  MessageTimestamp,
  MessageType,
} from './messages';
import {OriginAllowList} from './origin_allow_list';

class RemoteTool {
  constructor(readonly window: Window, readonly origin: string) {}
}

export class CrossToolProtocol
  implements WinscopeEventEmitter, WinscopeEventListener
{
  private remoteTool?: RemoteTool;
  private emitEvent: EmitEvent = FunctionUtils.DO_NOTHING_ASYNC;

  constructor() {
    window.addEventListener('message', async (event) => {
      await this.onMessageReceived(event);
    });
  }

  setEmitEvent(callback: EmitEvent) {
    this.emitEvent = callback;
  }

  async onWinscopeEvent(event: WinscopeEvent) {
    await event.visit(
      WinscopeEventType.TRACE_POSITION_UPDATE,
      async (event) => {
        if (!this.remoteTool) {
          return;
        }

        const timestamp = event.position.timestamp;
        if (timestamp.getType() !== TimestampType.REAL) {
          console.warn(
            'Cannot propagate timestamp change to remote tool.' +
              ` Remote tool expects timestamp type ${TimestampType.REAL},` +
              ` but Winscope wants to notify timestamp type ${timestamp.getType()}.`,
          );
          return;
        }

        const message = new MessageTimestamp(timestamp.getValueNs());
        this.remoteTool.window.postMessage(message, this.remoteTool.origin);
        console.log('Cross-tool protocol sent timestamp message:', message);
      },
    );
  }

  private async onMessageReceived(event: MessageEvent) {
    if (!OriginAllowList.isAllowed(event.origin)) {
      return;
    }

    const message = event.data as Message;
    if (message.type === undefined) {
      return;
    }

    if (!this.remoteTool) {
      this.remoteTool = new RemoteTool(event.source as Window, event.origin);
    }

    switch (message.type) {
      case MessageType.PING:
        console.log('Cross-tool protocol received ping message:', message);
        (event.source as Window).postMessage(new MessagePong(), event.origin);
        break;
      case MessageType.PONG:
        console.log(
          'Cross-tool protocol received unexpected pong message:',
          message,
        );
        break;
      case MessageType.BUGREPORT:
        console.log('Cross-tool protocol received bugreport message:', message);
        await this.onMessageBugreportReceived(message as MessageBugReport);
        console.log(
          'Cross-tool protocol processes bugreport message:',
          message,
        );
        break;
      case MessageType.TIMESTAMP:
        console.log('Cross-tool protocol received timestamp message:', message);
        await this.onMessageTimestampReceived(message as MessageTimestamp);
        console.log(
          'Cross-tool protocol processed timestamp message:',
          message,
        );
        break;
      case MessageType.FILES:
        console.log(
          'Cross-tool protocol received unexpected files message',
          message,
        );
        break;
      default:
        console.log(
          'Cross-tool protocol received unsupported message type:',
          message,
        );
        break;
    }
  }

  private async onMessageBugreportReceived(message: MessageBugReport) {
    await this.emitEvent(
      new RemoteToolBugreportReceived(message.file, message.timestampNs),
    );
  }

  private async onMessageTimestampReceived(message: MessageTimestamp) {
    await this.emitEvent(new RemoteToolTimestampReceived(message.timestampNs));
  }
}
