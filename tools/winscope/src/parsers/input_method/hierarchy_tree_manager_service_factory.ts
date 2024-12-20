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

export class HierarchyTreeManagerServiceFactory {
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

  private SetFormattersService: SetFormatters;
  private TranslateIntDefService: TranslateIntDef;
  private AddDefaultsServiceEager: AddDefaults;
  private AddDefaultsServiceLazy: AddDefaults;
  private SetFormattersEntry: SetFormatters;
  private AddDefaultsEntryEager: AddDefaults;
  private AddDefaultsEntryLazy: AddDefaults;

  constructor(
    entryField: TamperedProtoField,
    managerServiceField: TamperedProtoField,
  ) {
    this.SetFormattersService = new SetFormatters(managerServiceField);
    this.TranslateIntDefService = new TranslateIntDef(managerServiceField);
    this.AddDefaultsServiceEager = new AddDefaults(
      managerServiceField,
      HierarchyTreeManagerServiceFactory.SERVICE_EAGER_PROPERTIES,
    );
    this.AddDefaultsServiceLazy = new AddDefaults(
      managerServiceField,
      undefined,
      HierarchyTreeManagerServiceFactory.SERVICE_EAGER_PROPERTIES,
    );
    this.SetFormattersEntry = new SetFormatters(entryField);
    this.AddDefaultsEntryEager = new AddDefaults(
      entryField,
      HierarchyTreeManagerServiceFactory.ENTRY_EAGER_PROPERTIES,
    );
    this.AddDefaultsEntryLazy = new AddDefaults(
      entryField,
      undefined,
      HierarchyTreeManagerServiceFactory.ENTRY_EAGER_PROPERTIES.concat(
        HierarchyTreeManagerServiceFactory.ENTRY_DENYLIST_PROPERTIES,
      ),
    );
  }

  makeHierarchyTree(
    entryProto:
      | android.view.inputmethod.IInputMethodManagerServiceTraceProto
      | perfetto.protos.IInputMethodManagerServiceTraceProto,
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
      .setChildren(inputMethodManagerService ? [inputMethodManagerService] : [])
      .build();
  }

  private makeEntryEagerPropertiesTree(
    entryProto:
      | android.view.inputmethod.IInputMethodManagerServiceTraceProto
      | perfetto.protos.IInputMethodManagerServiceTraceProto,
  ): PropertyTreeNode {
    const denyList: string[] = [];
    Object.getOwnPropertyNames(entryProto).forEach((it) => {
      if (
        !HierarchyTreeManagerServiceFactory.ENTRY_EAGER_PROPERTIES.includes(it)
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
    entryProto: perfetto.protos.IInputMethodManagerServiceTraceProto,
  ): LazyPropertiesStrategyType {
    return async () => {
      return new PropertyTreeBuilderFromProto()
        .setData(entryProto)
        .setRootId('InputMethodManagerService')
        .setRootName('entry')
        .setDenyList(
          HierarchyTreeManagerServiceFactory.ENTRY_EAGER_PROPERTIES.concat(
            HierarchyTreeManagerServiceFactory.ENTRY_DENYLIST_PROPERTIES,
          ),
        )
        .build();
    };
  }

  private makeServiceEagerPropertiesTree(
    serviceProto: android.server.inputmethod.IInputMethodManagerServiceProto,
  ): PropertyTreeNode {
    const denyList: string[] = [];
    let data: object = serviceProto;
    if (serviceProto) {
      Object.getOwnPropertyNames(serviceProto).forEach((it) => {
        if (
          !HierarchyTreeManagerServiceFactory.SERVICE_EAGER_PROPERTIES.includes(
            it,
          )
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
        .setDenyList(
          HierarchyTreeManagerServiceFactory.SERVICE_EAGER_PROPERTIES,
        )
        .setVisitPrototype(false)
        .build();
    };
  }
}
