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
record_types {
  type_info {
    name: "test2::HelloAgain"
    size: 40
    alignment: 8
    referenced_type: "type-4"
    source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
    linker_set_key: "test2::HelloAgain"
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
    }
    vtable_components {
      kind: RTTI
      mangled_component_name: "_ZTIN5test210HelloAgainE"
      component_value: 0
    }
    vtable_components {
      kind: FunctionPointer
      mangled_component_name: "_ZN5test210HelloAgain5againEv"
      component_value: 0
    }
    vtable_components {
      kind: CompleteDtorPointer
      mangled_component_name: "_ZN5test210HelloAgainD1Ev"
      component_value: 0
    }
    vtable_components {
      kind: DeletingDtorPointer
      mangled_component_name: "_ZN5test210HelloAgainD0Ev"
      component_value: 0
    }
  }
  access: public_access
  record_kind: struct_kind
  tag_info {
    unique_id: "_ZTSN5test210HelloAgainE"
  }
}
record_types {
  type_info {
    name: "test3::ByeAgain<double>"
    size: 16
    alignment: 8
    referenced_type: "type-13"
    source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
    linker_set_key: "test3::ByeAgain<double>"
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
  tag_info {
    unique_id: "_ZTSN5test38ByeAgainIdEE"
  }
}
record_types {
  type_info {
    name: "test3::ByeAgain<float>"
    size: 8
    alignment: 4
    referenced_type: "type-15"
    source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
    linker_set_key: "test3::ByeAgain<float>"
    self_type: "type-15"
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
  tag_info {
    unique_id: "_ZTSN5test38ByeAgainIfEE"
  }
}
record_types {
  type_info {
    name: "test3::Outer"
    size: 4
    alignment: 4
    referenced_type: "type-17"
    source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
    linker_set_key: "test3::Outer"
    self_type: "type-17"
  }
  fields {
    referenced_type: "type-2"
    field_offset: 0
    field_name: "a"
    access: public_access
  }
  access: public_access
  record_kind: class_kind
  tag_info {
    unique_id: "_ZTSN5test35OuterE"
  }
}
record_types {
  type_info {
    name: "test3::Outer::Inner"
    size: 4
    alignment: 4
    referenced_type: "type-18"
    source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
    linker_set_key: "test3::Outer::Inner"
    self_type: "type-18"
  }
  fields {
    referenced_type: "type-2"
    field_offset: 0
    field_name: "b"
    access: private_access
  }
  access: private_access
  record_kind: class_kind
  tag_info {
    unique_id: "_ZTSN5test35Outer5InnerE"
  }
}
record_types {
  type_info {
    name: "Hello::(anonymous)::(anonymous) at /development/vndk/tools/header-checker/tests/input/example1.h:22:5"
    size: 4
    alignment: 4
    referenced_type: "type-22"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "Hello::(anonymous)::(anonymous) at /development/vndk/tools/header-checker/tests/input/example1.h:22:5"
    self_type: "type-22"
  }
  fields {
    referenced_type: "type-2"
    field_offset: 0
    field_name: "c"
    access: public_access
  }
  access: public_access
  is_anonymous: true
  record_kind: struct_kind
  tag_info {
    unique_id: "Hello::(anonymous)::(anonymous)"
  }
}
record_types {
  type_info {
    name: "Hello::(anonymous) at /development/vndk/tools/header-checker/tests/input/example1.h:19:3"
    size: 12
    alignment: 4
    referenced_type: "type-21"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "Hello::(anonymous) at /development/vndk/tools/header-checker/tests/input/example1.h:19:3"
    self_type: "type-21"
  }
  fields {
    referenced_type: "type-2"
    field_offset: 0
    field_name: "a"
    access: public_access
  }
  fields {
    referenced_type: "type-2"
    field_offset: 32
    field_name: "b"
    access: public_access
  }
  fields {
    referenced_type: "type-22"
    field_offset: 64
    field_name: ""
    access: public_access
  }
  access: public_access
  is_anonymous: true
  record_kind: struct_kind
  tag_info {
    unique_id: "Hello::(anonymous)"
  }
}
record_types {
  type_info {
    name: "Hello"
    size: 32
    alignment: 4
    referenced_type: "type-19"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "Hello"
    self_type: "type-19"
  }
  fields {
    referenced_type: "type-2"
    field_offset: 0
    field_name: "foo"
    access: public_access
  }
  fields {
    referenced_type: "type-2"
    field_offset: 32
    field_name: "bar"
    access: public_access
  }
  fields {
    referenced_type: "type-20"
    field_offset: 64
    field_name: "d"
    access: public_access
  }
  fields {
    referenced_type: "type-9"
    field_offset: 96
    field_name: "enum_field"
    access: public_access
  }
  fields {
    referenced_type: "type-9"
    field_offset: 128
    field_name: "enum_field2"
    access: public_access
  }
  fields {
    referenced_type: "type-21"
    field_offset: 160
    field_name: ""
    access: public_access
  }
  access: public_access
  record_kind: struct_kind
  tag_info {
    unique_id: "Hello"
  }
}
record_types {
  type_info {
    name: "CPPHello"
    size: 56
    alignment: 8
    referenced_type: "type-23"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "CPPHello"
    self_type: "type-23"
  }
  fields {
    referenced_type: "type-24"
    field_offset: 352
    field_name: "cpp_foo"
    access: public_access
  }
  fields {
    referenced_type: "type-25"
    field_offset: 384
    field_name: "cpp_bar"
    access: public_access
  }
  base_specifiers {
    referenced_type: "type-4"
    is_virtual: false
    access: private_access
  }
  base_specifiers {
    referenced_type: "type-15"
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
      mangled_component_name: "_ZTI8CPPHello"
      component_value: 0
    }
    vtable_components {
      kind: FunctionPointer
      mangled_component_name: "_ZN8CPPHello5againEv"
      component_value: 0
    }
    vtable_components {
      kind: CompleteDtorPointer
      mangled_component_name: "_ZN8CPPHelloD1Ev"
      component_value: 0
    }
    vtable_components {
      kind: DeletingDtorPointer
      mangled_component_name: "_ZN8CPPHelloD0Ev"
      component_value: 0
    }
  }
  access: public_access
  record_kind: struct_kind
  tag_info {
    unique_id: "_ZTS8CPPHello"
  }
}
record_types {
  type_info {
    name: "List<float>"
    size: 8
    alignment: 8
    referenced_type: "type-31"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "List<float>"
    self_type: "type-31"
  }
  fields {
    referenced_type: "type-33"
    field_offset: 0
    field_name: "middle"
    access: public_access
  }
  template_info {
    elements {
      referenced_type: "type-3"
    }
  }
  access: public_access
  record_kind: class_kind
  tag_info {
    unique_id: "_ZTS4ListIfE"
  }
}
record_types {
  type_info {
    name: "List<float>::_Node"
    size: 24
    alignment: 8
    referenced_type: "type-32"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "List<float>::_Node"
    self_type: "type-32"
  }
  fields {
    referenced_type: "type-3"
    field_offset: 0
    field_name: "mVal"
    access: private_access
  }
  fields {
    referenced_type: "type-33"
    field_offset: 64
    field_name: "mpPrev"
    access: private_access
  }
  fields {
    referenced_type: "type-33"
    field_offset: 128
    field_name: "mpNext"
    access: private_access
  }
  access: public_access
  record_kind: class_kind
  tag_info {
    unique_id: "_ZTSN4ListIfE5_NodeE"
  }
}
record_types {
  type_info {
    name: "List<int>"
    size: 8
    alignment: 8
    referenced_type: "type-35"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "List<int>"
    self_type: "type-35"
  }
  fields {
    referenced_type: "type-37"
    field_offset: 0
    field_name: "middle"
    access: public_access
  }
  template_info {
    elements {
      referenced_type: "type-2"
    }
  }
  access: public_access
  record_kind: class_kind
  tag_info {
    unique_id: "_ZTS4ListIiE"
  }
}
enum_types {
  type_info {
    name: "Foo_s"
    size: 4
    alignment: 4
    referenced_type: "type-8"
    source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
    linker_set_key: "Foo_s"
    self_type: "type-8"
  }
  underlying_type: "type-9"
  enum_fields {
    enum_field_value: 10
    name: "Foo_s::foosball"
  }
  enum_fields {
    enum_field_value: 11
    name: "Foo_s::foosbat"
  }
  access: public_access
  tag_info {
    unique_id: "_ZTS5Foo_s"
  }
}
enum_types {
  type_info {
    name: "test3::Kind"
    size: 4
    alignment: 4
    referenced_type: "type-16"
    source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
    linker_set_key: "test3::Kind"
    self_type: "type-16"
  }
  underlying_type: "type-9"
  enum_fields {
    enum_field_value: 24
    name: "test3::Kind::kind1"
  }
  enum_fields {
    enum_field_value: 2312
    name: "test3::Kind::kind2"
  }
  access: public_access
  tag_info {
    unique_id: "_ZTSN5test34KindE"
  }
}
enum_types {
  type_info {
    name: "CPPHello::Bla"
    size: 4
    alignment: 4
    referenced_type: "type-27"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "CPPHello::Bla"
    self_type: "type-27"
  }
  underlying_type: "type-9"
  enum_fields {
    enum_field_value: 1
    name: "CPPHello::Bla::BLA"
  }
  access: public_access
  tag_info {
    unique_id: "_ZTSN8CPPHello3BlaE"
  }
}
pointer_types {
  type_info {
    name: "test2::HelloAgain *"
    size: 8
    alignment: 8
    referenced_type: "type-4"
    source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
    linker_set_key: "test2::HelloAgain *"
    self_type: "type-7"
  }
}
pointer_types {
  type_info {
    name: "CPPHello *"
    size: 8
    alignment: 8
    referenced_type: "type-23"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "CPPHello *"
    self_type: "type-26"
  }
}
pointer_types {
  type_info {
    name: "int *"
    size: 8
    alignment: 8
    referenced_type: "type-2"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "int *"
    self_type: "type-29"
  }
}
pointer_types {
  type_info {
    name: "float *"
    size: 8
    alignment: 8
    referenced_type: "type-3"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "float *"
    self_type: "type-30"
  }
}
pointer_types {
  type_info {
    name: "List<float>::_Node *"
    size: 8
    alignment: 8
    referenced_type: "type-32"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "List<float>::_Node *"
    self_type: "type-33"
  }
}
pointer_types {
  type_info {
    name: "List<int>::_Node *"
    size: 8
    alignment: 8
    referenced_type: "type-36"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "List<int>::_Node *"
    self_type: "type-37"
  }
}
pointer_types {
  type_info {
    name: "const char *"
    size: 8
    alignment: 8
    referenced_type: "type-38"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "const char *"
    self_type: "type-39"
  }
}
lvalue_reference_types {
  type_info {
    name: "const float &"
    size: 8
    alignment: 8
    referenced_type: "type-25"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "const float &"
    self_type: "type-34"
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
builtin_types {
  type_info {
    name: "void"
    size: 0
    alignment: 0
    referenced_type: "type-6"
    source_file: ""
    linker_set_key: "void"
    self_type: "type-6"
  }
  is_unsigned: false
  is_integral: false
}
builtin_types {
  type_info {
    name: "unsigned int"
    size: 4
    alignment: 4
    referenced_type: "type-9"
    source_file: ""
    linker_set_key: "unsigned int"
    self_type: "type-9"
  }
  is_unsigned: true
  is_integral: true
}
builtin_types {
  type_info {
    name: "bool"
    size: 1
    alignment: 1
    referenced_type: "type-12"
    source_file: ""
    linker_set_key: "bool"
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
    linker_set_key: "double"
    self_type: "type-14"
  }
  is_unsigned: false
  is_integral: false
}
builtin_types {
  type_info {
    name: "wchar_t"
    size: 4
    alignment: 4
    referenced_type: "type-20"
    source_file: ""
    linker_set_key: "wchar_t"
    self_type: "type-20"
  }
  is_unsigned: false
  is_integral: true
}
builtin_types {
  type_info {
    name: "char"
    size: 1
    alignment: 1
    referenced_type: "type-40"
    source_file: ""
    linker_set_key: "char"
    self_type: "type-40"
  }
  is_unsigned: false
  is_integral: true
}
qualified_types {
  type_info {
    name: "bool const[2]"
    size: 2
    alignment: 1
    referenced_type: "type-10"
    source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
    linker_set_key: "bool const[2]"
    self_type: "type-11"
  }
  is_const: true
  is_volatile: false
  is_restricted: false
}
qualified_types {
  type_info {
    name: "const int"
    size: 4
    alignment: 4
    referenced_type: "type-2"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "const int"
    self_type: "type-24"
  }
  is_const: true
  is_volatile: false
  is_restricted: false
}
qualified_types {
  type_info {
    name: "const float"
    size: 4
    alignment: 4
    referenced_type: "type-3"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "const float"
    self_type: "type-25"
  }
  is_const: true
  is_volatile: false
  is_restricted: false
}
qualified_types {
  type_info {
    name: "const CPPHello"
    size: 56
    alignment: 8
    referenced_type: "type-23"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "const CPPHello"
    self_type: "type-28"
  }
  is_const: true
  is_volatile: false
  is_restricted: false
}
qualified_types {
  type_info {
    name: "const char"
    size: 1
    alignment: 1
    referenced_type: "type-40"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "const char"
    self_type: "type-38"
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
    linker_set_key: "bool [2]"
    self_type: "type-10"
  }
}
functions {
  return_type: "type-6"
  function_name: "test2::HelloAgain::~HelloAgain"
  source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
  parameters {
    referenced_type: "type-7"
    default_arg: false
    is_this_ptr: true
  }
  linker_set_key: "_ZN5test210HelloAgainD2Ev"
  access: public_access
}
functions {
  return_type: "type-6"
  function_name: "test2::HelloAgain::~HelloAgain"
  source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
  parameters {
    referenced_type: "type-7"
    default_arg: false
    is_this_ptr: true
  }
  linker_set_key: "_ZN5test210HelloAgainD1Ev"
  access: public_access
}
functions {
  return_type: "type-6"
  function_name: "test2::HelloAgain::~HelloAgain"
  source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
  parameters {
    referenced_type: "type-7"
    default_arg: false
    is_this_ptr: true
  }
  linker_set_key: "_ZN5test210HelloAgainD0Ev"
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
  return_type: "type-2"
  function_name: "CPPHello::again"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  parameters {
    referenced_type: "type-26"
    default_arg: false
    is_this_ptr: true
  }
  linker_set_key: "_ZN8CPPHello5againEv"
  access: public_access
}
functions {
  return_type: "type-6"
  function_name: "CPPHello::CPPHello"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  parameters {
    referenced_type: "type-26"
    default_arg: false
    is_this_ptr: true
  }
  linker_set_key: "_ZN8CPPHelloC2Ev"
  access: public_access
}
functions {
  return_type: "type-6"
  function_name: "CPPHello::CPPHello"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  parameters {
    referenced_type: "type-26"
    default_arg: false
    is_this_ptr: true
  }
  linker_set_key: "_ZN8CPPHelloC1Ev"
  access: public_access
}
functions {
  return_type: "type-2"
  function_name: "CPPHello::test_enum"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  parameters {
    referenced_type: "type-26"
    default_arg: false
    is_this_ptr: true
  }
  linker_set_key: "_ZN8CPPHello9test_enumEv"
  access: public_access
}
functions {
  return_type: "type-2"
  function_name: "boo"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  parameters {
    referenced_type: "type-28"
    default_arg: false
    is_this_ptr: false
  }
  parameters {
    referenced_type: "type-29"
    default_arg: false
    is_this_ptr: false
  }
  parameters {
    referenced_type: "type-30"
    default_arg: false
    is_this_ptr: false
  }
  linker_set_key: "_Z3boo8CPPHelloPiPf"
  access: public_access
}
functions {
  return_type: "type-6"
  function_name: "List<float>::_Node::_Node"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  parameters {
    referenced_type: "type-33"
    default_arg: false
    is_this_ptr: true
  }
  parameters {
    referenced_type: "type-34"
    default_arg: false
    is_this_ptr: false
  }
  linker_set_key: "_ZN4ListIfE5_NodeC2ERKf"
  access: public_access
}
functions {
  return_type: "type-6"
  function_name: "List<float>::_Node::_Node"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  parameters {
    referenced_type: "type-33"
    default_arg: false
    is_this_ptr: true
  }
  parameters {
    referenced_type: "type-34"
    default_arg: false
    is_this_ptr: false
  }
  linker_set_key: "_ZN4ListIfE5_NodeC1ERKf"
  access: public_access
}
functions {
  return_type: "type-6"
  function_name: "List<float>::_Node::~_Node"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  parameters {
    referenced_type: "type-33"
    default_arg: false
    is_this_ptr: true
  }
  linker_set_key: "_ZN4ListIfE5_NodeD2Ev"
  access: public_access
}
functions {
  return_type: "type-6"
  function_name: "List<float>::_Node::~_Node"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  parameters {
    referenced_type: "type-33"
    default_arg: false
    is_this_ptr: true
  }
  linker_set_key: "_ZN4ListIfE5_NodeD1Ev"
  access: public_access
}
functions {
  return_type: "type-31"
  function_name: "castInterface"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  parameters {
    referenced_type: "type-31"
    default_arg: false
    is_this_ptr: false
  }
  parameters {
    referenced_type: "type-39"
    default_arg: false
    is_this_ptr: false
  }
  parameters {
    referenced_type: "type-12"
    default_arg: false
    is_this_ptr: false
  }
  template_info {
    elements {
      referenced_type: "type-3"
    }
    elements {
      referenced_type: "type-3"
    }
    elements {
      referenced_type: "type-3"
    }
    elements {
      referenced_type: "type-3"
    }
  }
  linker_set_key: "_Z13castInterfaceIffffE4ListIT_ES0_IT0_EPKcb"
  access: public_access
}
functions {
  return_type: "type-6"
  function_name: "format"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  linker_set_key: "_Z6formatv"
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
  name: "__test_var"
  source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
  linker_set_key: "_ZL10__test_var"
  referenced_type: "type-11"
  access: public_access
}
global_vars {
  name: "test3::ByeAgain<float>::foo_forever"
  source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
  linker_set_key: "_ZN5test38ByeAgainIfE11foo_foreverE"
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
  name: "float_list_test"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  linker_set_key: "float_list_test"
  referenced_type: "type-31"
  access: public_access
}
global_vars {
  name: "int_list_test"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  linker_set_key: "int_list_test"
  referenced_type: "type-35"
  access: public_access
}
global_vars {
  name: "node"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  linker_set_key: "node"
  referenced_type: "type-32"
  access: public_access
}
