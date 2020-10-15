import {nanos_to_string, transform} from './transform.js'

function transform_ime_trace(entries) {
  return transform({
    obj: entries,
    kind: 'entries',
    name: 'entries',
    children: [
      [entries.entry, transform_entry]
    ]
  });
}

function transform_entry(entry) {
  return transform({
    obj: entry,
    kind: 'entry',
    name: nanos_to_string(entry.elapsedRealtimeNanos) + " - " + entry.where,
    children: [
      [[entry.clients], transform_client_dump]
    ],
    timestamp: entry.elapsedRealtimeNanos,
    stableId: 'entry'
  });
}

function transform_client_dump(entry) {
  return transform({
    obj: entry,
    kind: 'Clients',
    name: '',
    children: []
  });
}

export {transform_ime_trace};
