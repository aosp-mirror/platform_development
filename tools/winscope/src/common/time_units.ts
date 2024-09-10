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

export enum TIME_UNIT_TO_NANO {
  ns = 1,
  ms = 1000000,
  s = 1000000 * 1000,
  m = 1000000 * 1000 * 60,
  h = 1000000 * 1000 * 60 * 60,
  d = 1000000 * 1000 * 60 * 60 * 24,
}

export const TIME_UNITS = [
  {nanosInUnit: TIME_UNIT_TO_NANO['ns'], unit: 'ns'},
  {nanosInUnit: TIME_UNIT_TO_NANO['ms'], unit: 'ms'},
  {nanosInUnit: TIME_UNIT_TO_NANO['s'], unit: 's'},
  {nanosInUnit: TIME_UNIT_TO_NANO['m'], unit: 'm'},
  {nanosInUnit: TIME_UNIT_TO_NANO['h'], unit: 'h'},
  {nanosInUnit: TIME_UNIT_TO_NANO['d'], unit: 'd'},
];
