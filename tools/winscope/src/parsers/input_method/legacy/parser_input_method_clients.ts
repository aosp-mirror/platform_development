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
import {Timestamp} from 'common/time';
import {HierarchyTreeBuilderInputMethod} from 'parsers/input_method/hierarchy_tree_builder_input_method';
import {AbstractParser} from 'parsers/legacy/abstract_parser';
import {AddDefaults} from 'parsers/operations/add_defaults';
import {SetFormatters} from 'parsers/operations/set_formatters';
import {TranslateIntDef} from 'parsers/operations/translate_intdef';
import {TamperedMessageType} from 'parsers/tampered_message_type';
import root from 'protos/ime/udc/json';
import {android} from 'protos/ime/udc/static';
import {TraceType} from 'trace/trace_type';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {LazyPropertiesStrategyType} from 'trace/tree_node/properties_provider';
import {PropertiesProviderBuilder} from 'trace/tree_node/properties_provider_builder';
import {PropertyTreeBuilderFromProto} from 'trace/tree_node/property_tree_builder_from_proto';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';

class ParserInputMethodClients extends AbstractParser<HierarchyTreeNode> {
  private static readonly MAGIC_NUMBER = [
    0x09, 0x49, 0x4d, 0x43, 0x54, 0x52, 0x41, 0x43, 0x45,
  ]; // .IMCTRACE

  private static readonly ENTRY_DENYLIST_PROPERTIES = ['client'];
  private static readonly ENTRY_EAGER_PROPERTIES = ['where'];
  private static readonly CLIENT_EAGER_PROPERTIES = [
    'viewRootImpl',
    'inputMethodManager',
    'editorInfo',
  ];

  private static readonly InputMethodClientsTraceFileProto =
    TamperedMessageType.tamper(
      root.lookupType(
        'android.view.inputmethod.InputMethodClientsTraceFileProto',
      ),
    );
  private static readonly entryField =
    ParserInputMethodClients.InputMethodClientsTraceFileProto.fields['entry'];
  private static readonly clientField = assertDefined(
    ParserInputMethodClients.entryField.tamperedMessageType,
  ).fields['client'];

  private static readonly Operations = {
    SetFormattersClient: new SetFormatters(
      ParserInputMethodClients.clientField,
    ),
    TranslateIntDefClient: new TranslateIntDef(
      ParserInputMethodClients.clientField,
    ),
    AddDefaultsClientEager: new AddDefaults(
      ParserInputMethodClients.clientField,
      ParserInputMethodClients.CLIENT_EAGER_PROPERTIES,
    ),
    AddDefaultsClientLazy: new AddDefaults(
      ParserInputMethodClients.clientField,
      undefined,
      ParserInputMethodClients.CLIENT_EAGER_PROPERTIES,
    ),
    SetFormattersEntry: new SetFormatters(ParserInputMethodClients.entryField),
    AddDefaultsEntryEager: new AddDefaults(
      ParserInputMethodClients.entryField,
      ParserInputMethodClients.ENTRY_EAGER_PROPERTIES,
    ),
    AddDefaultsEntryLazy: new AddDefaults(
      ParserInputMethodClients.entryField,
      undefined,
      ParserInputMethodClients.ENTRY_EAGER_PROPERTIES.concat(
        ParserInputMethodClients.ENTRY_DENYLIST_PROPERTIES,
      ),
    ),
  };

  private realToBootTimeOffsetNs: bigint | undefined;

  override getTraceType(): TraceType {
    return TraceType.INPUT_METHOD_CLIENTS;
  }

  override getMagicNumber(): number[] {
    return ParserInputMethodClients.MAGIC_NUMBER;
  }

  override getRealToBootTimeOffsetNs(): bigint | undefined {
    return this.realToBootTimeOffsetNs;
  }

  override getRealToMonotonicTimeOffsetNs(): bigint | undefined {
    return undefined;
  }

  override decodeTrace(
    buffer: Uint8Array,
  ): android.view.inputmethod.IInputMethodClientsTraceProto[] {
    const decoded =
      ParserInputMethodClients.InputMethodClientsTraceFileProto.decode(
        buffer,
      ) as android.view.inputmethod.IInputMethodClientsTraceFileProto;
    const timeOffset = BigInt(
      decoded.realToElapsedTimeOffsetNanos?.toString() ?? '0',
    );
    this.realToBootTimeOffsetNs = timeOffset !== 0n ? timeOffset : undefined;
    return decoded.entry ?? [];
  }

