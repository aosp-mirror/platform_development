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

import {HierarchyTreeBuilder} from 'test/unit/hierarchy_tree_builder';
import {TraceType} from 'trace/trace_type';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {PropertySource} from 'trace/tree_node/property_tree_node';
import {AbstractPresenterInputMethodTest} from 'viewers/common/abstract_presenter_input_method_test';
import {TextFilter} from 'viewers/common/text_filter';
import {PresenterInputMethodClients} from './presenter_input_method_clients';

class PresenterInputMethodClientsTest extends AbstractPresenterInputMethodTest {
  override readonly numberOfDefaultProperties = 1;
  override readonly numberOfNonDefaultProperties = 2;
  override readonly propertiesFilter = new TextFilter('where', []);
  override readonly numberOfFilteredProperties = 1;

  protected override readonly PresenterInputMethod =
    PresenterInputMethodClients;
  protected override readonly imeTraceType = TraceType.INPUT_METHOD_CLIENTS;
  protected override readonly numberOfFlattenedChildren = 11;
  protected override readonly numberOfVisibleChildren = 1;
  protected override readonly numberOfNestedChildren = 2;

  override getSelectedNode(): HierarchyTreeNode {
    return new HierarchyTreeBuilder()
      .setId('InputMethodClients')
      .setName('entry')
      .setProperties({where: 'location', elapsedNanos: 0})
      .addChildProperty({
        name: 'test default property',
        value: 0,
        source: PropertySource.DEFAULT,
      })
      .build();
  }
}

describe('PresenterInputMethodClients', () => {
  new PresenterInputMethodClientsTest().execute();
});
