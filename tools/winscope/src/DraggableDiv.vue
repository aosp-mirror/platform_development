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
  <div class="draggable-container">
    <md-card class="draggable-card">
      <div class="header" @mousedown="onMouseDown">
        <md-icon class="drag-icon">
          drag_indicator
        </md-icon>
        <slot name="header" />
      </div>
      <slot name="main" />
    </md-card>
  </div>
</template>
<script>
export default {
  name: "DraggableDiv",
  data() {
    return {
      positions: {
        clientX: undefined,
        clientY: undefined,
        movementX: 0,
        movementY: 0,
        parentResizeObserver: null,
      }
    }
  },
  props: ['position'],
  methods: {
    onMouseDown(e) {
      e.preventDefault();

      this.initDragAction(e);
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
  },
  mounted() {
    const margin = 15;
    const parentHeight = this.$el.parentElement.clientHeight;

    const resizeObserver = new ResizeObserver(entries => {
      const divHeight = this.$el.clientHeight;

      switch (this.position) {
        case 'bottomLeft':
          this.$el.style.top = parentHeight - divHeight - margin + 'px';
          this.$el.style.left = margin + 'px';
          break;

        default:
          throw new Error('Unsupported starting position for DraggableDiv');
      }
    });

    // Listens of updates to size on container,
    // eg: if video needs to load it will change height at a later time than now
    resizeObserver.observe(this.$el);
    setTimeout(() => { resizeObserver.unobserve(this.$el); }, 500);

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
</style>