  protected override getTimestamp(
    entry: android.view.inputmethod.IInputMethodClientsTraceProto,
  ): Timestamp {
    return this.timestampConverter.makeTimestampFromBootTimeNs(
      BigInt(assertDefined(entry.elapsedRealtimeNanos).toString()),
    );
  }

  override processDecodedEntry(
    index: number,
    entry: android.view.inputmethod.IInputMethodClientsTraceProto,
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
    entryProto: android.view.inputmethod.IInputMethodClientsTraceProto,
  ): HierarchyTreeNode {
    const entry = new PropertiesProviderBuilder()
      .setEagerProperties(this.makeEntryEagerPropertiesTree(entryProto))
      .setLazyPropertiesStrategy(
        this.makeEntryLazyPropertiesStrategy(entryProto),
      )
      .setEagerOperations([
        ParserInputMethodClients.Operations.AddDefaultsEntryEager,
      ])
      .setCommonOperations([
        ParserInputMethodClients.Operations.SetFormattersEntry,
      ])
      .setLazyOperations([
        ParserInputMethodClients.Operations.AddDefaultsEntryLazy,
      ])
      .build();

    const client = new PropertiesProviderBuilder()
      .setEagerProperties(this.makeClientEagerPropertiesTree(entryProto.client))
      .setLazyPropertiesStrategy(
        this.makeClientLazyPropertiesStrategy(entryProto.client),
      )
      .setEagerOperations(
        entryProto.client
          ? [ParserInputMethodClients.Operations.AddDefaultsClientEager]
          : [],
      )
      .setCommonOperations([
        ParserInputMethodClients.Operations.SetFormattersClient,
        ParserInputMethodClients.Operations.TranslateIntDefClient,
      ])
      .setLazyOperations(
        entryProto.client
          ? [ParserInputMethodClients.Operations.AddDefaultsClientLazy]
          : [],
      )
      .build();

    return new HierarchyTreeBuilderInputMethod()
      .setRoot(entry)
      .setChildren([client])
      .build();
  }

  private makeEntryEagerPropertiesTree(
    entryProto: android.view.inputmethod.IInputMethodClientsTraceProto,
  ): PropertyTreeNode {
    const denyList: string[] = [];
    Object.getOwnPropertyNames(entryProto).forEach((it) => {
      if (!ParserInputMethodClients.ENTRY_EAGER_PROPERTIES.includes(it)) {
        denyList.push(it);
      }
    });

    return new PropertyTreeBuilderFromProto()
      .setData(entryProto)
      .setRootId('InputMethodClients')
      .setRootName('entry')
      .setDenyList(denyList)
      .build();
  }

  private makeEntryLazyPropertiesStrategy(
    entryProto: android.view.inputmethod.IInputMethodClientsTraceProto,
  ): LazyPropertiesStrategyType {
    return async () => {
      return new PropertyTreeBuilderFromProto()
        .setData(entryProto)
        .setRootId('InputMethodClients')
        .setRootName('entry')
        .setDenyList(
          ParserInputMethodClients.ENTRY_EAGER_PROPERTIES.concat(
            ParserInputMethodClients.ENTRY_DENYLIST_PROPERTIES,
          ),
        )
        .build();
    };
  }

  private makeClientEagerPropertiesTree(
    clientProto:
      | android.view.inputmethod.InputMethodClientsTraceProto.IClientSideProto
      | null
      | undefined,
  ): PropertyTreeNode {
    const denyList: string[] = [];
    let data: any = clientProto;
    if (clientProto) {
      Object.getOwnPropertyNames(clientProto).forEach((it) => {
        if (!ParserInputMethodClients.CLIENT_EAGER_PROPERTIES.includes(it)) {
          denyList.push(it);
        }
      });
    } else {
      data = {client: null};
    }

    return new PropertyTreeBuilderFromProto()
      .setData(data)
      .setRootId('InputMethodClients')
      .setRootName('client')
      .setDenyList(denyList)
      .setVisitPrototype(false)
      .build();
  }

  private makeClientLazyPropertiesStrategy(
    clientProto:
      | android.view.inputmethod.InputMethodClientsTraceProto.IClientSideProto
      | null
      | undefined,
  ): LazyPropertiesStrategyType {
    return async () => {
      return new PropertyTreeBuilderFromProto()
        .setData(clientProto ?? {client: null})
        .setRootId('InputMethodClients')
        .setRootName('client')
        .setDenyList(ParserInputMethodClients.CLIENT_EAGER_PROPERTIES)
        .setVisitPrototype(false)
        .build();
    };
  }
}

export {ParserInputMethodClients};
