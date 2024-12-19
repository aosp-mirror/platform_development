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

import {BigintMath} from 'common/bigint_math';
import {TIME_UNIT_TO_NANO} from 'common/time/time_units';

export class TimeDuration {
  constructor(private timeDiffNs: bigint) {}
  getValueNs(): bigint {
    return this.timeDiffNs;
  }

  format(): string {
    const msString = BigintMath.divideAndRound(
      this.timeDiffNs,
      BigInt(TIME_UNIT_TO_NANO.ms),
    );
    return msString.toLocaleString() + ' ms';
  }
}
