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

import {Operation} from 'trace/tree_node/operations/operation';
import {OperationChain} from 'trace/tree_node/operations/operation_chain';
import {TreeNode} from 'trace/tree_node/tree_node';

export class UiTreeFormatter<T extends TreeNode> {
  private uiTree: T | undefined;
  private operations = OperationChain.emptyChain<T>();

  setUiTree(value: T): this {
    this.uiTree = value;
    return this;
  }

  addOperation(operation: Operation<T>): this {
    this.operations.push(operation);
    return this;
  }

  format(): T {
    if (this.uiTree === undefined) {
      throw new Error('tree not set');
    }

    return this.operations.apply(this.uiTree);
  }
}
