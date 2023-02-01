<template>
  <div>

    <div v-if="source.type === 'vsyncEvent'" class="vsync">
      <div class="vsync-dot" />
      <md-tooltip md-direction="left">
        VSync
      </md-tooltip>
    </div>

    <div v-else
      class="entry"
      :class="{
        inactive: source.timestamp > currentTimestamp,
        selected: isSelected
      }"
      @click="onClick(source)"
    >
      <div class="time-column">
        <a @click="e => setTimelineTime(e, source.timestamp)" class="time-link">
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
          <span
            v-if="simplifyNames && surface.shortName &&
                surface.shortName !== surface.name"
            >{{surface.shortName}}>
          </span>
          <span v-else>
            <!-- eslint-disable-next-line max-len -->
            <span v-if="surface.name" class="surface-name">{{ surface.name }}</span>
            <span class="surface-id">
              <!-- eslint-disable-next-line max-len -->
              <span v-if="surface.name">(</span>{{surface.id}}<span v-if="surface.name">)</span>
            </span>
            <!-- eslint-disable-next-line max-len -->
            <span v-if="index + 1 < sufacesAffectedBy(source).length">,&nbsp;</span>
          </span>
        </span>
      </div>
      <div class="extra-info-column">
        <span v-if="source.identifier">
          <!-- eslint-disable-next-line max-len -->
          Tx Id: <span class="light">{{ prettifyTransactionId(source.identifier) }}</span><br/>
        </span>
        <span v-if="source.origin">
          PID: <span class="light">{{ source.origin.pid }}</span><br/>
          UID: <span class="light">{{ source.origin.uid }}</span><br/>
        </span>
      </div>
    </div>

  </div>
</template>

<script>
import { shortenName } from './flickerlib/mixin'

export default {
  name: 'transaction-entry-legacy',
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
    simplifyNames: {
      type: Boolean,
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
    setTimelineTime(e, timestamp) {
      e.preventDefault();
      e.stopPropagation();
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
        return [
          {
            name: transaction.layerName,
            shortName: shortenName(transaction.layerName),
            id: transaction.obj.id
          }];
      }

      const surfaceIds = new Set();
      const affectedSurfaces = [];
      for (const transaction of transaction.transactions) {
        const id = transaction.obj.id;
        if (!surfaceIds.has(id)) {
          surfaceIds.add(id);
          affectedSurfaces.push(
            {
              name: transaction.layerName,
              shortName: shortenName(transaction.layerName),
              id
            });
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

.entry > div {
  padding: 6px 10px;
  border-bottom: 1px solid #f1f1f1;
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

.vsync {
  position: relative;
}

.vsync-dot:before {
  content: "";
  position: absolute;
  left: 0;
  top: -5px;
  height: 10px;
  width: 10px;
  background-color: rgb(170, 65, 255);
  border-radius: 50%;
  display: inline-block;
}

.vsync-dot:after {
  content: "";
  position: absolute;
  left: 0;
  top: 0;
  height: 1px;
  width: 100%;
  background-color: rgb(170, 65, 255);
  display: inline-block;
}
</style>
