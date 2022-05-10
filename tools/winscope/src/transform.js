/*
 * Copyright 2017, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {DiffType} from './utils/diff.js';
import {regExpTimestampSearch} from './utils/consts';

// kind - a type used for categorization of different levels
// name - name of the node
// children - list of child entries. Each child entry is pair list
//            [raw object, nested transform function].
// bounds - used to calculate the full bounds of parents
// stableId - unique id for an entry. Used to maintain selection across frames.
function transform({
  obj,
  kind,
  name,
  shortName,
  children,
  timestamp,
  rect,
  bounds,
  highlight,
  rectsTransform,
  chips,
  visible,
  flattened,
  stableId,
  freeze = true,
}) {
  function call(fn, arg) {
    return (typeof fn == 'function') ? fn(arg) : fn;
  }
  function handleChildren(arg, transform) {
    return [].concat(...arg.map((item) => {
      const childrenFunc = item[0];
      const transformFunc = item[1];
      const childs = call(childrenFunc, obj);
      if (childs) {
        if (typeof childs.map != 'function') {
          throw new Error(
              'Childs should be an array, but is: ' + (typeof childs) + '.');
        }
        return transform ? childs.map(transformFunc) : childs;
      } else {
        return [];
      }
    }));
  }
  function concat(arg, args, argsmap) {
    const validArg = arg !== undefined && arg !== null;

    if (Array.isArray(args)) {
      if (validArg) {
        return [arg].concat(...args.map(argsmap));
      } else {
        return [].concat(...args.map(argsmap));
      }
    } else if (validArg) {
      return [arg];
    } else {
      return undefined;
    }
  }

  const transformedChildren = handleChildren(children, true /* transform */);
  rectsTransform = (rectsTransform === undefined) ? (e) => e : rectsTransform;

  const kindResolved = call(kind, obj);
  const nameResolved = call(name, obj);
  const shortNameResolved = call(shortName, obj);
  const rectResolved = call(rect, obj);
  // eslint-disable-next-line max-len
  const stableIdResolved = (stableId === undefined) ? kindResolved + '|-|' + nameResolved : call(stableId, obj);

  const result = {
    kind: kindResolved,
    name: nameResolved,
    shortName: shortNameResolved,
    collapsed: false,
    children: transformedChildren,
    obj: obj,
    timestamp: call(timestamp, obj),
    skip: handleChildren(children, false /* transform */),
    bounds: call(bounds, obj) || transformedChildren.map(
        (e) => e.bounds).find((e) => true) || undefined,
    rect: rectResolved,
    rects: rectsTransform(
        concat(rectResolved, transformedChildren, (e) => e.rects)),
    highlight: call(highlight, obj),
    chips: call(chips, obj),
    stableId: stableIdResolved,
    visible: call(visible, obj),
    childrenVisible: transformedChildren.some((c) => {
      return c.childrenVisible || c.isVisible;
    }),
    flattened: call(flattened, obj),
  };

  if (rectResolved) {
    rectResolved.ref = result;
  }

  return freeze ? Object.freeze(result) : result;
}

function getDiff(val, compareVal) {
  if (val && isTerminal(compareVal)) {
    return {type: DiffType.ADDED};
  } else if (isTerminal(val) && compareVal) {
    return {type: DiffType.DELETED};
  } else if (compareVal != val) {
    return {type: DiffType.MODIFIED};
  } else {
    return {type: DiffType.NONE};
  }
}

// Represents termination of the object traversal,
// differentiated with a null value in the object.
class Terminal { }

function isTerminal(obj) {
  return obj instanceof Terminal;
}

class ObjectTransformer {
  constructor(obj, rootName, stableId) {
    this.obj = obj;
    this.rootName = rootName;
    this.stableId = stableId;
    this.diff = false;
  }

  setOptions(options) {
    this.options = options;
    return this;
  }

  withDiff(obj, fieldOptions) {
    this.diff = true;
    this.compareWithObj = obj ?? new Terminal();
    this.compareWithFieldOptions = fieldOptions;
    return this;
  }

