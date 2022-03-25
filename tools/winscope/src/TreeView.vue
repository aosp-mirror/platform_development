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
      :class="[{
        leaf: isLeaf,
        selected: isSelected,
        'child-selected': immediateChildSelected,
        clickable: isClickable,
        hover: nodeHover,
        'child-hover': childHover,
      }, diffClass]"
      :style="nodeOffsetStyle"
      @click="clicked"
      @contextmenu.prevent="openContextMenu"
      ref="node"
    >
      <button
        class="toggle-tree-btn"
        @click="toggleTree"
        v-if="!isLeaf && !flattened"
        v-on:click.stop
      >
        <i aria-hidden="true" class="md-icon md-theme-default material-icons">
          {{isCollapsed ? "chevron_right" : "expand_more"}}
        </i>
      </button>
      <div class="leaf-node-icon-wrapper" v-else>
        <i class="leaf-node-icon"/>
      </div>
      <div class="description">
        <div v-if="elementView">
          <component
            :is="elementView"
            :item="item"
            :simplify-names="simplifyNames"
          />
        </div>
        <div v-else>
          <DefaultTreeElement
            :item="item"
            :simplify-names="simplifyNames"
            :errors="errors"
            :transitions="transitions"
          />
        </div>
      </div>
      <div v-show="isCollapsed">
        <button
          class="expand-tree-btn"
          :class="[{
            'child-selected': isCollapsed && childIsSelected
            }, collapseDiffClass]"
          v-if="children"
          @click="expandTree"
          v-on:click.stop
        >
          <i
            aria-hidden="true"
            class="md-icon md-theme-default material-icons"
          >
            more_horiz
          </i>
        </button>
      </div>
    </div>

    <node-context-menu
      ref="nodeContextMenu"
      v-on:collapseAllOtherNodes="collapseAllOtherNodes"
    />

    <div class="children" v-if="children" v-show="!isCollapsed" :style="childrenIndentation()">
      <tree-view
        v-for="(c,i) in children"
        :item="c"
        @item-selected="childItemSelected"
        :selected="selected"
        :key="i"
        :filter="childFilter(c)"
        :flattened="flattened"
        :onlyVisible="onlyVisible"
        :simplify-names="simplifyNames"
        :flickerTraceView="flickerTraceView"
        :presentTags="currentTags"
        :presentErrors="currentErrors"
        :force-flattened="applyingFlattened"
        v-show="filterMatches(c)"
        :items-clickable="itemsClickable"
        :initial-depth="depth + 1"
        :collapse="collapseChildren"
        :collapseChildren="collapseChildren"
        :useGlobalCollapsedState="useGlobalCollapsedState"
        v-on:hoverStart="childHover = true"
        v-on:hoverEnd="childHover = false"
        v-on:selected="immediateChildSelected = true"
        v-on:unselected="immediateChildSelected = false"
        :elementView="elementView"
        v-on:collapseSibling="collapseSibling"
        v-on:collapseAllOtherNodes="collapseAllOtherNodes"
        v-on:closeAllContextMenus="closeAllContextMenus"
        ref="children"
      />
    </div>
  </div>
</template>

<script>
import DefaultTreeElement from './DefaultTreeElement.vue';
import NodeContextMenu from './NodeContextMenu.vue';
import {DiffType} from './utils/diff.js';
import {isPropertyMatch} from './utils/utils.js';

/* in px, must be kept in sync with css, maybe find a better solution... */
const levelOffset = 24;

