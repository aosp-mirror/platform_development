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

import {Timestamp} from 'common/time';
import {TimeDuration} from 'common/time_duration';
import {RawDataUtils} from 'parsers/raw_data_utils';
import {TransformUtils} from 'parsers/surface_flinger/transform_utils';
import {PropertyTreeNode} from './property_tree_node';

const EMPTY_OBJ_STRING = '{empty}';
const EMPTY_ARRAY_STRING = '[empty]';

function formatNumber(value: number): string {
  if (!Number.isInteger(value)) {
    return value.toFixed(3).toString();
  }
  return value.toString();
}

interface PropertyFormatter {
  format(node: PropertyTreeNode): string;
}

class DefaultPropertyFormatter implements PropertyFormatter {
  format(node: PropertyTreeNode): string {
    const value = node.getValue();
    if (Array.isArray(value) && value.length === 0) {
      return EMPTY_ARRAY_STRING;
    }

    if (typeof value === 'number') {
      return formatNumber(value);
    }

    if (value?.toString) return value.toString();

    return `${value}`;
  }
}
const DEFAULT_PROPERTY_FORMATTER = new DefaultPropertyFormatter();

class ColorFormatter implements PropertyFormatter {
  format(node: PropertyTreeNode): string {
    const rNode = node.getChildByName('r');
    const gNode = node.getChildByName('g');
    const bNode = node.getChildByName('b');
    const alphaNode = node.getChildByName('a');

    const r = formatNumber(rNode?.getValue() ?? 0);
    const g = formatNumber(gNode?.getValue() ?? 0);
    const b = formatNumber(bNode?.getValue() ?? 0);
    if (rNode && gNode && bNode && !alphaNode) {
      return `(${r}, ${g}, ${b})`;
    }

    const alpha = formatNumber(alphaNode?.getValue() ?? 0);
    if (RawDataUtils.isEmptyObj(node)) {
      return `${EMPTY_OBJ_STRING}, alpha: ${alpha}`;
    }
    return `(${r}, ${g}, ${b}, ${alpha})`;
  }
}
const COLOR_FORMATTER = new ColorFormatter();

class RectFormatter implements PropertyFormatter {
  format(node: PropertyTreeNode): string {
    if (!RawDataUtils.isRect(node) || RawDataUtils.isEmptyObj(node)) {
      return EMPTY_OBJ_STRING;
    }
    const left = formatNumber(node.getChildByName('left')?.getValue() ?? 0);
    const top = formatNumber(node.getChildByName('top')?.getValue() ?? 0);
    const right = formatNumber(node.getChildByName('right')?.getValue() ?? 0);
    const bottom = formatNumber(node.getChildByName('bottom')?.getValue() ?? 0);

    return `(${left}, ${top}) - (${right}, ${bottom})`;
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

class MatrixFormatter implements PropertyFormatter {
  format(node: PropertyTreeNode): string {
    const dsdx = formatNumber(node.getChildByName('dsdx')?.getValue() ?? 0);
    const dtdx = formatNumber(node.getChildByName('dtdx')?.getValue() ?? 0);
    const dsdy = formatNumber(node.getChildByName('dsdy')?.getValue() ?? 0);
    const dtdy = formatNumber(node.getChildByName('dtdy')?.getValue() ?? 0);
    const tx = node.getChildByName('tx');
    const ty = node.getChildByName('ty');
    if (
      dsdx === '0' &&
      dtdx === '0' &&
      dsdy === '0' &&
      dtdy === '0' &&
      !tx &&
      !ty
    ) {
      return 'null';
    }
    const matrix22 = `dsdx: ${dsdx}, dtdx: ${dtdx}, dsdy: ${dsdy}, dtdy: ${dtdy}`;
    if (!tx && !ty) {
      return matrix22;
    }
    return (
      matrix22 +
      `, tx: ${formatNumber(tx?.getValue() ?? 0)}, ty: ${formatNumber(
        ty?.getValue() ?? 0,
      )}`
    );
  }
}
const MATRIX_FORMATTER = new MatrixFormatter();

class TransformFormatter implements PropertyFormatter {
  format(node: PropertyTreeNode): string {
    const type = node.getChildByName('type');
    return type !== undefined
      ? TransformUtils.getTypeFlags(type.getValue() ?? 0)
      : 'null';
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
    const x = formatNumber(node.getChildByName('x')?.getValue() ?? 0);
    const y = formatNumber(node.getChildByName('y')?.getValue() ?? 0);
    return `x: ${x}, y: ${y}`;
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
    if (typeof value === 'bigint' && this.valuesById[Number(value)]) {
      return this.valuesById[Number(value)];
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

class TimestampNodeFormatter implements PropertyFormatter {
  format(node: PropertyTreeNode): string {
    const timestamp = node.getValue();
    if (timestamp instanceof Timestamp || timestamp instanceof TimeDuration) {
      return timestamp.format();
    }
    return 'null';
  }
}
const TIMESTAMP_NODE_FORMATTER = new TimestampNodeFormatter();

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
  TIMESTAMP_NODE_FORMATTER,
  MATRIX_FORMATTER,
};
