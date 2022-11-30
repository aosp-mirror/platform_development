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

import {Message, MessageBugReport, MessagePong, MessageTimestamp, MessageType} from "./messages";
import {
  CrossToolProtocolDependencyInversion,
  OnBugreportReceived,
  OnTimestampReceived} from "cross_tool/cross_tool_protocol_dependency_inversion";
import {RealTimestamp} from "common/trace/timestamp";
import {FunctionUtils} from "common/utils/function_utils";

class CrossToolProtocol implements CrossToolProtocolDependencyInversion {
  static readonly TARGET = "http://localhost:8081";

  private remoteToolWindow?: Window;
  private onBugreportReceived: OnBugreportReceived = FunctionUtils.DO_NOTHING_ASYNC;
  private onTimestampReceived: OnTimestampReceived = FunctionUtils.DO_NOTHING_ASYNC;

  constructor() {
    window.addEventListener("message", async (event) => {
      await this.onMessageReceived(event);
    });
  }

  setOnBugreportReceived(callback: OnBugreportReceived) {
    this.onBugreportReceived = callback;
  }

  setOnTimestampReceived(callback: OnTimestampReceived) {
    this.onTimestampReceived = callback;
  }

  sendTimestamp(timestamp: RealTimestamp) {
    if (!this.remoteToolWindow) {
      return;
    }

    const message = new MessageTimestamp(timestamp.getValueNs());
    this.remoteToolWindow.postMessage(message, CrossToolProtocol.TARGET);
    console.log("Cross-tool protocol sent timestamp message:", message);
  }

  private async onMessageReceived(event: MessageEvent) {
    if (event.origin !== CrossToolProtocol.TARGET) {
      console.log("Cross-tool protocol ignoring message from unexpected origin.",
        "Origin:", event.origin, "Message:", event.data);
      return;
    }

    this.remoteToolWindow = event.source as Window;

    const message = event.data as Message;
    if (!message.type) {
      console.log("Cross-tool protocol received invalid message:", message);
      return;
    }

    switch(message.type) {
    case MessageType.PING:
      console.log("Cross-tool protocol received ping message:", message);
      (event.source as Window).postMessage(new MessagePong(), CrossToolProtocol.TARGET);
      break;
    case MessageType.PONG:
      console.log("Cross-tool protocol received unexpected pong message:", message);
      break;
    case MessageType.BUGREPORT:
      console.log("Cross-tool protocol received bugreport message:", message);
      await this.onMessageBugreportReceived(message as MessageBugReport);
      break;
    case MessageType.TIMESTAMP:
      console.log("Cross-tool protocol received timestamp message:", message);
      await this.onMessageTimestampReceived(message as MessageTimestamp);
      break;
    case MessageType.FILES:
      console.log("Cross-tool protocol received unexpected files message", message);
      break;
    default:
      console.log("Cross-tool protocol received unsupported message type:", message);
      break;
    }
  }

  private async onMessageBugreportReceived(message: MessageBugReport) {
    const timestamp = message.timestampNs !== undefined
      ? new RealTimestamp(message.timestampNs)
      : undefined;
    this.onBugreportReceived(message.file, timestamp);
  }

  private async onMessageTimestampReceived(message: MessageTimestamp) {
    const timestamp = new RealTimestamp(message.timestampNs);
    await this.onTimestampReceived(timestamp);
  }
}

export {CrossToolProtocol};
