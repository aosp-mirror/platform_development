/*
 * Copyright (C) 2022 The Android Open Source Project
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

import {assertDefined} from 'common/assert_utils';
import {Timestamp, TimestampType} from 'common/time';
import {LayerTraceEntry} from 'flickerlib/layers/LayerTraceEntry';
import {AbstractParser} from 'parsers/abstract_parser';
import {AddDefaults} from 'parsers/operations/add_defaults';
import {SetFormatters} from 'parsers/operations/set_formatters';
import {TranslateIntDef} from 'parsers/operations/translate_intdef';
import {TamperedMessageType} from 'parsers/tampered_message_type';
import root from 'protos/surfaceflinger/udc/json';
import {android} from 'protos/surfaceflinger/udc/static';
import {
  CustomQueryParserResultTypeMap,
  CustomQueryType,
  VisitableParserCustomQuery,
} from 'trace/custom_query';
import {EntriesRange} from 'trace/trace';
import {TraceFile} from 'trace/trace_file';
import {TraceType} from 'trace/trace_type';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {PropertiesProvider} from 'trace/tree_node/properties_provider';
import {PropertiesProviderBuilder} from 'trace/tree_node/properties_provider_builder';
import {HierarchyTreeBuilderSf} from './hierarchy_tree_builder_sf';
import {AddDisplayProperties} from './operations/add_display_properties';
import {AddExcludesCompositionState} from './operations/add_excludes_composition_state';
import {AddVerboseFlags} from './operations/add_verbose_flags';
import {UpdateTransforms} from './operations/update_transforms';
import {ParserSfUtils} from './parser_surface_flinger_utils';

class ParserSurfaceFlinger extends AbstractParser {
  private static readonly LayersTraceFileProto = TamperedMessageType.tamper(
    root.lookupType('android.surfaceflinger.LayersTraceFileProto')
  );
  private readonly entryField = ParserSurfaceFlinger.LayersTraceFileProto.fields['entry'];
  private readonly entryType = assertDefined(this.entryField.tamperedMessageType);

  private readonly layerField = assertDefined(this.entryType.fields['layers'].tamperedMessageType)
    .fields['layers'];
  private readonly layerType = assertDefined(this.layerField.tamperedMessageType);

  private realToElapsedTimeOffsetNs: undefined | bigint;
  private static readonly MAGIC_NUMBER = [0x09, 0x4c, 0x59, 0x52, 0x54, 0x52, 0x41, 0x43, 0x45]; // .LYRTRACE

  constructor(trace: TraceFile) {
    super(trace);
    this.realToElapsedTimeOffsetNs = undefined;
  }

  override getTraceType(): TraceType {
    return TraceType.SURFACE_FLINGER;
  }

  override getMagicNumber(): number[] {
    return ParserSurfaceFlinger.MAGIC_NUMBER;
  }

  override decodeTrace(buffer: Uint8Array): android.surfaceflinger.ILayersTraceProto[] {
    const decoded = ParserSurfaceFlinger.LayersTraceFileProto.decode(
      buffer
    ) as android.surfaceflinger.ILayersTraceFileProto;
    const timeOffset = BigInt(decoded.realToElapsedTimeOffsetNanos?.toString() ?? '0');
    this.realToElapsedTimeOffsetNs = timeOffset !== 0n ? timeOffset : undefined;
    return decoded.entry ?? [];
  }

  override getTimestamp(
    type: TimestampType,
    entry: android.surfaceflinger.ILayersTraceProto
  ): undefined | Timestamp {
    const isDump = !Object.prototype.hasOwnProperty.call(entry, 'elapsedRealtimeNanos');
    if (type === TimestampType.ELAPSED) {
      return isDump
        ? new Timestamp(type, 0n)
        : new Timestamp(type, BigInt(assertDefined(entry.elapsedRealtimeNanos).toString()));
    } else if (type === TimestampType.REAL && this.realToElapsedTimeOffsetNs !== undefined) {
      return isDump
        ? new Timestamp(type, 0n)
        : new Timestamp(
            type,
            this.realToElapsedTimeOffsetNs +
              BigInt(assertDefined(entry.elapsedRealtimeNanos).toString())
          );
    }
    return undefined;
  }

  override processDecodedEntry(
    index: number,
    timestampType: TimestampType,
    entry: android.surfaceflinger.ILayersTraceProto
  ): LayerTraceEntry {
    return LayerTraceEntry.fromProto(
      entry?.layers?.layers,
      entry.displays,
      BigInt(assertDefined(entry.elapsedRealtimeNanos).toString()),
      entry.vsyncId,
      entry.hwcBlob,
      entry.where,
      this.realToElapsedTimeOffsetNs,
      timestampType === TimestampType.ELAPSED /*useElapsedTime*/,
      entry.excludesCompositionState ?? false
    );
  }

  override customQuery<Q extends CustomQueryType>(
    type: Q,
    entriesRange: EntriesRange
  ): Promise<CustomQueryParserResultTypeMap[Q]> {
    return new VisitableParserCustomQuery(type)
      .visit(CustomQueryType.VSYNCID, () => {
        const result = this.decodedEntries
          .slice(entriesRange.start, entriesRange.end)
          .map((entry) => {
            return BigInt(entry.vsyncId.toString()); // convert Long to bigint
          });
        return Promise.resolve(result);
      })
      .visit(CustomQueryType.SF_LAYERS_ID_AND_NAME, () => {
        const result: Array<{id: number; name: string}> = [];
        this.decodedEntries
          .slice(entriesRange.start, entriesRange.end)
          .forEach((entry: android.surfaceflinger.ILayersTraceProto) => {
            entry.layers?.layers?.forEach((layer: android.surfaceflinger.ILayerProto) => {
              result.push({id: assertDefined(layer.id), name: assertDefined(layer.name)});
            });
          });
        return Promise.resolve(result);
      })
      .getResult();
  }

  private makeHierarchyTree(
    entryProto: android.surfaceflinger.ILayersTraceProto
  ): HierarchyTreeNode {
    const excludesCompositionState = entryProto?.excludesCompositionState ?? false;

    const processed = new Map<number, number>();

    const layers: PropertiesProvider[] = assertDefined(entryProto.layers?.layers).map(
      (layer: android.surfaceflinger.ILayerProto) => {
        const duplicateCount = processed.get(assertDefined(layer.id)) ?? 0;
        processed.set(assertDefined(layer.id), duplicateCount + 1);
        const eagerProperties = ParserSfUtils.makeEagerPropertiesTree(layer, duplicateCount);
        const lazyProperties = ParserSfUtils.makeLayerLazyPropertiesStrategy(layer, duplicateCount);

        const layerProps = new PropertiesProviderBuilder()
          .setEagerProperties(eagerProperties)
          .setLazyPropertiesStrategy(lazyProperties)
          .addCommonOperation(new UpdateTransforms())
          .addCommonOperation(new AddVerboseFlags())
          .addCommonOperation(new AddExcludesCompositionState(excludesCompositionState))
          .addCommonOperation(new SetFormatters(this.layerField, ParserSfUtils.CUSTOM_FORMATTERS))
          .addCommonOperation(new TranslateIntDef(this.layerField))
          .addEagerOperation(new AddDefaults(this.layerType, ParserSfUtils.EAGER_PROPERTIES))
          .addLazyOperation(
            new AddDefaults(
              this.layerType,
              undefined,
              ParserSfUtils.EAGER_PROPERTIES.concat(ParserSfUtils.DENYLIST_PROPERTIES)
            )
          )
          .build();
        return layerProps;
      }
    );

    const entry = new PropertiesProviderBuilder()
      .setEagerProperties(ParserSfUtils.makeEntryEagerPropertiesTree(entryProto))
      .setLazyPropertiesStrategy(ParserSfUtils.makeEntryLazyPropertiesStrategy(entryProto))
      .addEagerOperation(new AddDefaults(this.entryType, ['displays']))
      .addLazyOperation(
        new AddDefaults(this.entryType, undefined, ParserSfUtils.DENYLIST_PROPERTIES)
      )
      .addCommonOperation(new AddDisplayProperties())
      .addCommonOperation(new SetFormatters(this.entryField))
      .addCommonOperation(new TranslateIntDef(this.entryField))
      .build();

    return new HierarchyTreeBuilderSf()
      .setEntry(entry)
      .setLayers(layers)
      .setExcludesCompositionState(excludesCompositionState)
      .build();
  }
}

export {ParserSurfaceFlinger};
