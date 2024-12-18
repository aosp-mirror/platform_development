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

import { delay } from './utils';

/** Starts the promise to try executing. */
type RetryableFunction<T> = () => Promise<T>;

/** Determines whether a next retry should be attempted. */
type RetryIf = (attempt: number, error: unknown) => boolean;

/** A `RetryIf` that limits the number of attempts to `maxAttempts`. */
export function maxAttempts(maxAttempts: number): RetryIf {
  console.assert(maxAttempts > 0);
  return (retry) => retry < maxAttempts - 1;
}

/**
 * Delays the next retry, after failing the `attempt`th attempt.
 */
type RetryDelay = (attempt: number, error: unknown) => Promise<void>;

/** A `RetryDelay` that pauses the fixed amount of `delayMs` between reties */
export function fixedRetryDelay(delayMs: number): RetryDelay {
  return () => delay(delayMs);
}

/** Callback after the `attempt`th attempt failed with `error` */
type AttemptFailed = (attempt: number, error: unknown) => Promise<void>;

/**
 * Awaits the completion of the `Promise` returned by `code`, and retries if the
 * promise fails to complete.
 */
export async function retry<T>(
  code: RetryableFunction<T>,
  options?: {
    /** Whether to retry the failed invocation (default maxAttempts(3)) */
    shouldRerty?: RetryIf;
    /** Delays the next retry (defaults to a fixed delay of 100ms) */
    delay?: RetryDelay;
    /** When an invocation failed. */
    attemptFailed?: AttemptFailed;
  },
): Promise<T> {
  const shouldRerty = options?.shouldRerty || maxAttempts(3);
  const delay = options?.delay || fixedRetryDelay(100);
  const attemptFailed = options?.attemptFailed || (() => Promise.resolve());

  for (let attempt = 0; ; attempt++) {
    try {
      return await code();
    } catch (e: unknown) {
      if (e instanceof NonRetryableError) {
        throw e;
      }

      await attemptFailed(attempt, e);

      if (shouldRerty(attempt, e)) {
        await delay(attempt, e);
      } else {
        // permanently fail.
        throw e;
      }
    }
  }
}

/** Promises failing with this type of error will not be retried by `retry`. */
export class NonRetryableError extends Error {
  constructor(msg: string) {
    super(msg);
  }
}
