record_types {
  type_info {
    name: "HiddenBase"
    size: 8
    alignment: 4
    referenced_type: "HiddenBase"
    source_file: "/development/vndk/tools/header-checker/tests/input/example3.h"
    linker_set_key: "HiddenBase"
  }
  fields {
    referenced_type: "int"
    field_offset: 0
    field_name: "hide"
    access: private_access
  }
  fields {
    referenced_type: "float"
    field_offset: 32
    field_name: "seek"
    access: private_access
  }
  access: public_access
  record_kind: class_kind
}
builtin_types {
  type_info {
    name: "int"
    size: 4
    alignment: 4
    referenced_type: "int"
    source_file: ""
    linker_set_key: "int"
  }
  is_unsigned: false
  is_integral: true
}
builtin_types {
  type_info {
    name: "float"
    size: 4
    alignment: 4
    referenced_type: "float"
    source_file: ""
    linker_set_key: "float"
  }
  is_unsigned: false
  is_integral: false
}
