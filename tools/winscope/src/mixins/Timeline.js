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

import _ from "lodash";
import { nanos_to_string } from "../transform";
import { transitionMap } from "../utils/consts";

/**
 * Represents a continuous section of the timeline that is rendered into the
 * timeline svg.
 */
class Block {
  /**
   * Create a block.
   * @param {number} startPos - The start position of the block as a percentage
   * of the timeline width.
   * @param {number} width - The width of the block as a percentage of the
   * timeline width.
   */
  constructor(startPos, width) {
    this.startPos = startPos;
    this.width = width;
  }
}

//Represents a continuous section of the tag display that relates to a specific transition
class Transition {
  /**
   * Create a transition.
   * @param {number} startPos - The position of the start tag as a percentage
   * of the timeline width.
   * @param {number} startTime - The start timestamp in ms of the transition.
   * @param {number} endTime - The end timestamp in ms of the transition.
   * @param {number} width - The width of the transition as a percentage of the
   * timeline width.
   * @param {string} color - the color of transition depending on type.
   * @param {number} overlap - number of transitions with which this transition overlaps.
   * @param {string} tooltip - The tooltip of the transition, minus the type of transition.
   */
  constructor(startPos, startTime, endTime, width, color, overlap, tooltip) {
    this.startPos = startPos;
    this.startTime = startTime;
    this.endTime = endTime;
    this.width = width;
    this.color = color;
    this.overlap = overlap;
    this.tooltip = tooltip;
  }
}

/**
 * This Mixin should only be injected into components which have the following:
 * - An element in the template referenced as 'timeline' (this.$refs.timeline).
 */

