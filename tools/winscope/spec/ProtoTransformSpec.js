import { decodeAndTransformProto, FILE_TYPES, FILE_DECODERS } from '../src/decode';
import fs from 'fs';
import path from 'path';
import { expectedEntries, expectedLayers, layers_traces } from './traces/ExpectedTraces';

describe("Proto Transformations", () => {
  it("can transform surface flinger traces", () => {
    for (var i = 0; i < layers_traces.length; i++) {
      const trace = layers_traces[i];
      const buffer = new Uint8Array(fs.readFileSync(path.resolve(__dirname, trace)));
      const data = decodeAndTransformProto(buffer, FILE_DECODERS[FILE_TYPES.SURFACE_FLINGER_TRACE].decoderParams, true);

      // use final entry as this determines if there was any error in previous entry parsing
      const transformedEntry = data.entries[data.entries.length-1];
      const expectedEntry = expectedEntries[i];
      for (const property in expectedEntry) {
        expect(transformedEntry[property]).toEqual(expectedEntry[property]);
      }

      // check final flattened layer
      const transformedLayer = transformedEntry.flattenedLayers[transformedEntry.flattenedLayers.length-1];
      const expectedLayer = expectedLayers[i];
      for (const property in expectedLayer) {
        expect(transformedLayer[property]).toEqual(expectedLayer[property]);
      }
    }
  });
});