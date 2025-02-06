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

import {Store} from 'common/store/store';
import {Traces} from 'trace/traces';
import {TraceType} from 'trace/trace_type';
import {ViewerMediaBased} from './viewer_media_based';

export class ViewerScreenRecording extends ViewerMediaBased {
  static readonly DEPENDENCIES: TraceType[] = [TraceType.SCREEN_RECORDING];

  constructor(traces: Traces, store: Store) {
    super(traces, store);
  }

  protected override getTraceTypeForViewTitle(): TraceType {
    return TraceType.SCREEN_RECORDING;
  }
}
