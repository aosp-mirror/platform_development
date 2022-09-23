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

interface TreeNode {
  name: string;
  parent?: TreeNode;
  children?: TreeNode[];
}

type FilterType = (node: TreeNode | undefined | null) => boolean;

class TreeUtils
{
  public static findDescendantNode(node: TreeNode, isTargetNode: FilterType): TreeNode|undefined {
    if (isTargetNode(node)) {
      return node;
    }

    if (!node.children) {
      return;
    }

    for (const child of node.children) {
      const target = this.findDescendantNode(child, isTargetNode);
      if (target) {
        return target;
      }
    }

    return undefined;
  }

  public static findAncestorNode(node: TreeNode, isTargetNode: FilterType): TreeNode|undefined {
    let ancestor = node.parent;

    while (ancestor && !isTargetNode(ancestor)) {
      ancestor = ancestor.parent;
    }

    return ancestor;
  }

  public static makeNodeFilter(filterString: string): FilterType {
    const filterStrings = filterString.split(",");
    const positive: any[] = [];
    const negative: any[] = [];
    filterStrings.forEach((f) => {
      f = f.trim();
      if (f.startsWith("!")) {
        const regex = new RegExp(f.substring(1), "i");
        negative.push((s: any) => !regex.test(s));
      } else {
        const regex = new RegExp(f, "i");
        positive.push((s: any) => regex.test(s));
      }
    });
    const filter = (item: TreeNode | undefined | null) => {
      if (item) {
        const apply = (f: any) => f(`${item.name}`);
        return (positive.length === 0 || positive.some(apply)) &&
          (negative.length === 0 || negative.every(apply));
      }
      return false;
    };
    return filter;
  }
}

export {TreeNode, TreeUtils, FilterType};
