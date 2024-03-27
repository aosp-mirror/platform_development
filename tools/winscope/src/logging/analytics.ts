/*
 * Copyright (C) 2024 The Android Open Source Project
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

import {CoarseVersion} from 'trace/coarse_version';
import {Parser} from 'trace/parser';
import {TraceType} from 'trace/trace_type';
import {globalConfig} from '../common/global_config';

export class Analytics {
  private static TRACE_LOADED_EVENT = 'trace_loaded';

  static logTraceLoaded(parser: Parser<object>) {
    Analytics.doLogEvent(
      Analytics.TRACE_LOADED_EVENT,
      Analytics.createTraceLoadedEvent(parser),
    );
  }

  private static doLogEvent(
    eventName: Gtag.EventNames | (string & {}),
    eventParams?: Gtag.ControlParams | Gtag.EventParams | Gtag.CustomParams,
  ) {
    if (globalConfig.MODE === 'PROD') {
      gtag('event', eventName, eventParams);
    }
  }

  private static createTraceLoadedEvent(
    parser: Parser<object>,
  ): Gtag.EventParams {
    return {
      event_label: TraceType[parser.getTraceType()],
      event_category: CoarseVersion[parser.getCoarseVersion()],
    } as Gtag.EventParams;
  }
}
