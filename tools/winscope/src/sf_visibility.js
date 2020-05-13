/*
 * Copyright 2020, The Android Open Source Project
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

/**
 * Utility class for deriving state and visibility from the hierarchy. This
 * duplicates some of the logic in surface flinger. If the trace contains
 * composition state (visibleRegion), it will be used otherwise it will be
 * derived.
 */
import { multiply_rect, is_simple_rotation } from './matrix_utils.js'

// Layer flags
const FLAG_HIDDEN = 0x01;
const FLAG_OPAQUE = 0x02;
const FLAG_SECURE = 0x80;

function flags_to_string(flags) {
  if (!flags) return '';
  var verboseFlags = [];
  if (flags & FLAG_HIDDEN) verboseFlags.push("HIDDEN");
  if (flags & FLAG_OPAQUE) verboseFlags.push("OPAQUE");
  if (flags & FLAG_SECURE) verboseFlags.push("SECURE");
  return verboseFlags.join('|') + " (" + flags + ")";
}

function is_empty(region) {
  return region == undefined ||
      region.rect == undefined ||
      region.rect.length == 0 ||
      region.rect.every(function(r) { return is_empty_rect(r) } );
}

function is_empty_rect(rect) {
  var right = rect.right || 0;
  var left = rect.left || 0;
  var top = rect.top || 0;
  var bottom = rect.bottom || 0;

  return (right - left) <= 0 || (bottom - top) <= 0;
}

function is_rect_empty_and_valid(rect) {
  return rect &&
    (rect.left - rect.right === 0 || rect.top - rect.bottom === 0);
}

/**
 * The transformation matrix is defined as the product of:
 * | cos(a) -sin(a) |  \/  | X 0 |
 * | sin(a)  cos(a) |  /\  | 0 Y |
 *
 * where a is a rotation angle, and X and Y are scaling factors.
 * A transformation matrix is invalid when either X or Y is zero,
 * as a rotation matrix is valid for any angle. When either X or Y
 * is 0, then the scaling matrix is not invertible, which makes the
 * transformation matrix not invertible as well. A 2D matrix with
 * components | A B | is not invertible if and only if AD - BC = 0.
 *            | C D |
 * This check is included above.
 */
function is_transform_invalid(transform) {
  return !transform || (transform.dsdx * transform.dtdy ===
      transform.dtdx * transform.dsdy); //determinant of transform
}

function is_opaque(layer) {
  if (layer.color == undefined || layer.color.a == undefined || layer.color.a != 1) return false;
  return layer.isOpaque;
}

function fills_color(layer) {
  return layer.color && layer.color.a > 0 &&
      layer.color.r >= 0 && layer.color.g >= 0 &&
      layer.color.b >= 0;
}

function draws_shadows(layer) {
  return layer.shadowRadius && layer.shadowRadius > 0;
}

function has_blur(layer) {
  return layer.backgroundBlurRadius && layer.backgroundBlurRadius > 0;
}

function has_effects(layer) {
  // Support previous color layer
  if (layer.type === 'ColorLayer') return true;

  // Support newer effect layer
  return layer.type === 'EffectLayer' &&
      (fills_color(layer) || draws_shadows(layer) || has_blur(layer))
}

function is_hidden_by_policy(layer) {
  return layer.flags & FLAG_HIDDEN == FLAG_HIDDEN || 
    // offscreen layer root has a unique layer id
    layer.id == 0x7FFFFFFD;
}

/**
 * Checks if the layer is visible based on its visibleRegion if available
 * or its type, active buffer content, alpha and properties. 
 */
function is_visible(layer, hiddenByPolicy, includesCompositionState) {

  if (includesCompositionState) {
    return !is_empty(layer.visibleRegion);
  }

  if (hiddenByPolicy) {
    return false;
  }

  if (!layer.activeBuffer && !has_effects(layer)) {
    return false;
  }

  if (!layer.color || !layer.color.a || layer.color.a == 0) {
    return false;
  }
  
  if (layer.occludedBy && layer.occludedBy.length > 0) {
    return false;
  }

  if (!layer.bounds || is_empty_rect(layer.bounds)) {
    return false;
  }

  return true;
}

function get_visibility_reason(layer) {
  if (layer.type === 'ContainerLayer') {
    return 'ContainerLayer';
  }

  if (is_hidden_by_policy(layer)) {
    return 'Flag is hidden';
  }

  if (layer.hidden) {
    return 'Hidden by parent';
  }

  let isBufferLayer = (layer.type === 'BufferStateLayer' || layer.type === 'BufferQueueLayer');
  if (isBufferLayer && (!layer.activeBuffer ||
    layer.activeBuffer.height === 0 || layer.activeBuffer.width === 0)) {
    return 'Buffer is empty';
  }

  if (!layer.color || !layer.color.a || layer.color.a == 0) {
    return 'Alpha is 0';
  }

  if (is_rect_empty_and_valid(layer.crop)) {
    return 'Crop is 0x0';
  }

  if (!layer.bounds || is_empty_rect(layer.bounds)) {  
    return 'Bounds is 0x0';
  }

  if (is_transform_invalid(layer.transform)) {
    return 'Transform is invalid';
  }
  if (layer.isRelativeOf && layer.zOrderRelativeOf == -1) {
    return 'RelativeOf layer has been removed';
  }

  let isEffectLayer = (layer.type === 'EffectLayer');
  if (isEffectLayer && !fills_color(layer) && !draws_shadows(layer) && !has_blur(layer)) {
    return 'Effect layer does not have color fill, shadow or blur';
  }
  
  if (layer.occludedBy && layer.occludedBy.length > 0) {
    return 'Layer is occluded by:' + layer.occludedBy.join();
  } 

  if (layer.visible) {
    return "Unknown";
  };
}

