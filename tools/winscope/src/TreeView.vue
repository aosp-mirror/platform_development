<!-- Copyright (C) 2017 The Android Open Source Project

     Licensed under the Apache License, Version 2.0 (the "License");
     you may not use this file except in compliance with the License.
     You may obtain a copy of the License at

          http://www.apache.org/licenses/LICENSE-2.0

     Unless required by applicable law or agreed to in writing, software
     distributed under the License is distributed on an "AS IS" BASIS,
     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     See the License for the specific language governing permissions and
     limitations under the License.
-->
<template>
  <div class="tree-view" v-if="item">
    <div class="node"
      :class="{ leaf: isLeaf, selected: isSelected, clickable: isClickable, diffClass }"
      :style="nodeOffsetStyle"
      @click="clicked"
      ref="node"
    >
      <button class="toggle-tree-btn" @click="toggleTree" v-if="!isLeaf" v-on:click.stop>
        <i aria-hidden="true" class="md-icon md-theme-default material-icons">
          {{isCollapsed ? "chevron_right" : "expand_more"}}
        </i>
      </button>
      <div class="padding" v-else>
        <i aria-hidden="true" class="md-icon md-theme-default material-icons">
          arrow_right
        </i>
      </div>
      <div class="description">
        <span class="kind">{{item.kind}}</span>
        <span v-if="item.kind && item.name">-</span>
        <span>{{item.name}}</span>
        <div
          v-for="c in item.chips"
          v-bind:key="c.long"
          :title="c.long"
          :class="chipClassForChip(c)"
        >
          {{c.short}}
        </div>
      </div>
      <div v-show="isCollapsed">
        <button class="expand-tree-btn"  :class="{ 'child-selected': isCollapsed && childIsSelected }" v-if="children" @click="expandTree" v-on:click.stop>
          <i aria-hidden="true" class="md-icon md-theme-default material-icons">more_horiz</i>
        </button>
      </div>
    </div>
    <div class="children" v-if="children" v-show="!isCollapsed">
      <tree-view
        v-for="(c,i) in children"
        :item="c"
        @item-selected="childItemSelected"
        :selected="selected"
        :key="i"
        :chip-class="chipClass"
        :filter="childFilter(c)"
        :flattened="flattened"
        :force-flattened="applyingFlattened"
        v-show="filterMatches(c)"
        :items-clickable="itemsClickable"
        :initial-depth="depth + 1"
        :collapse="collapseChildren"
        :collapseChildren="collapseChildren"
        :useGlobalCollapsedState="useGlobalCollapsedState"
        ref="children"
      />
    </div>
  </div>
</template>

<script>
import jsonProtoDefs from "frameworks/base/core/proto/android/server/windowmanagertrace.proto";
import protobuf from "protobufjs";

import { DiffType } from "./utils/diff.js";

var protoDefs = protobuf.Root.fromJSON(jsonProtoDefs);
var TraceMessage = protoDefs.lookupType(
  "com.android.server.wm.WindowManagerTraceFileProto"
);
var ServiceMessage = protoDefs.lookupType(
  "com.android.server.wm.WindowManagerServiceDumpProto"
);

const levelOffset = 24; /* in px, must be kept in sync with css, maybe find a better solution... */

