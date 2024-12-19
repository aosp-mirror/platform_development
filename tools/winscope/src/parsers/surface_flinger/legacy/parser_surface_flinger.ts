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
import {Timestamp} from 'common/time/time';
import {AbstractParser} from 'parsers/legacy/abstract_parser';
import {AddDefaults} from 'parsers/operations/add_defaults';
import {SetFormatters} from 'parsers/operations/set_formatters';
import {TranslateIntDef} from 'parsers/operations/translate_intdef';
import {DENYLIST_PROPERTIES} from 'parsers/surface_flinger/denylist_properties';
import {EAGER_PROPERTIES} from 'parsers/surface_flinger/eager_properties';
import {EntryHierarchyTreeFactory} from 'parsers/surface_flinger/entry_hierarchy_tree_factory';
import {TamperedMessageType} from 'parsers/tampered_message_type';
import root from 'protos/surfaceflinger/udc/json';
import {android} from 'protos/surfaceflinger/udc/static';
import {
  CustomQueryParserResultTypeMap,
  CustomQueryType,
  VisitableParserCustomQuery,
} from 'trace/custom_query';
import {EntriesRange} from 'trace/trace';
import {TraceType} from 'trace/trace_type';
import {EnumFormatter, LAYER_ID_FORMATTER} from 'trace/tree_node/formatters';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';

class ParserSurfaceFlinger extends AbstractParser<HierarchyTreeNode> {
  private static readonly MAGIC_NUMBER = [
    0x09, 0x4c, 0x59, 0x52, 0x54, 0x52, 0x41, 0x43, 0x45,
  ]; // .LYRTRACE
  private static readonly CUSTOM_FORMATTERS = new Map([
    ['cropLayerId', LAYER_ID_FORMATTER],
    ['zOrderRelativeOf', LAYER_ID_FORMATTER],
    [
      'hwcCompositionType',
      new EnumFormatter(android.surfaceflinger.HwcCompositionType),
    ],
  ]);

  private static readonly LayersTraceFileProto = TamperedMessageType.tamper(
    root.lookupType('android.surfaceflinger.LayersTraceFileProto'),
  );
  private static readonly entryField =
    ParserSurfaceFlinger.LayersTraceFileProto.fields['entry'];
  private static readonly layerField = assertDefined(
    ParserSurfaceFlinger.entryField.tamperedMessageType?.fields['layers']
      .tamperedMessageType,
  ).fields['layers'];

  static readonly Operations = {
    SetFormattersLayer: new SetFormatters(
      ParserSurfaceFlinger.layerField,
      ParserSurfaceFlinger.CUSTOM_FORMATTERS,
    ),
    TranslateIntDefLayer: new TranslateIntDef(ParserSurfaceFlinger.layerField),
    AddDefaultsLayerEager: new AddDefaults(
      ParserSurfaceFlinger.layerField,
      EAGER_PROPERTIES,
    ),
    AddDefaultsLayerLazy: new AddDefaults(
      ParserSurfaceFlinger.layerField,
      undefined,
      EAGER_PROPERTIES.concat(DENYLIST_PROPERTIES),
    ),
    SetFormattersEntry: new SetFormatters(
      ParserSurfaceFlinger.entryField,
      ParserSurfaceFlinger.CUSTOM_FORMATTERS,
    ),
    TranslateIntDefEntry: new TranslateIntDef(ParserSurfaceFlinger.entryField),
    AddDefaultsEntryEager: new AddDefaults(ParserSurfaceFlinger.entryField, [
      'displays',
    ]),
    AddDefaultsEntryLazy: new AddDefaults(
      ParserSurfaceFlinger.entryField,
      undefined,
      DENYLIST_PROPERTIES,
    ),
  };

  private readonly factory = new EntryHierarchyTreeFactory();
  private realToMonotonicTimeOffsetNs: bigint | undefined;
  private isDump = false;

  override getTraceType(): TraceType {
    return TraceType.SURFACE_FLINGER;
  }

  override getMagicNumber(): number[] {
    return ParserSurfaceFlinger.MAGIC_NUMBER;
  }

  override getRealToBootTimeOffsetNs(): bigint | undefined {
    return undefined;
  }

  override getRealToMonotonicTimeOffsetNs(): bigint | undefined {
    return this.realToMonotonicTimeOffsetNs;
  }

  override decodeTrace(
    buffer: Uint8Array,
  ): android.surfaceflinger.ILayersTraceProto[] {
    const decoded = ParserSurfaceFlinger.LayersTraceFileProto.decode(
      buffer,
    ) as android.surfaceflinger.ILayersTraceFileProto;
    const timeOffset = BigInt(
      decoded.realToElapsedTimeOffsetNanos?.toString() ?? '0',
    );
    this.realToMonotonicTimeOffsetNs =
      timeOffset !== 0n ? timeOffset : undefined;
    this.isDump =
      decoded.entry?.length === 1 &&
      !Object.prototype.hasOwnProperty.call(
        decoded.entry[0],
        'elapsedRealtimeNanos',
      );
    return decoded.entry ?? [];
  }

  protected override getTimestamp(
    entry: android.surfaceflinger.ILayersTraceProto,
  ): Timestamp {
    if (this.isDump) {
      return this.timestampConverter.makeZeroTimestamp();
    }
    return this.timestampConverter.makeTimestampFromMonotonicNs(
      BigInt(assertDefined(entry.elapsedRealtimeNanos).toString()),
    );
  }

  override processDecodedEntry(
    index: number,
    entry: android.surfaceflinger.ILayersTraceProto,
  ): HierarchyTreeNode {
    return this.factory.makeEntryHierarchyTree(
      entry,
      assertDefined(entry.layers?.layers),
      ParserSurfaceFlinger,
    );
  }

  override customQuery<Q extends CustomQueryType>(
    type: Q,
    entriesRange: EntriesRange,
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
            entry.layers?.layers?.forEach(
              (layer: android.surfaceflinger.ILayerProto) => {
                result.push({
                  id: assertDefined(layer.id),
                  name: assertDefined(layer.name),
                });
              },
            );
          });
        return Promise.resolve(result);
      })
      .getResult();
  }
}

export {ParserSurfaceFlinger};
