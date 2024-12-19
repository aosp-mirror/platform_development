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

/**
 * Asserts that the given value is defined.
 *
 * @param value The value to assert.
 * @param lazyErrorMessage A function that returns a message to be included in the error if the assertion fails.
 * @throws {Error} If the value is not defined.
 * @return The value, asserted to be defined.
 */
export function assertDefined<A>(
  value: A | null | undefined,
  lazyErrorMessage?: () => string,
): A {
  if (value === undefined || value === null) {
    throw new Error(
      lazyErrorMessage
        ? lazyErrorMessage()
        : `Expected value, but found '${value}'`,
    );
  }
  return value;
}

/**
 * Asserts that the given value is true.
 *
 * @param value The value to assert.
 * @param lazyErrorMessage A function that returns a message to be included in the error if the assertion fails.
 * @throws {Error} If the value is not true.
 */
export function assertTrue(value: boolean, lazyErrorMessage?: () => string) {
  if (!value) {
    throw new Error(
      lazyErrorMessage ? lazyErrorMessage() : 'Expected value to be true',
    );
  }
}

/**
 * Ensures at compile-time that a certain line is not reachable.
 * E.g., make sure that a switch/case handles all possible input values.
 *
 * @param x The value to assert.
 * @throws {Error} If the line is reachable.
 * @return The value, asserted to be unreachable.
 */
export function assertUnreachable(x: never): never {
  throw new Error('This line should never execute');
}