export default {
  name: "tree-view",
  props: [
    "item",
    "selected",
    "chipClass",
    "filter",
    "flattened",
    "force-flattened",
    "items-clickable",
    "initial-depth",
    "collapse",
    "collapseChildren",
    // Allows collapse state to be tracked by Vuex so that collapse state of
    // items with same stableId can remain consisten accross time and easily
    // toggled from anywhere in the app.
    // Should be true if you are using the same TreeView to display multiple
    // trees throughout the component's lifetime to make sure same nodes are
    // toggled when switching back and forth between trees.
    // If true, requires all nodes in tree to have a stableId.
    "useGlobalCollapsedState",
  ],
  data() {
    const isCollapsedByDefault = this.collapse ?? false;

    return {
      isChildSelected: false,
      clickTimeout: null,
      isCollapsedByDefault,
      localCollapsedState: isCollapsedByDefault,
      diffSymbol: {
        [DiffType.NONE]: "",
        [DiffType.ADDED]: "+",
        [DiffType.DELETED]: "-",
        [DiffType.MODIFIED]: ".",
      },
    };
  },
  methods: {
    setCollapseValue(isCollapsed) {
      if (this.useGlobalCollapsedState) {
        this.$store.commit('setCollapsedState', {
          item: this.item,
          isCollapsed,
        });
      } else {
        this.localCollapsedState = isCollapsed;
      }
    },
    toggleTree() {
      this.setCollapseValue(!this.isCollapsed);
    },
    expandTree() {
      this.setCollapseValue(false);
    },
    selectNext(found, inCollapsedTree) {
      // Check if this is the next visible item
      if (found && this.filterMatches(this.item) && !inCollapsedTree) {
        this.select();
        return false;
      }

      // Set traversal state variables
      if (this.isSelected) {
        found = true;
      }
      if (this.isCollapsed) {
        inCollapsedTree = true;
      }

      // Travers children trees recursively in reverse to find currently
      // selected item and select the next visible one
      if (this.$refs.children) {
        for (var c of this.$refs.children) {
          found = c.selectNext(found, inCollapsedTree);
        }
      }

      return found;
    },
    selectPrev(found, inCollapsedTree) {
      // Set inCollapseTree flag to make sure elements in collapsed trees are not selected.
      const isRootCollapse = !inCollapsedTree && this.isCollapsed;
      if (isRootCollapse) {
        inCollapsedTree = true;
      }

      // Travers children trees recursively in reverse to find currently
      // selected item and select the previous visible one
      if (this.$refs.children) {
        for (var c of [...this.$refs.children].reverse()) {
          found = c.selectPrev(found, inCollapsedTree);
        }
      }

      // Unset inCollapseTree flag as we are no longer in a collapsed tree.
      if (isRootCollapse) {
        inCollapsedTree = false;
      }

      // Check if this is the previous visible item
      if (found && this.filterMatches(this.item) && !inCollapsedTree) {
        this.select();
        return false;
      }

      // Set found flag so that the next visited visible item can be selected.
      if (this.isSelected) {
        found = true;
      }

      return found;
    },
    childItemSelected(item) {
      this.isChildSelected = true;
      this.$emit("item-selected", item);
    },
    select() {
      this.$emit('item-selected', this.item);
    },
    clicked(e) {
      if (window.getSelection().type === "range") {
        // Ignore click if is selection
        return;
      }

      if (!this.isLeaf && e.detail % 2 === 0) {
        // Double click collaspable node
        this.toggleTree();
      } else {
        this.select();
      }
    },
    chipClassForChip(c) {
      return [
        "tree-view-internal-chip",
        this.chipClassOrDefault,
        this.chipClassOrDefault + "-" + (c.class || "default")
      ];
    },
    filterMatches(c) {
      // If a filter is set, consider the item matches if the current item or any of its
      // children matches.
      if (this.filter) {
        var thisMatches = this.filter(c);
        const childMatches = (child) => this.filterMatches(child);
        return thisMatches || (!this.applyingFlattened &&
            c.children && c.children.some(childMatches));
      }
      return true;
    },
    childFilter(c) {
      if (this.filter) {
        if (this.filter(c)) {
          // Filter matched c, don't apply further filtering on c's children.
          return undefined;
        }
      }
      return this.filter;
    },
    isCurrentSelected() {
      return this.selected === this.item;
    },
  },
  computed: {
    isCollapsed() {
      if (this.item.children.length === 0) {
        return false;
      }

      if (this.useGlobalCollapsedState) {
        return this.$store.getters.collapsedStateStoreFor(this.item) ??
        this.isCollapsedByDefault;
      }

      return this.localCollapsedState;
    },
    isSelected() {
      return this.selected === this.item;
    },
    childIsSelected() {
      if (this.$refs.children) {
        for (var c of this.$refs.children) {
          if (c.isSelected || c.childIsSelected) {
            return true;
          }
        }
      }

      return false;
    },
    diffClass() {
      return this.item.diff ? this.item.diff.type : ''
    },
    chipClassOrDefault() {
      return this.chipClass || "tree-view-chip";
    },
    applyingFlattened() {
      return (this.flattened && this.item.flattened) || this.forceFlattened;
    },
    children() {
      return this.applyingFlattened ? this.item.flattened : this.item.children;
    },
    isLeaf() {
      return !this.children || this.children.length == 0;
    },
    isClickable() {
      return !this.isLeaf || this.itemsClickable;
    },
    depth() {
      return this.initialDepth || 0;
    },
    nodeOffsetStyle() {
      const offest = levelOffset * (this.depth + this.isLeaf) + 'px';

      return {
        marginLeft: '-' + offest,
        paddingLeft: offest,
      }
    }
  },
  mounted() {
    // Prevent highlighting on multiclick of node element
    this.$refs.node?.addEventListener('mousedown', (e) => {
      if (e.detail > 1) {
        e.preventDefault();
        return false;
      }

      return true;
    });
  },

};
</script>
<style>
.data-card > .tree-view {
  border: none;
}

.tree-view {
  display: block;
}

.tree-view .node {
  display: flex;
  padding: 2px;
  align-items: flex-start;
}

.tree-view .node.clickable {
  cursor: pointer;
}

.tree-view .node:hover:not(.selected) {
  background: #f1f1f1;
}

.tree-view .node:not(.selected).added {
  background: chartreuse;
}

.tree-view .node:not(.selected).removed {
  background: coral;
}

.tree-view .node:not(.selected).modified {
  background: cyan;
}

.children {
  /* Aligns border with collapse arrows */
  margin-left: 12px;
  padding-left: 12px;
  border-left: 1px solid rgb(238, 238, 238);
  margin-top: 0px;
}

.tree-view .node:hover + .children {
  border-left: 1px solid rgb(200, 200, 200);
}

.kind {
  color: #333;
  font-weight: bold;
}

.selected {
  background-color: #365179;
  color: white;
}

.childSelected {
  border-left: 1px solid rgb(233, 22, 22)
}

.selected .kind {
  color: #e9e9e9;
}

.tree-view-internal-chip {
  display: inline-block;
}

.tree-view-chip {
  padding: 0 10px;
  border-radius: 10px;
  background-color: #aaa;
  color: black;
}

.tree-view-chip.tree-view-chip-warn {
  background-color: #ffaa6b;
  color: black;
}

.tree-view-chip.tree-view-chip-error {
  background-color: #ff6b6b;
  color: black;
}

.tree-view-chip.tree-view-chip-gpu {
  background-color: #00c853;
  color: black;
}

.tree-view-chip.tree-view-chip-hwc {
  background-color: #448aff;
  color: black;
}

.toggle-tree-btn, .expand-tree-btn {
  background: none;
  color: inherit;
  border: none;
  padding: 0;
  font: inherit;
  cursor: pointer;
  outline: inherit;
}

.expand-tree-btn {
  margin-left: 5px;
}

.expand-tree-btn.child-selected {
  color: #3f51b5;
}

</style>
