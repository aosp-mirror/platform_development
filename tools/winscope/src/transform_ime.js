import {nanos_to_string, transform} from './transform.js'

function transform_ime_trace_clients(entries) {
  return transform({
    obj: entries,
    kind: 'entries',
    name: 'entries',
    children: [
      [entries.entry, transform_entry_clients]
    ]
  });
}

function transform_entry_clients(entry) {
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

function transform_ime_trace_service(entries) {
  return transform({
    obj: entries,
    kind: 'entries',
    name: 'entries',
    children: [
      [entries.entry, transform_entry_service]
    ]
  });
}

function transform_entry_service(entry) {
  return transform({
    obj: entry,
    kind: 'entry',
    name: nanos_to_string(entry.elapsedRealtimeNanos) + " - " + entry.where,
    children: [
      [[entry.inputMethodService], transform_service_dump]
    ],
    timestamp: entry.elapsedRealtimeNanos,
    stableId: 'entry'
  });
}

function transform_service_dump(entry) {
  return transform({
    obj: entry,
    kind: 'InputMethodService',
    name: '',
    children: []
  });
}

export {transform_ime_trace_clients, transform_ime_trace_service};
