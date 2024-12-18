export interface MotionGolden {
  id: string;
  result: 'PASSED' | 'FAILED' | 'MISSING_REFERENCE';
  label: string;
  goldenRepoPath: string;
  updated: boolean;
  testClassName: string;
  testMethodName: string;
  testTime: string;

  actualUrl: string;
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
