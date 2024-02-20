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
import {AbstractParser} from 'parsers/abstract_parser';
import {AddDefaults} from 'parsers/operations/add_defaults';
import {SetFormatters} from 'parsers/operations/set_formatters';
import {TranslateIntDef} from 'parsers/operations/translate_intdef';
import {TamperedMessageType} from 'parsers/tampered_message_type';
import root from 'protos/ime/latest/json';
import {android} from 'protos/ime/latest/static';
import {TraceType} from 'trace/trace_type';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {LazyPropertiesStrategyType} from 'trace/tree_node/properties_provider';
import {PropertiesProviderBuilder} from 'trace/tree_node/properties_provider_builder';
import {PropertyTreeBuilderFromProto} from 'trace/tree_node/property_tree_builder_from_proto';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {HierarchyTreeBuilderInputMethod} from './hierarchy_tree_builder_input_method';

class ParserInputMethodService extends AbstractParser<HierarchyTreeNode> {
  private static readonly MAGIC_NUMBER = [
    0x09, 0x49, 0x4d, 0x53, 0x54, 0x52, 0x41, 0x43, 0x45,
  ]; // .IMSTRACE

  private static readonly ENTRY_DENYLIST_PROPERTIES = ['inputMethodService'];
  private static readonly ENTRY_EAGER_PROPERTIES = ['where'];
  private static readonly SERVICE_EAGER_PROPERTIES = [
    'windowVisible',
    'decorViewVisible',
    'inputEditorInfo',
  ];

  private static readonly InputMethodServiceTraceFileProto =
    TamperedMessageType.tamper(
      root.lookupType(
        'android.view.inputmethod.InputMethodServiceTraceFileProto',
      ),
    );
  private static readonly entryField =
    ParserInputMethodService.InputMethodServiceTraceFileProto.fields['entry'];
  private static readonly serviceField = assertDefined(
    ParserInputMethodService.entryField.tamperedMessageType,
  ).fields['inputMethodService'];

  private static readonly Operations = {
    SetFormattersService: new SetFormatters(
      ParserInputMethodService.serviceField,
    ),
    TranslateIntDefService: new TranslateIntDef(
      ParserInputMethodService.serviceField,
    ),
    AddDefaultsServiceEager: new AddDefaults(
      ParserInputMethodService.serviceField,
      ParserInputMethodService.SERVICE_EAGER_PROPERTIES,
    ),
    AddDefaultsServiceLazy: new AddDefaults(
      ParserInputMethodService.serviceField,
      undefined,
      ParserInputMethodService.SERVICE_EAGER_PROPERTIES,
    ),
    SetFormattersEntry: new SetFormatters(ParserInputMethodService.entryField),
    AddDefaultsEntryEager: new AddDefaults(
      ParserInputMethodService.entryField,
      ParserInputMethodService.ENTRY_EAGER_PROPERTIES,
    ),
    AddDefaultsEntryLazy: new AddDefaults(
      ParserInputMethodService.entryField,
      undefined,
      ParserInputMethodService.ENTRY_EAGER_PROPERTIES.concat(
        ParserInputMethodService.ENTRY_DENYLIST_PROPERTIES,
      ),
    ),
  };

  private realToElapsedTimeOffsetNs: undefined | bigint;

  override getTraceType(): TraceType {
    return TraceType.INPUT_METHOD_SERVICE;
  }

  override getMagicNumber(): number[] {
    return ParserInputMethodService.MAGIC_NUMBER;
  }

  override decodeTrace(
    buffer: Uint8Array,
  ): android.view.inputmethod.IInputMethodServiceTraceProto[] {
    const decoded =
      ParserInputMethodService.InputMethodServiceTraceFileProto.decode(
        buffer,
      ) as android.view.inputmethod.IInputMethodServiceTraceFileProto;
    const timeOffset = BigInt(
      decoded.realToElapsedTimeOffsetNanos?.toString() ?? '0',
    );
    this.realToElapsedTimeOffsetNs = timeOffset !== 0n ? timeOffset : undefined;
    return decoded.entry ?? [];
  }

  override getTimestamp(
    type: TimestampType,
    entry: android.view.inputmethod.IInputMethodServiceTraceProto,
  ): undefined | Timestamp {
    const elapsedRealtimeNanos = BigInt(
      assertDefined(entry.elapsedRealtimeNanos).toString(),
    );
    if (
      this.timestampFactory.canMakeTimestampFromType(
        type,
        this.realToElapsedTimeOffsetNs,
      )
    ) {
      return this.timestampFactory.makeTimestampFromType(
        type,
        elapsedRealtimeNanos,
        this.realToElapsedTimeOffsetNs,
      );
    }
    return undefined;
  }

