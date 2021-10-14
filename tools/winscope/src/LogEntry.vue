<template>
  <div
    class="entry"
    :class="[
      {
        'inactive': !source.occured,
        'just-inactivated': source.justInactivated,
      },
      source.level.toLowerCase()
    ]"
  >
    <div class="level-column">
      <div>
        <div class="icon" v-if="source.level.toLowerCase() === 'verbose'">
          v
        </div>
        <i class="material-icons icon" v-else>
          {{ levelIcons[source.level.toLowerCase()] }}
        </i>
        <md-tooltip md-direction="right" style="margin-left: -15px">
          {{ source.level.toLowerCase() }}
        </md-tooltip>
      </div>
    </div>
    <div class="time-column">
      <a @click="setTimelineTime(source.timestamp)" class="time-link">
        {{source.time}}
      </a>
      <div
        class="new-badge"
        :style="{visibility: source.new ? 'visible' : 'hidden'} "
      >
        New
      </div>
    </div>
    <div class="tag-column">{{source.tag}}</div>
    <div class="at-column">{{source.at}}</div>
    <div class="message-column">{{source.text}}</div>
  </div>
</template>

<script>
import {logLevel} from './utils/consts';

export default {
  name: 'logentry',
  props: {
    index: {
      type: Number,
    },
    source: {
      type: Object,
      default() {
        return {};
      },
    },
  },
  data() {
    return {
      levelIcons: {
        [logLevel.INFO]: 'info_outline',
        [logLevel.DEBUG]: 'help_outline',
        [logLevel.VERBOSE]: 'assignment',
        [logLevel.WARN]: 'warning',
        [logLevel.ERROR]: 'error',
        [logLevel.WTF]: 'bolt',
      },
    };
  },
  methods: {
    setTimelineTime(timestamp) {
      this.$store.dispatch('updateTimelineTime', timestamp);
    },
  },
};
</script>
<style scoped>
.level-column {
  width: 2em;
  display: inline-flex;
}

.level-column > div {
  align-self: start;
}

.time-column {
  display: inline-flex;
  width: 13em;
}

.time-column .time-link {
  width: 9em;
}

.tag-column {
  width: 11em;
  min-width: 11em;
}

.at-column {
  width: 30em;
  min-width: 30em;
}

.message-column {
  min-width: 50em;
  flex-grow: 1;
  word-wrap: break-word;
}

.entry {
  width: 100%;
  display: inline-flex;
}

.entry:hover {
  background: #f1f1f1;
}

.entry > div {
  padding: 6px 10px;
  border-bottom: 1px solid #f1f1f1;
}

a {
  cursor: pointer;
}

.inactive {
  color: gray;
}

.inactive a {
  color: gray;
}

.just-inactivated {
  background: #dee2e3;
}

.new-badge {
  display: inline-block;
  background: rgb(84, 139, 247);
  border-radius: 3px;
  color: white;
  padding: 0 5px;
  margin-left: 5px;
  font-size: 10px;
  align-self: flex-start;
}

.entry.warn, .entry.warn > div {
  background: #FFE0B2;
}

.entry.warn.inactive, .entry.warn.inactive > div {
  background: #FFF3E0;
}

.entry.error, .entry.error > div,
.entry.wtf, .entry.wtf > div {
  background: #FFCCBC;
}

.entry.error.inactive, .entry.error.inactive > div,
.entry.wtf.inactive, .entry.wtf.inactive > div {
  background: #FBE9E7;
}

.level-column .icon {
  font-size: 15px;
  color: gray;
  width: 15px;
  height: 15px;
  text-align: center;
}

.entry.warn .level-column .icon {
  color: #FBC02D;
  font-size: 20px;
}

.entry.error .level-column .icon, .entry.wtf .level-column .icon  {
  color: #FF6E40;
  font-size: 20px;
}
</style>