export default {
  name: 'tree-view',
  props: [
    'item',
    'selected',
    'filter',
    'simplify-names',
    'flattened',
    'force-flattened',
    'items-clickable',
    'initial-depth',
    'collapse',
    'collapseChildren',
    // Allows collapse state to be tracked by Vuex so that collapse state of
    // items with same stableId can remain consisten accross time and easily
    // toggled from anywhere in the app.
    // Should be true if you are using the same TreeView to display multiple
    // trees throughout the component's lifetime to make sure same nodes are
    // toggled when switching back and forth between trees.
    // If true, requires all nodes in tree to have a stableId.
    'useGlobalCollapsedState',
    // Custom view to use to render the elements in the tree view
    'elementView',
    'onlyVisible',
    'flickerTraceView',
    'presentTags',
    'presentErrors',
  ],
  data() {
    const isCollapsedByDefault = this.collapse ?? false;

    return {
      isChildSelected: false,
      immediateChildSelected: false,
      clickTimeout: null,
      isCollapsedByDefault,
      localCollapsedState: isCollapsedByDefault,
      collapseDiffClass: null,
      nodeHover: false,
      childHover: false,
      diffSymbol: {
        [DiffType.NONE]: '',
        [DiffType.ADDED]: '+',
        [DiffType.DELETED]: '-',
        [DiffType.MODIFIED]: '.',
        [DiffType.MOVED]: '.',
      },
      currentTags: [],
      currentErrors: [],
      transitions: [],
      errors: [],
    };
  },
  watch: {
    stableId() {
      // Update anything that is required to change when item changes.
      this.updateCollapsedDiffClass();
    },
    hasDiff(hasDiff) {
      if (!hasDiff) {
        this.collapseDiffClass = null;
      } else {
        this.updateCollapsedDiffClass();
      }
    },
    currentTimestamp() {
      // Update anything that is required to change when time changes.
      this.currentTags = this.getCurrentItems(this.presentTags);
      this.currentErrors = this.getCurrentItems(this.presentErrors);
      this.transitions = this.getCurrentTransitions();
      this.errors = this.getCurrentErrorTags();
      this.updateCollapsedDiffClass();
    },
    isSelected(isSelected) {
      if (isSelected) {
        this.$emit('selected');
      } else {
        this.$emit('unselected');
      }
    },
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
      if (!this.isCollapsed) {
        this.recordExpandedPropertyEvent(this.item.name)
      }
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
        for (const c of this.$refs.children) {
          found = c.selectNext(found, inCollapsedTree);
        }
      }

      return found;
    },
    selectPrev(found, inCollapsedTree) {
      // Set inCollapseTree flag to make sure elements in collapsed trees are
      // not selected.
      const isRootCollapse = !inCollapsedTree && this.isCollapsed;
      if (isRootCollapse) {
        inCollapsedTree = true;
      }

      // Travers children trees recursively in reverse to find currently
      // selected item and select the previous visible one
      if (this.$refs.children) {
        for (const c of [...this.$refs.children].reverse()) {
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
      this.$emit('item-selected', item);
    },
    select() {
      this.$emit('item-selected', this.item);
    },
    clicked(e) {
      if (window.getSelection().type === 'range') {
        // Ignore click if is selection
        return;
      }

      if (!this.isLeaf && e.detail % 2 === 0) {
        // Double click collapsable node
        this.toggleTree();
      } else {
        this.select();
      }
    },
    filterMatches(c) {
      // If a filter is set, consider the item matches if the current item or
      // any of its children matches.
      if (this.filter) {
        const thisMatches = this.filter(c);
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
    updateCollapsedDiffClass() {
      // NOTE: Could be memoized in $store map like collapsed state if
      // performance ever becomes a problem.
      if (this.item) {
        this.collapseDiffClass = this.computeCollapseDiffClass();
      }
    },
    getAllDiffTypesOfChildren(item) {
      if (!item.children) {
        return new Set();
      }

      const classes = new Set();
      for (const child of item.children) {
        if (child.diff) {
          classes.add(child.diff.type);
        }
        for (const diffClass of this.getAllDiffTypesOfChildren(child)) {
          classes.add(diffClass);
        }
      }

      return classes;
    },
    computeCollapseDiffClass() {
      if (!this.isCollapsed) {
        return '';
      }

      const childrenDiffClasses = this.getAllDiffTypesOfChildren(this.item);

      childrenDiffClasses.delete(DiffType.NONE);
      childrenDiffClasses.delete(undefined);

      if (childrenDiffClasses.size === 0) {
        return '';
      }
      if (childrenDiffClasses.size === 1) {
        const diff = childrenDiffClasses.values().next().value;
        return diff;
      }

      return DiffType.MODIFIED;
    },
    collapseAllOtherNodes() {
      this.$emit('collapseAllOtherNodes');
      this.$emit('collapseSibling', this.item);
    },
    collapseSibling(item) {
      if (!this.$refs.children) {
        return;
      }

      for (const child of this.$refs.children) {
        if (child.item === item) {
          continue;
        }

        child.collapseAll();
      }
    },
    collapseAll() {
      this.setCollapseValue(true);

      if (!this.$refs.children) {
        return;
      }

      for (const child of this.$refs.children) {
        child.collapseAll();
      }
    },
    openContextMenu(e) {
      this.closeAllContextMenus();
      // vue-context takes in the event and uses clientX and clientY to
      // determine the position of the context meny.
      // This doesn't satisfy our use case so we specify our own positions for
      // this.
      this.$refs.nodeContextMenu.open({
        clientX: e.x,
        clientY: e.y,
      });
    },
    closeAllContextMenus(requestOrigin) {
      this.$refs.nodeContextMenu.close();
      this.$emit('closeAllContextMenus', this.item);
      this.closeAllChildrenContextMenus(requestOrigin);
    },
    closeAllChildrenContextMenus(requestOrigin) {
      if (!this.$refs.children) {
        return;
      }

      for (const child of this.$refs.children) {
        if (child.item === requestOrigin) {
          continue;
        }

        child.$refs.nodeContextMenu.close();
        child.closeAllChildrenContextMenus();
      }
    },
    childrenIndentation() {
      if (this.flattened || this.forceFlattened) {
        return {
          marginLeft: '0px',
          paddingLeft: '0px',
          marginTop: '0px',
        }
      } else {
        //Aligns border with collapse arrows
        return {
          marginLeft: '12px',
          paddingLeft: '11px',
          borderLeft: '1px solid rgb(238, 238, 238)',
          marginTop: '0px',
        }
      }
    },

    /** Performs check for id match between entry and present tags/errors
     * exits once match has been found
     */
    matchItems(flickerItems) {
      var match = false;
      flickerItems.every(flickerItem => {
        if (isPropertyMatch(flickerItem, this.item)) {
          match = true;
          return false;
        }
      });
      return match;
    },
    /** Returns check for id match between entry and present tags/errors */
    isEntryTagMatch() {
      return this.matchItems(this.currentTags) || this.matchItems(this.currentErrors);
    },

    getCurrentItems(items) {
      if (!items) return [];
      else return items.filter(item => item.timestamp===this.currentTimestamp);
    },
    getCurrentTransitions() {
      var transitions = [];
      var ids = [];
      this.currentTags.forEach(tag => {
        if (!ids.includes(tag.id) && isPropertyMatch(tag, this.item)) {
          transitions.push(tag.transition);
          ids.push(tag.id);
        }
      });
      return transitions;
    },
    getCurrentErrorTags() {
      return this.currentErrors.filter(error => isPropertyMatch(error, this.item));
    },
  },
  computed: {
    hasDiff() {
      return this.item?.diff !== undefined;
    },
    stableId() {
      return this.item?.stableId;
    },
    currentTimestamp() {
      return this.$store.state.currentTimestamp;
    },
    isCollapsed() {
      if (!this.item.children || this.item.children?.length === 0) {
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
        for (const c of this.$refs.children) {
          if (c.isSelected || c.childIsSelected) {
            return true;
          }
        }
      }

      return false;
    },
    diffClass() {
      return this.item.diff ? this.item.diff.type : '';
    },
    applyingFlattened() {
      return (this.flattened && this.item.flattened) || this.forceFlattened;
    },
    children() {
      return this.applyingFlattened ? this.item.flattened : this.item.children;
    },
    isLeaf() {
      return !this.children || this.children.length === 0;
    },
    isClickable() {
      return !this.isLeaf || this.itemsClickable;
    },
    depth() {
      return this.initialDepth || 0;
    },
    nodeOffsetStyle() {
      const offset = levelOffset * (this.depth + this.isLeaf) + 'px';

      var display = "";
      if (!this.item.timestamp
        && this.flattened
        && (this.onlyVisible && !this.item.isVisible ||
            this.flickerTraceView && !this.isEntryTagMatch())) {
        display = 'none';
      }

      return {
        marginLeft: '-' + offset,
        paddingLeft: offset,
        display: display,
      };
    },
  },
  mounted() {
    // Prevent highlighting on multiclick of node element
    this.nodeMouseDownEventListner = (e) => {
      if (e.detail > 1) {
        e.preventDefault();
        return false;
      }

      return true;
    };
    this.$refs.node?.addEventListener('mousedown',
        this.nodeMouseDownEventListner);

    this.updateCollapsedDiffClass();

    this.nodeMouseEnterEventListener = (e) => {
      this.nodeHover = true;
      this.$emit('hoverStart');
    };
    this.$refs.node?.addEventListener('mouseenter',
        this.nodeMouseEnterEventListener);

    this.nodeMouseLeaveEventListener = (e) => {
      this.nodeHover = false;
      this.$emit('hoverEnd');
    };
    this.$refs.node?.addEventListener('mouseleave',
        this.nodeMouseLeaveEventListener);
  },
  beforeDestroy() {
    this.$refs.node?.removeEventListener('mousedown',
        this.nodeMouseDownEventListner);
    this.$refs.node?.removeEventListener('mouseenter',
        this.nodeMouseEnterEventListener);
    this.$refs.node?.removeEventListener('mouseleave',
        this.nodeMouseLeaveEventListener);
  },
  components: {
    DefaultTreeElement,
    NodeContextMenu,
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

.tree-view .node:not(.selected).added,
.tree-view .node:not(.selected).addedMove,
.tree-view .expand-tree-btn.added,
.tree-view .expand-tree-btn.addedMove {
  background: #03ff35;
}

.tree-view .node:not(.selected).deleted,
.tree-view .node:not(.selected).deletedMove,
.tree-view .expand-tree-btn.deleted,
.tree-view .expand-tree-btn.deletedMove {
  background: #ff6b6b;
}

.tree-view .node:not(.selected).modified,
.tree-view .expand-tree-btn.modified {
  background: cyan;
}

.tree-view .node.addedMove:after,
.tree-view .node.deletedMove:after {
  content: 'moved';
  margin: 0 5px;
  background: #448aff;
  border-radius: 5px;
  padding: 3px;
  color: white;
}

.tree-view .node.child-selected + .children {
  border-left: 1px solid #b4b4b4;
}

.tree-view .node.selected + .children {
  border-left: 1px solid rgb(200, 200, 200);
}

.tree-view .node.child-hover + .children {
  border-left: 1px solid #b4b4b4;
}

.tree-view .node.hover + .children {
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

.description {
  display: flex;
  flex: 1 1 auto;
}

.description > div {
  display: flex;
  flex: 1 1 auto;
}

.leaf-node-icon-wrapper {
  width: 24px;
  height: 24px;
  display: inline-flex;
  align-content: center;
  align-items: center;
  justify-content: center;
}

.leaf-node-icon {
  content: "";
  display: inline-block;
  height: 5px;
  width: 5px;
  margin-top: -2px;
  border-radius: 50%;
  background-color: #9b9b9b;
}

</style>
