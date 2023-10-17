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
  OnTracePositionUpdate,
  TracePositionUpdateEmitter,
} from 'interfaces/trace_position_update_emitter';
import {TracePositionUpdateListener} from 'interfaces/trace_position_update_listener';
import {TracePosition} from 'trace/trace_position';

export class TimelineComponentStub
  implements TracePositionUpdateEmitter, TracePositionUpdateListener
{
  setOnTracePositionUpdate(callback: OnTracePositionUpdate) {
    // do nothing
  }

  onTracePositionUpdate(position: TracePosition) {
    // do nothing
  }
}
