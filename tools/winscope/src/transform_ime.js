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
    name: nanos_to_string(entry.elapsedRealtimeNanos),
    children: [
      [[entry.inputMethodManagerService], transform_imms_dump],
      [[entry.inputMethodService], transform_ims_dump],
      [[entry.clients], transform_client_dump]
    ],
    timestamp: entry.elapsedRealtimeNanos,
    stableId: 'entry'
  });
}

function transform_imms_dump(entry) {
  return transform({
    obj: entry,
    kind: 'InputMethodManagerService',
    name: '',
    children: []
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

function transform_ims_dump(entry) {
  return transform({
    obj: entry,
    kind: 'InputMethodService',
    name: '',
    children: []
  });
}

export {transform_ime_trace};