export default {
  name: 'timeline',
  props: {
    /**
     * A 'timeline' as an array of timestamps
     */
    'timeline': {
      type: Array,
    },
    /**
     * A scale factor is an array of two elements, the min and max timestamps of
     * the timeline
     */
    'scale': {
      type: Array,
    },
    'tags': {
      type: Array,
    },
    'errors': {
      type: Array,
    },
    'flickerMode': {
      type: Boolean,
    }
  },
  data() {
    return {
      /**
       * Is a number representing the percentage of the timeline a block should
       * be at a minimum or what percentage of the timeline a single entry takes
       * up when rendered.
       */
      pointWidth: 1,
    };
  },
  computed: {
    /**
     * Converts the timeline (list of timestamps) to an array of blocks to be
     * displayed. This is to have fewer elements in the rendered timeline.
     * Instead of having one rect for each timestamp in the timeline we only
     * have one for each continuous segment of the timeline. This is to improve
     * both the Vue patching step's performance and the DOM rendering
     * performance.
     */
    timelineBlocks() {
      const blocks = [];

      // The difference in time between two timestamps after which they are no
      // longer rendered as a continuous segment/block.
      const overlapDistanceInTs = (this.scale[1] - this.scale[0]) *
        ((this.crop?.right ?? 1) - (this.crop?.left ?? 0)) *
        1 / (100 - this.pointWidth);

      let blockStartTs = this.timeline[0];
      for (let i = 1; i < this.timeline.length; i++) {
        const lastTs = this.timeline[i - 1];
        const ts = this.timeline[i];
        if (ts - lastTs > overlapDistanceInTs) {
          const block = this.generateTimelineBlock(blockStartTs, lastTs);
          blocks.push(block);
          blockStartTs = ts;
        }
      }

      const blockEndTs = this.timeline[this.timeline.length - 1];
      const block = this.generateTimelineBlock(blockStartTs, blockEndTs);
      blocks.push(block);

      return Object.freeze(blocks);
    },

    //Generates list of transitions to be displayed in flicker mode
    timelineTransitions() {
      const transitions = [];

      //group tags by transition and 'id' property
      const groupedTags = _.groupBy(this.tags, tag => `"${tag.transition} ${tag.id}"`);

      for (const transitionId in groupedTags) {
        const id = groupedTags[transitionId];
        //there are at least two tags per id, maybe more if multiple traces
        // determine which tag is the start (min of start times), which is end (max of end times)
        const startTimes = id.filter(tag => tag.isStartTag).map(tag => tag.timestamp);
        const endTimes = id.filter(tag => !tag.isStartTag).map(tag => tag.timestamp);

        const transitionStartTime = Math.min(...startTimes);
        const transitionEndTime = Math.max(...endTimes);

        //do not freeze new transition, as overlap still to be handled (defaulted to 0)
        const transition = this.generateTransition(
          transitionStartTime,
          transitionEndTime,
          id[0].transition,
          0,
          id[0].layerId,
          id[0].taskId,
          id[0].windowToken
        );
        transitions.push(transition);
      }

      //sort transitions in ascending start position in order to handle overlap
      transitions.sort((a, b) => (a.startPos > b.startPos) ? 1 : -1);

      //compare each transition to the ones that came before
      for (let curr=0; curr<transitions.length; curr++) {
        let processedTransitions = [];

        for (let prev=0; prev<curr; prev++) {
          processedTransitions.push(transitions[prev]);

          if (this.isSimultaneousTransition(transitions[curr], transitions[prev])) {
            transitions[curr].overlap++;
          }
        }

        let overlapStore = processedTransitions.map(transition => transition.overlap);

        if (transitions[curr].overlap === Math.max(...overlapStore)) {
          let previousTransition = processedTransitions.find(transition => {
            return transition.overlap===transitions[curr].overlap;
          });
          if (this.isSimultaneousTransition(transitions[curr], previousTransition)) {
            transitions[curr].overlap++;
          }
        }
      }

      return Object.freeze(transitions);
    },
    errorPositions() {
      if (!this.flickerMode) return [];
      const errorPositions = this.errors.map(
        error => ({ pos: this.position(error.timestamp), ts: error.timestamp })
      );
      return Object.freeze(errorPositions);
    },
  },
  methods: {
    position(item) {
      let pos;
      pos = this.translate(item);
      pos = this.applyCrop(pos);

      return pos * (100 - this.pointWidth);
    },

    translate(cx) {
      const scale = [...this.scale];
      if (scale[0] >= scale[1]) {
        return cx;
      }

      return (cx - scale[0]) / (scale[1] - scale[0]);
    },

    untranslate(pos) {
      const scale = [...this.scale];
      if (scale[0] >= scale[1]) {
        return pos;
      }

      return pos * (scale[1] - scale[0]) + scale[0];
    },

    applyCrop(cx) {
      if (!this.crop) {
        return cx;
      }

      return (cx - this.crop.left) / (this.crop.right - this.crop.left);
    },

    unapplyCrop(pos) {
      if (!this.crop) {
        return pos;
      }

      return pos * (this.crop.right - this.crop.left) + this.crop.left;
    },

    objectWidth(startTs, endTs) {
      return this.position(endTs) - this.position(startTs) + this.pointWidth;
    },

    isSimultaneousTransition(currTransition, prevTransition) {
      return prevTransition.startPos <= currTransition.startPos
        && currTransition.startPos <= prevTransition.startPos+prevTransition.width
        && currTransition.overlap === prevTransition.overlap;
    },

    /**
     * Converts a position as a percentage of the timeline width to a timestamp.
     * @param {number} position - target position as a percentage of the
     *                            timeline's width.
     * @return {number} The index of the closest timestamp in the timeline to
     *                  the target position.
     */
    positionToTsIndex(position) {
      let targetTimestamp = position / (100 - this.pointWidth);
      targetTimestamp = this.unapplyCrop(targetTimestamp);
      targetTimestamp = this.untranslate(targetTimestamp);

      // The index of the timestamp in the timeline that is closest to the
      // targetTimestamp.
      const closestTsIndex = this.findClosestTimestampIndexTo(targetTimestamp);

      return closestTsIndex;
    },

    indexOfClosestElementTo(target, array) {
      let smallestDiff = Math.abs(target - array[0]);
      let closestIndex = 0;
      for (let i = 1; i < array.length; i++) {
        const elem = array[i];
        if (Math.abs(target - elem) < smallestDiff) {
          closestIndex = i;
          smallestDiff = Math.abs(target - elem);
        }
      }

      return closestIndex;
    },

    findClosestTimestampIndexTo(ts) {
      let left = 0;
      let right = this.timeline.length - 1;
      let mid = Math.floor((left + right) / 2);

      while (left < right) {
        if (ts < this.timeline[mid]) {
          right = mid - 1;
        } else if (ts > this.timeline[mid]) {
          left = mid + 1;
        } else {
          return mid;
        }
        mid = Math.floor((left + right) / 2);
      }

      const candidateElements = this.timeline.slice(left - 1, right + 2);
      const closestIndex =
        this.indexOfClosestElementTo(ts, candidateElements) + (left - 1);
      return closestIndex;
    },

    /**
     * Transforms an absolute position in the timeline to a timestamp present in
     * the timeline.
     * @param {number} absolutePosition - Pixels from the left of the timeline.
     * @return {number} The timestamp in the timeline that is closest to the
     *                  target position.
     */
    absolutePositionAsTimestamp(absolutePosition) {
      const timelineWidth = this.$refs.timeline.clientWidth;
      const position = (absolutePosition / timelineWidth) * 100;

      return this.timeline[this.positionToTsIndex(position)];
    },

    /**
     * Handles the block click event.
     * When a block in the timeline is clicked this function will determine
     * the target timeline index and update the timeline to match this index.
     * @param {MouseEvent} e - The mouse event of the click on a timeline block.
     */
    onBlockClick(e) {
      const clickOffset = e.offsetX;
      const timelineWidth = this.$refs.timeline.clientWidth;
      const clickOffsetAsPercentage = (clickOffset / timelineWidth) * 100;

      const clickedOnTsIndex =
        this.positionToTsIndex(clickOffsetAsPercentage - this.pointWidth / 2);

      if (this.disabled) {
        return;
      }

      var timestamp = parseInt(this.timeline[clickedOnTsIndex]);

      //pointWidth is always 1
      //if offset percentage < 1, clickedOnTsIndex becomes negative, leading to a negative index
      if (clickedOnTsIndex < 0) {
        timestamp = parseInt(this.timeline[0])
      }
      this.$store.dispatch('updateTimelineTime', timestamp);
    },

    /**
     * Handles the error click event.
     * When an error in the timeline is clicked this function will update the timeline
     * to match the error timestamp.
     * @param {number} errorTimestamp
     */
    onErrorClick(errorTimestamp) {
      this.$store.dispatch('updateTimelineTime', errorTimestamp);
    },

    /**
     * Generate a block object that can be used by the timeline SVG to render
     * a transformed block that starts at `startTs` and ends at `endTs`.
     * @param {number} startTs - The timestamp at which the block starts.
     * @param {number} endTs - The timestamp at which the block ends.
     * @return {Block} A block object transformed to the timeline's crop and
     *                 scale parameter.
     */
    generateTimelineBlock(startTs, endTs) {
      const blockWidth = this.objectWidth(startTs, endTs);
      return Object.freeze(new Block(this.position(startTs), blockWidth));
    },
    /**
     * Generate a transition object that can be used by the tag-timeline to render
     * a transformed transition that starts at `startTs` and ends at `endTs`.
     * @param {number} startTs - The timestamp at which the transition starts.
     * @param {number} endTs - The timestamp at which the transition ends.
     * @param {string} transitionType - The type of transition.
     * @param {number} overlap - The degree to which the transition overlaps with others.
     * @param {number} layerId - Helps determine if transition is associated with SF trace.
     * @param {number} taskId - Helps determine if transition is associated with WM trace.
     * @param {number} windowToken - Helps determine if transition is associated with WM trace.
     * @return {Transition} A transition object transformed to the timeline's crop and
     *                 scale parameter.
     */
    generateTransition(startTs, endTs, transitionType, overlap, layerId, taskId, windowToken) {
      const transitionWidth = this.objectWidth(startTs, endTs);
      const transitionDesc = transitionMap.get(transitionType).desc;
      const transitionColor = transitionMap.get(transitionType).color;
      var tooltip = `${transitionDesc}. Start: ${nanos_to_string(startTs)}. End: ${nanos_to_string(endTs)}.`;

      if (layerId !== 0 && taskId === 0 && windowToken === "") {
        tooltip += " SF only.";
      } else if ((taskId !== 0 || windowToken !== "") && layerId === 0) {
        tooltip += " WM only.";
      }

      return new Transition(this.position(startTs), startTs, endTs, transitionWidth, transitionColor, overlap, tooltip);
    },
  },
};