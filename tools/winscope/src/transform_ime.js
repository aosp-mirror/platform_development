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
      [[entry.client], transform_client_dump]
    ],
    timestamp: entry.elapsedRealtimeNanos,
    stableId: 'entry'
  });
}

function transform_client_dump(entry) {
  return transform({
    obj: entry,
    kind: 'Client',
    name: '\n- methodId ' + entry?.inputMethodManager?.curId
        + '\n- view ' + entry?.viewRootImpl?.view
        + '\n- packageName ' + entry?.editorInfo?.packageName,
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
    name: '\n- windowVisible ' + entry?.windowVisible
        + '\n- decorViewVisible ' + entry?.decorViewVisible
        + '\n- packageName ' + entry?.inputEditorInfo?.packageName,
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
    kind: 'entry',
    name: nanos_to_string(entry.elapsedRealtimeNanos) + " - " + entry.where,
    children: [
      [[entry.inputMethodManagerService], transform_managerservice_dump]
    ],
    timestamp: entry.elapsedRealtimeNanos,
    stableId: 'entry'
  });
}

function transform_managerservice_dump(entry) {
  return transform({
    obj: entry,
    kind: 'InputMethodManagerService',
    name: '\n- methodId ' + entry?.curMethodId
        + '\n- curFocusedWindow ' + entry?.curFocusedWindowName
        + '\n- lastImeTargetWindow ' + entry?.lastImeTargetWindowName
        + '\n- inputShown ' + entry?.inputShown,
    children: [],
    stableId: 'managerservice'
  });
}

export {transform_ime_trace_clients, transform_ime_trace_service, transform_ime_trace_managerservice};
