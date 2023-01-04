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

import {PropertiesTreeNode} from './ui_tree_utils';

class PropertiesTreeGenerator {
  generate(key: string, value: any): PropertiesTreeNode {
    if (this.isLeaf(value)) {
      return {
        propertyKey: key,
        propertyValue: this.leafToString(value)!,
      };
    }

    let children: PropertiesTreeNode[];

    if (Array.isArray(value)) {
      children = value.map((element, index) => this.generate('' + index, element));
    } else {
      children = Object.keys(value).map((childName) => this.generate(childName, value[childName]));
    }

    return {
      propertyKey: key,
      children,
    };
  }

  private isLeaf(value: any): boolean {
    return this.leafToString(value) !== undefined;
  }

  private leafToString(value: any): undefined | string {
    if (value == null) {
      return '';
    }
    if (typeof value === 'boolean') {
      return '' + value;
    }
    if (typeof value === 'number') {
      return '' + value;
    }
    if (typeof value === 'string') {
      return value;
    }
    if (this.isLong(value)) {
      return value.toString();
    }
    if (Array.isArray(value) && value.length === 0) {
      return '[]';
    }
    if (typeof value === 'object' && Object.keys(value).length === 0) {
      return '{}';
    }
    return undefined;
  }

  private isLong(value: any) {
    return (
      Object.prototype.hasOwnProperty.call(value, 'high') &&
      Object.prototype.hasOwnProperty.call(value, 'low') &&
      Object.prototype.hasOwnProperty.call(value, 'unsigned')
    );
  }
}

export {PropertiesTreeGenerator};
