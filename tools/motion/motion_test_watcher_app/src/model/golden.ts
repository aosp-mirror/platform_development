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

export interface MotionGolden {
  actualUrl: string;
  expectedUrl: string;
  goldenRepoPath: string;
  id: string;
  label: string;
  result: 'PASSED' | 'FAILED' | 'MISSING_REFERENCE';
  testClassName: string;
  testMethodName: string;
  testTime: string;
  updated: boolean;
  videoUrl: string | undefined;
}

export interface MotionGoldenData {
  frame_ids: Array<string | number>;
  features: MotionGoldenFeature[];
}

export interface MotionGoldenFeature {
  name: string;
  type: string;
  data_points: Array<DataPoint>;
}

type DataPointTypes =
  | string
  | number
  | boolean
  | null
  | DataPointArray
  | DataPointObject;
export interface DataPointObject {
  [member: string]: DataPointTypes;
}
export interface DataPointArray extends Array<DataPointTypes> {}
export interface NotFound {
  type: 'not_found';
}
export type DataPoint = DataPointTypes | NotFound;

export function isNotFound(dataPoint: DataPoint) {
  return (
    dataPoint instanceof Object &&
    'type' in dataPoint &&
    dataPoint.type === 'not_found'
  );
}
