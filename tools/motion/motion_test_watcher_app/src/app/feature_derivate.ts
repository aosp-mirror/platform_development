import { Feature } from './feature';
import { DataPoint } from './golden';
import { Visualization } from './visualization';

export interface Derivate {
  name: string;

  create(feature: Feature): DerivedFeature;
}

export class DerivedFeature implements Feature {
  constructor(
    readonly id: string,
    readonly name: string,

    private readonly source: Feature,
    readonly dataPoints: Array<DataPoint>,
    readonly visualization: Visualization,

    readonly derivates: Array<Derivate>,
  ) {}
}
