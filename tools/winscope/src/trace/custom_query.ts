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
  SF_LAYERS_ID_AND_NAME,
  VIEW_CAPTURE_METADATA,
  VSYNCID,
  WM_WINDOWS_TOKEN_AND_TITLE,
  INITIALIZE_TRACE_SEARCH,
}

export class ProcessParserResult {
  static [CustomQueryType.SF_LAYERS_ID_AND_NAME]<T>(
    parserResult: CustomQueryParserResultTypeMap[CustomQueryType.SF_LAYERS_ID_AND_NAME],
  ): CustomQueryResultTypeMap<T>[CustomQueryType.SF_LAYERS_ID_AND_NAME] {
    return parserResult;
  }

  static [CustomQueryType.VIEW_CAPTURE_METADATA]<T>(
    parserResult: CustomQueryParserResultTypeMap[CustomQueryType.VIEW_CAPTURE_METADATA],
  ): CustomQueryResultTypeMap<T>[CustomQueryType.VIEW_CAPTURE_METADATA] {
    return parserResult;
  }

  static [CustomQueryType.VSYNCID]<T>(
    parserResult: CustomQueryParserResultTypeMap[CustomQueryType.VSYNCID],
    makeTraceEntry: (
      index: RelativeEntryIndex,
      vsyncId: bigint,
    ) => TraceEntryEager<T, bigint>,
  ): CustomQueryResultTypeMap<T>[CustomQueryType.VSYNCID] {
    return parserResult.map((vsyncId, index) => {
      return makeTraceEntry(index, vsyncId);
    });
  }

  static [CustomQueryType.WM_WINDOWS_TOKEN_AND_TITLE]<T>(
    parserResult: CustomQueryParserResultTypeMap[CustomQueryType.WM_WINDOWS_TOKEN_AND_TITLE],
  ): CustomQueryResultTypeMap<T>[CustomQueryType.WM_WINDOWS_TOKEN_AND_TITLE] {
    return parserResult;
  }

  static [CustomQueryType.INITIALIZE_TRACE_SEARCH]<T>(
    parserResult: CustomQueryParserResultTypeMap[CustomQueryType.INITIALIZE_TRACE_SEARCH],
  ): CustomQueryResultTypeMap<T>[CustomQueryType.INITIALIZE_TRACE_SEARCH] {
    return parserResult;
  }
}

export interface CustomQueryParamTypeMap {
  [CustomQueryType.SF_LAYERS_ID_AND_NAME]: never;
  [CustomQueryType.VIEW_CAPTURE_METADATA]: never;
  [CustomQueryType.VSYNCID]: never;
  [CustomQueryType.WM_WINDOWS_TOKEN_AND_TITLE]: never;
  [CustomQueryType.INITIALIZE_TRACE_SEARCH]: never;
}

export interface CustomQueryParserResultTypeMap {
  [CustomQueryType.SF_LAYERS_ID_AND_NAME]: Array<{id: number; name: string}>;
  [CustomQueryType.VIEW_CAPTURE_METADATA]: {
    packageName: string;
    windowName: string;
  };
  [CustomQueryType.VSYNCID]: Array<bigint>;
  [CustomQueryType.WM_WINDOWS_TOKEN_AND_TITLE]: Array<{
    token: string;
    title: string;
  }>;
  [CustomQueryType.INITIALIZE_TRACE_SEARCH]: void;
}

export interface CustomQueryResultTypeMap<T> {
  [CustomQueryType.SF_LAYERS_ID_AND_NAME]: Array<{id: number; name: string}>;
  [CustomQueryType.VIEW_CAPTURE_METADATA]: {
    packageName: string;
    windowName: string;
  };
  [CustomQueryType.VSYNCID]: Array<TraceEntryEager<T, bigint>>;
  [CustomQueryType.WM_WINDOWS_TOKEN_AND_TITLE]: Array<{
    token: string;
    title: string;
  }>;
  [CustomQueryType.INITIALIZE_TRACE_SEARCH]: void;
}

export class VisitableParserCustomQuery<Q extends CustomQueryType> {
  private readonly type: CustomQueryType;
  private result: Promise<CustomQueryParserResultTypeMap[Q]> | undefined;

  constructor(type: Q) {
    this.type = type;
  }

  visit<R extends CustomQueryType>(
    type: R,
    visitor: () => Promise<CustomQueryParserResultTypeMap[R]>,
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
        `No result available. Looks like custom query (type: ${this.type}) is not implemented!`,
      );
    }
    return this.result;
  }
}
