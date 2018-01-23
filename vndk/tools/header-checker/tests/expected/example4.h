record_types {
  type_info {
    name: "Test"
    size: 16
    alignment: 8
    referenced_type: "type-1"
    source_file: "/development/vndk/tools/header-checker/tests/input/example4.h"
    linker_set_key: "Test"
    self_type: "type-1"
  }
  fields {
    referenced_type: "type-2"
    field_offset: 64
    field_name: "c"
    access: private_access
  }
  vtable_layout {
    vtable_components {
      kind: OffsetToTop
      mangled_component_name: ""
      component_value: 0
    }
    vtable_components {
      kind: RTTI
      mangled_component_name: "_ZTI4Test"
      component_value: 0
    }
    vtable_components {
      kind: FunctionPointer
      mangled_component_name: "_ZN4Test3fooEv"
      component_value: 0
    }
  }
  access: public_access
  record_kind: class_kind
  tag_info {
    unique_id: "_ZTS4Test"
  }
}
record_types {
  type_info {
    name: "TestChild"
    size: 16
    alignment: 8
    referenced_type: "type-3"
    source_file: "/development/vndk/tools/header-checker/tests/input/example4.h"
    linker_set_key: "TestChild"
    self_type: "type-3"
  }
  fields {
    referenced_type: "type-2"
    field_offset: 96
    field_name: "d"
    access: private_access
  }
  base_specifiers {
    referenced_type: "type-1"
    is_virtual: false
    access: public_access
  }
  vtable_layout {
    vtable_components {
      kind: OffsetToTop
      mangled_component_name: ""
      component_value: 0
    }
    vtable_components {
      kind: RTTI
      mangled_component_name: "_ZTI9TestChild"
      component_value: 0
    }
    vtable_components {
      kind: FunctionPointer
      mangled_component_name: "_ZN9TestChild3fooEv"
      component_value: 0
    }
  }
  access: public_access
  record_kind: class_kind
  tag_info {
    unique_id: "_ZTS9TestChild"
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
