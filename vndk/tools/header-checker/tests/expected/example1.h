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
record_types {
  type_info {
    name: "List<float>"
    size: 8
    alignment: 8
    referenced_type: "_ZTI4ListIfE"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "_ZTI4ListIfE"
    self_type: "_ZTI4ListIfE"
  }
  fields {
    referenced_type: "_ZTIPN4ListIfE5_NodeE"
    field_offset: 0
    field_name: "middle"
    access: public_access
  }
  template_info {
    elements {
      referenced_type: "_ZTIf"
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
    referenced_type: "_ZTI4ListIiE"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "_ZTI4ListIiE"
    self_type: "_ZTI4ListIiE"
  }
  fields {
    referenced_type: "_ZTIPN4ListIiE5_NodeE"
    field_offset: 0
    field_name: "middle"
    access: public_access
  }
  template_info {
    elements {
      referenced_type: "_ZTIi"
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
    referenced_type: "_ZTI5Hello"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "_ZTI5Hello"
    self_type: "_ZTI5Hello"
  }
  fields {
    referenced_type: "_ZTIi"
    field_offset: 0
    field_name: "foo"
    access: public_access
  }
  fields {
    referenced_type: "_ZTIi"
    field_offset: 32
    field_name: "bar"
    access: public_access
  }
  fields {
    referenced_type: "_ZTIw"
    field_offset: 64
    field_name: "d"
    access: public_access
  }
  fields {
    referenced_type: "_ZTIN5Hello2$AE"
    field_offset: 96
    field_name: "enum_field"
    access: public_access
  }
  fields {
    referenced_type: "_ZTIN5Hello2$CE"
    field_offset: 128
    field_name: "enum_field2"
    access: public_access
  }
  fields {
    referenced_type: "_ZTIN5HelloUt1_E"
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
    referenced_type: "_ZTI8CPPHello"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "_ZTI8CPPHello"
    self_type: "_ZTI8CPPHello"
  }
  fields {
    referenced_type: "_ZTIKi"
    field_offset: 352
    field_name: "cpp_foo"
    access: public_access
  }
  fields {
    referenced_type: "_ZTIKf"
    field_offset: 384
    field_name: "cpp_bar"
    access: public_access
  }
  base_specifiers {
    referenced_type: "_ZTIN5test210HelloAgainE"
    is_virtual: false
    access: private_access
  }
  base_specifiers {
    referenced_type: "_ZTIN5test38ByeAgainIfEE"
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
    referenced_type: "_ZTIN4ListIfE5_NodeE"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "_ZTIN4ListIfE5_NodeE"
    self_type: "_ZTIN4ListIfE5_NodeE"
  }
  fields {
    referenced_type: "_ZTIf"
    field_offset: 0
    field_name: "mVal"
    access: private_access
  }
  fields {
    referenced_type: "_ZTIPN4ListIfE5_NodeE"
    field_offset: 64
    field_name: "mpPrev"
    access: private_access
  }
  fields {
    referenced_type: "_ZTIPN4ListIfE5_NodeE"
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
    referenced_type: "_ZTIN5HelloUt1_E"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "_ZTIN5HelloUt1_E"
    self_type: "_ZTIN5HelloUt1_E"
  }
  fields {
    referenced_type: "_ZTIi"
    field_offset: 0
    field_name: "a"
    access: public_access
  }
  fields {
    referenced_type: "_ZTIi"
    field_offset: 32
    field_name: "b"
    access: public_access
  }
  fields {
    referenced_type: "_ZTIN5HelloUt1_Ut_E"
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
    referenced_type: "_ZTIN5HelloUt1_Ut_E"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "_ZTIN5HelloUt1_Ut_E"
    self_type: "_ZTIN5HelloUt1_Ut_E"
  }
  fields {
    referenced_type: "_ZTIi"
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
    referenced_type: "_ZTIN5test210HelloAgainE"
    source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
    linker_set_key: "_ZTIN5test210HelloAgainE"
    self_type: "_ZTIN5test210HelloAgainE"
  }
  fields {
    referenced_type: "_ZTINSt3__16vectorIPN5test210HelloAgainENS_9allocatorIS3_EEEE"
    field_offset: 64
    field_name: "foo_again"
    access: public_access
  }
  fields {
    referenced_type: "_ZTIi"
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
    referenced_type: "_ZTIN5test35Outer5InnerE"
    source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
    linker_set_key: "_ZTIN5test35Outer5InnerE"
    self_type: "_ZTIN5test35Outer5InnerE"
  }
  fields {
    referenced_type: "_ZTIi"
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
    referenced_type: "_ZTIN5test35OuterE"
    source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
    linker_set_key: "_ZTIN5test35OuterE"
    self_type: "_ZTIN5test35OuterE"
  }
  fields {
    referenced_type: "_ZTIi"
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
    referenced_type: "_ZTIN5test38ByeAgainIdEE"
    source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
    linker_set_key: "_ZTIN5test38ByeAgainIdEE"
    self_type: "_ZTIN5test38ByeAgainIdEE"
  }
  fields {
    referenced_type: "_ZTId"
    field_offset: 0
    field_name: "foo_again"
    access: public_access
  }
  fields {
    referenced_type: "_ZTIi"
    field_offset: 64
    field_name: "bar_again"
    access: public_access
  }
  template_info {
    elements {
      referenced_type: "_ZTId"
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
    referenced_type: "_ZTIN5test38ByeAgainIfEE"
    source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
    linker_set_key: "_ZTIN5test38ByeAgainIfEE"
    self_type: "_ZTIN5test38ByeAgainIfEE"
  }
  fields {
    referenced_type: "_ZTIf"
    field_offset: 0
    field_name: "foo_again"
    access: public_access
  }
  fields {
    referenced_type: "_ZTIf"
    field_offset: 32
    field_name: "bar_Again"
    access: public_access
  }
  template_info {
    elements {
      referenced_type: "_ZTIf"
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
    referenced_type: "_ZTI5Foo_s"
    source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
    linker_set_key: "_ZTI5Foo_s"
    self_type: "_ZTI5Foo_s"
  }
  underlying_type: "_ZTIj"
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
    name: "Hello::(anonymous enum at /development/vndk/tools/header-checker/tests/input/example1.h:17:3)"
    size: 4
    alignment: 4
    referenced_type: "_ZTIN5Hello2$AE"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "_ZTIN5Hello2$AE"
    self_type: "_ZTIN5Hello2$AE"
  }
  underlying_type: "_ZTIj"
  enum_fields {
    enum_field_value: 0
    name: "Hello::A"
  }
  enum_fields {
    enum_field_value: 1
    name: "Hello::B"
  }
  access: public_access
}
enum_types {
  type_info {
    name: "Hello::(anonymous enum at /development/vndk/tools/header-checker/tests/input/example1.h:18:3)"
    size: 4
    alignment: 4
    referenced_type: "_ZTIN5Hello2$CE"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "_ZTIN5Hello2$CE"
    self_type: "_ZTIN5Hello2$CE"
  }
  underlying_type: "_ZTIj"
  enum_fields {
    enum_field_value: 0
    name: "Hello::C"
  }
  enum_fields {
    enum_field_value: 1
    name: "Hello::D"
  }
  access: public_access
}
enum_types {
  type_info {
    name: "test3::Kind"
    size: 4
    alignment: 4
    referenced_type: "_ZTIN5test34KindE"
    source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
    linker_set_key: "_ZTIN5test34KindE"
    self_type: "_ZTIN5test34KindE"
  }
  underlying_type: "_ZTIj"
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
    referenced_type: "_ZTIN8CPPHello3BlaE"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "_ZTIN8CPPHello3BlaE"
    self_type: "_ZTIN8CPPHello3BlaE"
  }
  underlying_type: "_ZTIj"
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
    referenced_type: "_ZTI18ForwardDeclaration"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "_ZTIP18ForwardDeclaration"
    self_type: "_ZTIP18ForwardDeclaration"
  }
}
pointer_types {
  type_info {
    name: "List<int> *"
    size: 8
    alignment: 8
    referenced_type: "_ZTI4ListIiE"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "_ZTIP4ListIiE"
    self_type: "_ZTIP4ListIiE"
  }
}
pointer_types {
  type_info {
    name: "CPPHello *"
    size: 8
    alignment: 8
    referenced_type: "_ZTI8CPPHello"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "_ZTIP8CPPHello"
    self_type: "_ZTIP8CPPHello"
  }
}
pointer_types {
  type_info {
    name: "StackNode<int> *"
    size: 8
    alignment: 8
    referenced_type: "_ZTI9StackNodeIiE"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "_ZTIP9StackNodeIiE"
    self_type: "_ZTIP9StackNodeIiE"
  }
}
pointer_types {
  type_info {
    name: "const List<float>::_Node *"
    size: 8
    alignment: 8
    referenced_type: "_ZTIKN4ListIfE5_NodeE"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "_ZTIPKN4ListIfE5_NodeE"
    self_type: "_ZTIPKN4ListIfE5_NodeE"
  }
}
pointer_types {
  type_info {
    name: "const char *"
    size: 8
    alignment: 8
    referenced_type: "_ZTIKc"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "_ZTIPKc"
    self_type: "_ZTIPKc"
  }
}
pointer_types {
  type_info {
    name: "List<float>::_Node *"
    size: 8
    alignment: 8
    referenced_type: "_ZTIN4ListIfE5_NodeE"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "_ZTIPN4ListIfE5_NodeE"
    self_type: "_ZTIPN4ListIfE5_NodeE"
  }
}
pointer_types {
  type_info {
    name: "List<int>::_Node *"
    size: 8
    alignment: 8
    referenced_type: "_ZTIN4ListIiE5_NodeE"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "_ZTIPN4ListIiE5_NodeE"
    self_type: "_ZTIPN4ListIiE5_NodeE"
  }
}
pointer_types {
  type_info {
    name: "test2::HelloAgain *"
    size: 8
    alignment: 8
    referenced_type: "_ZTIN5test210HelloAgainE"
    source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
    linker_set_key: "_ZTIPN5test210HelloAgainE"
    self_type: "_ZTIPN5test210HelloAgainE"
  }
}
pointer_types {
  type_info {
    name: "float *"
    size: 8
    alignment: 8
    referenced_type: "_ZTIf"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "_ZTIPf"
    self_type: "_ZTIPf"
  }
}
pointer_types {
  type_info {
    name: "int *"
    size: 8
    alignment: 8
    referenced_type: "_ZTIi"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "_ZTIPi"
    self_type: "_ZTIPi"
  }
}
lvalue_reference_types {
  type_info {
    name: "const float &"
    size: 8
    alignment: 8
    referenced_type: "_ZTIKf"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "_ZTIRKf"
    self_type: "_ZTIRKf"
  }
}
lvalue_reference_types {
  type_info {
    name: "float &"
    size: 8
    alignment: 8
    referenced_type: "_ZTIf"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "_ZTIRf"
    self_type: "_ZTIRf"
  }
}
lvalue_reference_types {
  type_info {
    name: "int &"
    size: 8
    alignment: 8
    referenced_type: "_ZTIi"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "_ZTIRi"
    self_type: "_ZTIRi"
  }
}
builtin_types {
  type_info {
    name: "bool"
    size: 1
    alignment: 1
    referenced_type: "_ZTIb"
    source_file: ""
    linker_set_key: "_ZTIb"
    self_type: "_ZTIb"
  }
  is_unsigned: true
  is_integral: true
}
builtin_types {
  type_info {
    name: "char"
    size: 1
    alignment: 1
    referenced_type: "_ZTIc"
    source_file: ""
    linker_set_key: "_ZTIc"
    self_type: "_ZTIc"
  }
  is_unsigned: false
  is_integral: true
}
builtin_types {
  type_info {
    name: "double"
    size: 8
    alignment: 8
    referenced_type: "_ZTId"
    source_file: ""
    linker_set_key: "_ZTId"
    self_type: "_ZTId"
  }
  is_unsigned: false
  is_integral: false
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
builtin_types {
  type_info {
    name: "unsigned int"
    size: 4
    alignment: 4
    referenced_type: "_ZTIj"
    source_file: ""
    linker_set_key: "_ZTIj"
    self_type: "_ZTIj"
  }
  is_unsigned: true
  is_integral: true
}
builtin_types {
  type_info {
    name: "void"
    size: 0
    alignment: 0
    referenced_type: "_ZTIv"
    source_file: ""
    linker_set_key: "_ZTIv"
    self_type: "_ZTIv"
  }
  is_unsigned: false
  is_integral: false
}
builtin_types {
  type_info {
    name: "wchar_t"
    size: 4
    alignment: 4
    referenced_type: "_ZTIw"
    source_file: ""
    linker_set_key: "_ZTIw"
    self_type: "_ZTIw"
  }
  is_unsigned: false
  is_integral: true
}
qualified_types {
  type_info {
    name: "bool const[2]"
    size: 2
    alignment: 1
    referenced_type: "_ZTIA2_b"
    source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
    linker_set_key: "_ZTIA2_Kb"
    self_type: "_ZTIA2_Kb"
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
    referenced_type: "_ZTI8CPPHello"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "_ZTIK8CPPHello"
    self_type: "_ZTIK8CPPHello"
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
    referenced_type: "_ZTIN4ListIfE5_NodeE"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "_ZTIKN4ListIfE5_NodeE"
    self_type: "_ZTIKN4ListIfE5_NodeE"
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
    referenced_type: "_ZTIc"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "_ZTIKc"
    self_type: "_ZTIKc"
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
    referenced_type: "_ZTIf"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "_ZTIKf"
    self_type: "_ZTIKf"
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
    referenced_type: "_ZTIi"
    source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
    linker_set_key: "_ZTIKi"
    self_type: "_ZTIKi"
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
    referenced_type: "_ZTIb"
    source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
    linker_set_key: "_ZTIA2_b"
    self_type: "_ZTIA2_b"
  }
}
functions {
  return_type: "_ZTIi"
  function_name: "ListMangle"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  parameters {
    referenced_type: "_ZTIP4ListIiE"
    default_arg: false
    is_this_ptr: false
  }
  parameters {
    referenced_type: "_ZTIP9StackNodeIiE"
    default_arg: false
    is_this_ptr: false
  }
  linker_set_key: "_Z10ListMangleP4ListIiEP9StackNodeIiE"
  access: public_access
}
functions {
  return_type: "_ZTIv"
  function_name: "fooVariadic"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  parameters {
    referenced_type: "_ZTIRi"
    default_arg: false
    is_this_ptr: false
  }
  parameters {
    referenced_type: "_ZTIPi"
    default_arg: false
    is_this_ptr: false
  }
  linker_set_key: "_Z11fooVariadicRiPiz"
  access: public_access
}
functions {
  return_type: "_ZTI4ListIfE"
  function_name: "castInterface"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  parameters {
    referenced_type: "_ZTI4ListIfE"
    default_arg: false
    is_this_ptr: false
  }
  parameters {
    referenced_type: "_ZTIPKc"
    default_arg: false
    is_this_ptr: false
  }
  parameters {
    referenced_type: "_ZTIb"
    default_arg: false
    is_this_ptr: false
  }
  template_info {
    elements {
      referenced_type: "_ZTIf"
    }
    elements {
      referenced_type: "_ZTIf"
    }
    elements {
      referenced_type: "_ZTIf"
    }
    elements {
      referenced_type: "_ZTIf"
    }
  }
  linker_set_key: "_Z13castInterfaceIffffE4ListIT_ES0_IT0_EPKcb"
  access: public_access
}
functions {
  return_type: "_ZTIi"
  function_name: "boo"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  parameters {
    referenced_type: "_ZTIK8CPPHello"
    default_arg: false
    is_this_ptr: false
  }
  parameters {
    referenced_type: "_ZTIPi"
    default_arg: false
    is_this_ptr: false
  }
  parameters {
    referenced_type: "_ZTIPf"
    default_arg: false
    is_this_ptr: false
  }
  linker_set_key: "_Z3boo8CPPHelloPiPf"
  access: public_access
}
functions {
  return_type: "_ZTIv"
  function_name: "format"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  linker_set_key: "_Z6formatv"
  access: public_access
}
functions {
  return_type: "_ZTIv"
  function_name: "List<float>::_Node::PrivateNode"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  parameters {
    referenced_type: "_ZTIPN4ListIfE5_NodeE"
    default_arg: false
    is_this_ptr: true
  }
  linker_set_key: "_ZN4ListIfE5_Node11PrivateNodeEv"
  access: private_access
}
functions {
  return_type: "_ZTIRf"
  function_name: "List<float>::_Node::getRef"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  parameters {
    referenced_type: "_ZTIPN4ListIfE5_NodeE"
    default_arg: false
    is_this_ptr: true
  }
  linker_set_key: "_ZN4ListIfE5_Node6getRefEv"
  access: public_access
}
functions {
  return_type: "_ZTIv"
  function_name: "List<float>::_Node::_Node"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  parameters {
    referenced_type: "_ZTIPN4ListIfE5_NodeE"
    default_arg: false
    is_this_ptr: true
  }
  parameters {
    referenced_type: "_ZTIRKf"
    default_arg: false
    is_this_ptr: false
  }
  linker_set_key: "_ZN4ListIfE5_NodeC1ERKf"
  access: public_access
}
functions {
  return_type: "_ZTIv"
  function_name: "List<float>::_Node::_Node"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  parameters {
    referenced_type: "_ZTIPN4ListIfE5_NodeE"
    default_arg: false
    is_this_ptr: true
  }
  parameters {
    referenced_type: "_ZTIRKf"
    default_arg: false
    is_this_ptr: false
  }
  linker_set_key: "_ZN4ListIfE5_NodeC2ERKf"
  access: public_access
}
functions {
  return_type: "_ZTIv"
  function_name: "List<float>::_Node::~_Node"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  parameters {
    referenced_type: "_ZTIPN4ListIfE5_NodeE"
    default_arg: false
    is_this_ptr: true
  }
  linker_set_key: "_ZN4ListIfE5_NodeD1Ev"
  access: public_access
}
functions {
  return_type: "_ZTIv"
  function_name: "List<float>::_Node::~_Node"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  parameters {
    referenced_type: "_ZTIPN4ListIfE5_NodeE"
    default_arg: false
    is_this_ptr: true
  }
  linker_set_key: "_ZN4ListIfE5_NodeD2Ev"
  access: public_access
}
functions {
  return_type: "_ZTIv"
  function_name: "test2::HelloAgain::~HelloAgain"
  source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
  parameters {
    referenced_type: "_ZTIPN5test210HelloAgainE"
    default_arg: false
    is_this_ptr: true
  }
  linker_set_key: "_ZN5test210HelloAgainD0Ev"
  access: public_access
}
functions {
  return_type: "_ZTIv"
  function_name: "test2::HelloAgain::~HelloAgain"
  source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
  parameters {
    referenced_type: "_ZTIPN5test210HelloAgainE"
    default_arg: false
    is_this_ptr: true
  }
  linker_set_key: "_ZN5test210HelloAgainD1Ev"
  access: public_access
}
functions {
  return_type: "_ZTIv"
  function_name: "test2::HelloAgain::~HelloAgain"
  source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
  parameters {
    referenced_type: "_ZTIPN5test210HelloAgainE"
    default_arg: false
    is_this_ptr: true
  }
  linker_set_key: "_ZN5test210HelloAgainD2Ev"
  access: public_access
}
functions {
  return_type: "_ZTIb"
  function_name: "test3::End"
  source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
  parameters {
    referenced_type: "_ZTIf"
    default_arg: true
    is_this_ptr: false
  }
  linker_set_key: "_ZN5test33EndEf"
  access: public_access
}
functions {
  return_type: "_ZTIi"
  function_name: "CPPHello::again"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  parameters {
    referenced_type: "_ZTIP8CPPHello"
    default_arg: false
    is_this_ptr: true
  }
  linker_set_key: "_ZN8CPPHello5againEv"
  access: public_access
}
functions {
  return_type: "_ZTIi"
  function_name: "CPPHello::test_enum"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  parameters {
    referenced_type: "_ZTIP8CPPHello"
    default_arg: false
    is_this_ptr: true
  }
  linker_set_key: "_ZN8CPPHello9test_enumEv"
  access: public_access
}
functions {
  return_type: "_ZTIv"
  function_name: "CPPHello::CPPHello"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  parameters {
    referenced_type: "_ZTIP8CPPHello"
    default_arg: false
    is_this_ptr: true
  }
  linker_set_key: "_ZN8CPPHelloC1Ev"
  access: public_access
}
functions {
  return_type: "_ZTIv"
  function_name: "CPPHello::CPPHello"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  parameters {
    referenced_type: "_ZTIP8CPPHello"
    default_arg: false
    is_this_ptr: true
  }
  linker_set_key: "_ZN8CPPHelloC2Ev"
  access: public_access
}
functions {
  return_type: "_ZTIRKf"
  function_name: "List<float>::_Node::getRef"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  parameters {
    referenced_type: "_ZTIPKN4ListIfE5_NodeE"
    default_arg: false
    is_this_ptr: true
  }
  linker_set_key: "_ZNK4ListIfE5_Node6getRefEv"
  access: public_access
}
functions {
  return_type: "_ZTIi"
  function_name: "uses_forward_decl"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  parameters {
    referenced_type: "_ZTIP18ForwardDeclaration"
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
  referenced_type: "_ZTIA2_Kb"
  access: public_access
}
global_vars {
  name: "test2::HelloAgain::hello_forever"
  source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
  linker_set_key: "_ZN5test210HelloAgain13hello_foreverE"
  referenced_type: "_ZTIi"
  access: public_access
}
global_vars {
  name: "test3::double_bye"
  source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
  linker_set_key: "_ZN5test310double_byeE"
  referenced_type: "_ZTIN5test38ByeAgainIdEE"
  access: public_access
}
global_vars {
  name: "test3::ByeAgain<float>::foo_forever"
  source_file: "/development/vndk/tools/header-checker/tests/input/example2.h"
  linker_set_key: "_ZN5test38ByeAgainIfE11foo_foreverE"
  referenced_type: "_ZTIi"
  access: public_access
}
global_vars {
  name: "float_list_test"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  linker_set_key: "float_list_test"
  referenced_type: "_ZTI4ListIfE"
  access: public_access
}
global_vars {
  name: "int_list_test"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  linker_set_key: "int_list_test"
  referenced_type: "_ZTI4ListIiE"
  access: public_access
}
global_vars {
  name: "node"
  source_file: "/development/vndk/tools/header-checker/tests/input/example1.h"
  linker_set_key: "node"
  referenced_type: "_ZTIN4ListIfE5_NodeE"
  access: public_access
}
