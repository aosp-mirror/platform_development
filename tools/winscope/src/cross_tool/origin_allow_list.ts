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

import {globalConfig} from 'common/global_config';

export class OriginAllowList {
  private static readonly ALLOW_LIST_PROD = [
    new RegExp('^https://([^\\/]*\\.)*googleplex\\.com$'),
    new RegExp('^https://([^\\/]*\\.)*google\\.com$'),
  ];

  private static readonly ALLOW_LIST_DEV = [
    ...OriginAllowList.ALLOW_LIST_PROD,
    new RegExp('^(http|https)://localhost:8081$'), // remote tool mock
  ];

  static isAllowed(originUrl: string, mode = globalConfig.MODE): boolean {
    const list = OriginAllowList.getList(mode);

    for (const regex of list) {
      if (regex.test(originUrl)) {
        return true;
      }
    }

    return false;
  }

  private static getList(mode: typeof globalConfig.MODE): RegExp[] {
    switch (mode) {
      case 'DEV':
        return OriginAllowList.ALLOW_LIST_DEV;
      case 'KARMA_TEST':
        return OriginAllowList.ALLOW_LIST_DEV;
      case 'PROD':
        return OriginAllowList.ALLOW_LIST_PROD;
      default:
        throw new Error(`Unhandled mode: ${globalConfig.MODE}`);
    }
  }
}
