import { Feature } from './feature';
import { DataPoint } from './golden';
import { Visualization } from './visualization';

export interface Derivate {
  name: string;

  create(feature: Feature): Feature;
}
