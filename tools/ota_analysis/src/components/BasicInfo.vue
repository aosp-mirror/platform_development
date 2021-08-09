<template>
  <h3>Basic infos</h3>
  <div
    v-if="zipFile"
    v-bind="$attrs"
  >
    <ul class="align">
      <li><strong> File name </strong> {{ zipFile.name }}</li>
      <li><strong> File size </strong> {{ zipFile.size }} Bytes</li>
      <li>
        <strong> File last modified date </strong>
        {{ zipFile.lastModifiedDate }}
      </li>
    </ul>
  </div>
  <div
    v-if="payload && payload.metadata"
    v-bind="$attrs"
  >
    <ul class="align">
      <li
        v-for="formatter in MetadataFormat"
        :key="formatter.name"
      >
        <strong> {{ formatter.name }} </strong>
        <p class="wrap">
          {{ String(payload[formatter.key]) }}
        </p>
      </li>
    </ul>
  </div>
  <div v-if="payload && payload.manifest">
    <ul class="align">
      <li>
        <strong> Incremental </strong>
        <!-- Check if the first partition is incremental or not -->
        <span v-if="payload.preBuild">
          &#9989;
        </span>
        <span v-else> &#10060; </span>
      </li>
      <li>
        <strong> Partial </strong>
        <span v-if="payload.manifest.partialUpdate"> &#9989; </span>
        <span v-else> &#10060; </span>
      </li>
      <li>
        <strong> A/B update </strong>
        <span v-if="!payload.manifest.nonAB">
          &#9989;
        </span>
        <span v-else> &#10060; </span>
      </li>
      <li>
        <strong> VAB </strong>
        <span v-if="payload.manifest.dynamicPartitionMetadata.snapshotEnabled">
          &#9989;
        </span>
        <span v-else> &#10060; </span>
      </li>
      <li>
        <strong> VABC </strong>
        <span v-if="payload.manifest.dynamicPartitionMetadata.vabcEnabled">
          &#9989;
        </span>
        <span v-else> &#10060; </span>
      </li>
    </ul>
  </div>
</template>

<script>
import { Payload, MetadataFormat } from '@/services/payload.js'

export default {
  props: {
    zipFile: {
      type: File,
      required: true,
    },
    payload: {
      type: Payload,
      required: true,
    },
  },
  data() {
    return {
      MetadataFormat
    }
  }
}
</script>

<style scoped>
.align strong {
  display: inline-block;
  width: 50%;
  position: relative;
  padding-right: 10px; /* Ensures colon does not overlay the text */
  text-align: right;
}

.align strong::after {
  content: ':';
}

li {
  list-style-type: none;
}

.wrap {
  width: 50%;
  display: inline-block;
  white-space: pre-wrap;
  word-wrap: break-word;
  font-family: inherit;
}
</style>