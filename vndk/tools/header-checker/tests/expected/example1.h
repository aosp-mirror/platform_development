records {
  basic_abi {
    type_abi {
      name: "HiddenBase"
      size: 8
      alignment: 4
    }
    name: "HiddenBase"
    access: public_access
    linker_set_key: "HiddenBase"
  }
  fields {
    basic_abi {
      type_abi {
        name: "int"
        size: 4
        alignment: 4
      }
      name: "hide"
      access: private_access
    }
  }
  fields {
    basic_abi {
      type_abi {
        name: "float"
        size: 4
        alignment: 4
      }
      name: "seek"
      access: private_access
    }
  }
  source_file: "./input/example3.h"
  mangled_record_name: "HiddenBase"
}
records {
  basic_abi {
    type_abi {
      name: "test2::HelloAgain"
      size: 40
      alignment: 8
    }
    name: "test2::HelloAgain"
    access: public_access
    linker_set_key: "test2::HelloAgain"
  }
  fields {
    basic_abi {
      type_abi {
        name: "std::vector<test2::HelloAgain *, std::allocator<test2::HelloAgain *> >"
        size: 24
        alignment: 8
      }
      name: "foo_again"
      access: public_access
    }
  }
  fields {
    basic_abi {
      type_abi {
        name: "int"
        size: 4
        alignment: 4
      }
      name: "bar_again"
      access: public_access
    }
  }
  source_file: "./input/example2.h"
  mangled_record_name: "test2::HelloAgain"
  vtable_layout {
    vtable_components {
      kind: OffsetToTop
      mangled_component_name: ""
      value: 0
    }
    vtable_components {
      kind: RTTI
      mangled_component_name: "test2::HelloAgain"
      value: 0
    }
    vtable_components {
      kind: FunctionPointer
      mangled_component_name: "_ZN5test210HelloAgain5againEv"
      value: 0
    }
  }
}
records {
  basic_abi {
    type_abi {
      name: "test3::ByeAgain<double>"
      size: 16
      alignment: 8
    }
    name: "test3::ByeAgain"
    access: public_access
    linker_set_key: "test3::ByeAgain<double>"
  }
  fields {
    basic_abi {
      type_abi {
        name: "double"
        size: 8
        alignment: 8
      }
      name: "foo_again"
      access: public_access
    }
  }
  fields {
    basic_abi {
      type_abi {
        name: "int"
        size: 4
        alignment: 4
      }
      name: "bar_again"
      access: public_access
    }
  }
  source_file: "./input/example2.h"
  template_info {
    elements {
      basic_abi {
        type_abi {
          name: "double"
        }
        linker_set_key: "double"
      }
    }
  }
  mangled_record_name: "test3::ByeAgain<double>"
}
records {
  basic_abi {
    type_abi {
      name: "test3::ByeAgain<float>"
      size: 8
      alignment: 4
    }
    name: "test3::ByeAgain"
    access: public_access
    linker_set_key: "test3::ByeAgain<float>"
  }
  fields {
    basic_abi {
      type_abi {
        name: "float"
        size: 4
        alignment: 4
      }
      name: "foo_again"
      access: public_access
    }
  }
  fields {
    basic_abi {
      type_abi {
        name: "float"
        size: 4
        alignment: 4
      }
      name: "bar_Again"
      access: public_access
    }
  }
  source_file: "./input/example2.h"
  template_info {
    elements {
      basic_abi {
        type_abi {
          name: "float"
        }
        linker_set_key: "float"
      }
    }
  }
  mangled_record_name: "test3::ByeAgain<float>"
}
records {
  basic_abi {
    type_abi {
      name: "test3::Outer"
      size: 4
      alignment: 4
    }
    name: "test3::Outer"
    access: public_access
    linker_set_key: "test3::Outer"
  }
  fields {
    basic_abi {
      type_abi {
        name: "int"
        size: 4
        alignment: 4
      }
      name: "a"
      access: public_access
    }
  }
  source_file: "./input/example2.h"
  mangled_record_name: "test3::Outer"
}
records {
  basic_abi {
    type_abi {
      name: "test3::Outer::Inner"
      size: 4
      alignment: 4
    }
    name: "test3::Outer::Inner"
    access: private_access
    linker_set_key: "test3::Outer::Inner"
  }
  fields {
    basic_abi {
      type_abi {
        name: "int"
        size: 4
        alignment: 4
      }
      name: "b"
      access: private_access
    }
  }
  source_file: "./input/example2.h"
  mangled_record_name: "test3::Outer::Inner"
}
records {
  basic_abi {
    type_abi {
      name: "Hello"
      size: 8
      alignment: 4
    }
    name: "Hello"
    access: public_access
    linker_set_key: "Hello"
  }
  fields {
    basic_abi {
      type_abi {
        name: "int"
        size: 4
        alignment: 4
      }
      name: "foo"
      access: public_access
    }
  }
  fields {
    basic_abi {
      type_abi {
        name: "int"
        size: 4
        alignment: 4
      }
      name: "bar"
      access: public_access
    }
  }
  source_file: "./input/example1.h"
  mangled_record_name: "Hello"
}
records {
  basic_abi {
    type_abi {
      name: "CPPHello"
      size: 56
      alignment: 8
    }
    name: "CPPHello"
    access: public_access
    linker_set_key: "CPPHello"
  }
  fields {
    basic_abi {
      type_abi {
        name: "const int"
        size: 4
        alignment: 4
      }
      name: "cpp_foo"
      access: public_access
    }
  }
  fields {
    basic_abi {
      type_abi {
        name: "const float"
        size: 4
        alignment: 4
      }
      name: "cpp_bar"
      access: public_access
    }
  }
  base_specifiers {
    basic_abi {
      type_abi {
        name: "test2::HelloAgain"
      }
      name: ""
      access: private_access
    }
    is_virtual: false
  }
  base_specifiers {
    basic_abi {
      type_abi {
        name: "test3::ByeAgain<float>"
      }
      name: ""
      access: public_access
    }
    is_virtual: false
  }
  source_file: "./input/example1.h"
  mangled_record_name: "CPPHello"
  vtable_layout {
    vtable_components {
      kind: OffsetToTop
      mangled_component_name: ""
      value: 0
    }
    vtable_components {
      kind: RTTI
      mangled_component_name: "CPPHello"
      value: 0
    }
    vtable_components {
      kind: FunctionPointer
      mangled_component_name: "_ZN8CPPHello5againEv"
      value: 0
    }
  }
}
records {
  basic_abi {
    type_abi {
      name: "List<float>"
      size: 8
      alignment: 8
    }
    name: "List"
    access: public_access
    linker_set_key: "List<float>"
  }
  fields {
    basic_abi {
      type_abi {
        name: "List<float>::_Node *"
        size: 8
        alignment: 8
      }
      name: "middle"
      access: protected_access
    }
  }
  source_file: "./input/example1.h"
  template_info {
    elements {
      basic_abi {
        type_abi {
          name: "float"
        }
        linker_set_key: "float"
      }
    }
  }
  mangled_record_name: "List<float>"
}
records {
  basic_abi {
    type_abi {
      name: "List<int>"
      size: 8
      alignment: 8
    }
    name: "List"
    access: public_access
    linker_set_key: "List<int>"
  }
  fields {
    basic_abi {
      type_abi {
        name: "List<int>::_Node *"
        size: 8
        alignment: 8
      }
      name: "middle"
      access: protected_access
    }
  }
  source_file: "./input/example1.h"
  template_info {
    elements {
      basic_abi {
        type_abi {
          name: "int"
        }
        linker_set_key: "int"
      }
    }
  }
  mangled_record_name: "List<int>"
}
functions {
  basic_abi {
    type_abi {
      name: "int"
      size: 4
      alignment: 4
    }
    name: "test2::HelloAgain::again"
    access: public_access
    linker_set_key: "_ZN5test210HelloAgain5againEv"
  }
  mangled_function_name: "_ZN5test210HelloAgain5againEv"
  source_file: "./input/example2.h"
}
functions {
  basic_abi {
    type_abi {
      name: "double"
      size: 8
      alignment: 8
    }
    name: "test3::ByeAgain<double>::method_foo"
    access: public_access
    linker_set_key: "_ZN5test38ByeAgainIdE10method_fooEd"
  }
  mangled_function_name: "_ZN5test38ByeAgainIdE10method_fooEd"
  source_file: "./input/example2.h"
  parameters {
    basic_abi {
      type_abi {
        name: "double"
        size: 8
        alignment: 8
      }
      name: ""
      access: public_access
      linker_set_key: "false"
    }
    default_arg: false
  }
}
functions {
  basic_abi {
    type_abi {
      name: "float"
      size: 4
      alignment: 4
    }
    name: "test3::ByeAgain<float>::method_foo"
    access: public_access
    linker_set_key: "_ZN5test38ByeAgainIfE10method_fooEi"
  }
  mangled_function_name: "_ZN5test38ByeAgainIfE10method_fooEi"
  source_file: "./input/example2.h"
  parameters {
    basic_abi {
      type_abi {
        name: "int"
        size: 4
        alignment: 4
      }
      name: ""
      access: public_access
      linker_set_key: "false"
    }
    default_arg: false
  }
}
functions {
  basic_abi {
    type_abi {
      name: "bool"
      size: 1
      alignment: 1
    }
    name: "test3::Begin"
    access: public_access
    linker_set_key: "_ZN5test35BeginIfiEEbT_T0_i"
  }
  mangled_function_name: "_ZN5test35BeginIfiEEbT_T0_i"
  source_file: "./input/example2.h"
  parameters {
    basic_abi {
      type_abi {
        name: "float"
        size: 4
        alignment: 4
      }
      name: "arg1"
      access: public_access
      linker_set_key: "false"
    }
    default_arg: false
  }
  parameters {
    basic_abi {
      type_abi {
        name: "int"
        size: 4
        alignment: 4
      }
      name: "arg2"
      access: public_access
      linker_set_key: "false"
    }
    default_arg: false
  }
  parameters {
    basic_abi {
      type_abi {
        name: "int"
        size: 4
        alignment: 4
      }
      name: "c"
      access: public_access
      linker_set_key: "false"
    }
    default_arg: false
  }
  template_info {
    elements {
      basic_abi {
        type_abi {
          name: "float"
        }
        linker_set_key: "float"
      }
    }
    elements {
      basic_abi {
        type_abi {
          name: "int"
        }
        linker_set_key: "int"
      }
    }
  }
}
functions {
  basic_abi {
    type_abi {
      name: "bool"
      size: 1
      alignment: 1
    }
    name: "test3::End"
    access: public_access
    linker_set_key: "_ZN5test33EndEf"
  }
  mangled_function_name: "_ZN5test33EndEf"
  source_file: "./input/example2.h"
  parameters {
    basic_abi {
      type_abi {
        name: "float"
        size: 4
        alignment: 4
      }
      name: "arg"
      access: public_access
      linker_set_key: "true"
    }
    default_arg: true
  }
}
functions {
  basic_abi {
    type_abi {
      name: "std::vector<int *, std::allocator<int *> >"
    }
    name: "test3::Dummy"
    access: public_access
    linker_set_key: "_ZN5test35DummyEi"
  }
  mangled_function_name: "_ZN5test35DummyEi"
  source_file: "./input/example2.h"
  parameters {
    basic_abi {
      type_abi {
        name: "int"
        size: 4
        alignment: 4
      }
      name: "t"
      access: public_access
      linker_set_key: "false"
    }
    default_arg: false
  }
}
functions {
  basic_abi {
    type_abi {
      name: "int"
      size: 4
      alignment: 4
    }
    name: "CPPHello::again"
    access: public_access
    linker_set_key: "_ZN8CPPHello5againEv"
  }
  mangled_function_name: "_ZN8CPPHello5againEv"
  source_file: "./input/example1.h"
}
functions {
  basic_abi {
    type_abi {
      name: "void"
    }
    name: "CPPHello::CPPHello"
    access: public_access
    linker_set_key: "_ZN8CPPHelloC2Ev"
  }
  mangled_function_name: "_ZN8CPPHelloC2Ev"
  source_file: "./input/example1.h"
}
functions {
  basic_abi {
    type_abi {
      name: "void"
    }
    name: "CPPHello::CPPHello"
    access: public_access
    linker_set_key: "_ZN8CPPHelloC1Ev"
  }
  mangled_function_name: "_ZN8CPPHelloC1Ev"
  source_file: "./input/example1.h"
}
enums {
  basic_abi {
    type_abi {
      name: "unsigned int"
      size: 4
      alignment: 4
    }
    name: "Foo_s"
    access: public_access
    linker_set_key: "Foo_s"
  }
  enum_fields {
    basic_abi {
      type_abi {
        name: "Foo_s"
        size: 4
        alignment: 4
      }
      name: "Foo_s::foosball"
      access: public_access
      linker_set_key: "10"
    }
    enum_field_value: 10
  }
  enum_fields {
    basic_abi {
      type_abi {
        name: "Foo_s"
        size: 4
        alignment: 4
      }
      name: "Foo_s::foosbat"
      access: public_access
      linker_set_key: "11"
    }
    enum_field_value: 11
  }
  source_file: "./input/example2.h"
}
enums {
  basic_abi {
    type_abi {
      name: "unsigned int"
      size: 4
      alignment: 4
    }
    name: "test3::Kind"
    access: public_access
    linker_set_key: "test3::Kind"
  }
  enum_fields {
    basic_abi {
      type_abi {
        name: "test3::Kind"
        size: 4
        alignment: 4
      }
      name: "test3::Kind::kind1"
      access: public_access
      linker_set_key: "24"
    }
    enum_field_value: 24
  }
  enum_fields {
    basic_abi {
      type_abi {
        name: "test3::Kind"
        size: 4
        alignment: 4
      }
      name: "test3::Kind::kind2"
      access: public_access
      linker_set_key: "2312"
    }
    enum_field_value: 2312
  }
  source_file: "./input/example2.h"
}
global_vars {
  basic_abi {
    type_abi {
      name: "int"
      size: 4
      alignment: 4
    }
    name: "test2::HelloAgain::hello_forever"
    access: public_access
    linker_set_key: "_ZN5test210HelloAgain13hello_foreverE"
  }
  source_file: "./input/example2.h"
}
global_vars {
  basic_abi {
    type_abi {
      name: "bool const[2]"
      size: 2
      alignment: 1
    }
    name: "__test_var"
    access: public_access
    linker_set_key: "_ZL10__test_var"
  }
  source_file: "./input/example2.h"
}
global_vars {
  basic_abi {
    type_abi {
      name: "int"
      size: 4
      alignment: 4
    }
    name: "test3::ByeAgain<float>::foo_forever"
    access: public_access
    linker_set_key: "_ZN5test38ByeAgainIfE11foo_foreverE"
  }
  source_file: "./input/example2.h"
}
global_vars {
  basic_abi {
    type_abi {
      name: "test3::ByeAgain<double>"
      size: 16
      alignment: 8
    }
    name: "test3::double_bye"
    access: public_access
    linker_set_key: "_ZN5test310double_byeE"
  }
  source_file: "./input/example2.h"
}
global_vars {
  basic_abi {
    type_abi {
      name: "List<float>"
      size: 8
      alignment: 8
    }
    name: "float_list_test"
    access: public_access
    linker_set_key: "float_list_test"
  }
  source_file: "./input/example1.h"
}
global_vars {
  basic_abi {
    type_abi {
      name: "List<int>"
      size: 8
      alignment: 8
    }
    name: "int_list_test"
    access: public_access
    linker_set_key: "int_list_test"
  }
  source_file: "./input/example1.h"
}
