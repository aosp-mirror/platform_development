/*
 * Copyright 2024 Google LLC
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
 * Ensures the truth of an expression involving one or more parameters to the
 * calling method.
 */
export function checkArgument(condition: boolean, message?: string) {
  if (!condition) {
    throw new Error(`Invalid argument [detail: ${message}]`);
  }
}

/**
 * Ensures that an object reference passed as a parameter to the calling method
 * is not null or undefined.
 */
export function checkNotNull<T>(
  reference: T | null | undefined,
  message?: string
): T {
  if (reference === null || reference === undefined) {
    throw new Error(`null reference exception [detail: ${message}]`);
  }
  return reference;
}

/**
 * Ensures the truth of an expression involving the state of the calling
 * instance, but not involving any parameters to the calling method.
 */
export function checkState(condition: boolean, message?: string) {
  if (!condition) {
    throw new Error(`Invalid state [detail: ${message}]`);
  }
}
