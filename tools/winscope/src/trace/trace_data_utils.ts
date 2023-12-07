/*
 * Copyright (C) 2023 The Android Open Source Project
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

import {Rect} from 'common/geometry_utils';
import {Transform} from 'parsers/transform_utils';

/* DATA INTERFACES */
class Item {
  constructor(public id: string, public label: string) {}
}

class TreeNode<T> extends Item {
  constructor(id: string, label: string, public children: Array<TreeNode<T> & T> = []) {
    super(id, label);
  }
}

class TraceRect extends Item implements Rect {
  constructor(
    id: string,
    label: string,
    public x: number,
    public y: number,
    public w: number,
    public h: number,
    public cornerRadius: number,
    public transform: Transform,
    public zAbs: number,
    public groupId: number,
    public isVisible: boolean,
    public isDisplay: boolean,
    public isVirtual: boolean
  ) {
    super(id, label);
  }
}

type Proto = any;

/* GET PROPERTIES TYPES */
type GetPropertiesFromProtoType = (proto: Proto) => PropertyTreeNode;
type GetPropertiesType = () => PropertyTreeNode;

/* MIXINS */
interface PropertiesGetter {
  getProperties: GetPropertiesType;
}

enum PropertySource {
  PROTO,
  DEFAULT,
  CALCULATED,
}

interface PropertyDetails {
  value: string;
  source: PropertySource;
}

interface AssociatedProperty {
  property: null | PropertyDetails;
}

interface Constructor<T = {}> {
  new (...args: any[]): T;
}

type PropertyTreeNode = TreeNode<AssociatedProperty> & AssociatedProperty;
type HierarchyTreeNode = TreeNode<PropertiesGetter> & PropertiesGetter;

const EMPTY_OBJ_STRING = '{empty}';
const EMPTY_ARRAY_STRING = '[empty]';

export {
  Item,
  TreeNode,
  TraceRect,
  Proto,
  GetPropertiesType,
  GetPropertiesFromProtoType,
  PropertiesGetter,
  PropertySource,
  PropertyDetails,
  AssociatedProperty,
  Constructor,
  PropertyTreeNode,
  HierarchyTreeNode,
  EMPTY_OBJ_STRING,
  EMPTY_ARRAY_STRING,
};
