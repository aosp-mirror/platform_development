export interface MotionGolden {
  id: string;
  result: 'PASSED' | 'FAILED' | 'MISSING_REFERENCE';
  label: string;
  goldenRepoPath: string;
  updated: boolean;

  type: 'motion';

  actualUrl: String;
  filmstripUrl: String | undefined;
}

export interface ScreenshotGolden {
  id: string;
  result: 'PASSED' | 'FAILED' | 'MISSING_REFERENCE';
  label: string;
  goldenRepoPath: string;
  updated: boolean;

  type: 'screenshot';

  actualUrl: String;
  expectedUrl: String | undefined;
  diffUrl: String | undefined;
}

export type Golden = MotionGolden | ScreenshotGolden;
