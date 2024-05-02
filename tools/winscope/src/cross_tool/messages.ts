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

export enum MessageType {
  UNKNOWN = 0,
  PING,
  PONG,
  BUGREPORT,
  TIMESTAMP,
  FILES,
}

export enum TimestampType {
  UNKNOWN = 0,
  CLOCK_BOOTTIME,
  CLOCK_REALTIME,
}

export interface Message {
  type: MessageType;
}

export class MessagePing implements Message {
  type = MessageType.PING;
}

export class MessagePong implements Message {
  type = MessageType.PONG;
}

export class MessageBugReport implements Message {
  type = MessageType.BUGREPORT;

  constructor(
    public file: File,
    public timestampNs?: bigint,
    public timestampType?: TimestampType,
    public issueId?: string,
  ) {}
}

export class MessageTimestamp implements Message {
  type = MessageType.TIMESTAMP;

  constructor(
    public timestampNs: bigint,
    public timestampType?: TimestampType,
    public sections?: string[],
  ) {}
}

export class MessageFiles implements Message {
  type = MessageType.FILES;

  constructor(
    public files: File[],
    public timestampNs?: bigint,
    public timestampType?: TimestampType,
  ) {}
}
