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

export abstract class TreeBuilder<T, U> {
  protected id: string | undefined;
  protected name: string | undefined;
  protected children: U[] = [];

  setName(value: string): this {
    this.name = value;
    return this;
  }

  setChildren(value: U[]): this {
    this.children = value;
    return this;
  }

  build(): T {
    if (this.id === undefined) {
      throw new Error('id not set');
    }
    if (this.name === undefined) {
      throw new Error('name not set');
    }

    const rootNode = this.makeRootNode();

    this.children.forEach((child) =>
      this.addOrReplaceChildNode(rootNode, child),
    );

    return rootNode;
  }

  protected abstract makeRootNode(): T;
  protected abstract addOrReplaceChildNode(rootNode: T, child: U): void;
}
