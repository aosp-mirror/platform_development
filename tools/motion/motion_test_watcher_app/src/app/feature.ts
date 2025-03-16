import { Derivate } from './feature_derivate';
import {
  DataPoint,
  DataPointObject,
  isNotFound,
  MotionGoldenFeature,
} from './golden';
import {
  DataPointVisualization,
  LineGraphVisualization,
  PROBE_COLORS,
  Visualization,
} from './visualization';

export class Feature {
  constructor(
    readonly id: string,
    readonly name: string,
    readonly type: string,
    readonly dataPoints: Array<DataPoint>,
  ) {}

  private _visualization: Visualization | undefined;

  get visualization(): Visualization {
    if (this._visualization === undefined) {
      this._visualization = createVisualization(this);
    }

    return this._visualization;
  }

  private _subFeatures: Array<Feature> | undefined;

  get subFeatures(): Array<Feature> {
    if (this._subFeatures === undefined) {
      this._subFeatures = createSubFeatures(this);
    }

    return this._subFeatures!!;
  }

  private _derivates: Array<Derivate> | undefined;

  get derivates(): Array<Derivate> {
    if (this._derivates === undefined) {
      this._derivates = createDerivatives(this);
    }
    return this._derivates;
  }
}

export function recordedFeatureFactory(
  featureData: MotionGoldenFeature,
): Feature {
  return new Feature(
    featureData.name,
    featureData.name,
    featureData.type,
    featureData.data_points,
  );
}

function createVisualization(feature: Feature): Visualization {
  const { name, type } = feature;

  let color: string | null = null;

  if (name === 'input') {
    color = PROBE_COLORS[0];
  } else if (name === 'output_target') {
    color = PROBE_COLORS[1];
  } else if (name === 'output') {
    color = PROBE_COLORS[2];
  }

  if (name === 'alpha' && type === 'float') {
    return new LineGraphVisualization(/* minValue */ 0, /*maxValue*/ 1, color);
  }

  if (['float', 'int', 'dp'].includes(type)) {
    const numericValues = feature.dataPoints.filter(
      (it): it is number => typeof it === 'number',
    );
    const minValue = Math.min(...numericValues) ?? 0;
    let maxValue = Math.max(...numericValues) ?? 1;

    if (minValue === maxValue) {
      maxValue += 1;
    }

    return new LineGraphVisualization(minValue, maxValue, color);
  }

  return new DataPointVisualization();
}

function createDerivatives(feature: Feature): Derivate[] {
  const { name, type } = feature;

  const result: Derivate[] = [];

  return result;
}

function createSubFeatures(feature: Feature): Feature[] {
  const { name, type } = feature;

  switch (type) {
    case 'intSize':
    case 'dpSize':
      return [
        createSubFeature(feature, 'width', 'float', (point) => point['width']),
        createSubFeature(
          feature,
          'height',
          'float',
          (point) => point['height'],
        ),
      ];
    case 'intOffset':
    case 'dpOffset':
    case 'offset':
      return [
        createSubFeature(feature, 'x', 'float', (point) => point['x']),
        createSubFeature(feature, 'y', 'float', (point) => point['y']),
      ];

    case 'animatedVisibilityTransitions':
      return [
        ...new Set(
          feature.dataPoints.flatMap((it) =>
            Object.keys(it as DataPointObject),
          ),
        ),
      ]
        .sort()
        .map((it) =>
          createSubFeature(
            feature,
            it,
            'animatedVisibilityValues',
            (point) => point[it],
          ),
        );

    case 'animatedVisibilityValues':
      return [
        ...new Set(
          feature.dataPoints.flatMap((it) =>
            it ? 
            Object.keys(it as DataPointObject) : [],
          ),
        ),
      ]
        .sort()
        .flatMap((name) => {
          let type: string;

          switch (name) {
            case 'alpha':
              type = 'float';
              break;
            case 'slide':
              type = 'intOffset';
              break;
            case 'scale':
              type = 'float';
              break;
            case 'size':
              type = 'intSize';
              break;
            default:
              return [];
          }

          return [createSubFeature(feature, name, type, (point) => point[name])];
        })
  }

  return [];
}

function createSubFeature(
  parent: Feature,
  key: string,
  type: string,
  extract: (dataPoint: DataPointObject) => DataPoint,
): Feature {
  return new Feature(
    `${parent.id}::${key}`,
    `${parent.name}[${key}]`,
    type,
    parent.dataPoints.map((it) => {
      if (!isNotFound(it) && it instanceof Object)
        return extract(it as DataPointObject);

      return { type: 'not_found' };
    }),
  );
}
