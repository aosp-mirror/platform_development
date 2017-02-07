classes {
  fields {
    field_name: "foo"
    field_type: "int"
    access: "public"
  }
  fields {
    field_name: "bar"
    field_type: "int"
    access: "public"
  }
  fully_qualified_name: "Hello"
  source_file: "./input/example1.h"
  access: "public"
}
classes {
  fields {
    field_name: "cpp_foo"
    field_type: "const int"
    access: "public"
  }
  fields {
    field_name: "cpp_bar"
    field_type: "const float"
    access: "public"
  }
  base_specifiers {
    fully_qualified_name: "test2::HelloAgain"
    access: "private"
    is_virtual: false
  }
  base_specifiers {
    fully_qualified_name: "test3::ByeAgain<float>"
    access: "public"
    is_virtual: false
  }
  fully_qualified_name: "CPPHello"
  source_file: "./input/example1.h"
  access: "public"
}
classes {
  fields {
    field_name: "value_"
    field_type: "T"
    access: "public"
  }
  fields {
    field_name: "next_"
    field_type: "StackNode<T> *"
    access: "public"
  }
  fully_qualified_name: "StackNode"
  source_file: "./input/example1.h"
  access: "public"
}
classes {
  fields {
    field_name: "head_"
    field_type: "StackNode<T> *"
    access: "private"
  }
  fully_qualified_name: "Stack"
  source_file: "./input/example1.h"
  access: "public"
}
functions {
  function_name: "CPPHello::CPPHello"
  mangled_function_name: "_ZN8CPPHelloC1Ev"
  source_file: "./input/example1.h"
  return_type: "void"
  access: "public"
  template_kind: 0
}
functions {
  function_name: "StackNode::StackNode<T>"
  mangled_function_name: "_ZN9StackNodeC1ET_P9StackNodeIS0_E"
  source_file: "./input/example1.h"
  parameters {
    field_name: "t"
    field_type: "T"
    default_arg: false
  }
  parameters {
    field_name: "next"
    field_type: "StackNode<T> *"
    default_arg: true
  }
  return_type: "void"
  access: "public"
  template_kind: 0
}
functions {
  function_name: "Stack::Stack<T>"
  mangled_function_name: "_ZN5StackC1Ev"
  source_file: "./input/example1.h"
  return_type: "void"
  access: "public"
  template_kind: 0
}
functions {
  function_name: "Stack::push"
  mangled_function_name: "push_ZN5Stack4pushET_"
  source_file: "./input/example1.h"
  parameters {
    field_name: "t"
    field_type: "T"
    default_arg: false
  }
  return_type: "void"
  access: "public"
  template_kind: 0
}
functions {
  function_name: "Stack::pop"
  mangled_function_name: "pop_ZN5Stack3popEv"
  source_file: "./input/example1.h"
  return_type: "T"
  access: "public"
  template_kind: 0
}
functions {
  function_name: "Global_Foo"
  mangled_function_name: "Global_Foo_Z10Global_Fooi"
  source_file: "./input/example1.h"
  parameters {
    field_name: "global_bar"
    field_type: "int"
    default_arg: false
  }
  return_type: "const volatile int"
  access: "public"
  template_kind: 0
}
