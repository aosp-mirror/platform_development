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

import {RawDataUtils} from 'parsers/raw_data_utils';
import {TransformUtils} from 'parsers/surface_flinger/transform_utils';
import {PropertyTreeNode} from './property_tree_node';

const EMPTY_OBJ_STRING = '{empty}';
const EMPTY_ARRAY_STRING = '[empty]';

interface PropertyFormatter {
  format(node: PropertyTreeNode): string;
}

class DefaultPropertyFormatter implements PropertyFormatter {
  format(node: PropertyTreeNode): string {
    const value = node.getValue();
    if (Array.isArray(value) && value.length === 0) {
      return EMPTY_ARRAY_STRING;
    }

    if (value?.toString) return value.toString();

    return `${value}`;
  }
}
const DEFAULT_PROPERTY_FORMATTER = new DefaultPropertyFormatter();

class ColorFormatter implements PropertyFormatter {
  format(node: PropertyTreeNode): string {
    if (RawDataUtils.isEmptyObj(node)) {
      return `${EMPTY_OBJ_STRING}, alpha: ${node.getChildByName('a')?.getValue() ?? 'unknown'}`;
    }
    return `(${node.getChildByName('r')?.getValue() ?? 0}, ${
      node.getChildByName('g')?.getValue() ?? 0
    }, ${node.getChildByName('b')?.getValue() ?? 0}, ${node.getChildByName('a')?.getValue() ?? 0})`;
  }
}
const COLOR_FORMATTER = new ColorFormatter();

class RectFormatter implements PropertyFormatter {
  format(node: PropertyTreeNode): string {
    if (RawDataUtils.isEmptyObj(node)) {
      return EMPTY_OBJ_STRING;
    }
    return `(${node.getChildByName('left')?.getValue() ?? 0}, ${
      node.getChildByName('top')?.getValue() ?? 0
    }) - (${node.getChildByName('right')?.getValue() ?? 0}, ${
      node.getChildByName('bottom')?.getValue() ?? 0
    })`;
  }
}
const RECT_FORMATTER = new RectFormatter();

class BufferFormatter implements PropertyFormatter {
  format(node: PropertyTreeNode): string {
    return `w: ${node.getChildByName('width')?.getValue() ?? 0}, h: ${
      node.getChildByName('height')?.getValue() ?? 0
    }, stride: ${node.getChildByName('stride')?.getValue()}, format: ${node
      .getChildByName('format')
      ?.getValue()}`;
  }
}
const BUFFER_FORMATTER = new BufferFormatter();

class LayerIdFormatter implements PropertyFormatter {
  format(node: PropertyTreeNode): string {
    const value = node.getValue();
    return value === -1 || value === 0 ? 'none' : `${value}`;
  }
}
const LAYER_ID_FORMATTER = new LayerIdFormatter();

class TransformFormatter implements PropertyFormatter {
  format(node: PropertyTreeNode): string {
    const type = node.getChildByName('type');
    return type !== undefined ? TransformUtils.getTypeFlags(type.getValue() ?? 0) : 'null';
  }
}
const TRANSFORM_FORMATTER = new TransformFormatter();

class SizeFormatter implements PropertyFormatter {
  format(node: PropertyTreeNode): string {
    return `${node.getChildByName('w')?.getValue() ?? 0} x ${
      node.getChildByName('h')?.getValue() ?? 0
    }`;
  }
}
const SIZE_FORMATTER = new SizeFormatter();

class PositionFormatter implements PropertyFormatter {
  format(node: PropertyTreeNode): string {
    return `x: ${node.getChildByName('x')?.getValue() ?? 0}, y: ${
      node.getChildByName('y')?.getValue() ?? 0
    }`;
  }
}
const POSITION_FORMATTER = new PositionFormatter();

class RegionFormatter implements PropertyFormatter {
  format(node: PropertyTreeNode): string {
    let res = 'SkRegion(';
    node
      .getChildByName('rect')
      ?.getAllChildren()
      .forEach((rectNode: PropertyTreeNode) => {
        res += `(${rectNode.getChildByName('left')?.getValue() ?? 0}, ${
          rectNode.getChildByName('top')?.getValue() ?? 0
        }, ${rectNode.getChildByName('right')?.getValue() ?? 0}, ${
          rectNode.getChildByName('bottom')?.getValue() ?? 0
        })`;
      });
    return res + ')';
  }
}
const REGION_FORMATTER = new RegionFormatter();

class EnumFormatter implements PropertyFormatter {
  constructor(private readonly valuesById: {[key: number]: string}) {}

  format(node: PropertyTreeNode): string {
    const value = node.getValue();
    if (typeof value === 'number' && this.valuesById[value]) {
      return this.valuesById[value];
    }
    return `${value}`;
  }
}

class FixedStringFormatter implements PropertyFormatter {
  constructor(private readonly fixedStringValue: string) {}

  format(node: PropertyTreeNode): string {
    return this.fixedStringValue;
  }
}

export {
  EMPTY_OBJ_STRING,
  EMPTY_ARRAY_STRING,
  PropertyFormatter,
  DEFAULT_PROPERTY_FORMATTER,
  COLOR_FORMATTER,
  RECT_FORMATTER,
  BUFFER_FORMATTER,
  LAYER_ID_FORMATTER,
  TRANSFORM_FORMATTER,
  SIZE_FORMATTER,
  POSITION_FORMATTER,
  REGION_FORMATTER,
  EnumFormatter,
  FixedStringFormatter,
};
