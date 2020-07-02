<template>
  <div class="entry" :class="{inactive: source.timestamp > currentTimestamp}" @click="onClick(source)">
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
    <div class="type-column">{{transactionTypeOf(source)}}</div>
    <div class="origin-column">
      <span style="white-space: pre;">{{formatOrigin(source)}}</span>
    </div>
    <div class="affected-surfaces-column">
      <span v-for="(surface, index) in sufacesAffectedBy(source)">
        {{surface.id}}<span v-if="surface.name"> ({{ surface.name }})</span>
        <span v-if="index + 1 < sufacesAffectedBy(source).length">,&nbsp;</span>
      </span>
    </div>
  </div>
</template>

<script>
export default {
  name: 'transaction-entry',
  props: {
    index: {
      type: Number
    },
    source: {
      type: Object,
      default () {
        return {}
      }
    },
    onClick: {
      type: Function,
    },
  },
  computed: {
    currentTimestamp() {
      return this.$store.state.currentTimestamp;
    }
  },
  methods: {
    setTimelineTime(timestamp) {
      this.$store.dispatch('updateTimelineTime', timestamp);
    },
    transactionTypeOf(transaction) {
      if (transaction.type !== 'transaction') {
        return transaction.type;
      }

      if (transaction.transactions.length === 0) {
        return "Empty Transaction";
      }

      const types = new Set();
      transaction.transactions.forEach(t => types.add(t.type));

      return Array.from(types).join(", ");
    },
    sufacesAffectedBy(transaction) {
      if (transaction.type !== 'transaction') {
        return [{name: transaction.layerName, id: transaction.obj.id}];
      }

      const surfaceIds = new Set();
      const affectedSurfaces = [];
      for (const transaction of transaction.transactions) {
        const id = transaction.obj.id;
        if (!surfaceIds.has(id)) {
          surfaceIds.add(id);
          affectedSurfaces.push({name: transaction.layerName, id});
        }
      }

      return affectedSurfaces
    },
    formatOrigin(transaction) {
      if (!transaction.origin) {
        return "unavailable";
      }

      return `PID: ${transaction.origin.pid},\nUID: ${transaction.origin.uid}`;
    },
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

.type-column {
  width: 12em;
}

.origin-column {
  width: 9em;
}

.affected-surfaces-column {
  word-wrap: break-word;
  width: 30em;
}

.entry {
  display: inline-flex;
  cursor: pointer;
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
}
</style>