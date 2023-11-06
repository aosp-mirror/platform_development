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
import {RelativeEntryIndex, TraceEntryEager} from './trace';

export enum CustomQueryType {
  VSYNCID,
  SF_LAYERS_ID_AND_NAME,
  WM_WINDOWS_TOKEN_AND_TITLE,
}

export class ProcessParserResult {
  static [CustomQueryType.VSYNCID]<T>(
    parserResult: CustomQueryParserResultTypeMap[CustomQueryType.VSYNCID],
    makeTraceEntry: (index: RelativeEntryIndex, vsyncId: bigint) => TraceEntryEager<T, bigint>
  ): CustomQueryResultTypeMap<T>[CustomQueryType.VSYNCID] {
    return parserResult.map((vsyncId, index) => {
      return makeTraceEntry(index, vsyncId);
    });
  }

  static [CustomQueryType.SF_LAYERS_ID_AND_NAME]<T>(
    parserResult: CustomQueryParserResultTypeMap[CustomQueryType.SF_LAYERS_ID_AND_NAME]
  ): CustomQueryResultTypeMap<T>[CustomQueryType.SF_LAYERS_ID_AND_NAME] {
    return parserResult;
  }

  static [CustomQueryType.WM_WINDOWS_TOKEN_AND_TITLE]<T>(
    parserResult: CustomQueryParserResultTypeMap[CustomQueryType.WM_WINDOWS_TOKEN_AND_TITLE]
  ): CustomQueryResultTypeMap<T>[CustomQueryType.WM_WINDOWS_TOKEN_AND_TITLE] {
    return parserResult;
  }
}

export interface CustomQueryParserResultTypeMap {
  [CustomQueryType.VSYNCID]: Array<bigint>;
  [CustomQueryType.SF_LAYERS_ID_AND_NAME]: Array<{id: number; name: string}>;
  [CustomQueryType.WM_WINDOWS_TOKEN_AND_TITLE]: Array<{token: string; title: string}>;
}

export interface CustomQueryResultTypeMap<T> {
  [CustomQueryType.VSYNCID]: Array<TraceEntryEager<T, bigint>>;
  [CustomQueryType.SF_LAYERS_ID_AND_NAME]: Array<{id: number; name: string}>;
  [CustomQueryType.WM_WINDOWS_TOKEN_AND_TITLE]: Array<{token: string; title: string}>;
}

export class VisitableParserCustomQuery<Q extends CustomQueryType> {
  private readonly type: CustomQueryType;
  private result: Promise<CustomQueryParserResultTypeMap[Q]> | undefined;

  constructor(type: Q) {
    this.type = type;
  }

  visit<R extends CustomQueryType>(
    type: R,
    visitor: () => Promise<CustomQueryParserResultTypeMap[R]>
  ): VisitableParserCustomQuery<Q> {
    if (type !== this.type) {
      return this;
    }
    this.result = visitor() as Promise<CustomQueryParserResultTypeMap[Q]>;
    return this;
  }

  getResult(): Promise<CustomQueryParserResultTypeMap[Q]> {
    if (this.result === undefined) {
      throw new Error(
        `No result available. Looks like custom query (type: ${this.type}) is not implemented!`
      );
    }
    return this.result;
  }
}
