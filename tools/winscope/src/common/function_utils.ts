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

export type OnProgressUpdateType = (percentage: number) => void;

export class FunctionUtils {
  static readonly DO_NOTHING = () => {
    // do nothing
  };

  static readonly DO_NOTHING_ASYNC = (): Promise<void> => {
    return Promise.resolve();
  };

  static mixin<T extends object, U extends object>(a: T, b: U): T & U {
    const ret = {};
    Object.assign(ret, a);
    Object.assign(ret, b);

    const assignMethods = (dst: object, src: object) => {
      for (const methodName of Object.getOwnPropertyNames(
        Object.getPrototypeOf(src),
      )) {
        const method = (src as any)[methodName];
        (dst as any)[methodName] = method;
      }
    };

    assignMethods(ret, a);
    assignMethods(ret, b);

    return ret as T & U;
  }
}
