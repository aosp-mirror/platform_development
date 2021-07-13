<template>
  <TableLite
    :columns="columns"
    :is-re-search="isReSearch"
    :is-loading="isLoading"
    :rows="rows"
    :sortable="sortable"
    :total="total"
    @do-search="doSearch"
  />
</template>

<script>
import TableLite from 'vue3-table-lite'
import FormDate from '../services/FormDate.js'

export default {
  components: {
    TableLite
  },
  props: {
    jobs: {
      type: Array,
      required: true
    }
  },
  data () {
    return {
      rows: null,
      columns: [
        {
          label: "Source build",
          field: "incremental_name",
          sortable: true
        },
        {
          label: "Target build",
          field: "target_name",
          sortable: true
        },
        {
          label: "Status",
          field: "status",
          sortable: true,
          display: function (row) {
            return (
              "<a href=/check/" + row.id + '>'
              + row.status
              + "</a>"
            );
          }
        },
        {
          label: "Partial",
          field: "isPartial",
          sortable: true
        },
        {
          label: "Start Time",
          field: "start_time",
          sortable: true,
          display: function (row) {
            return FormDate.formDate(row.start_time)
          }
        },
        {
          label: "Finish Time",
          field: "finish_time",
          sortable: true,
          display: function (row) {
            return FormDate.formDate(row.finish_time)
          }
        }
      ],
      sortable: {
        order: "start_time",
        sort: "desc",
      },
      isReSearch: false,
      isLoading: false,
      total: 0
    }
  },
  created() {
    this.rows = this.jobs
    this.total = this.jobs.length
  },
  methods: {
    sort(arr, key, sortOrder, offset, limit) {
      let orderNumber = 1
      if (sortOrder==="asc") {
        orderNumber = -1
      }
      return arr.sort(function(a, b) {
        var keyA = a[key],
          keyB = b[key];
        if (keyA < keyB) return -1*orderNumber;
        if (keyA > keyB) return 1*orderNumber;
        return 0;
      }).slice(offset, limit);
    },
    doSearch(offset, limit, order, sort) {
      this.isLoading = true
      setTimeout(() => {
        this.sortable.order = order
        this.sortable.sort = sort
        this.rows = this.sort(this.jobs, order, sort, offset, limit)
        this.total = this.jobs.length
      }, 600)
      setTimeout(() => {this.isLoading=false}, 1000)
    }
  }
}
</script>