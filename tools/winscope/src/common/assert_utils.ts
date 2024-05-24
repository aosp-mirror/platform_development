/*
 * Copyright (C) 2023 The Android Open Source Project
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

export function assertDefined<A>(value: A | null | undefined): A {
  if (value === undefined || value === null) {
    throw new Error(`Expected value, but found '${value}'`);
  }
  return value;
}

export function assertTrue(value: boolean, lazyErrorMessage?: () => string) {
  if (!value) {
    throw new Error(
      lazyErrorMessage ? lazyErrorMessage() : 'Expected value to be true',
    );
  }
}

// Ensure at compile-time that a certain line is not reachable.
// E.g. make sure that a switch/case handles all possible input values.
export function assertUnreachable(x: never): never {
  throw new Error('This line should never execute');
}
