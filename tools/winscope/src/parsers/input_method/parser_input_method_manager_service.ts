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

class ParserInputMethodManagerService extends AbstractParser<HierarchyTreeNode> {
  private static readonly MAGIC_NUMBER = [
    0x09, 0x49, 0x4d, 0x4d, 0x54, 0x52, 0x41, 0x43, 0x45,
  ]; // .IMMTRACE

  private static readonly ENTRY_DENYLIST_PROPERTIES = [
    'inputMethodManagerService',
  ];
  private static readonly ENTRY_EAGER_PROPERTIES = ['where'];
  private static readonly SERVICE_EAGER_PROPERTIES = [
    'curMethodId',
    'curFocusedWindowName',
    'lastImeTargetWindowName',
    'inputShown',
  ];

  private static readonly InputMethodManagerServiceTraceFileProto =
    TamperedMessageType.tamper(
      root.lookupType(
        'android.view.inputmethod.InputMethodManagerServiceTraceFileProto',
      ),
    );
  private static readonly entryField =
    ParserInputMethodManagerService.InputMethodManagerServiceTraceFileProto
      .fields['entry'];
  private static readonly serviceField = assertDefined(
    ParserInputMethodManagerService.entryField.tamperedMessageType,
  ).fields['inputMethodManagerService'];

  private static readonly Operations = {
    SetFormattersService: new SetFormatters(
      ParserInputMethodManagerService.serviceField,
    ),
    TranslateIntDefService: new TranslateIntDef(
      ParserInputMethodManagerService.serviceField,
    ),
    AddDefaultsServiceEager: new AddDefaults(
      ParserInputMethodManagerService.serviceField,
      ParserInputMethodManagerService.SERVICE_EAGER_PROPERTIES,
    ),
    AddDefaultsServiceLazy: new AddDefaults(
      ParserInputMethodManagerService.serviceField,
      undefined,
      ParserInputMethodManagerService.SERVICE_EAGER_PROPERTIES,
    ),
    SetFormattersEntry: new SetFormatters(
      ParserInputMethodManagerService.entryField,
    ),
    AddDefaultsEntryEager: new AddDefaults(
      ParserInputMethodManagerService.entryField,
      ParserInputMethodManagerService.ENTRY_EAGER_PROPERTIES,
    ),
    AddDefaultsEntryLazy: new AddDefaults(
      ParserInputMethodManagerService.entryField,
      undefined,
      ParserInputMethodManagerService.ENTRY_EAGER_PROPERTIES.concat(
        ParserInputMethodManagerService.ENTRY_DENYLIST_PROPERTIES,
      ),
    ),
  };

  private realToElapsedTimeOffsetNs: undefined | bigint;

  override getTraceType(): TraceType {
    return TraceType.INPUT_METHOD_MANAGER_SERVICE;
  }

  override getMagicNumber(): number[] {
    return ParserInputMethodManagerService.MAGIC_NUMBER;
  }

  override decodeTrace(
    buffer: Uint8Array,
  ): android.view.inputmethod.IInputMethodManagerServiceTraceProto[] {
    const decoded =
      ParserInputMethodManagerService.InputMethodManagerServiceTraceFileProto.decode(
        buffer,
      ) as android.view.inputmethod.IInputMethodManagerServiceTraceFileProto;
    const timeOffset = BigInt(
      decoded.realToElapsedTimeOffsetNanos?.toString() ?? '0',
    );
    this.realToElapsedTimeOffsetNs = timeOffset !== 0n ? timeOffset : undefined;
    return decoded.entry ?? [];
  }

