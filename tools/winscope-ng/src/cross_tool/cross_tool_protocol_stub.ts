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
  CrossToolProtocolDependencyInversion,
  OnBugreportReceived,
  OnTimestampReceived} from "cross_tool/cross_tool_protocol_dependency_inversion";
import {RealTimestamp} from "common/trace/timestamp";
import {FunctionUtils} from "common/utils/function_utils";

export class CrossToolProtocolStub implements CrossToolProtocolDependencyInversion {
  onBugreportReceived: OnBugreportReceived = FunctionUtils.DO_NOTHING_ASYNC;
  onTimestampReceived: OnTimestampReceived = FunctionUtils.DO_NOTHING_ASYNC;

  setOnBugreportReceived(callback: OnBugreportReceived) {
    this.onBugreportReceived = callback;
  }

  setOnTimestampReceived(callback: OnTimestampReceived) {
    this.onTimestampReceived = callback;
  }

  sendTimestamp(timestamp: RealTimestamp) {
    // do nothing
  }
}
