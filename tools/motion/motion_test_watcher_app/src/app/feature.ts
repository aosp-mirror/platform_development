import { Derivate } from './feature_derivate';
import { DataPoint, MotionGoldenFeature } from './golden';
import {
  DataPointVisualization,
  LineGraphVisualization,
  Visualization,
} from './visualization';

export interface Feature {
  id: string;
  name: string;
  dataPoints: Array<DataPoint>;
  visualization: Visualization;
  derivates: Array<Derivate>;
}

export class RecordedFeature implements Feature {
  constructor(
    private readonly featureData: MotionGoldenFeature,
    readonly visualization: Visualization,
    readonly derivates: Array<Derivate>,
  ) {}

  get id(): string {
    return this.featureData.name;
  }

  get name(): string {
    return this.featureData.name;
  }

  get dataPoints(): Array<DataPoint> {
    return this.featureData.data_points;
  }
}

export function recordedFeatureFactory(
  featureData: MotionGoldenFeature,
): RecordedFeature {
  const visualization = createVisualization(featureData);
  const derivatives = createDerivatives(featureData);

  return new RecordedFeature(featureData, visualization, derivatives);
}

function createVisualization(featureData: MotionGoldenFeature): Visualization {
  const { name, type } = featureData;

  if (name === 'alpha' && type === 'float') {
    return new LineGraphVisualization(/* minValue */ 0, /*maxValue*/ 1);
  }

  if (['float', 'int', 'dp'].includes(type)) {
    const numericValues = featureData.data_points.filter(
      (it): it is number => typeof it === 'number',
    );
    const minValue = Math.min(...numericValues) ?? 0;
    let maxValue = Math.max(...numericValues) ?? 1;

    if (minValue === maxValue) {
      maxValue += 1;
    }

    return new LineGraphVisualization(minValue, maxValue);
  }

  return new DataPointVisualization();
}

function createDerivatives(featureData: MotionGoldenFeature): Derivate[] {
  const { name, type } = featureData;

  const result: Derivate[] = [];

  return result;
}