  protected override getTimestamp(
    type: TimestampType,
    entry: android.view.inputmethod.IInputMethodManagerServiceTraceProto,
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

  protected override processDecodedEntry(
    index: number,
    timestampType: TimestampType,
    entry: android.view.inputmethod.IInputMethodManagerServiceTraceProto,
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
    entryProto: android.view.inputmethod.IInputMethodManagerServiceTraceProto,
  ): HierarchyTreeNode {
    const entry = new PropertiesProviderBuilder()
      .setEagerProperties(this.makeEntryEagerPropertiesTree(entryProto))
      .setLazyPropertiesStrategy(
        this.makeEntryLazyPropertiesStrategy(entryProto),
      )
      .setEagerOperations([
        ParserInputMethodManagerService.Operations.AddDefaultsEntryEager,
      ])
      .setCommonOperations([
        ParserInputMethodManagerService.Operations.SetFormattersEntry,
      ])
      .setLazyOperations([
        ParserInputMethodManagerService.Operations.AddDefaultsEntryLazy,
      ])
      .build();

    const inputMethodManagerService = entryProto.inputMethodManagerService
      ? new PropertiesProviderBuilder()
          .setEagerProperties(
            this.makeServiceEagerPropertiesTree(
              entryProto.inputMethodManagerService,
            ),
          )
          .setLazyPropertiesStrategy(
            this.makeServiceLazyPropertiesStrategy(
              entryProto.inputMethodManagerService,
            ),
          )
          .setEagerOperations([
            ParserInputMethodManagerService.Operations.AddDefaultsServiceEager,
          ])
          .setCommonOperations([
            ParserInputMethodManagerService.Operations.SetFormattersService,
            ParserInputMethodManagerService.Operations.TranslateIntDefService,
          ])
          .setLazyOperations([
            ParserInputMethodManagerService.Operations.AddDefaultsServiceLazy,
          ])
          .build()
      : undefined;

    return new HierarchyTreeBuilderInputMethod()
      .setRoot(entry)
      .setChildren(inputMethodManagerService ? [inputMethodManagerService] : [])
      .build();
  }

  private makeEntryEagerPropertiesTree(
    entryProto: android.view.inputmethod.IInputMethodManagerServiceTraceProto,
  ): PropertyTreeNode {
    const denyList: string[] = [];
    Object.getOwnPropertyNames(entryProto).forEach((it) => {
      if (
        !ParserInputMethodManagerService.ENTRY_EAGER_PROPERTIES.includes(it)
      ) {
        denyList.push(it);
      }
    });

    return new PropertyTreeBuilderFromProto()
      .setData(entryProto)
      .setRootId('InputMethodManagerService')
      .setRootName('entry')
      .setDenyList(denyList)
      .build();
  }

  private makeEntryLazyPropertiesStrategy(
    entryProto: android.view.inputmethod.IInputMethodManagerServiceTraceProto,
  ): LazyPropertiesStrategyType {
    return async () => {
      return new PropertyTreeBuilderFromProto()
        .setData(entryProto)
        .setRootId('InputMethodManagerService')
        .setRootName('entry')
        .setDenyList(
          ParserInputMethodManagerService.ENTRY_EAGER_PROPERTIES.concat(
            ParserInputMethodManagerService.ENTRY_DENYLIST_PROPERTIES,
          ),
        )
        .build();
    };
  }

  private makeServiceEagerPropertiesTree(
    serviceProto: android.server.inputmethod.IInputMethodManagerServiceProto,
  ): PropertyTreeNode {
    const denyList: string[] = [];
    let data: any = serviceProto;
    if (serviceProto) {
      Object.getOwnPropertyNames(serviceProto).forEach((it) => {
        if (
          !ParserInputMethodManagerService.SERVICE_EAGER_PROPERTIES.includes(it)
        ) {
          denyList.push(it);
        }
      });
    } else {
      data = {inputMethodManagerService: null};
    }

    return new PropertyTreeBuilderFromProto()
      .setData(data)
      .setRootId('InputMethodManagerService')
      .setRootName('inputMethodManagerService')
      .setDenyList(denyList)
      .setVisitPrototype(false)
      .build();
  }

  private makeServiceLazyPropertiesStrategy(
    serviceProto: android.server.inputmethod.IInputMethodManagerServiceProto,
  ): LazyPropertiesStrategyType {
    return async () => {
      return new PropertyTreeBuilderFromProto()
        .setData(serviceProto ?? {inputMethodManagerService: null})
        .setRootId('InputMethodManagerService')
        .setRootName('inputMethodManagerService')
        .setDenyList(ParserInputMethodManagerService.SERVICE_EAGER_PROPERTIES)
        .setVisitPrototype(false)
        .build();
    };
  }
}

export {ParserInputMethodManagerService};
