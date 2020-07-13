<template>
  <div class="entry" :class="{inactive: !source.occured}">
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
    <div class="text-column">{{source.text}}</div>
  </div>
</template>

<script>
export default {
  name: 'logentry',
  props: {
    index: {
      type: Number
    },
    source: {
      type: Object,
      default () {
        return {}
      }
    }
  },
  methods: {
    setTimelineTime(timestamp) {
      this.$store.dispatch('updateTimelineTime', timestamp);
    }
  },
}
</script>
<style scoped>
.time-column {
  display: inline-flex;
  width: 13em;
}

.time-column .time-link {
  width: 9em;
}

.tag-column {
  width: 12em;
  min-width: 12em;
}

.at-column {
  width: 30em;
  min-width: 30em;
}

.text-column {
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
</style>