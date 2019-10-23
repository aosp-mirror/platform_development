record_types {
  type_info {
    name: "HiddenBase"
    size: 8
    alignment: 4
    referenced_type: "type-1"
    source_file: "/development/vndk/tools/header-checker/tests/input/example3.h"
    linker_set_key: "_ZTI10HiddenBase"
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
}
record_types {
  type_info {
    name: "test2::HelloAgain"
    size: 40
    alignment: 8
    referenced_type: "type-4"
    source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
    linker_set_key: "_ZTIN5test210HelloAgainE"
    self_type: "type-4"
  }
  fields {
    referenced_type: "type-5"
    field_offset: 64
    field_name: "foo_again"
    access: public_access
  }
  fields {
    referenced_type: "type-2"
    field_offset: 256
    field_name: "bar_again"
    access: public_access
  }
  vtable_layout {
    vtable_components {
      kind: OffsetToTop
      mangled_component_name: ""
      component_value: 0
      is_pure: false
    }
    vtable_components {
      kind: RTTI
      mangled_component_name: "_ZTIN5test210HelloAgainE"
      component_value: 0
      is_pure: false
    }
    vtable_components {
      kind: FunctionPointer
      mangled_component_name: "_ZN5test210HelloAgain5againEv"
      component_value: 0
      is_pure: false
    }
    vtable_components {
      kind: CompleteDtorPointer
      mangled_component_name: "_ZN5test210HelloAgainD1Ev"
      component_value: 0
      is_pure: false
    }
    vtable_components {
      kind: DeletingDtorPointer
      mangled_component_name: "_ZN5test210HelloAgainD0Ev"
      component_value: 0
      is_pure: false
    }
  }
  access: public_access
  record_kind: struct_kind
}
record_types {
  type_info {
    name: "test3::Outer::Inner"
    size: 4
    alignment: 4
    referenced_type: "type-20"
    source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
    linker_set_key: "_ZTIN5test35Outer5InnerE"
    self_type: "type-20"
  }
  fields {
    referenced_type: "type-2"
    field_offset: 0
    field_name: "b"
    access: private_access
  }
  access: private_access
  record_kind: class_kind
}
record_types {
  type_info {
    name: "test3::Outer"
    size: 4
    alignment: 4
    referenced_type: "type-19"
    source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
    linker_set_key: "_ZTIN5test35OuterE"
    self_type: "type-19"
  }
  fields {
    referenced_type: "type-2"
    field_offset: 0
    field_name: "a"
    access: public_access
  }
  access: public_access
  record_kind: class_kind
}
record_types {
  type_info {
    name: "test3::ByeAgain<double>"
    size: 16
    alignment: 8
    referenced_type: "type-13"
    source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
    linker_set_key: "_ZTIN5test38ByeAgainIdEE"
    self_type: "type-13"
  }
  fields {
    referenced_type: "type-14"
    field_offset: 0
    field_name: "foo_again"
    access: public_access
  }
  fields {
    referenced_type: "type-2"
    field_offset: 64
    field_name: "bar_again"
    access: public_access
  }
  template_info {
    elements {
      referenced_type: "type-14"
    }
  }
  access: public_access
  record_kind: struct_kind
}
record_types {
  type_info {
    name: "test3::ByeAgain<float>"
    size: 8
    alignment: 4
    referenced_type: "type-16"
    source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
    linker_set_key: "_ZTIN5test38ByeAgainIfEE"
    self_type: "type-16"
  }
  fields {
    referenced_type: "type-3"
    field_offset: 0
    field_name: "foo_again"
    access: public_access
  }
  fields {
    referenced_type: "type-3"
    field_offset: 32
    field_name: "bar_Again"
    access: public_access
  }
  template_info {
    elements {
      referenced_type: "type-3"
    }
  }
  access: public_access
  record_kind: struct_kind
}
enum_types {
  type_info {
    name: "Foo_s"
    size: 4
    alignment: 4
    referenced_type: "type-8"
    source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
    linker_set_key: "_ZTI5Foo_s"
    self_type: "type-8"
  }
  underlying_type: "type-9"
  enum_fields {
    enum_field_value: 10
    name: "foosball"
  }
  enum_fields {
    enum_field_value: 11
    name: "foosbat"
  }
  access: public_access
}
enum_types {
  type_info {
    name: "test3::Kind"
    size: 4
    alignment: 4
    referenced_type: "type-18"
    source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
    linker_set_key: "_ZTIN5test34KindE"
    self_type: "type-18"
  }
  underlying_type: "type-9"
  enum_fields {
    enum_field_value: 24
    name: "test3::kind1"
  }
  enum_fields {
    enum_field_value: 2312
    name: "test3::kind2"
  }
  access: public_access
}
pointer_types {
  type_info {
    name: "test2::HelloAgain *"
    size: 8
    alignment: 8
    referenced_type: "type-4"
    source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
    linker_set_key: "_ZTIPN5test210HelloAgainE"
    self_type: "type-6"
  }
}
pointer_types {
  type_info {
    name: "test3::ByeAgain<double> *"
    size: 8
    alignment: 8
    referenced_type: "type-13"
    source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
    linker_set_key: "_ZTIPN5test38ByeAgainIdEE"
    self_type: "type-15"
  }
}
pointer_types {
  type_info {
    name: "test3::ByeAgain<float> *"
    size: 8
    alignment: 8
    referenced_type: "type-16"
    source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
    linker_set_key: "_ZTIPN5test38ByeAgainIfEE"
    self_type: "type-17"
  }
}
builtin_types {
  type_info {
    name: "bool"
    size: 1
    alignment: 1
    referenced_type: "type-12"
    source_file: ""
    linker_set_key: "_ZTIb"
    self_type: "type-12"
  }
  is_unsigned: true
  is_integral: true
}
builtin_types {
  type_info {
    name: "double"
    size: 8
    alignment: 8
    referenced_type: "type-14"
    source_file: ""
    linker_set_key: "_ZTId"
    self_type: "type-14"
  }
  is_unsigned: false
  is_integral: false
}
builtin_types {
  type_info {
    name: "float"
    size: 4
    alignment: 4
    referenced_type: "type-3"
    source_file: ""
    linker_set_key: "_ZTIf"
    self_type: "type-3"
  }
  is_unsigned: false
  is_integral: false
}
builtin_types {
  type_info {
    name: "int"
    size: 4
    alignment: 4
    referenced_type: "type-2"
    source_file: ""
    linker_set_key: "_ZTIi"
    self_type: "type-2"
  }
  is_unsigned: false
  is_integral: true
}
builtin_types {
  type_info {
    name: "unsigned int"
    size: 4
    alignment: 4
    referenced_type: "type-9"
    source_file: ""
    linker_set_key: "_ZTIj"
    self_type: "type-9"
  }
  is_unsigned: true
  is_integral: true
}
builtin_types {
  type_info {
    name: "void"
    size: 0
    alignment: 0
    referenced_type: "type-7"
    source_file: ""
    linker_set_key: "_ZTIv"
    self_type: "type-7"
  }
  is_unsigned: false
  is_integral: false
}
qualified_types {
  type_info {
    name: "bool const[2]"
    size: 2
    alignment: 1
    referenced_type: "type-10"
    source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
    linker_set_key: "_ZTIA2_Kb"
    self_type: "type-11"
  }
  is_const: true
  is_volatile: false
  is_restricted: false
}
array_types {
  type_info {
    name: "bool [2]"
    size: 2
    alignment: 1
    referenced_type: "type-12"
    source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
    linker_set_key: "_ZTIA2_b"
    self_type: "type-10"
  }
}
functions {
  return_type: "type-2"
  function_name: "test2::HelloAgain::again"
  source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
  parameters {
    referenced_type: "type-6"
    default_arg: false
    is_this_ptr: true
  }
  linker_set_key: "_ZN5test210HelloAgain5againEv"
  access: public_access
}
functions {
  return_type: "type-7"
  function_name: "test2::HelloAgain::~HelloAgain"
  source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
  parameters {
    referenced_type: "type-6"
    default_arg: false
    is_this_ptr: true
  }
  linker_set_key: "_ZN5test210HelloAgainD0Ev"
  access: public_access
}
functions {
  return_type: "type-7"
  function_name: "test2::HelloAgain::~HelloAgain"
  source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
  parameters {
    referenced_type: "type-6"
    default_arg: false
    is_this_ptr: true
  }
  linker_set_key: "_ZN5test210HelloAgainD1Ev"
  access: public_access
}
functions {
  return_type: "type-7"
  function_name: "test2::HelloAgain::~HelloAgain"
  source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
  parameters {
    referenced_type: "type-6"
    default_arg: false
    is_this_ptr: true
  }
  linker_set_key: "_ZN5test210HelloAgainD2Ev"
  access: public_access
}
functions {
  return_type: "type-12"
  function_name: "test3::End"
  source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
  parameters {
    referenced_type: "type-3"
    default_arg: true
    is_this_ptr: false
  }
  linker_set_key: "_ZN5test33EndEf"
  access: public_access
}
functions {
  return_type: "type-12"
  function_name: "test3::Begin"
  source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
  parameters {
    referenced_type: "type-3"
    default_arg: false
    is_this_ptr: false
  }
  parameters {
    referenced_type: "type-2"
    default_arg: false
    is_this_ptr: false
  }
  parameters {
    referenced_type: "type-2"
    default_arg: false
    is_this_ptr: false
  }
  template_info {
    elements {
      referenced_type: "type-3"
    }
    elements {
      referenced_type: "type-2"
    }
  }
  linker_set_key: "_ZN5test35BeginIfiEEbT_T0_i"
  access: public_access
}
functions {
  return_type: "type-21"
  function_name: "test3::Dummy"
  source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
  parameters {
    referenced_type: "type-2"
    default_arg: false
    is_this_ptr: false
  }
  linker_set_key: "_ZN5test35DummyEi"
  access: public_access
}
functions {
  return_type: "type-14"
  function_name: "test3::ByeAgain<double>::method_foo"
  source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
  parameters {
    referenced_type: "type-15"
    default_arg: false
    is_this_ptr: true
  }
  parameters {
    referenced_type: "type-14"
    default_arg: false
    is_this_ptr: false
  }
  linker_set_key: "_ZN5test38ByeAgainIdE10method_fooEd"
  access: public_access
}
functions {
  return_type: "type-3"
  function_name: "test3::ByeAgain<float>::method_foo"
  source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
  parameters {
    referenced_type: "type-17"
    default_arg: false
    is_this_ptr: true
  }
  parameters {
    referenced_type: "type-2"
    default_arg: false
    is_this_ptr: false
  }
  linker_set_key: "_ZN5test38ByeAgainIfE10method_fooEi"
  access: public_access
}
global_vars {
  name: "__test_var"
  source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
  linker_set_key: "_ZL10__test_var"
  referenced_type: "type-11"
  access: public_access
}
global_vars {
  name: "test2::HelloAgain::hello_forever"
  source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
  linker_set_key: "_ZN5test210HelloAgain13hello_foreverE"
  referenced_type: "type-2"
  access: public_access
}
global_vars {
  name: "test3::double_bye"
  source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
  linker_set_key: "_ZN5test310double_byeE"
  referenced_type: "type-13"
  access: public_access
}
global_vars {
  name: "test3::ByeAgain<float>::foo_forever"
  source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
  linker_set_key: "_ZN5test38ByeAgainIfE11foo_foreverE"
  referenced_type: "type-2"
  access: public_access
}
