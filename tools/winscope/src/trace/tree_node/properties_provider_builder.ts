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

import {assertDefined} from 'common/assert_utils';
import {Operation} from 'trace/tree_node/operations/operation';
import {OperationChain} from 'trace/tree_node/operations/operation_chain';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {
  LazyPropertiesStrategyType,
  PropertiesProvider,
} from './properties_provider';

export class PropertiesProviderBuilder {
  private eagerProperties: PropertyTreeNode | undefined;
  private lazyPropertiesStrategy: LazyPropertiesStrategyType | undefined;
  private commonOperations = OperationChain.emptyChain<PropertyTreeNode>();
  private eagerOperations = OperationChain.emptyChain<PropertyTreeNode>();
  private lazyOperations = OperationChain.emptyChain<PropertyTreeNode>();

  setEagerProperties(value: PropertyTreeNode): this {
    this.eagerProperties = value;
    return this;
  }

  setLazyPropertiesStrategy(value: LazyPropertiesStrategyType): this {
    this.lazyPropertiesStrategy = value;
    return this;
  }

  setCommonOperations(operations: Array<Operation<PropertyTreeNode>>): this {
    this.commonOperations = new OperationChain<PropertyTreeNode>(operations);
    return this;
  }

  setEagerOperations(operations: Array<Operation<PropertyTreeNode>>): this {
    this.eagerOperations = new OperationChain<PropertyTreeNode>(operations);
    return this;
  }

  setLazyOperations(operations: Array<Operation<PropertyTreeNode>>): this {
    this.lazyOperations = new OperationChain<PropertyTreeNode>(operations);
    return this;
  }

  build(): PropertiesProvider {
    return new PropertiesProvider(
      assertDefined(this.eagerProperties),
      this.lazyPropertiesStrategy,
      this.commonOperations,
      this.eagerOperations,
      this.lazyOperations,
    );
  }
}