  override processDecodedEntry(
    index: number,
    timestampType: TimestampType,
    entry: android.view.inputmethod.IInputMethodServiceTraceProto,
  ): HierarchyTreeNode {
    if (
      entry.elapsedRealtimeNanos === undefined ||
      entry.elapsedRealtimeNanos === null
    ) {
      throw Error('Missing elapsedRealtimeNanos on entry');
    }
    return this.makeHierarchyTree(entry);
  }

  private makeHierarchyTree(
    entryProto: android.view.inputmethod.IInputMethodServiceTraceProto,
  ): HierarchyTreeNode {
    const entry = new PropertiesProviderBuilder()
      .setEagerProperties(this.makeEntryEagerPropertiesTree(entryProto))
      .setLazyPropertiesStrategy(
        this.makeEntryLazyPropertiesStrategy(entryProto),
      )
      .setEagerOperations([
        ParserInputMethodService.Operations.AddDefaultsEntryEager,
      ])
      .setCommonOperations([
        ParserInputMethodService.Operations.SetFormattersEntry,
      ])
      .setLazyOperations([
        ParserInputMethodService.Operations.AddDefaultsEntryLazy,
      ])
      .build();

    const inputMethodService = entryProto.inputMethodService
      ? new PropertiesProviderBuilder()
          .setEagerProperties(
            this.makeServiceEagerPropertiesTree(entryProto.inputMethodService),
          )
          .setLazyPropertiesStrategy(
            this.makeServiceLazyPropertiesStrategy(
              entryProto.inputMethodService,
            ),
          )
          .setEagerOperations([
            ParserInputMethodService.Operations.AddDefaultsServiceEager,
          ])
          .setCommonOperations([
            ParserInputMethodService.Operations.SetFormattersService,
            ParserInputMethodService.Operations.TranslateIntDefService,
          ])
          .setLazyOperations([
            ParserInputMethodService.Operations.AddDefaultsServiceLazy,
          ])
          .build()
      : undefined;

    return new HierarchyTreeBuilderInputMethod()
      .setRoot(entry)
      .setChildren(inputMethodService ? [inputMethodService] : [])
      .build();
  }

  private makeEntryEagerPropertiesTree(
    entryProto: android.view.inputmethod.IInputMethodServiceTraceProto,
  ): PropertyTreeNode {
    const denyList: string[] = [];
    Object.getOwnPropertyNames(entryProto).forEach((it) => {
      if (!ParserInputMethodService.ENTRY_EAGER_PROPERTIES.includes(it)) {
        denyList.push(it);
      }
    });

    return new PropertyTreeBuilderFromProto()
      .setData(entryProto)
      .setRootId('InputMethodService')
      .setRootName('entry')
      .setDenyList(denyList)
      .build();
  }

  private makeEntryLazyPropertiesStrategy(
    entryProto: android.view.inputmethod.IInputMethodServiceTraceProto,
  ): LazyPropertiesStrategyType {
    return async () => {
      return new PropertyTreeBuilderFromProto()
        .setData(entryProto)
        .setRootId('InputMethodService')
        .setRootName('entry')
        .setDenyList(
          ParserInputMethodService.ENTRY_EAGER_PROPERTIES.concat(
            ParserInputMethodService.ENTRY_DENYLIST_PROPERTIES,
          ),
        )
        .build();
    };
  }

  private makeServiceEagerPropertiesTree(
    serviceProto: android.inputmethodservice.IInputMethodServiceProto,
  ): PropertyTreeNode {
    const denyList: string[] = [];
    let data: any = serviceProto;
    if (serviceProto) {
      Object.getOwnPropertyNames(serviceProto).forEach((it) => {
        if (!ParserInputMethodService.SERVICE_EAGER_PROPERTIES.includes(it)) {
          denyList.push(it);
        }
      });
    } else {
      data = {inputMethodService: null};
    }
    return new PropertyTreeBuilderFromProto()
      .setData(serviceProto)
      .setRootId('InputMethodService')
      .setRootName('inputMethodService')
      .setDenyList(denyList)
      .setVisitPrototype(false)
      .build();
  }

  private makeServiceLazyPropertiesStrategy(
    serviceProto: android.inputmethodservice.IInputMethodServiceProto,
  ): LazyPropertiesStrategyType {
    return async () => {
      return new PropertyTreeBuilderFromProto()
        .setData(serviceProto ?? {inputMethodService: null})
        .setRootId('InputMethodService')
        .setRootName('inputMethodService')
        .setDenyList(ParserInputMethodService.SERVICE_EAGER_PROPERTIES)
        .setVisitPrototype(false)
        .build();
    };
  }
}

export {ParserInputMethodService};
