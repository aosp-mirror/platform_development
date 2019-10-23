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
    name: "List<float>"
    size: 8
    alignment: 8
    referenced_type: "type-34"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "_ZTI4ListIfE"
    self_type: "type-34"
  }
  fields {
    referenced_type: "type-36"
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
}
record_types {
  type_info {
    name: "List<int>"
    size: 8
    alignment: 8
    referenced_type: "type-41"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "_ZTI4ListIiE"
    self_type: "type-41"
  }
  fields {
    referenced_type: "type-43"
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
}
record_types {
  type_info {
    name: "Hello"
    size: 32
    alignment: 4
    referenced_type: "type-21"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "_ZTI5Hello"
    self_type: "type-21"
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
    referenced_type: "type-22"
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
    referenced_type: "type-23"
    field_offset: 160
    field_name: ""
    access: public_access
  }
  access: public_access
  record_kind: struct_kind
}
record_types {
  type_info {
    name: "CPPHello"
    size: 56
    alignment: 8
    referenced_type: "type-25"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "_ZTI8CPPHello"
    self_type: "type-25"
  }
  fields {
    referenced_type: "type-26"
    field_offset: 352
    field_name: "cpp_foo"
    access: public_access
  }
  fields {
    referenced_type: "type-27"
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
      is_pure: false
    }
    vtable_components {
      kind: RTTI
      mangled_component_name: "_ZTI8CPPHello"
      component_value: 0
      is_pure: false
    }
    vtable_components {
      kind: FunctionPointer
      mangled_component_name: "_ZN8CPPHello5againEv"
      component_value: 0
      is_pure: false
    }
    vtable_components {
      kind: CompleteDtorPointer
      mangled_component_name: "_ZN8CPPHelloD1Ev"
      component_value: 0
      is_pure: false
    }
    vtable_components {
      kind: DeletingDtorPointer
      mangled_component_name: "_ZN8CPPHelloD0Ev"
      component_value: 0
      is_pure: false
    }
  }
  access: public_access
  record_kind: struct_kind
}
record_types {
  type_info {
    name: "List<float>::_Node"
    size: 24
    alignment: 8
    referenced_type: "type-35"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "_ZTIN4ListIfE5_NodeE"
    self_type: "type-35"
  }
  fields {
    referenced_type: "type-3"
    field_offset: 0
    field_name: "mVal"
    access: private_access
  }
  fields {
    referenced_type: "type-36"
    field_offset: 64
    field_name: "mpPrev"
    access: private_access
  }
  fields {
    referenced_type: "type-36"
    field_offset: 128
    field_name: "mpNext"
    access: private_access
  }
  access: public_access
  record_kind: class_kind
}
record_types {
  type_info {
    name: "Hello::(anonymous struct at /development/vndk/tools/header-checker/tests/input/example1.h:19:3)"
    size: 12
    alignment: 4
    referenced_type: "type-23"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "_ZTIN5HelloUt1_E"
    self_type: "type-23"
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
    referenced_type: "type-24"
    field_offset: 64
    field_name: ""
    access: public_access
  }
  access: public_access
  is_anonymous: true
  record_kind: struct_kind
}
record_types {
  type_info {
    name: "Hello::(anonymous struct at /development/vndk/tools/header-checker/tests/input/example1.h:19:3)::(anonymous struct at /development/vndk/tools/header-checker/tests/input/example1.h:22:5)"
    size: 4
    alignment: 4
    referenced_type: "type-24"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "_ZTIN5HelloUt1_Ut_E"
    self_type: "type-24"
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
    referenced_type: "type-18"
    source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
    linker_set_key: "_ZTIN5test35Outer5InnerE"
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
}
record_types {
  type_info {
    name: "test3::Outer"
    size: 4
    alignment: 4
    referenced_type: "type-17"
    source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
    linker_set_key: "_ZTIN5test35OuterE"
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
    referenced_type: "type-15"
    source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
    linker_set_key: "_ZTIN5test38ByeAgainIfEE"
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
    referenced_type: "type-16"
    source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
    linker_set_key: "_ZTIN5test34KindE"
    self_type: "type-16"
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
enum_types {
  type_info {
    name: "CPPHello::Bla"
    size: 4
    alignment: 4
    referenced_type: "type-29"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "_ZTIN8CPPHello3BlaE"
    self_type: "type-29"
  }
  underlying_type: "type-9"
  enum_fields {
    enum_field_value: 1
    name: "CPPHello::BLA"
  }
  access: public_access
}
pointer_types {
  type_info {
    name: "ForwardDeclaration *"
    size: 8
    alignment: 8
    referenced_type: "type-19"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "_ZTIP18ForwardDeclaration"
    self_type: "type-20"
  }
}
pointer_types {
  type_info {
    name: "List<int> *"
    size: 8
    alignment: 8
    referenced_type: "type-41"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "_ZTIP4ListIiE"
    self_type: "type-44"
  }
}
pointer_types {
  type_info {
    name: "CPPHello *"
    size: 8
    alignment: 8
    referenced_type: "type-25"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "_ZTIP8CPPHello"
    self_type: "type-28"
  }
}
pointer_types {
  type_info {
    name: "StackNode<int> *"
    size: 8
    alignment: 8
    referenced_type: "type-45"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "_ZTIP9StackNodeIiE"
    self_type: "type-46"
  }
}
pointer_types {
  type_info {
    name: "const List<float>::_Node *"
    size: 8
    alignment: 8
    referenced_type: "type-39"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "_ZTIPKN4ListIfE5_NodeE"
    self_type: "type-40"
  }
}
pointer_types {
  type_info {
    name: "const char *"
    size: 8
    alignment: 8
    referenced_type: "type-47"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "_ZTIPKc"
    self_type: "type-48"
  }
}
pointer_types {
  type_info {
    name: "List<float>::_Node *"
    size: 8
    alignment: 8
    referenced_type: "type-35"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "_ZTIPN4ListIfE5_NodeE"
    self_type: "type-36"
  }
}
pointer_types {
  type_info {
    name: "List<int>::_Node *"
    size: 8
    alignment: 8
    referenced_type: "type-42"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "_ZTIPN4ListIiE5_NodeE"
    self_type: "type-43"
  }
}
pointer_types {
  type_info {
    name: "test2::HelloAgain *"
    size: 8
    alignment: 8
    referenced_type: "type-4"
    source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
    linker_set_key: "_ZTIPN5test210HelloAgainE"
    self_type: "type-7"
  }
}
pointer_types {
  type_info {
    name: "float *"
    size: 8
    alignment: 8
    referenced_type: "type-3"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "_ZTIPf"
    self_type: "type-33"
  }
}
pointer_types {
  type_info {
    name: "int *"
    size: 8
    alignment: 8
    referenced_type: "type-2"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "_ZTIPi"
    self_type: "type-31"
  }
}
lvalue_reference_types {
  type_info {
    name: "const float &"
    size: 8
    alignment: 8
    referenced_type: "type-27"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "_ZTIRKf"
    self_type: "type-37"
  }
}
lvalue_reference_types {
  type_info {
    name: "float &"
    size: 8
    alignment: 8
    referenced_type: "type-3"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "_ZTIRf"
    self_type: "type-38"
  }
}
lvalue_reference_types {
  type_info {
    name: "int &"
    size: 8
    alignment: 8
    referenced_type: "type-2"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "_ZTIRi"
    self_type: "type-30"
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
    name: "char"
    size: 1
    alignment: 1
    referenced_type: "type-49"
    source_file: ""
    linker_set_key: "_ZTIc"
    self_type: "type-49"
  }
  is_unsigned: false
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
    referenced_type: "type-6"
    source_file: ""
    linker_set_key: "_ZTIv"
    self_type: "type-6"
  }
  is_unsigned: false
  is_integral: false
}
builtin_types {
  type_info {
    name: "wchar_t"
    size: 4
    alignment: 4
    referenced_type: "type-22"
    source_file: ""
    linker_set_key: "_ZTIw"
    self_type: "type-22"
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
    linker_set_key: "_ZTIA2_Kb"
    self_type: "type-11"
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
    referenced_type: "type-25"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "_ZTIK8CPPHello"
    self_type: "type-32"
  }
  is_const: true
  is_volatile: false
  is_restricted: false
}
qualified_types {
  type_info {
    name: "const List<float>::_Node"
    size: 24
    alignment: 8
    referenced_type: "type-35"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "_ZTIKN4ListIfE5_NodeE"
    self_type: "type-39"
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
    referenced_type: "type-49"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "_ZTIKc"
    self_type: "type-47"
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
    linker_set_key: "_ZTIKf"
    self_type: "type-27"
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
    linker_set_key: "_ZTIKi"
    self_type: "type-26"
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
  function_name: "ListMangle"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  parameters {
    referenced_type: "type-44"
    default_arg: false
    is_this_ptr: false
  }
  parameters {
    referenced_type: "type-46"
    default_arg: false
    is_this_ptr: false
  }
  linker_set_key: "_Z10ListMangleP4ListIiEP9StackNodeIiE"
  access: public_access
}
functions {
  return_type: "type-6"
  function_name: "fooVariadic"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  parameters {
    referenced_type: "type-30"
    default_arg: false
    is_this_ptr: false
  }
  parameters {
    referenced_type: "type-31"
    default_arg: false
    is_this_ptr: false
  }
  linker_set_key: "_Z11fooVariadicRiPiz"
  access: public_access
}
functions {
  return_type: "type-34"
  function_name: "castInterface"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  parameters {
    referenced_type: "type-34"
    default_arg: false
    is_this_ptr: false
  }
  parameters {
    referenced_type: "type-48"
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
  return_type: "type-2"
  function_name: "boo"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  parameters {
    referenced_type: "type-32"
    default_arg: false
    is_this_ptr: false
  }
  parameters {
    referenced_type: "type-31"
    default_arg: false
    is_this_ptr: false
  }
  parameters {
    referenced_type: "type-33"
    default_arg: false
    is_this_ptr: false
  }
  linker_set_key: "_Z3boo8CPPHelloPiPf"
  access: public_access
}
functions {
  return_type: "type-6"
  function_name: "format"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  linker_set_key: "_Z6formatv"
  access: public_access
}
functions {
  return_type: "type-6"
  function_name: "List<float>::_Node::PrivateNode"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  parameters {
    referenced_type: "type-36"
    default_arg: false
    is_this_ptr: true
  }
  linker_set_key: "_ZN4ListIfE5_Node11PrivateNodeEv"
  access: private_access
}
functions {
  return_type: "type-38"
  function_name: "List<float>::_Node::getRef"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  parameters {
    referenced_type: "type-36"
    default_arg: false
    is_this_ptr: true
  }
  linker_set_key: "_ZN4ListIfE5_Node6getRefEv"
  access: public_access
}
functions {
  return_type: "type-6"
  function_name: "List<float>::_Node::_Node"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  parameters {
    referenced_type: "type-36"
    default_arg: false
    is_this_ptr: true
  }
  parameters {
    referenced_type: "type-37"
    default_arg: false
    is_this_ptr: false
  }
  linker_set_key: "_ZN4ListIfE5_NodeC1ERKf"
  access: public_access
}
functions {
  return_type: "type-6"
  function_name: "List<float>::_Node::_Node"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  parameters {
    referenced_type: "type-36"
    default_arg: false
    is_this_ptr: true
  }
  parameters {
    referenced_type: "type-37"
    default_arg: false
    is_this_ptr: false
  }
  linker_set_key: "_ZN4ListIfE5_NodeC2ERKf"
  access: public_access
}
functions {
  return_type: "type-6"
  function_name: "List<float>::_Node::~_Node"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  parameters {
    referenced_type: "type-36"
    default_arg: false
    is_this_ptr: true
  }
  linker_set_key: "_ZN4ListIfE5_NodeD1Ev"
  access: public_access
}
functions {
  return_type: "type-6"
  function_name: "List<float>::_Node::~_Node"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  parameters {
    referenced_type: "type-36"
    default_arg: false
    is_this_ptr: true
  }
  linker_set_key: "_ZN4ListIfE5_NodeD2Ev"
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
  return_type: "type-2"
  function_name: "CPPHello::again"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  parameters {
    referenced_type: "type-28"
    default_arg: false
    is_this_ptr: true
  }
  linker_set_key: "_ZN8CPPHello5againEv"
  access: public_access
}
functions {
  return_type: "type-2"
  function_name: "CPPHello::test_enum"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  parameters {
    referenced_type: "type-28"
    default_arg: false
    is_this_ptr: true
  }
  linker_set_key: "_ZN8CPPHello9test_enumEv"
  access: public_access
}
functions {
  return_type: "type-6"
  function_name: "CPPHello::CPPHello"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  parameters {
    referenced_type: "type-28"
    default_arg: false
    is_this_ptr: true
  }
  linker_set_key: "_ZN8CPPHelloC1Ev"
  access: public_access
}
functions {
  return_type: "type-6"
  function_name: "CPPHello::CPPHello"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  parameters {
    referenced_type: "type-28"
    default_arg: false
    is_this_ptr: true
  }
  linker_set_key: "_ZN8CPPHelloC2Ev"
  access: public_access
}
functions {
  return_type: "type-37"
  function_name: "List<float>::_Node::getRef"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  parameters {
    referenced_type: "type-40"
    default_arg: false
    is_this_ptr: true
  }
  linker_set_key: "_ZNK4ListIfE5_Node6getRefEv"
  access: public_access
}
functions {
  return_type: "type-2"
  function_name: "uses_forward_decl"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  parameters {
    referenced_type: "type-20"
    default_arg: false
    is_this_ptr: false
  }
  linker_set_key: "uses_forward_decl"
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
global_vars {
  name: "float_list_test"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  linker_set_key: "float_list_test"
  referenced_type: "type-34"
  access: public_access
}
global_vars {
  name: "int_list_test"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  linker_set_key: "int_list_test"
  referenced_type: "type-41"
  access: public_access
}
global_vars {
  name: "node"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  linker_set_key: "node"
  referenced_type: "type-35"
  access: public_access
}
