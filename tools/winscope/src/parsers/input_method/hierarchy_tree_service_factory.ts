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

export class HierarchyTreeServiceFactory {
  private static readonly ENTRY_DENYLIST_PROPERTIES = ['inputMethodService'];
  private static readonly ENTRY_EAGER_PROPERTIES = ['where'];
  private static readonly SERVICE_EAGER_PROPERTIES = [
    'windowVisible',
    'decorViewVisible',
    'inputEditorInfo',
  ];

  private SetFormattersService: SetFormatters;
  private TranslateIntDefService: TranslateIntDef;
  private AddDefaultsServiceEager: AddDefaults;
  private AddDefaultsServiceLazy: AddDefaults;
  private SetFormattersEntry: SetFormatters;
  private AddDefaultsEntryEager: AddDefaults;
  private AddDefaultsEntryLazy: AddDefaults;

  constructor(
    entryField: TamperedProtoField,
    serviceField: TamperedProtoField,
  ) {
    this.SetFormattersService = new SetFormatters(serviceField);
    this.TranslateIntDefService = new TranslateIntDef(serviceField);
    this.AddDefaultsServiceEager = new AddDefaults(
      serviceField,
      HierarchyTreeServiceFactory.SERVICE_EAGER_PROPERTIES,
    );
    this.AddDefaultsServiceLazy = new AddDefaults(
      serviceField,
      undefined,
      HierarchyTreeServiceFactory.SERVICE_EAGER_PROPERTIES,
    );
    this.SetFormattersEntry = new SetFormatters(entryField);
    this.AddDefaultsEntryEager = new AddDefaults(
      entryField,
      HierarchyTreeServiceFactory.ENTRY_EAGER_PROPERTIES,
    );
    this.AddDefaultsEntryLazy = new AddDefaults(
      entryField,
      undefined,
      HierarchyTreeServiceFactory.ENTRY_EAGER_PROPERTIES.concat(
        HierarchyTreeServiceFactory.ENTRY_DENYLIST_PROPERTIES,
      ),
    );
  }

  makeHierarchyTree(
    entryProto:
      | android.view.inputmethod.IInputMethodServiceTraceProto
      | perfetto.protos.IInputMethodServiceTraceProto,
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
          .setEagerOperations([this.AddDefaultsServiceEager])
          .setCommonOperations([
            this.SetFormattersService,
            this.TranslateIntDefService,
          ])
          .setLazyOperations([this.AddDefaultsServiceLazy])
          .build()
      : undefined;

    return new HierarchyTreeBuilderInputMethod()
      .setRoot(entry)
      .setChildren(inputMethodService ? [inputMethodService] : [])
      .build();
  }

  private makeEntryEagerPropertiesTree(
    entryProto:
      | android.view.inputmethod.IInputMethodServiceTraceProto
      | perfetto.protos.IInputMethodServiceTraceProto,
  ): PropertyTreeNode {
    const denyList: string[] = [];
    Object.getOwnPropertyNames(entryProto).forEach((it) => {
      if (!HierarchyTreeServiceFactory.ENTRY_EAGER_PROPERTIES.includes(it)) {
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
    entryProto:
      | android.view.inputmethod.IInputMethodServiceTraceProto
      | perfetto.protos.IInputMethodServiceTraceProto,
  ): LazyPropertiesStrategyType {
    return async () => {
      return new PropertyTreeBuilderFromProto()
        .setData(entryProto)
        .setRootId('InputMethodService')
        .setRootName('entry')
        .setDenyList(
          HierarchyTreeServiceFactory.ENTRY_EAGER_PROPERTIES.concat(
            HierarchyTreeServiceFactory.ENTRY_DENYLIST_PROPERTIES,
          ),
        )
        .build();
    };
  }

  private makeServiceEagerPropertiesTree(
    serviceProto:
      | android.inputmethodservice.IInputMethodServiceProto
      | perfetto.protos.IInputMethodServiceProto,
  ): PropertyTreeNode {
    const denyList: string[] = [];
    let data: object = serviceProto;
    if (serviceProto) {
      Object.getOwnPropertyNames(serviceProto).forEach((it) => {
        if (
          !HierarchyTreeServiceFactory.SERVICE_EAGER_PROPERTIES.includes(it)
        ) {
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
    serviceProto:
      | android.inputmethodservice.IInputMethodServiceProto
      | perfetto.protos.IInputMethodServiceProto,
  ): LazyPropertiesStrategyType {
    return async () => {
      return new PropertyTreeBuilderFromProto()
        .setData(serviceProto ?? {inputMethodService: null})
        .setRootId('InputMethodService')
        .setRootName('inputMethodService')
        .setDenyList(HierarchyTreeServiceFactory.SERVICE_EAGER_PROPERTIES)
        .setVisitPrototype(false)
        .build();
    };
  }
}
