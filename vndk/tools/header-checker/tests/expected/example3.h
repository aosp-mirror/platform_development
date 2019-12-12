record_types {
  type_info {
    name: "HiddenBase"
    size: 8
    alignment: 4
    referenced_type: "_ZTI10HiddenBase"
    source_file: "/development/vndk/tools/header-checker/tests/input/example3.h"
    linker_set_key: "_ZTI10HiddenBase"
    self_type: "_ZTI10HiddenBase"
  }
  fields {
    referenced_type: "_ZTIi"
    field_offset: 0
    field_name: "hide"
    access: private_access
  }
  fields {
    referenced_type: "_ZTIf"
    field_offset: 32
    field_name: "seek"
    access: private_access
  }
  access: public_access
  record_kind: class_kind
}
builtin_types {
  type_info {
    name: "float"
    size: 4
    alignment: 4
    referenced_type: "_ZTIf"
    source_file: ""
    linker_set_key: "_ZTIf"
    self_type: "_ZTIf"
  }
  is_unsigned: false
  is_integral: false
}
builtin_types {
  type_info {
    name: "int"
    size: 4
    alignment: 4
    referenced_type: "_ZTIi"
    source_file: ""
    linker_set_key: "_ZTIi"
    self_type: "_ZTIi"
  }
  is_unsigned: false
  is_integral: true
}
