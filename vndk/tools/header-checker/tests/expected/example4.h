record_types {
  type_info {
    name: "Test"
    size: 16
    alignment: 8
    referenced_type: "Test"
    source_file: "/development/vndk/tools/header-checker/tests/input/example4.h"
    linker_set_key: "Test"
  }
  fields {
    referenced_type: "int"
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
      mangled_component_name: "Test"
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
}
record_types {
  type_info {
    name: "TestChild"
    size: 16
    alignment: 8
    referenced_type: "TestChild"
    source_file: "/development/vndk/tools/header-checker/tests/input/example4.h"
    linker_set_key: "TestChild"
  }
  fields {
    referenced_type: "int"
    field_offset: 96
    field_name: "d"
    access: private_access
  }
  base_specifiers {
    referenced_type: "Test"
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
      mangled_component_name: "TestChild"
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
}
pointer_types {
  type_info {
    name: "Test *"
    size: 8
    alignment: 8
    referenced_type: "Test"
    source_file: "/development/vndk/tools/header-checker/tests/input/example4.h"
    linker_set_key: "Test *"
  }
}
pointer_types {
  type_info {
    name: "TestChild *"
    size: 8
    alignment: 8
    referenced_type: "TestChild"
    source_file: "/development/vndk/tools/header-checker/tests/input/example4.h"
    linker_set_key: "TestChild *"
  }
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
functions {
  return_type: "int"
  function_name: "Test::foo"
  source_file: "/development/vndk/tools/header-checker/tests/input/example4.h"
  parameters {
    referenced_type: "Test *"
    default_arg: false
  }
  linker_set_key: "_ZN4Test3fooEv"
  access: private_access
}
functions {
  return_type: "int"
  function_name: "TestChild::foo"
  source_file: "/development/vndk/tools/header-checker/tests/input/example4.h"
  parameters {
    referenced_type: "TestChild *"
    default_arg: false
  }
  linker_set_key: "_ZN9TestChild3fooEv"
  access: private_access
}
