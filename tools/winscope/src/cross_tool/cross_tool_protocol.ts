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

import {assertDefined, assertUnreachable} from 'common/assert_utils';
import {FunctionUtils} from 'common/function_utils';
import {Timestamp} from 'common/time';
import {RemoteToolTimestampConverter} from 'common/timestamp_converter';
import {
  RemoteToolFilesReceived,
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
  MessageFiles,
  MessagePong,
  MessageTimestamp,
  MessageType,
  TimestampType,
} from './messages';
import {OriginAllowList} from './origin_allow_list';

class RemoteTool {
  timestampType?: TimestampType;

  constructor(readonly window: Window, readonly origin: string) {}
}

export class CrossToolProtocol
  implements WinscopeEventEmitter, WinscopeEventListener
{
  private remoteTool?: RemoteTool;
  private emitEvent: EmitEvent = FunctionUtils.DO_NOTHING_ASYNC;
  private timestampConverter: RemoteToolTimestampConverter;
  private allowTimestampSync = true;

  constructor(timestampConverter: RemoteToolTimestampConverter) {
    this.timestampConverter = timestampConverter;

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
        if (
          !this.remoteTool ||
          !this.remoteTool.timestampType ||
          !this.allowTimestampSync
        ) {
          return;
        }

        const timestampNs = this.getTimestampNsForRemoteTool(
          event.position.timestamp,
        );
        if (timestampNs === undefined) {
          return;
        }

        const message = new MessageTimestamp(
          timestampNs,
          this.remoteTool.timestampType,
        );
        this.remoteTool.window.postMessage(message, this.remoteTool.origin);
        console.log('Cross-tool protocol sent timestamp message:', message);
      },
    );
  }

  isConnected() {
    return this.remoteTool !== undefined;
  }

  setAllowTimestampSync(value: boolean) {
    this.allowTimestampSync = value;
  }

  getAllowTimestampSync() {
    return this.allowTimestampSync;
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
          'Cross-tool protocol processed bugreport message:',
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
        console.log('Cross-tool protocol received files message:', message);
        await this.onMessageFilesReceived(message as MessageFiles);
        console.log('Cross-tool protocol processed files message:', message);
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
    this.setRemoteToolTimestampTypeIfNeeded(message.timestampType);
    const deferredTimestamp = this.makeDeferredTimestampForWinscope(
      message.timestampNs,
    );
    await this.emitEvent(
      new RemoteToolFilesReceived([message.file], deferredTimestamp),
    );
  }

  private async onMessageFilesReceived(message: MessageFiles) {
    this.setRemoteToolTimestampTypeIfNeeded(message.timestampType);
    const deferredTimestamp = this.makeDeferredTimestampForWinscope(
      message.timestampNs,
    );
    await this.emitEvent(
      new RemoteToolFilesReceived(message.files, deferredTimestamp),
    );
  }

  private async onMessageTimestampReceived(message: MessageTimestamp) {
    if (!this.allowTimestampSync) {
      return;
    }
    this.setRemoteToolTimestampTypeIfNeeded(message.timestampType);
    const deferredTimestamp = this.makeDeferredTimestampForWinscope(
      message.timestampNs,
    );
    await this.emitEvent(
      new RemoteToolTimestampReceived(assertDefined(deferredTimestamp)),
    );
  }

  private setRemoteToolTimestampTypeIfNeeded(type: TimestampType | undefined) {
    const remoteTool = assertDefined(this.remoteTool);

    if (remoteTool.timestampType !== undefined) {
      return;
    }

    // Default to CLOCK_REALTIME for backward compatibility.
    // The initial protocol's version didn't provide an explicit timestamp type
    // and all timestamps were supposed to be CLOCK_REALTIME.
    remoteTool.timestampType = type ?? TimestampType.CLOCK_REALTIME;
  }

  private getTimestampNsForRemoteTool(
    timestamp: Timestamp,
  ): bigint | undefined {
    const timestampType = this.remoteTool?.timestampType;
    switch (timestampType) {
      case undefined:
        return undefined;
      case TimestampType.UNKNOWN:
        return undefined;
      case TimestampType.CLOCK_BOOTTIME:
        return this.timestampConverter.tryGetBootTimeNs(timestamp);
      case TimestampType.CLOCK_REALTIME:
        return this.timestampConverter.tryGetRealTimeNs(timestamp);
      default:
        assertUnreachable(timestampType);
    }
  }

  // Make a deferred timestamp: a lambda meant to be executed at a later point to create a
  // timestamp. The lambda is needed to defer timestamp creation to the point where traces
  // are loaded into TracePipeline and TimestampConverter is properly initialized and ready
  // to instantiate timestamps.
  private makeDeferredTimestampForWinscope(
    timestampNs: bigint | undefined,
  ): (() => Timestamp | undefined) | undefined {
    const timestampType = assertDefined(this.remoteTool?.timestampType);

    if (timestampNs === undefined || timestampType === undefined) {
      return undefined;
    }

    switch (timestampType) {
      case TimestampType.UNKNOWN:
        return undefined;
      case TimestampType.CLOCK_BOOTTIME:
        return () => {
          try {
            return this.timestampConverter.makeTimestampFromBootTimeNs(
              timestampNs,
            );
          } catch (error) {
            return undefined;
          }
        };
      case TimestampType.CLOCK_REALTIME:
        return () => {
          try {
            return this.timestampConverter.makeTimestampFromRealNs(timestampNs);
          } catch (error) {
            return undefined;
          }
        };
      default:
        assertUnreachable(timestampType);
    }
  }
}