  /**
   * Transform the raw JS Object into a TreeView compatible object
   * @param {Object} transformOptions detailed below
   * @param {bool} keepOriginal whether or not to store the original object in
   *                            the obj property of a tree node for future
   *                            reference
   * @param {bool} freeze whether or not the returned objected should be frozen
   *                      to prevent changing any of its properties
   * @param {string} metadataKey the key that contains a node's metadata to be
   *                             accessible after the transformation
   * @return {Object} the transformed JS object compatible with treeviews.
   */
  transform(transformOptions = {
    keepOriginal: false, freeze: true, metadataKey: null,
  }) {
    const {formatter} = this.options;
    if (!formatter) {
      throw new Error('Missing formatter, please set with setOptions()');
    }

    return this._transform(this.obj, this.rootName, null,
        this.compareWithObj, this.rootName, null,
        this.stableId, transformOptions);
  }

  /**
   * @param {Object} obj the object to transform to a treeview compatible object
   * @param {Object} fieldOptions options on how to transform fields
   * @param {*} metadataKey if 'obj' contains this key, it is excluded from the
   *                        transformation
   * @return {Object} the transformed JS object compatible with treeviews.
   */
  _transformObject(obj, fieldOptions, metadataKey) {
    const {skip, formatter} = this.options;
    const transformedObj = {
      obj: {},
      fieldOptions: {},
    };
    let formatted = undefined;

    if (skip && skip.includes(obj)) {
      // skip
    } else if ((formatted = formatter(obj))) {
      // Obj has been formatted into a terminal node — has no children.
      transformedObj.obj[formatted] = new Terminal();
      transformedObj.fieldOptions[formatted] = fieldOptions;
    } else if (Array.isArray(obj)) {
      obj.forEach((e, i) => {
        transformedObj.obj['' + i] = e;
        transformedObj.fieldOptions['' + i] = fieldOptions;
      });
    } else if (typeof obj == 'string') {
      // Object is a primitive type — has no children. Set to terminal
      // to differentiate between null object and Terminal element.
      transformedObj.obj[obj] = new Terminal();
      transformedObj.fieldOptions[obj] = fieldOptions;
    } else if (typeof obj == 'number' || typeof obj == 'boolean') {
      // Similar to above — primitive type node has no children.
      transformedObj.obj['' + obj] = new Terminal();
      transformedObj.fieldOptions['' + obj] = fieldOptions;
    } else if (obj && typeof obj == 'object') {
      Object.keys(obj).forEach((key) => {
        if (key === metadataKey) {
          return;
        }
        transformedObj.obj[key] = obj[key];
        transformedObj.fieldOptions[key] = obj.$type?.fields[key]?.options;
      });
    } else if (obj === null) {
      // Null object is a has no children — set to be terminal node.
      transformedObj.obj.null = new Terminal();
      transformedObj.fieldOptions.null = undefined;
    }

    return transformedObj;
  }

  /**
   * Extract the value of obj's property with key 'metadataKey'
   * @param {Object} obj the obj we want to extract the metadata from
   * @param {string} metadataKey the key that stores the metadata in the object
   * @return {Object} the metadata value or null in no metadata is present
   */
  _getMetadata(obj, metadataKey) {
    if (metadataKey && obj[metadataKey]) {
      const metadata = obj[metadataKey];
      obj[metadataKey] = undefined;
      return metadata;
    } else {
      return null;
    }
  }

