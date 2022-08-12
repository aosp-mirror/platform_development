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

enum TimestampType {
  ELAPSED,
  REAL,
}

class Timestamp {
  constructor(type: TimestampType, valueNs: bigint) {
    this.type = type;
    this.valueNs = valueNs;
  }

  public getType(): TimestampType {
    return this.type;
  }

  public getValueNs(): bigint {
    return this.valueNs;
  }

  public valueOf(): bigint {
    return this.getValueNs();
  }

  private readonly type: TimestampType;
  private readonly valueNs: bigint;
}

export {Timestamp, TimestampType};
