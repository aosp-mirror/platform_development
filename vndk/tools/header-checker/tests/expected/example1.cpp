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
record_types {
  type_info {
    name: "test2::HelloAgain"
    size: 40
    alignment: 8
    referenced_type: "test2::HelloAgain"
    source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
    linker_set_key: "test2::HelloAgain"
  }
  fields {
    referenced_type: "std::vector<test2::HelloAgain *, std::allocator<test2::HelloAgain *> >"
    field_offset: 64
    field_name: "foo_again"
    access: public_access
  }
  fields {
    referenced_type: "int"
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
      mangled_component_name: "test2::HelloAgain"
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
}
record_types {
  type_info {
    name: "test3::ByeAgain<double>"
    size: 16
    alignment: 8
    referenced_type: "test3::ByeAgain<double>"
    source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
    linker_set_key: "test3::ByeAgain<double>"
  }
  fields {
    referenced_type: "double"
    field_offset: 0
    field_name: "foo_again"
    access: public_access
  }
  fields {
    referenced_type: "int"
    field_offset: 64
    field_name: "bar_again"
    access: public_access
  }
  template_info {
    elements {
      referenced_type: "double"
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
    referenced_type: "test3::ByeAgain<float>"
    source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
    linker_set_key: "test3::ByeAgain<float>"
  }
  fields {
    referenced_type: "float"
    field_offset: 0
    field_name: "foo_again"
    access: public_access
  }
  fields {
    referenced_type: "float"
    field_offset: 32
    field_name: "bar_Again"
    access: public_access
  }
  template_info {
    elements {
      referenced_type: "float"
    }
  }
  access: public_access
  record_kind: struct_kind
}
record_types {
  type_info {
    name: "test3::Outer"
    size: 4
    alignment: 4
    referenced_type: "test3::Outer"
    source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
    linker_set_key: "test3::Outer"
  }
  fields {
    referenced_type: "int"
    field_offset: 0
    field_name: "a"
    access: public_access
  }
  access: public_access
  record_kind: class_kind
}
record_types {
  type_info {
    name: "test3::Outer::Inner"
    size: 4
    alignment: 4
    referenced_type: "test3::Outer::Inner"
    source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
    linker_set_key: "test3::Outer::Inner"
  }
  fields {
    referenced_type: "int"
    field_offset: 0
    field_name: "b"
    access: private_access
  }
  access: private_access
  record_kind: class_kind
}
record_types {
  type_info {
    name: "Hello::(anonymous)::(anonymous)"
    size: 4
    alignment: 4
    referenced_type: "Hello::(anonymous)::(anonymous)"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "Hello::(anonymous)5::(anonymous)2"
  }
  fields {
    referenced_type: "int"
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
    name: "Hello::(anonymous)"
    size: 12
    alignment: 4
    referenced_type: "Hello::(anonymous)"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "Hello::(anonymous)5"
  }
  fields {
    referenced_type: "int"
    field_offset: 0
    field_name: "a"
    access: public_access
  }
  fields {
    referenced_type: "int"
    field_offset: 32
    field_name: "b"
    access: public_access
  }
  fields {
    referenced_type: "Hello::(anonymous)5::(anonymous)2"
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
    name: "Hello"
    size: 32
    alignment: 4
    referenced_type: "Hello"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "Hello"
  }
  fields {
    referenced_type: "int"
    field_offset: 0
    field_name: "foo"
    access: public_access
  }
  fields {
    referenced_type: "int"
    field_offset: 32
    field_name: "bar"
    access: public_access
  }
  fields {
    referenced_type: "wchar_t"
    field_offset: 64
    field_name: "d"
    access: public_access
  }
  fields {
    referenced_type: "unsigned int"
    field_offset: 96
    field_name: "enum_field"
    access: public_access
  }
  fields {
    referenced_type: "unsigned int"
    field_offset: 128
    field_name: "enum_field2"
    access: public_access
  }
  fields {
    referenced_type: "Hello::(anonymous)5"
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
    referenced_type: "CPPHello"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "CPPHello"
  }
  fields {
    referenced_type: "const int"
    field_offset: 352
    field_name: "cpp_foo"
    access: public_access
  }
  fields {
    referenced_type: "const float"
    field_offset: 384
    field_name: "cpp_bar"
    access: public_access
  }
  base_specifiers {
    referenced_type: "test2::HelloAgain"
    is_virtual: false
    access: private_access
  }
  base_specifiers {
    referenced_type: "test3::ByeAgain<float>"
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
      mangled_component_name: "CPPHello"
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
}
record_types {
  type_info {
    name: "List<float>"
    size: 8
    alignment: 8
    referenced_type: "List<float>"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "List<float>"
  }
  fields {
    referenced_type: "List<float>::_Node *"
    field_offset: 0
    field_name: "middle"
    access: public_access
  }
  template_info {
    elements {
      referenced_type: "float"
    }
  }
  access: public_access
  record_kind: class_kind
}
record_types {
  type_info {
    name: "List<float>::_Node"
    size: 24
    alignment: 8
    referenced_type: "List<float>::_Node"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "List<float>::_Node"
  }
  fields {
    referenced_type: "float"
    field_offset: 0
    field_name: "mVal"
    access: private_access
  }
  fields {
    referenced_type: "List<float>::_Node *"
    field_offset: 64
    field_name: "mpPrev"
    access: private_access
  }
  fields {
    referenced_type: "List<float>::_Node *"
    field_offset: 128
    field_name: "mpNext"
    access: private_access
  }
  access: public_access
  record_kind: class_kind
}
record_types {
  type_info {
    name: "List<int>"
    size: 8
    alignment: 8
    referenced_type: "List<int>"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "List<int>"
  }
  fields {
    referenced_type: "List<int>::_Node *"
    field_offset: 0
    field_name: "middle"
    access: public_access
  }
  template_info {
    elements {
      referenced_type: "int"
    }
  }
  access: public_access
  record_kind: class_kind
}
enum_types {
  type_info {
    name: "Foo_s"
    size: 4
    alignment: 4
    referenced_type: "Foo_s"
    source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
    linker_set_key: "Foo_s"
  }
  underlying_type: "unsigned int"
  enum_fields {
    enum_field_value: 10
    name: "Foo_s::foosball"
  }
  enum_fields {
    enum_field_value: 11
    name: "Foo_s::foosbat"
  }
  access: public_access
}
enum_types {
  type_info {
    name: "test3::Kind"
    size: 4
    alignment: 4
    referenced_type: "test3::Kind"
    source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
    linker_set_key: "test3::Kind"
  }
  underlying_type: "unsigned int"
  enum_fields {
    enum_field_value: 24
    name: "test3::Kind::kind1"
  }
  enum_fields {
    enum_field_value: 2312
    name: "test3::Kind::kind2"
  }
  access: public_access
}
enum_types {
  type_info {
    name: "CPPHello::Bla"
    size: 4
    alignment: 4
    referenced_type: "CPPHello::Bla"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "CPPHello::Bla"
  }
  underlying_type: "unsigned int"
  enum_fields {
    enum_field_value: 1
    name: "CPPHello::Bla::BLA"
  }
  access: public_access
}
pointer_types {
  type_info {
    name: "test2::HelloAgain *"
    size: 8
    alignment: 8
    referenced_type: "test2::HelloAgain"
    source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
    linker_set_key: "test2::HelloAgain *"
  }
}
pointer_types {
  type_info {
    name: "test3::ByeAgain<double> *"
    size: 8
    alignment: 8
    referenced_type: "test3::ByeAgain<double>"
    source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
    linker_set_key: "test3::ByeAgain<double> *"
  }
}
pointer_types {
  type_info {
    name: "test3::ByeAgain<float> *"
    size: 8
    alignment: 8
    referenced_type: "test3::ByeAgain<float>"
    source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
    linker_set_key: "test3::ByeAgain<float> *"
  }
}
pointer_types {
  type_info {
    name: "ForwardDeclaration *"
    size: 8
    alignment: 8
    referenced_type: "ForwardDeclaration"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "ForwardDeclaration *"
  }
}
pointer_types {
  type_info {
    name: "CPPHello *"
    size: 8
    alignment: 8
    referenced_type: "CPPHello"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "CPPHello *"
  }
}
pointer_types {
  type_info {
    name: "int *"
    size: 8
    alignment: 8
    referenced_type: "int"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "int *"
  }
}
pointer_types {
  type_info {
    name: "float *"
    size: 8
    alignment: 8
    referenced_type: "float"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "float *"
  }
}
pointer_types {
  type_info {
    name: "List<float>::_Node *"
    size: 8
    alignment: 8
    referenced_type: "List<float>::_Node"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "List<float>::_Node *"
  }
}
pointer_types {
  type_info {
    name: "const List<float>::_Node *"
    size: 8
    alignment: 8
    referenced_type: "const List<float>::_Node"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "const List<float>::_Node *"
  }
}
pointer_types {
  type_info {
    name: "List<int>::_Node *"
    size: 8
    alignment: 8
    referenced_type: "List<int>::_Node"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "List<int>::_Node *"
  }
}
pointer_types {
  type_info {
    name: "List<int> *"
    size: 8
    alignment: 8
    referenced_type: "List<int>"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "List<int> *"
  }
}
pointer_types {
  type_info {
    name: "StackNode<int> *"
    size: 8
    alignment: 8
    referenced_type: "StackNode<int>"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "StackNode<int> *"
  }
}
pointer_types {
  type_info {
    name: "const char *"
    size: 8
    alignment: 8
    referenced_type: "const char"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "const char *"
  }
}
lvalue_reference_types {
  type_info {
    name: "int &"
    size: 8
    alignment: 8
    referenced_type: "int"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "int &"
  }
}
lvalue_reference_types {
  type_info {
    name: "const float &"
    size: 8
    alignment: 8
    referenced_type: "const float"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "const float &"
  }
}
lvalue_reference_types {
  type_info {
    name: "float &"
    size: 8
    alignment: 8
    referenced_type: "float"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "float &"
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
builtin_types {
  type_info {
    name: "void"
    size: 0
    alignment: 0
    referenced_type: "void"
    source_file: ""
    linker_set_key: "void"
  }
  is_unsigned: false
  is_integral: false
}
builtin_types {
  type_info {
    name: "unsigned int"
    size: 4
    alignment: 4
    referenced_type: "unsigned int"
    source_file: ""
    linker_set_key: "unsigned int"
  }
  is_unsigned: true
  is_integral: true
}
builtin_types {
  type_info {
    name: "bool"
    size: 1
    alignment: 1
    referenced_type: "bool"
    source_file: ""
    linker_set_key: "bool"
  }
  is_unsigned: true
  is_integral: true
}
builtin_types {
  type_info {
    name: "double"
    size: 8
    alignment: 8
    referenced_type: "double"
    source_file: ""
    linker_set_key: "double"
  }
  is_unsigned: false
  is_integral: false
}
builtin_types {
  type_info {
    name: "wchar_t"
    size: 4
    alignment: 4
    referenced_type: "wchar_t"
    source_file: ""
    linker_set_key: "wchar_t"
  }
  is_unsigned: false
  is_integral: true
}
builtin_types {
  type_info {
    name: "char"
    size: 1
    alignment: 1
    referenced_type: "char"
    source_file: ""
    linker_set_key: "char"
  }
  is_unsigned: false
  is_integral: true
}
qualified_types {
  type_info {
    name: "bool const[2]"
    size: 2
    alignment: 1
    referenced_type: "bool [2]"
    source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
    linker_set_key: "bool const[2]"
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
    referenced_type: "int"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "const int"
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
    referenced_type: "float"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "const float"
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
    referenced_type: "CPPHello"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "const CPPHello"
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
    referenced_type: "List<float>::_Node"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "const List<float>::_Node"
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
    referenced_type: "char"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "const char"
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
    referenced_type: "bool"
    source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
    linker_set_key: "bool [2]"
  }
}
functions {
  return_type: "int"
  function_name: "test2::HelloAgain::again"
  source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
  parameters {
    referenced_type: "test2::HelloAgain *"
    default_arg: false
  }
  linker_set_key: "_ZN5test210HelloAgain5againEv"
  access: public_access
}
functions {
  return_type: "void"
  function_name: "test2::HelloAgain::~HelloAgain"
  source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
  parameters {
    referenced_type: "test2::HelloAgain *"
    default_arg: false
  }
  linker_set_key: "_ZN5test210HelloAgainD2Ev"
  access: public_access
}
functions {
  return_type: "void"
  function_name: "test2::HelloAgain::~HelloAgain"
  source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
  parameters {
    referenced_type: "test2::HelloAgain *"
    default_arg: false
  }
  linker_set_key: "_ZN5test210HelloAgainD1Ev"
  access: public_access
}
functions {
  return_type: "void"
  function_name: "test2::HelloAgain::~HelloAgain"
  source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
  parameters {
    referenced_type: "test2::HelloAgain *"
    default_arg: false
  }
  linker_set_key: "_ZN5test210HelloAgainD0Ev"
  access: public_access
}
functions {
  return_type: "double"
  function_name: "test3::ByeAgain<double>::method_foo"
  source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
  parameters {
    referenced_type: "test3::ByeAgain<double> *"
    default_arg: false
  }
  parameters {
    referenced_type: "double"
    default_arg: false
  }
  linker_set_key: "_ZN5test38ByeAgainIdE10method_fooEd"
  access: public_access
}
functions {
  return_type: "float"
  function_name: "test3::ByeAgain<float>::method_foo"
  source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
  parameters {
    referenced_type: "test3::ByeAgain<float> *"
    default_arg: false
  }
  parameters {
    referenced_type: "int"
    default_arg: false
  }
  linker_set_key: "_ZN5test38ByeAgainIfE10method_fooEi"
  access: public_access
}
functions {
  return_type: "bool"
  function_name: "test3::Begin"
  source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
  parameters {
    referenced_type: "float"
    default_arg: false
  }
  parameters {
    referenced_type: "int"
    default_arg: false
  }
  parameters {
    referenced_type: "int"
    default_arg: false
  }
  template_info {
    elements {
      referenced_type: "float"
    }
    elements {
      referenced_type: "int"
    }
  }
  linker_set_key: "_ZN5test35BeginIfiEEbT_T0_i"
  access: public_access
}
functions {
  return_type: "bool"
  function_name: "test3::End"
  source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
  parameters {
    referenced_type: "float"
    default_arg: true
  }
  linker_set_key: "_ZN5test33EndEf"
  access: public_access
}
functions {
  return_type: "std::vector<int *, std::allocator<int *> >"
  function_name: "test3::Dummy"
  source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
  parameters {
    referenced_type: "int"
    default_arg: false
  }
  linker_set_key: "_ZN5test35DummyEi"
  access: public_access
}
functions {
  return_type: "int"
  function_name: "uses_forward_decl"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  parameters {
    referenced_type: "ForwardDeclaration *"
    default_arg: false
  }
  linker_set_key: "uses_forward_decl"
  access: public_access
}
functions {
  return_type: "int"
  function_name: "CPPHello::again"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  parameters {
    referenced_type: "CPPHello *"
    default_arg: false
  }
  linker_set_key: "_ZN8CPPHello5againEv"
  access: public_access
}
functions {
  return_type: "void"
  function_name: "CPPHello::CPPHello"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  parameters {
    referenced_type: "CPPHello *"
    default_arg: false
  }
  linker_set_key: "_ZN8CPPHelloC2Ev"
  access: public_access
}
functions {
  return_type: "void"
  function_name: "CPPHello::CPPHello"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  parameters {
    referenced_type: "CPPHello *"
    default_arg: false
  }
  linker_set_key: "_ZN8CPPHelloC1Ev"
  access: public_access
}
functions {
  return_type: "int"
  function_name: "CPPHello::test_enum"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  parameters {
    referenced_type: "CPPHello *"
    default_arg: false
  }
  linker_set_key: "_ZN8CPPHello9test_enumEv"
  access: public_access
}
functions {
  return_type: "void"
  function_name: "fooVariadic"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  parameters {
    referenced_type: "int &"
    default_arg: false
  }
  parameters {
    referenced_type: "int *"
    default_arg: false
  }
  linker_set_key: "_Z11fooVariadicRiPiz"
  access: public_access
}
functions {
  return_type: "int"
  function_name: "boo"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  parameters {
    referenced_type: "const CPPHello"
    default_arg: false
  }
  parameters {
    referenced_type: "int *"
    default_arg: false
  }
  parameters {
    referenced_type: "float *"
    default_arg: false
  }
  linker_set_key: "_Z3boo8CPPHelloPiPf"
  access: public_access
}
functions {
  return_type: "void"
  function_name: "List<float>::_Node::_Node"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  parameters {
    referenced_type: "List<float>::_Node *"
    default_arg: false
  }
  parameters {
    referenced_type: "const float &"
    default_arg: false
  }
  linker_set_key: "_ZN4ListIfE5_NodeC2ERKf"
  access: public_access
}
functions {
  return_type: "void"
  function_name: "List<float>::_Node::_Node"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  parameters {
    referenced_type: "List<float>::_Node *"
    default_arg: false
  }
  parameters {
    referenced_type: "const float &"
    default_arg: false
  }
  linker_set_key: "_ZN4ListIfE5_NodeC1ERKf"
  access: public_access
}
functions {
  return_type: "void"
  function_name: "List<float>::_Node::~_Node"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  parameters {
    referenced_type: "List<float>::_Node *"
    default_arg: false
  }
  linker_set_key: "_ZN4ListIfE5_NodeD2Ev"
  access: public_access
}
functions {
  return_type: "void"
  function_name: "List<float>::_Node::~_Node"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  parameters {
    referenced_type: "List<float>::_Node *"
    default_arg: false
  }
  linker_set_key: "_ZN4ListIfE5_NodeD1Ev"
  access: public_access
}
functions {
  return_type: "float &"
  function_name: "List<float>::_Node::getRef"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  parameters {
    referenced_type: "List<float>::_Node *"
    default_arg: false
  }
  linker_set_key: "_ZN4ListIfE5_Node6getRefEv"
  access: public_access
}
functions {
  return_type: "const float &"
  function_name: "List<float>::_Node::getRef"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  parameters {
    referenced_type: "const List<float>::_Node *"
    default_arg: false
  }
  linker_set_key: "_ZNK4ListIfE5_Node6getRefEv"
  access: public_access
}
functions {
  return_type: "void"
  function_name: "List<float>::_Node::PrivateNode"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  parameters {
    referenced_type: "List<float>::_Node *"
    default_arg: false
  }
  linker_set_key: "_ZN4ListIfE5_Node11PrivateNodeEv"
  access: private_access
}
functions {
  return_type: "int"
  function_name: "ListMangle"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  parameters {
    referenced_type: "List<int> *"
    default_arg: false
  }
  parameters {
    referenced_type: "StackNode<int> *"
    default_arg: false
  }
  linker_set_key: "_Z10ListMangleP4ListIiEP9StackNodeIiE"
  access: public_access
}
functions {
  return_type: "List<float>"
  function_name: "castInterface"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  parameters {
    referenced_type: "List<float>"
    default_arg: false
  }
  parameters {
    referenced_type: "const char *"
    default_arg: false
  }
  parameters {
    referenced_type: "bool"
    default_arg: false
  }
  template_info {
    elements {
      referenced_type: "float"
    }
    elements {
      referenced_type: "float"
    }
    elements {
      referenced_type: "float"
    }
    elements {
      referenced_type: "float"
    }
  }
  linker_set_key: "_Z13castInterfaceIffffE4ListIT_ES0_IT0_EPKcb"
  access: public_access
}
functions {
  return_type: "void"
  function_name: "format"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  linker_set_key: "_Z6formatv"
  access: public_access
}
global_vars {
  name: "test2::HelloAgain::hello_forever"
  source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
  linker_set_key: "_ZN5test210HelloAgain13hello_foreverE"
  referenced_type: "int"
  access: public_access
}
global_vars {
  name: "__test_var"
  source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
  linker_set_key: "_ZL10__test_var"
  referenced_type: "bool const[2]"
  access: public_access
}
global_vars {
  name: "test3::ByeAgain<float>::foo_forever"
  source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
  linker_set_key: "_ZN5test38ByeAgainIfE11foo_foreverE"
  referenced_type: "int"
  access: public_access
}
global_vars {
  name: "test3::double_bye"
  source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
  linker_set_key: "_ZN5test310double_byeE"
  referenced_type: "test3::ByeAgain<double>"
  access: public_access
}
global_vars {
  name: "float_list_test"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  linker_set_key: "float_list_test"
  referenced_type: "List<float>"
  access: public_access
}
global_vars {
  name: "int_list_test"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  linker_set_key: "int_list_test"
  referenced_type: "List<int>"
  access: public_access
}
global_vars {
  name: "node"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  linker_set_key: "node"
  referenced_type: "List<float>::_Node"
  access: public_access
}
