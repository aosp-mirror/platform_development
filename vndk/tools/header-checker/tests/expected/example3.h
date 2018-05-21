record_types {
  type_info {
    name: "HiddenBase"
    size: 8
    alignment: 4
    referenced_type: "type-1"
    source_file: "/development/vndk/tools/header-checker/tests/input/example3.h"
    linker_set_key: "HiddenBase"
    self_type: "type-1"
  }
  fields {
    referenced_type: "type-2"
    field_offset: 0
    field_name: "hide"
    access: private_access
  }
  fields {
    referenced_type: "type-3"
    field_offset: 32
    field_name: "seek"
    access: private_access
  }
  access: public_access
  record_kind: class_kind
  tag_info {
    unique_id: "_ZTS10HiddenBase"
  }
}
builtin_types {
  type_info {
    name: "int"
    size: 4
    alignment: 4
    referenced_type: "type-2"
    source_file: ""
    linker_set_key: "int"
    self_type: "type-2"
  }
  is_unsigned: false
  is_integral: true
}
builtin_types {
  type_info {
    name: "float"
    size: 4
    alignment: 4
    referenced_type: "type-3"
    source_file: ""
    linker_set_key: "float"
    self_type: "type-3"
  }
  is_unsigned: false
  is_integral: false
}
