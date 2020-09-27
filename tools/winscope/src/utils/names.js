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

// Returns just the class name and root package information
function getComponentClassName(componentFullName) {
  const classParts = componentFullName.split('.');

  if (classParts.length <= 3) {
    return componentFullName;
  }

  const className = classParts.slice(-1).pop();

  return `${classParts[0]}.${classParts[1]}.(...).${className}`
}

const hashCode = /([A-Fa-f0-9]{7}|[A-Fa-f0-9]{6})/;
const packageRegex = /(([a-z][a-z_0-9]*\.)*([a-z][a-z_0-9]*))/;
const qualifiedClassNameRegex = /(([a-z][a-z_0-9]*\.)*[A-Z_]($[A-Z_]|[\w_])*)/;

const surfaceRegex =
  new RegExp(/^Surface\(.*\)\/@0x/.source + hashCode.source +
    / - .*/.source + "$");

const moduleName =
  new RegExp("^" +
    "(" + packageRegex.source + /\//.source + ")?" +
    qualifiedClassNameRegex.source +
    /(\$.*)?/.source +
    /(\#.*)?/.source +
    "$");

function getSimplifiedLayerName(layerName) {
  // Get rid of prepended hash code
  let removedHashCodePrefix = false;
  if (new RegExp("^" + hashCode.source + " ").test(layerName)) {
    layerName = layerName.split(" ").slice(1).join(" ");
    removedHashCodePrefix = true;
  }

  if (/^ActivityRecord\{.*\}?(\#[0-9]+)?$/.test(layerName)) {
    return "ActivityRecord";
  }

  if (/^WindowToken\{.*\}(\#[0-9]*)?$/.test(layerName)) {
    return "WindowToken";
  }

  if (/^WallpaperWindowToken\{.*\}(\#[0-9]*)?$/.test(layerName)) {
    return "WallpaperWindowToken";
  }

  if (surfaceRegex.test(layerName)) {
    return "Surface - " + layerName.split("- ").slice(-1).pop();
  }

  if (moduleName.test(layerName)) {
    return layerName.split(".").slice(-1).pop();
  }

  if (removedHashCodePrefix) {
    return layerName;
  }

  return null;
}

export { getComponentClassName, getSimplifiedLayerName };