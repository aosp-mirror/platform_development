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
    kind: 'InputMethodClient entry',
    name: nanos_to_string(entry.elapsedRealtimeNanos) + " - " + entry.where,
    children: [
      [[entry.client], transform_client_dump]
    ],
    timestamp: entry.elapsedRealtimeNanos,
    stableId: 'entry',
    freeze: false,
  });
}

function transform_client_dump(entry) {
  return transform({
    obj: transform_input_connection_call(entry),
    kind: 'Client',
    name: entry?.viewRootImpl?.view,
    children: [],
    stableId: 'client'
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
    kind: 'InputMethodService entry',
    name: nanos_to_string(entry.elapsedRealtimeNanos) + " - " + entry.where,
    children: [
      [[entry.inputMethodService], transform_service_dump]
    ],
    timestamp: entry.elapsedRealtimeNanos,
    stableId: 'entry',
    freeze: false,
  });
}

function transform_service_dump(entry) {
  return transform({
    obj: transform_input_connection_call(entry),
    kind: 'InputMethodService',
    name: '',
    children: [],
    stableId: 'service'
  });
}

function transform_ime_trace_managerservice(entries) {
  return transform({
    obj: entries,
    kind: 'entries',
    name: 'entries',
    children: [
      [entries.entry, transform_entry_managerservice]
    ]
  });
}

function transform_entry_managerservice(entry) {
  return transform({
    obj: entry,
    kind: 'InputMethodManagerService entry',
    name: nanos_to_string(entry.elapsedRealtimeNanos) + " - " + entry.where,
    children: [
      [[entry.inputMethodManagerService], transform_managerservice_dump]
    ],
    timestamp: entry.elapsedRealtimeNanos,
    stableId: 'entry',
    freeze: false,
  });
}

function transform_managerservice_dump(entry) {
  return transform({
    obj: entry,
    kind: 'InputMethodManagerService',
    name: '',
    children: [],
    stableId: 'managerservice'
  });
}

function transform_input_connection_call(entry) {
  const obj = Object.assign({}, entry)
  if (obj.inputConnectionCall) {
    Object.getOwnPropertyNames(obj.inputConnectionCall).forEach(name => {
        const value = Object.getOwnPropertyDescriptor(obj.inputConnectionCall, name)
        if (!value.value) delete obj.inputConnectionCall[name]
    })
  }
  return obj
}

export {transform_ime_trace_clients, transform_ime_trace_service, transform_ime_trace_managerservice};
