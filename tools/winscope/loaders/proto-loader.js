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

var path = require("path");
var fs = require('fs');
var protobuf = require('protobufjs');
var loaderUtils = require('loader-utils');
var jsonTarget = require('protobufjs/cli/targets/json');


module.exports = function(source) {
  var self = this;

  var root = new protobuf.Root();

  var paths = loaderUtils.getOptions(this)['paths'] || [];

  // Search include paths when resolving imports
  root.resolvePath = function pbjsResolvePath(origin, target) {
    var normOrigin = protobuf.util.path.normalize(origin);
    var normTarget = protobuf.util.path.normalize(target);

    var resolved = protobuf.util.path.resolve(normOrigin, normTarget, true);
    if (fs.existsSync(resolved)) {
      self.addDependency(resolved);
      return resolved;
    }

    for (var i = 0; i < paths.length; ++i) {
      var iresolved = protobuf.util.path.resolve(paths[i] + "/", target);
      if (fs.existsSync(iresolved)) {
        self.addDependency(iresolved);
        return iresolved;
      }
    }

    self.addDependency(resolved);
    return resolved;
  };

  root.loadSync(self.resourcePath).resolveAll();

  var result = JSON.stringify(root, null, 2);

  return `module.exports = ${result}`;
};
