/*
 * Copyright (C) 2024 The Android Open Source Project
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

import {HierarchyTreeBuilderInputMethod} from 'parsers/input_method/hierarchy_tree_builder_input_method';
import {AddDefaults} from 'parsers/operations/add_defaults';
import {SetFormatters} from 'parsers/operations/set_formatters';
import {TranslateIntDef} from 'parsers/operations/translate_intdef';
import {TamperedProtoField} from 'parsers/tampered_message_type';
import {perfetto} from 'protos/ime/latest/static';
import {android} from 'protos/ime/udc/static';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {LazyPropertiesStrategyType} from 'trace/tree_node/properties_provider';
import {PropertiesProviderBuilder} from 'trace/tree_node/properties_provider_builder';
import {PropertyTreeBuilderFromProto} from 'trace/tree_node/property_tree_builder_from_proto';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';

export class HierarchyTreeClientsFactory {
  private static readonly ENTRY_DENYLIST_PROPERTIES = ['client'];
  private static readonly ENTRY_EAGER_PROPERTIES = ['where'];
  private static readonly CLIENT_EAGER_PROPERTIES = [
    'viewRootImpl',
    'inputMethodManager',
    'editorInfo',
  ];

  private SetFormattersClient: SetFormatters;
  private TranslateIntDefClient: TranslateIntDef;
  private AddDefaultsClientEager: AddDefaults;
  private AddDefaultsClientLazy: AddDefaults;
  private SetFormattersEntry: SetFormatters;
  private AddDefaultsEntryEager: AddDefaults;
  private AddDefaultsEntryLazy: AddDefaults;

  constructor(entryField: TamperedProtoField, clientField: TamperedProtoField) {
    this.SetFormattersClient = new SetFormatters(clientField);
    this.TranslateIntDefClient = new TranslateIntDef(clientField);
    this.AddDefaultsClientEager = new AddDefaults(
      clientField,
      HierarchyTreeClientsFactory.CLIENT_EAGER_PROPERTIES,
    );
    this.AddDefaultsClientLazy = new AddDefaults(
      clientField,
      undefined,
      HierarchyTreeClientsFactory.CLIENT_EAGER_PROPERTIES,
    );
    this.SetFormattersEntry = new SetFormatters(entryField);
    this.AddDefaultsEntryEager = new AddDefaults(
      entryField,
      HierarchyTreeClientsFactory.ENTRY_EAGER_PROPERTIES,
    );
    this.AddDefaultsEntryLazy = new AddDefaults(
      entryField,
      undefined,
      HierarchyTreeClientsFactory.ENTRY_EAGER_PROPERTIES.concat(
        HierarchyTreeClientsFactory.ENTRY_DENYLIST_PROPERTIES,
      ),
    );
  }

  makeHierarchyTree(
    entryProto:
      | android.view.inputmethod.IInputMethodClientsTraceProto
      | perfetto.protos.IInputMethodClientsTraceProto,
  ): HierarchyTreeNode {
    const entry = new PropertiesProviderBuilder()
      .setEagerProperties(this.makeEntryEagerPropertiesTree(entryProto))
      .setLazyPropertiesStrategy(
        this.makeEntryLazyPropertiesStrategy(entryProto),
      )
      .setEagerOperations([this.AddDefaultsEntryEager])
      .setCommonOperations([this.SetFormattersEntry])
      .setLazyOperations([this.AddDefaultsEntryLazy])
      .build();

    const client = new PropertiesProviderBuilder()
      .setEagerProperties(this.makeClientEagerPropertiesTree(entryProto.client))
      .setLazyPropertiesStrategy(
        this.makeClientLazyPropertiesStrategy(entryProto.client),
      )
      .setEagerOperations(
        entryProto.client ? [this.AddDefaultsClientEager] : [],
      )
      .setCommonOperations([
        this.SetFormattersClient,
        this.TranslateIntDefClient,
      ])
      .setLazyOperations(entryProto.client ? [this.AddDefaultsClientLazy] : [])
      .build();

    return new HierarchyTreeBuilderInputMethod()
      .setRoot(entry)
      .setChildren([client])
      .build();
  }

  private makeEntryEagerPropertiesTree(
    entryProto:
      | android.view.inputmethod.IInputMethodClientsTraceProto
      | perfetto.protos.IInputMethodClientsTraceProto,
  ): PropertyTreeNode {
    const denyList: string[] = [];
    Object.getOwnPropertyNames(entryProto).forEach((it) => {
      if (!HierarchyTreeClientsFactory.ENTRY_EAGER_PROPERTIES.includes(it)) {
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
    entryProto:
      | android.view.inputmethod.IInputMethodClientsTraceProto
      | perfetto.protos.IInputMethodClientsTraceProto,
  ): LazyPropertiesStrategyType {
    return async () => {
      return new PropertyTreeBuilderFromProto()
        .setData(entryProto)
        .setRootId('InputMethodClients')
        .setRootName('entry')
        .setDenyList(
          HierarchyTreeClientsFactory.ENTRY_EAGER_PROPERTIES.concat(
            HierarchyTreeClientsFactory.ENTRY_DENYLIST_PROPERTIES,
          ),
        )
        .build();
    };
  }

  private makeClientEagerPropertiesTree(
    clientProto:
      | android.view.inputmethod.InputMethodClientsTraceProto.IClientSideProto
      | perfetto.protos.InputMethodClientsTraceProto.IClientSideProto
      | null
      | undefined,
  ): PropertyTreeNode {
    const denyList: string[] = [];
    let data: object | undefined = clientProto ?? undefined;
    if (clientProto) {
      Object.getOwnPropertyNames(clientProto).forEach((it) => {
        if (!HierarchyTreeClientsFactory.CLIENT_EAGER_PROPERTIES.includes(it)) {
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
      | perfetto.protos.InputMethodClientsTraceProto.IClientSideProto
      | null
      | undefined,
  ): LazyPropertiesStrategyType {
    return async () => {
      return new PropertyTreeBuilderFromProto()
        .setData(clientProto ?? {client: null})
        .setRootId('InputMethodClients')
        .setRootName('client')
        .setDenyList(HierarchyTreeClientsFactory.CLIENT_EAGER_PROPERTIES)
        .setVisitPrototype(false)
        .build();
    };
  }
}
