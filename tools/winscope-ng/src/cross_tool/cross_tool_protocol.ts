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
import {OriginAllowList} from "./origin_allow_list";
import {
  CrossToolProtocolDependencyInversion,
  OnBugreportReceived,
  OnTimestampReceived} from "cross_tool/cross_tool_protocol_dependency_inversion";
import {RealTimestamp} from "common/trace/timestamp";
import {FunctionUtils} from "common/utils/function_utils";

class RemoteTool {
  constructor(
    public readonly window: Window,
    public readonly origin: string) {
  }
}

export class CrossToolProtocol implements CrossToolProtocolDependencyInversion {
  private remoteTool?: RemoteTool;
  private onBugreportReceived: OnBugreportReceived = FunctionUtils.DO_NOTHING_ASYNC;
  private onTimestampReceived: OnTimestampReceived = FunctionUtils.DO_NOTHING;

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
    if (!this.remoteTool) {
      return;
    }

    const message = new MessageTimestamp(timestamp.getValueNs());
    this.remoteTool.window.postMessage(message, this.remoteTool.origin);
    console.log("Cross-tool protocol sent timestamp message:", message);
  }

  private async onMessageReceived(event: MessageEvent) {
    if (!OriginAllowList.isAllowed(event.origin)) {
      console.log("Cross-tool protocol ignoring message from non-allowed origin.",
        "Origin:", event.origin, "Message:", event.data);
      return;
    }

    const message = event.data as Message;
    if (!message.type) {
      console.log("Cross-tool protocol received invalid message:", message);
      return;
    }

    if (!this.remoteTool) {
      this.remoteTool = new RemoteTool(event.source as Window, event.origin);
    }

    switch(message.type) {
    case MessageType.PING:
      console.log("Cross-tool protocol received ping message:", message);
      (event.source as Window).postMessage(new MessagePong(), event.origin);
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
      this.onMessageTimestampReceived(message as MessageTimestamp);
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

  private onMessageTimestampReceived(message: MessageTimestamp) {
    const timestamp = new RealTimestamp(message.timestampNs);
    this.onTimestampReceived(timestamp);
  }
}
