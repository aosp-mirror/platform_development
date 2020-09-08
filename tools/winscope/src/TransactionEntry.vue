<template>
  <div class="entry"
    :class="{
      inactive: source.timestamp > currentTimestamp,
      selected: isSelected
    }"
    @click="onClick(source)"
  >
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
    <div class="affected-surfaces-column">
      <span
        v-for="(surface, index) in sufacesAffectedBy(source)"
        v-bind:key="surface.id"
      >
        <span v-if="surface.name" class="surface-name">{{ surface.name }}</span>
        <span class="surface-id">
          <!-- eslint-disable-next-line max-len -->
          <span v-if="surface.name">(</span>{{surface.id}}<span v-if="surface.name">)</span>
        </span>
        <span v-if="index + 1 < sufacesAffectedBy(source).length">,&nbsp;</span>
      </span>
    </div>
    <div class="extra-info-column">
      <span v-if="source.identifier">
        <!-- eslint-disable-next-line max-len -->
        Tx Id: <span class="light">{{ prettifyTransactionId(source.identifier) }}</span><br/>
      </span>
      <span v-if="source.origin">
        PID: <span class="light">{{ source.origin.pid }}</span><br/>
        TID: <span class="light">{{ source.origin.uid }}</span><br/>
      </span>
    </div>
  </div>
</template>

<script>
export default {
  name: 'transaction-entry',
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
    onClick: {
      type: Function,
    },
    selectedTransaction: {
      type: Object,
    },
    transactionsTrace: {
      type: Object,
    },
    prettifyTransactionId: {
      type: Function,
    },
  },
  computed: {
    currentTimestamp() {
      return this.$store.state.currentTimestamp;
    },
    isSelected() {
      return this.source === this.selectedTransaction;
    },
    hasOverrideChangeDueToMerge() {
      const transaction = this.source;

      if (!transaction.identifier) {
        return;
      }

      // console.log('transaction', transaction.identifier);

      // const history = this.transactionsTrace.transactionHistory;

      // const allTransactionsMergedInto = history
      //     .allTransactionsMergedInto(transaction.identifier);
      // console.log('All merges', allTransactionsMergedInto);

      // console.log('Direct merges',
      //     history.allDirectMergesInto(transaction.identifier));


      return true;
    },
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
        return 'Empty Transaction';
      }

      const types = new Set();
      transaction.transactions.forEach((t) => types.add(t.type));

      return Array.from(types).join(', ');
    },
    sufacesAffectedBy(transaction) {
      if (transaction.type !== 'transaction') {
        // TODO (b/162402459): Shorten layer name
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

      return affectedSurfaces;
    },
  },
};
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

.extra-info-column {
  width: 20em;
}

.entry {
  display: inline-flex;
  cursor: pointer;
}

.entry.selected {
  background-color: #365179;
  color: white;
}

.entry.selected a {
  color: white;
}

.entry:not(.selected):hover {
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

.affected-surfaces-column .surface-id {
  color: #999999
}

.inactive .affected-surfaces-column .surface-id {
  color: #b4b4b4
}

.light {
  color: #999999
}

.inactive .light {
  color: #b4b4b4
}
</style>
