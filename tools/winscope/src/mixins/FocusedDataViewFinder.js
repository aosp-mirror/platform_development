export default {
  name: 'FocusedDataViewFinder',
  created() {
    document.addEventListener("scroll", this.updateFocusedView);
  },
  deleted() {
    document.removeEventListener("scroll", this.updateFocusedView);
  },
  computed: {
    timelineFiles() {
      return this.$store.getters.timelineFiles;
    },
  },
  methods: {
    updateFocusedView() {
      const positions = this.getDataViewPositions();
      const focusedFile = this.findFocusedDataView(positions);

      this.$store.commit('setFocusedFile', focusedFile);
    },
    getDataViewPositions() {
      const positions = {}

      for (const file of this.files) {
        const dataView = this.$refs[file.filename];
        if (!dataView || dataView.length === 0) {
          continue;
        }

        const dataViewEl = dataView[0].$el;
        positions[file.filename] = dataViewEl.getBoundingClientRect();
      }

      return positions;
    },
    /**
     * Returns the file of the DataView that takes up the most of the visible
     * screen space.
     * @param positions A map from filenames to their respective boundingClientRect.
     */
    findFocusedDataView(positions) {
      const visibleHeight =
        Math.max(document.documentElement.clientHeight || 0, window.innerHeight || 0);

      let maxScreenSpace = 0;
      let focusedDataView = this.files[0];
      for (const file of this.files) {
        const pos = positions[file.filename];
        if (!pos) {
          continue;
        }

        let screenSpace = 0;
        if (0 <= pos.top && pos.top <= visibleHeight) {
          screenSpace = Math.min(visibleHeight, pos.bottom) - pos.top;
        } else if (0 <= pos.bottom && pos.bottom <= visibleHeight) {
          screenSpace = pos.bottom - Math.max(0, pos.top);
        } else if (pos.top <= 0 && pos.bottom >= visibleHeight) {
          screenSpace = visibleHeight;
        }

        if (screenSpace >= maxScreenSpace) {
          maxScreenSpace = screenSpace;
          focusedDataView = file;
        }
      }

      return focusedDataView;
    }
  }
}