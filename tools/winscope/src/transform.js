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

// kind - a type used for categorization of different levels
// name - name of the node
// children - list of child entries. Each child entry is pair list [raw object, nested transform function].
// bounds - used to calculate the full bounds of parents
// stableId - unique id for an entry. Used to maintain selection across frames.
function transform({obj, kind, name, children, timestamp, rect, bounds, highlight, rects_transform, chips, visible, flattened, stableId}) {
	function call(fn, arg) {
		return (typeof fn == 'function') ? fn(arg) : fn;
	}
	function handle_children(arg, transform) {
		return [].concat(...arg.map((item) => {
			var childrenFunc = item[0];
			var transformFunc = item[1];
			var childs = call(childrenFunc, obj);
			if (childs) {
				if (typeof childs.map != 'function'){
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


function transform_json(obj, name, options) {
	let {skip, formatter} = options;

	var children = [];
	var formatted = undefined;

	if (skip && skip.includes(obj)) {
		// skip
	} else if ((formatted = formatter(obj))) {
		children.push(transform_json(null, formatted, options));
	} else if (Array.isArray(obj)) {
		obj.forEach((e, i) => {
			children.push(transform_json(e, ""+i, options));
		})
	} else if (typeof obj == 'string') {
		children.push(transform_json(null, obj, options));
	} else if (typeof obj == 'number' || typeof obj == 'boolean') {
		children.push(transform_json(null, ""+obj, options));
	} else if (obj && typeof obj == 'object') {
		Object.keys(obj).forEach((key) => {
			children.push(transform_json(obj[key], key, options));
		});
	}

	if (children.length == 1 && !children[0].combined) {
		return Object.freeze({
			kind: "",
			name: name + ": " + children[0].name,
			children: children[0].children,
			combined: true
		});
	}

	return Object.freeze({
		kind: "",
		name: name,
		children: children,
	});
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
	return {short: 'V', long: "visible", class: 'default'};
 }

export {transform, transform_json, nanos_to_string, get_visible_chip};
