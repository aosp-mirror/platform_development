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

import { DiffType } from './utils/diff.js';

// kind - a type used for categorization of different levels
// name - name of the node
// children - list of child entries. Each child entry is pair list [raw object, nested transform function].
// bounds - used to calculate the full bounds of parents
// stableId - unique id for an entry. Used to maintain selection across frames.
function transform({ obj, kind, name, children, timestamp, rect, bounds, highlight, rects_transform, chips, visible, flattened, stableId }) {
	function call(fn, arg) {
		return (typeof fn == 'function') ? fn(arg) : fn;
	}
	function handle_children(arg, transform) {
		return [].concat(...arg.map((item) => {
			var childrenFunc = item[0];
			var transformFunc = item[1];
			var childs = call(childrenFunc, obj);
			if (childs) {
				if (typeof childs.map != 'function') {
					throw 'Childs should be an array, but is: ' + (typeof childs) + '.'
				}
				return transform ? childs.map(transformFunc) : childs;
			} else {
				return [];
			}
		}));
	}
	function concat(arg, args, argsmap) {
		var validArg = arg !== undefined && arg !== null;

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

	var transformed_children = handle_children(children, true /* transform */);
	rects_transform = (rects_transform === undefined) ? (e) => e : rects_transform;

	var kindResolved = call(kind, obj);
	var nameResolved = call(name, obj);
	var rectResolved = call(rect, obj);
	var stableIdResolved = (stableId === undefined) ?
		kindResolved + '|-|' + nameResolved :
		call(stableId, obj);

	var result = {
		kind: kindResolved,
		name: nameResolved,
		collapsed: false,
		children: transformed_children,
		obj: obj,
		timestamp: call(timestamp, obj),
		skip: handle_children(children, false /* transform */),
		bounds: call(bounds, obj) || transformed_children.map((e) => e.bounds).find((e) => true) || undefined,
		rect: rectResolved,
		rects: rects_transform(concat(rectResolved, transformed_children, (e) => e.rects)),
		highlight: call(highlight, obj),
		chips: call(chips, obj),
		stableId: stableIdResolved,
		visible: call(visible, obj),
		childrenVisible: transformed_children.some((c) => {
			return c.childrenVisible || c.visible
		}),
		flattened: call(flattened, obj),
	};

	if (rectResolved) {
		rectResolved.ref = result;
	}

	return Object.freeze(result);
}

function getDiff(val, compareVal) {
	if (val && isTerminal(compareVal)) {
		return { type: DiffType.ADDED };
	} else if (isTerminal(val) && compareVal) {
		return { type: DiffType.DELETED };
	} else if (compareVal != val) {
		return { type: DiffType.MODIFIED };
	} else {
		return { type: DiffType.NONE };
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

	withDiff(obj) {
		this.diff = true;
		this.compareWithObj = obj ?? new Terminal();
		return this;
	}

	transform() {
		const { formatter } = this.options;
		if (!formatter) {
			throw new Error("Missing formatter, please set with setOptions()");
		}

		return this._transform(this.obj, this.rootName, this.compareWithObj, this.rootName, this.stableId);
	}

	_transformKeys(obj) {
		const { skip, formatter } = this.options;
		const transformedObj = {};
		let formatted = undefined;

		if (skip && skip.includes(obj)) {
			// skip
		} else if ((formatted = formatter(obj))) {
			// Obj has been formatted into a terminal node — has no children.
			transformedObj[formatted] = new Terminal();
		} else if (Array.isArray(obj)) {
			obj.forEach((e, i) => {
				transformedObj["" + i] = e;
			});
		} else if (typeof obj == 'string') {
			// Object is a primitive type — has no children. Set to terminal
			// to differentiate between null object and Terminal element.
			transformedObj[obj] = new Terminal();
		} else if (typeof obj == 'number' || typeof obj == 'boolean') {
			// Similar to above — primitive type node has no children.
			transformedObj["" + obj] = new Terminal();
		} else if (obj && typeof obj == 'object') {
			Object.keys(obj).forEach((key) => {
				transformedObj[key] = obj[key];
			});
		} else if (obj === null) {
			// Null object is a has no children — set to be terminal node.
			transformedObj.null = new Terminal();
		}

		return transformedObj;
	}

	_transform(obj, name, compareWithObj, compareWithName, stableId) {
		const children = [];

		if (!isTerminal(obj)) {
			obj = this._transformKeys(obj);
		}
		if (!isTerminal(compareWithObj)) {
			compareWithObj = this._transformKeys(compareWithObj);
		}

		for (const key in obj) {
			if (obj.hasOwnProperty(key)) {
				let compareWithChild = new Terminal();
				let compareWithName = new Terminal();
				if (compareWithObj.hasOwnProperty(key)) {
					compareWithChild = compareWithObj[key];
					compareWithName = key;
				}
				children.push(this._transform(obj[key], key, compareWithChild, compareWithName, `${stableId}.${key}`));
			}
		}

		// Takes care of adding deleted items to final tree
		for (const key in compareWithObj) {
			if (!obj.hasOwnProperty(key) && compareWithObj.hasOwnProperty(key)) {
				children.push(this._transform(new Terminal(), new Terminal(), compareWithObj[key], key));
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
				kind: "",
				name: name + ": " + child.name,
				stableId: stableId,
				children: child.children,
				combined: true,
			}

			if (this.diff) {
				transformedObj.diff = child.diff;
			}
		} else {
			transformedObj = {
				kind: "",
				name,
				stableId: stableId,
				children,
			};

			if (this.diff) {
				const diff = getDiff(name, compareWithName);
				transformedObj.diff = diff;

				if (diff.type == DiffType.DELETED) {
					transformedObj.name = compareWithName;
				}
			}
		}

		return Object.freeze(transformedObj);
	}
}

function nanos_to_string(elapsedRealtimeNanos) {
	var units = [
		[1000000, '(ns)'],
		[1000, 'ms'],
		[60, 's'],
		[60, 'm'],
		[24, 'h'],
		[Infinity, 'd'],
	];

	var parts = []
	units.some(([div, str], i) => {
		var part = (elapsedRealtimeNanos % div).toFixed()
		if (!str.startsWith('(')) {
			parts.push(part + str);
		}
		elapsedRealtimeNanos = Math.floor(elapsedRealtimeNanos / div);
		return elapsedRealtimeNanos == 0;
	});

	return parts.reverse().join('');
}

// Returns a UI element used highlight a visible entry.
function get_visible_chip() {
	return { short: 'V', long: "visible", class: 'default' };
}

export { transform, ObjectTransformer, nanos_to_string, get_visible_chip };
