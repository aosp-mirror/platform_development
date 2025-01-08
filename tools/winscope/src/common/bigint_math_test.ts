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

import {BigintMath} from './bigint_math';

describe('BigintMath', () => {
  it('divideAndRound()', () => {
    expect(BigintMath.divideAndRound(0n, 10n)).toEqual(0n);
    expect(BigintMath.divideAndRound(10n, 10n)).toEqual(1n);
    expect(BigintMath.divideAndRound(10n, 6n)).toEqual(2n);
    expect(BigintMath.divideAndRound(10n, 5n)).toEqual(2n);
    expect(BigintMath.divideAndRound(10n, 4n)).toEqual(3n);
    expect(() => BigintMath.divideAndRound(1n, 0n)).toThrowError();
    expect(BigintMath.divideAndRound(10000n + 4999n, 10000n)).toEqual(1n);
    expect(BigintMath.divideAndRound(10000n + 5000n, 10000n)).toEqual(2n);
  });
});
