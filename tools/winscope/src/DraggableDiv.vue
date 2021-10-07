<!-- Copyright (C) 2020 The Android Open Source Project

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
  <div
    class="draggable-container"
    :style="{visibility: contentIsLoaded ? 'visible' : 'hidden'}"
  >
    <md-card class="draggable-card">
      <div class="header" @mousedown="onHeaderMouseDown">
        <md-icon class="drag-icon">
          drag_indicator
        </md-icon>
        <slot name="header" />
      </div>
      <div class="content">
        <slot name="main" ref="content"/>
        <div class="resizer" v-show="resizeable" @mousedown="onResizerMouseDown"/>
      </div>
    </md-card>
  </div>
</template>
<script>
export default {
  name: "DraggableDiv",
  // If asyncLoad is enabled must call contentLoaded when content is ready
  props: ['position', 'asyncLoad', 'resizeable'],
  data() {
    return {
      positions: {
        clientX: undefined,
        clientY: undefined,
        movementX: 0,
        movementY: 0,
      },
      parentResizeObserver: null,
      contentIsLoaded: false,
      extraWidth: 0,
      extraHeight: 0,
    }
  },
  methods: {
    onHeaderMouseDown(e) {
      e.preventDefault();

      this.initDragAction(e);
    },
    onResizerMouseDown(e) {
      e.preventDefault();

      this.startResize(e);
    },
    initDragAction(e) {
      this.positions.clientX = e.clientX;
      this.positions.clientY = e.clientY;
      document.onmousemove = this.startDrag;
      document.onmouseup = this.stopDrag;
    },
    startDrag(e) {
      e.preventDefault();

      this.positions.movementX = this.positions.clientX - e.clientX;
      this.positions.movementY = this.positions.clientY - e.clientY;
      this.positions.clientX = e.clientX;
      this.positions.clientY = e.clientY;

      const parentHeight = this.$el.parentElement.clientHeight;
      const parentWidth = this.$el.parentElement.clientWidth;

      const divHeight = this.$el.clientHeight;
      const divWidth = this.$el.clientWidth;

      let top = this.$el.offsetTop - this.positions.movementY;
      if (top < 0) {
        top = 0;
      }
      if (top + divHeight > parentHeight) {
        top = parentHeight - divHeight;
      }

      let left = this.$el.offsetLeft - this.positions.movementX;
      if (left < 0) {
        left = 0;
      }
      if (left + divWidth > parentWidth) {
        left = parentWidth - divWidth;
      }

      this.$el.style.top = top + 'px';
      this.$el.style.left = left + 'px';
    },
    stopDrag() {
      document.onmouseup = null;
      document.onmousemove = null;
    },
    startResize(e) {
      e.preventDefault();
      this.startResizeX = e.clientX;
      this.startResizeY = e.clientY;
      document.onmousemove = this.resizing;
      document.onmouseup = this.stopResize;
      document.body.style.cursor = "nwse-resize";
    },
    resizing(e) {
      let extraWidth = this.extraWidth + (e.clientX - this.startResizeX);
      if (extraWidth < 0) {
        extraWidth = 0;
      }
      this.$emit('requestExtraWidth', extraWidth);

      let extraHeight = this.extraHeight + (e.clientY - this.startResizeY);
      if (extraHeight < 0) {
        extraHeight = 0;
      }
      this.$emit('requestExtraHeight', extraHeight);
    },
    stopResize(e) {
      this.extraWidth += e.clientX - this.startResizeX;
      if (this.extraWidth < 0) {
        this.extraWidth = 0;
      }
      this.extraHeight +=  e.clientY - this.startResizeY;
      if (this.extraHeight < 0) {
        this.extraHeight = 0;
      }
      document.onmousemove = null;
      document.onmouseup = null;
      document.body.style.cursor = null;
    },
    onParentResize() {
      const parentHeight = this.$el.parentElement.clientHeight;
      const parentWidth = this.$el.parentElement.clientWidth;

      const elHeight = this.$el.clientHeight;
      const elWidth = this.$el.clientWidth;
      const rect = this.$el.getBoundingClientRect();

      const offsetBottom = parentHeight - (rect.y + elHeight);
      if (offsetBottom < 0) {
        this.$el.style.top = parseInt(this.$el.style.top) + offsetBottom + 'px';
      }

      const offsetRight = parentWidth - (rect.x + elWidth);
      if (offsetRight < 0) {
        this.$el.style.left = parseInt(this.$el.style.left) + offsetRight + 'px';
      }
    },
    contentLoaded() {
      // To be called if content is loaded async (eg: video), so that div may
      // position itself correctly.

      if (this.contentIsLoaded) {
        return;
      }

      this.contentIsLoaded = true;
      const margin = 15;

      switch (this.position) {
        case 'bottomLeft':
          this.moveToBottomLeft(margin);
          break;

        default:
          throw new Error('Unsupported starting position for DraggableDiv');
      }
    },
    moveToBottomLeft(margin) {
      margin = margin || 0;

      const divHeight = this.$el.clientHeight;
      const parentHeight = this.$el.parentElement.clientHeight;

      this.$el.style.top = parentHeight - divHeight - margin + 'px';
      this.$el.style.left = margin + 'px';
    },
  },
  mounted() {
    if (!this.asyncLoad) {
      this.contentLoaded();
    }

    // Listen for changes in parent height to avoid element exiting visible view
    this.parentResizeObserver = new ResizeObserver(this.onParentResize);

    this.parentResizeObserver.observe(this.$el.parentElement);
  },
  destroyed() {
    this.parentResizeObserver.unobserve(this.$el.parentElement);
  },
}
</script>
<style scoped>
.draggable-container {
  position: absolute;
}

.draggable-card {
  margin: 0;
}

.header {
  cursor: grab;
  padding: 3px;
}

.resizer {
  position: absolute;
  right: 0;
  bottom: 0;
  width: 0;
  height: 0;
  border-style: solid;
  border-width: 0 0 15px 15px;
  border-color: transparent transparent #ffffff transparent;
  cursor: nwse-resize;
}
</style>