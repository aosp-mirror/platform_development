/*
 * Copyright (C) 2022 The Android Open Source Project
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
import ObjectFormatter from "common/trace/flickerlib/ObjectFormatter";

import {
  FilterType,
  PropertiesTree,
  Tree,
  DiffType,
  Terminal
} from "./tree_utils";

interface TransformOptions {
  freeze: boolean;
  keepOriginal: boolean;
  metadataKey: string | null;
}
interface TreeTransformerOptions {
  skip?: any;
  formatter?: any;
}
interface TransformedPropertiesObject {
  properties: any;
  diffType?: string;
}

export class TreeTransformer {
  private stableId: string;
  private rootName: string;
  private isShowDefaults = false;
  private isShowDiff = false;
  private filter: FilterType;
  private properties: PropertiesTree;
  private compareWithProperties: PropertiesTree | null = null;
  private options?: TreeTransformerOptions;
  private transformOptions: TransformOptions = {
    keepOriginal: false, freeze: true, metadataKey: null,
  };

  constructor(tree: Tree, filter: FilterType) {
    this.stableId = this.compatibleStableId(tree);
    this.rootName = tree.name;
    this.filter = filter;
    this.setProperties(tree);
    this.setTransformerOptions({});
  }

  public setIsShowDefaults(enabled: boolean) {
    this.isShowDefaults = enabled;
    return this;
  }

  public setIsShowDiff(enabled: boolean) {
    this.isShowDiff = enabled;
    return this;
  }

  public setTransformerOptions(options: TreeTransformerOptions) {
    this.options = options;
    if (!this.options.formatter) {
      this.options.formatter = this.formatProto;
    }
    return this;
  }

  public setProperties(tree: Tree) {
    const target = tree.obj ?? tree;
    ObjectFormatter.displayDefaults = this.isShowDefaults;
    this.properties = this.getPropertiesForDisplay(target);
  }

  public setDiffProperties(previousEntry: any) {
    if (this.isShowDiff) {
      const tree = this.findTree(previousEntry, this.stableId);
      const target = tree ? tree.obj ?? tree : null;
      this.compareWithProperties = this.getPropertiesForDisplay(target);
    }
    return this;
  }

  public getOriginalLayer(entry: any, stableId: string) {
    return this.findTree(entry, stableId);
  }

  private getPropertiesForDisplay(entry: any): any {
    if (!entry) {
      return;
    }

    let obj: any = {};
    obj.proto = Object.assign({}, entry.proto);
    if (obj.proto.children) delete obj.proto.children;
    if (obj.proto.childWindows) delete obj.proto.childWindows;
    if (obj.proto.childrenWindows) delete obj.proto.childrenWindows;
    if (obj.proto.childContainers) delete obj.proto.childContainers;
    if (obj.proto.windowToken) delete obj.proto.windowToken;
    if (obj.proto.rootDisplayArea) delete obj.proto.rootDisplayArea;
    if (obj.proto.rootWindowContainer) delete obj.proto.rootWindowContainer;
    if (obj.proto.windowContainer?.children) delete obj.proto.windowContainer.children;

    obj = ObjectFormatter.format(obj);

    Object.keys(obj.proto).forEach((prop: string) => {
      if (Object.keys(obj.proto[prop]).length === 0) {
        obj.proto[prop] = "empty";
      }
    });
    return obj;
  }

  private findTree(tree: any, stableId: string) {
    if (!tree) {
      return null;
    }

    if (tree.stableId && tree.stableId === stableId) {
      return tree;
    }

    if (!tree.children) {
      return null;
    }

    for (const child of tree.children) {
      const foundEntry: any = this.findTree(child, stableId);
      if (foundEntry) {
        return foundEntry;
      }
    }

    return null;
  }


  public transform() {
    const {formatter} = this.options!;
    if (!formatter) {
      throw new Error("Missing formatter, please set with setOptions()");
    }

    const transformedTree = this.transformTree(this.properties, this.rootName,
      this.compareWithProperties, this.rootName,
      this.stableId, this.transformOptions);

    return transformedTree;
  }

  private transformTree(
    properties: PropertiesTree | Terminal,
    name: string | Terminal,
    compareWithProperties: PropertiesTree | Terminal,
    compareWithName: string | Terminal,
    stableId: string,
    transformOptions: TransformOptions,
  ) {
    const originalProperties = properties;
    const metadata = this.getMetadata(
      originalProperties, transformOptions.metadataKey
    );

    const children: any[] = [];

    if (!this.isTerminal(properties)) {
      const transformedProperties = this.transformProperties(properties, transformOptions.metadataKey);
      properties = transformedProperties.properties;
    }

    if (!this.isTerminal(compareWithProperties)) {
      const transformedProperties = this.transformProperties(
        compareWithProperties,
        transformOptions.metadataKey
      );
      compareWithProperties = transformedProperties.properties;
    }

    for (const key in properties) {
      if (properties[key]) {
        let compareWithChild = new Terminal();
        let compareWithChildName = new Terminal();
        if (compareWithProperties[key]) {
          compareWithChild = compareWithProperties[key];
          compareWithChildName = key;
        }
        const child = this.transformTree(properties[key], key,
          compareWithChild, compareWithChildName,
          `${stableId}.${key}`, transformOptions);

        children.push(child);
      }
    }

    // Takes care of adding deleted items to final tree
    for (const key in compareWithProperties) {
      if (!properties[key] && compareWithProperties[key]) {
        const child = this.transformTree(new Terminal(), new Terminal(),
          compareWithProperties[key], key,
          `${stableId}.${key}`, transformOptions);

        children.push(child);
      }
    }

    let transformedProperties: any;
    if (
      children.length == 1 &&
      children[0].children?.length == 0 &&
      !children[0].combined
    ) {
      // Merge leaf key value pairs.
      const child = children[0];

      transformedProperties = {
        kind: "",
        name: (this.isTerminal(name) ? compareWithName : name) + ": " + child.name,
        stableId,
        children: child.children,
        combined: true,
      };

      if (this.isShowDiff) {
        transformedProperties.diffType = child.diffType;
      }
    } else {
      transformedProperties = {
        kind: "",
        name,
        stableId,
        children,
      };

      if (this.isShowDiff) {
        const diffType = this.getDiff(name, compareWithName);
        transformedProperties.diffType = diffType;

        if (diffType == DiffType.DELETED) {
          transformedProperties.name = compareWithName;
        }
      }
    }

    if (transformOptions.keepOriginal) {
      transformedProperties.properties = originalProperties;
    }

    if (metadata && transformOptions.metadataKey) {
      transformedProperties[transformOptions.metadataKey] = metadata;
    }

    if (!this.isTerminal(transformedProperties.name)) {
      transformedProperties.propertyKey = this.getPropertyKey(transformedProperties);
      transformedProperties.propertyValue = this.getPropertyValue(transformedProperties);
    }

    if (!this.filterMatches(transformedProperties) &&
        !this.hasChildMatchingFilter(transformedProperties?.children)) {
      transformedProperties.propertyKey = new Terminal();
    }

    return transformOptions.freeze ? Object.freeze(transformedProperties) : transformedProperties;
  }

  private hasChildMatchingFilter(children: PropertiesTree[] | null | undefined) {
    if (!children || children.length === 0) return false;

    let match = false;
    for (let i=0; i<children.length; i++) {
      if (this.filterMatches(children[i]) || this.hasChildMatchingFilter(children[i].children)) {
        match = true;
      }
    }

    return match;
  }

  private getMetadata(obj: PropertiesTree, metadataKey: string | null) {
    if (metadataKey && obj[metadataKey]) {
      const metadata = obj[metadataKey];
      obj[metadataKey] = undefined;
      return metadata;
    } else {
      return null;
    }
  }

  private getPropertyKey(item: PropertiesTree) {
    if (!item.children || item.children.length === 0) {
      return item.name.split(": ")[0];
    }
    return item.name;
  }

  private getPropertyValue(item: PropertiesTree) {
    if (!item.children || item.children.length === 0) {
      return item.name.split(": ").slice(1).join(": ");
    }
    return null;
  }


  private filterMatches(item: PropertiesTree | null): boolean {
    return this.filter(item) ?? false;
  }

  private transformProperties(properties: PropertiesTree, metadataKey: string | null) {
    const {skip, formatter} = this.options!;
    const transformedProperties: TransformedPropertiesObject = {
      properties: {},
    };
    let formatted = undefined;

    if (skip && skip.includes(properties)) {
      // skip
    } else if ((formatted = formatter(properties))) {
      // Obj has been formatted into a terminal node — has no children.
      transformedProperties.properties[formatted] = new Terminal();
    } else if (Array.isArray(properties)) {
      properties.forEach((e, i) => {
        transformedProperties.properties["" + i] = e;
      });
    } else if (typeof properties == "string") {
      // Object is a primitive type — has no children. Set to terminal
      // to differentiate between null object and Terminal element.
      transformedProperties.properties[properties] = new Terminal();
    } else if (typeof properties == "number" || typeof properties == "boolean") {
      // Similar to above — primitive type node has no children.
      transformedProperties.properties["" + properties] = new Terminal();
    } else if (properties && typeof properties == "object") {
      Object.keys(properties).forEach((key) => {
        if (key === metadataKey) {
          return;
        }
        transformedProperties.properties[key] = properties[key];
      });
    } else if (properties === null) {
      // Null object has no children — set to be terminal node.
      transformedProperties.properties.null = new Terminal();
    }
    return transformedProperties;
  }

  private getDiff(val: string | Terminal, compareVal: string | Terminal) {
    if (val && this.isTerminal(compareVal)) {
      return DiffType.ADDED;
    } else if (this.isTerminal(val) && compareVal) {
      return DiffType.DELETED;
    } else if (compareVal != val) {
      return DiffType.MODIFIED;
    } else {
      return DiffType.NONE;
    }
  }

  private compatibleStableId(item: Tree) {
    // For backwards compatibility
    // (the only item that doesn't have a unique stable ID in the tree)
    if (item.stableId === "winToken|-|") {
      return item.stableId + item.children[0].stableId;
    }
    return item.stableId;
  }

  private formatProto(item: any) {
    if (item?.prettyPrint) {
      return item.prettyPrint();
    }
  }

  private isTerminal(item: any) {
    return item instanceof Terminal;
  }
}
