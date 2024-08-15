/*
 * Copyright 2022 Google LLC
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

/** Promise that completes after the specified `timeMs` */
export function delay(timeMs: number): Promise<void> {
  return new Promise<void>((complete) => setTimeout(complete, timeMs));
}

/**
 * A `Promise` to be completed
 */
export class Deferred<T> extends Promise<T> {
  readonly resolve: (value: T) => void;
  readonly reject: (reason?: any) => void;

  constructor() {
    let capturedResolve: (value: T) => void;
    let capturedReject: (reason?: any) => void;

    super((resolve, reject) => {
      capturedResolve = resolve;
      capturedReject = reject;
    });

    this.resolve = capturedResolve!;
    this.reject = capturedReject!;
  }
}

export function namedError(name: string, message?: string): Error {
  const result = new Error(message);
  result.name = name;
  return result;
}

export function isNamedError(e: unknown, name: string): e is Error {
  return e instanceof Error && e.name === name;
}

/** Checks whether both `Uint8Array` have the same contents. */
export function arrayBufferEquals(
  left: Uint8Array,
  right: Uint8Array,
): boolean {
  if (left.byteLength != right.byteLength) return false;

  for (let i = 0; i < left.byteLength; i++) {
    if (left.at(i) != right.at(i)) return false;
  }

  return true;
}

export function asciiStringToBytes(contents: string): Uint8Array {
  const result = new TextEncoder().encode(contents);
  if (result.length != contents.length) {
    // non-ascii characters require multiple bytes in utf-8.
    throw new Error(`non-ascii characters found in '${contents}'`);
  }
  return result;
}
