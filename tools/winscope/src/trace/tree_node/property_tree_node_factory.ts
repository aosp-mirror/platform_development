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

import {Timestamp} from 'common/time/time';
import {TimeDuration} from 'common/time/time_duration';
import {
  PropertySource,
  PropertyTreeNode,
} from 'trace/tree_node/property_tree_node';

export class PropertyTreeNodeFactory {
  constructor(
    private denylistProperties: string[] = [],
    private visitPrototype = true,
  ) {}

  makePropertyRoot(
    rootId: string,
    rootName: string,
    source: PropertySource,
    value: any,
  ): PropertyTreeNode {
    return new PropertyTreeNode(rootId, rootName, source, value);
  }

  makeProtoProperty(
    rootId: string,
    name: string,
    value: any,
  ): PropertyTreeNode {
    return this.makeProperty(rootId, name, PropertySource.PROTO, value);
  }

  makeDefaultProperty(
    rootId: string,
    name: string,
    defaultValue: any,
  ): PropertyTreeNode {
    return this.makeSimpleChildProperty(
      rootId,
      name,
      defaultValue,
      PropertySource.DEFAULT,
    );
  }

  makeCalculatedProperty(
    rootId: string,
    propertyName: string,
    value: any,
  ): PropertyTreeNode {
    return this.makeProperty(
      rootId,
      propertyName,
      PropertySource.CALCULATED,
      value,
    );
  }

  private makeProperty(
    rootId: string,
    name: string,
    source: PropertySource,
    value: any,
  ): PropertyTreeNode {
    if (this.hasInnerProperties(value)) {
      return this.makeNestedProperty(rootId, name, source, value);
    } else {
      return this.makeSimpleChildProperty(rootId, name, value, source);
    }
  }

  private makeNestedProperty(
    rootId: string,
    name: string,
    source: PropertySource,
    value: object | any[],
  ): PropertyTreeNode {
    const rootName = rootId.split(' ');

    const innerRoot = this.makePropertyRoot(
      name.length > 0 ? `${rootId}.${name}` : rootId,
      name.length > 0 ? name : rootName.slice(1, rootName.length).join(' '),
      source,
      undefined,
    );
    this.addInnerProperties(innerRoot, value, source);

    return innerRoot;
  }

  private makeSimpleChildProperty(
    rootId: string,
    key: string,
    value: any,
    source: PropertySource,
  ): PropertyTreeNode {
    return new PropertyTreeNode(`${rootId}.${key}`, key, source, value);
  }

  private hasInnerProperties(value: any): boolean {
    if (!value) return false;
    if (Array.isArray(value)) return value.length > 0;
    if (this.isLongType(value)) return false;
    if (value instanceof Timestamp) return false;
    if (value instanceof TimeDuration) return false;
    return typeof value === 'object' && Object.keys(value).length > 0;
  }

  private isLongType(value: any): boolean {
    const typeOfVal = value.$type?.name ?? value.constructor?.name;
    if (typeOfVal === 'Long' || typeOfVal === 'BigInt') return true;
    return false;
  }

  private addInnerProperties(
    root: PropertyTreeNode,
    value: object | any[],
    source: PropertySource,
  ): void {
    if (Array.isArray(value)) {
      this.addArrayProperties(root, value, source);
    } else {
      this.addObjectProperties(root, value, source);
    }
  }

  private addArrayProperties(
    root: PropertyTreeNode,
    value: any[],
    source: PropertySource,
  ) {
    for (const [key, val] of Object.entries(value)) {
      root.addOrReplaceChild(this.makeProperty(`${root.id}`, key, source, val));
    }
  }

  private addObjectProperties(
    root: PropertyTreeNode,
    value: any,
    source: PropertySource,
  ) {
    const keys = this.getValidPropertyNames(value);

    for (const key of keys) {
      root.addOrReplaceChild(
        this.makeProperty(`${root.id}`, key, source, value[key]),
      );
    }
  }

  private getValidPropertyNames(objProto: any): string[] {
    if (objProto === null || objProto === undefined) {
      return [];
    }
    const props: string[] = [];
    let obj = objProto;

    do {
      const properties = Object.getOwnPropertyNames(obj).filter((it) => {
        if (typeof objProto[it] === 'function') return false;
        if (it.includes(`$`)) return false;
        if (it.startsWith(`_`)) return false;
        if (this.denylistProperties.includes(it)) return false;

        const value = objProto[it];
        if (Array.isArray(value) && value.length > 0) return !value[0].stableId;

        return value !== undefined;
      });

      properties.forEach((prop) => {
        if (
          typeof objProto[prop] !== 'function' &&
          props.indexOf(prop) === -1
        ) {
          props.push(prop);
        }
      });
      obj = this.visitPrototype ? Object.getPrototypeOf(obj) : undefined;
    } while (obj);
    return props;
  }
}

export const DEFAULT_PROPERTY_TREE_NODE_FACTORY = new PropertyTreeNodeFactory();