// Returns true if rectA overlaps rectB
function overlaps(rectA, rectB) {
  return rectA.left < rectB.right && rectA.right > rectB.left &&
      rectA.top < rectB.bottom && rectA.bottom > rectA.top;
}

// Returns true if outer rect contains inner rect
function contains(outerLayer, innerLayer) {
  if (!is_simple_rotation(outerLayer.transform) || !is_simple_rotation(innerLayer.transform)) {
    return false;
  }
  const outer = screen_bounds(outerLayer);
  const inner = screen_bounds(innerLayer);
  return inner.left >= outer.left && inner.top >= outer.top &&
     inner.right <= outer.right && inner.bottom <= outer.bottom;
}

function screen_bounds(layer) {
  if (layer.screenBounds) return layer.screenBounds;
  let transformMatrix = layer.transform;
  var tx = layer.position ? layer.position.x || 0 : 0;
  var ty = layer.position ? layer.position.y || 0 : 0;

  transformMatrix.tx = tx
  transformMatrix.ty = ty
  return multiply_rect(transformMatrix, layer.bounds);
}

// Traverse in z-order from top to bottom and fill in occlusion data
function fill_occlusion_state(layerMap, rootLayers, includesCompositionState) {
  const layers = rootLayers.filter(layer => !layer.isRelativeOf);
  traverse_top_to_bottom(layerMap, layers, {opaqueRects:[], transparentRects:[], screenBounds:null}, (layer, globalState) => {

    if (layer.name.startsWith("Root#0") && layer.sourceBounds) {
      globalState.screenBounds = {left:0,  top:0, bottom:layer.sourceBounds.bottom, right:layer.sourceBounds.right};
    }
  
    const visible = is_visible(layer, layer.hidden, includesCompositionState);
    if (visible) {
      let fullyOccludes = (testLayer) => contains(testLayer, layer);
      let partiallyOccludes = (testLayer) => overlaps(screen_bounds(testLayer), screen_bounds(layer));
      let covers = (testLayer) => overlaps(screen_bounds(testLayer), screen_bounds(layer));
  
      layer.occludedBy = globalState.opaqueRects.filter(fullyOccludes).map(layer => layer.id);
      layer.partiallyOccludedBy = globalState.opaqueRects.filter(partiallyOccludes).map(layer => layer.id);
      layer.coveredBy = globalState.transparentRects.filter(covers).map(layer => layer.id);

      if (is_opaque(layer)) {
        globalState.opaqueRects.push(layer);
      } else {
          globalState.transparentRects.push(layer);
      }
    }

    layer.visible = is_visible(layer, layer.hidden, includesCompositionState);
    if (!layer.visible) {
      layer.invisibleDueTo = get_visibility_reason(layer);
    } 
  });
}

function traverse_top_to_bottom(layerMap, rootLayers, globalState, fn) {
  for (var i = rootLayers.length-1; i >=0; i--) {   
    const relatives = rootLayers[i].relatives.map(id => layerMap[id]);
    const children = rootLayers[i].children.map(id => layerMap[id])

    // traverse through relatives and children that are not relatives
    const traverseList = relatives.concat(children.filter(layer => !layer.isRelativeOf));
    traverseList.sort((lhs, rhs) => rhs.z - lhs.z);
    
    traverseList.filter((layer) => layer.z >=0).forEach(layer => {
      traverse_top_to_bottom(layerMap, [layer], globalState, fn);
    });

    fn(rootLayers[i], globalState);
    
    traverseList.filter((layer) => layer.z < 0).forEach(layer => {
      traverse_top_to_bottom(layerMap, [layer], globalState, fn);
    });

  }
}

// Traverse all children and fill in any inherited states.
function fill_inherited_state(layerMap, rootLayers) {
  traverse(layerMap, rootLayers, (layer, parent) => {
    const parentHidden = parent && parent.hidden;
    layer.hidden = is_hidden_by_policy(layer) || parentHidden;
    layer.verboseFlags = flags_to_string(layer.flags);

    if (!layer.bounds) {
      if (!layer.sourceBounds) {
        layer.bounds = layer.sourceBounds;
      } else if (parent) {
        layer.bounds = parent.bounds;
      } else {
        layer.bounds = {left:0, top:0, right:0, bottom:0};
      }
    }
  });
} 

function traverse(layerMap, rootLayers, fn) {
  for (var i = rootLayers.length-1; i >=0; i--) {
    const parentId = rootLayers[i].parent;
    const parent = parentId == -1 ? null : layerMap[parentId];
    fn(rootLayers[i], parent);
    const children = rootLayers[i].children.map(id => layerMap[id]);
    traverse(layerMap, children, fn);
  }
}

export {fill_occlusion_state, fill_inherited_state};