  _transform(obj, name, fieldOptions,
      compareWithObj, compareWithName, compareWithFieldOptions,
      stableId, transformOptions) {
    const originalObj = obj;
    const metadata = this._getMetadata(obj, transformOptions.metadataKey);

    const children = [];

    if (!isTerminal(obj)) {
      const transformedObj =
          this._transformObject(
              obj, fieldOptions, transformOptions.metadataKey);
      obj = transformedObj.obj;
      fieldOptions = transformedObj.fieldOptions;
    }
    if (!isTerminal(compareWithObj)) {
      const transformedObj =
          this._transformObject(
              compareWithObj, compareWithFieldOptions,
              transformOptions.metadataKey);
      compareWithObj = transformedObj.obj;
      compareWithFieldOptions = transformedObj.fieldOptions;
    }

    for (const key in obj) {
      if (obj.hasOwnProperty(key)) {
        let compareWithChild = new Terminal();
        let compareWithChildName = new Terminal();
        let compareWithChildFieldOptions = undefined;
        if (compareWithObj.hasOwnProperty(key)) {
          compareWithChild = compareWithObj[key];
          compareWithChildName = key;
          compareWithChildFieldOptions = compareWithFieldOptions[key];
        }
        children.push(this._transform(obj[key], key, fieldOptions[key],
            compareWithChild, compareWithChildName,
            compareWithChildFieldOptions,
            `${stableId}.${key}`, transformOptions));
      }
    }

    // Takes care of adding deleted items to final tree
    for (const key in compareWithObj) {
      if (!obj.hasOwnProperty(key) && compareWithObj.hasOwnProperty(key)) {
        children.push(this._transform(new Terminal(), new Terminal(), undefined,
            compareWithObj[key], key, compareWithFieldOptions[key],
            `${stableId}.${key}`, transformOptions));
      }
    }

    let transformedObj;
    if (
      children.length == 1 &&
      children[0].children.length == 0 &&
      !children[0].combined
    ) {
      // Merge leaf key value pairs.
      const child = children[0];

      transformedObj = {
        kind: '',
        name: (isTerminal(name) ? compareWithName : name) + ': ' + child.name,
        stableId,
        children: child.children,
        combined: true,
      };

      if (this.diff) {
        transformedObj.diff = child.diff;
      }
    } else {
      transformedObj = {
        kind: '',
        name,
        stableId,
        children,
      };

      let fieldOptionsToUse = fieldOptions;

      if (this.diff) {
        const diff = getDiff(name, compareWithName);
        transformedObj.diff = diff;

        if (diff.type == DiffType.DELETED) {
          transformedObj.name = compareWithName;
          fieldOptionsToUse = compareWithFieldOptions;
        }
      }
    }

    if (transformOptions.keepOriginal) {
      transformedObj.obj = originalObj;
    }

    if (metadata) {
      transformedObj[transformOptions.metadataKey] = metadata;
    }

    return transformOptions.freeze ?
      Object.freeze(transformedObj) : transformedObj;
  }
}

// eslint-disable-next-line camelcase
function nanos_to_string(elapsedRealtimeNanos) {
  const units = [
    [1000000, '(ns)'],
    [1000, 'ms'],
    [60, 's'],
    [60, 'm'],
    [24, 'h'],
    [Infinity, 'd'],
  ];

  const parts = [];
  units.some(([div, str], i) => {
    const part = (elapsedRealtimeNanos % div).toFixed();
    if (!str.startsWith('(')) {
      parts.push(part + str);
    }
    elapsedRealtimeNanos = Math.floor(elapsedRealtimeNanos / div);
    return elapsedRealtimeNanos == 0;
  });

  return parts.reverse().join('');
}

function string_to_nanos(stringTime) {
  //isolate the times for each unit in an array
  var times = stringTime.split(/\D+/).filter(unit => unit.length > 0);

  //add zeroes to start of array if only partial timestamp is input
  while (times.length<5) {
    times.unshift("0");
  }

  var units = [24*60*60, 60*60, 60, 1, 0.001];
  var nanos = 0;
  //multiply the times by the relevant unit and sum
  for (var x=0; x<5; x++) {
    nanos += units[x]*parseInt(times[x]);
  }
  return nanos*(10**9);
}

// Returns a UI element used highlight a visible entry.
// eslint-disable-next-line camelcase
function get_visible_chip() {
  return {short: 'V', long: 'visible', class: 'default'};
}

// Returns closest timestamp in timeline based on search input*/
function getClosestTimestamp(searchInput, timeline) {
  if (regExpTimestampSearch.test(searchInput)) {
    var roundedTimestamp = parseInt(searchInput);
  } else {
    var roundedTimestamp = string_to_nanos(searchInput);
  }
  const closestTimestamp = timeline.reduce((prev, curr) => {
    return Math.abs(curr-roundedTimestamp) < Math.abs(prev-roundedTimestamp) ? curr : prev;
  });
  return closestTimestamp;
}

// eslint-disable-next-line camelcase
export {transform, ObjectTransformer, nanos_to_string, string_to_nanos, get_visible_chip, getClosestTimestamp};
