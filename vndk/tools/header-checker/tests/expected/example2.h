classes {
  fields {
    field_name: "foo_again"
    field_type: "std::unique_ptr<test2::HelloAgain, std::default_delete<test2::HelloAgain> >"
    access: "public"
  }
  fields {
    field_name: "bar_again"
    field_type: "int"
    access: "public"
  }
  fully_qualified_name: "test2::HelloAgain"
  source_file: "./input/example2.h"
  access: "public"
}
classes {
  fields {
    field_name: "foo_again"
    field_type: "T"
    access: "public"
  }
  fields {
    field_name: "bar_again"
    field_type: "int"
    access: "public"
  }
  fully_qualified_name: "test3::ByeAgain"
  source_file: "./input/example2.h"
  access: "public"
}
classes {
  fields {
    field_name: "foo_again"
    field_type: "double"
    access: "public"
  }
  fields {
    field_name: "bar_again"
    field_type: "int"
    access: "public"
  }
  fully_qualified_name: "test3::ByeAgain"
  source_file: "./input/example2.h"
  template_info {
    template_parameters {
      field_type: "double"
    }
  }
  access: "public"
}
classes {
  fields {
    field_name: "foo_again"
    field_type: "float"
    access: "public"
  }
  fields {
    field_name: "bar_Again"
    field_type: "float"
    access: "public"
  }
  fully_qualified_name: "test3::ByeAgain"
  source_file: "./input/example2.h"
  template_info {
    template_parameters {
      field_type: "float"
    }
  }
  access: "public"
}
classes {
  fields {
    field_name: "a"
    field_type: "int"
    access: "public"
  }
  fully_qualified_name: "test3::Outer"
  source_file: "./input/example2.h"
  access: "public"
}
classes {
  fields {
    field_name: "b"
    field_type: "int"
    access: "private"
  }
  fully_qualified_name: "test3::Outer::Inner"
  source_file: "./input/example2.h"
  access: "private"
}
functions {
  function_name: "test3::ByeAgain::method_foo"
  mangled_function_name: "method_foo_ZN5test38ByeAgain10method_fooET_"
  source_file: "./input/example2.h"
  parameters {
    field_name: ""
    field_type: "T"
    default_arg: false
  }
  return_type: "T"
  access: "public"
  template_kind: 0
}
functions {
  function_name: "test3::ByeAgain<double>::method_foo"
  mangled_function_name: "method_foo_ZN5test38ByeAgainIdE10method_fooEd"
  source_file: "./input/example2.h"
  parameters {
    field_name: ""
    field_type: "double"
    default_arg: false
  }
  return_type: "double"
  access: "public"
  template_kind: 2
}
functions {
  function_name: "test3::ByeAgain<float>::method_foo"
  mangled_function_name: "method_foo_ZN5test38ByeAgainIfE10method_fooEi"
  source_file: "./input/example2.h"
  parameters {
    field_name: ""
    field_type: "int"
    default_arg: false
  }
  return_type: "float"
  access: "public"
  template_kind: 0
}
functions {
  function_name: "test3::Begin"
  mangled_function_name: "Begin_ZN5test35BeginET_T0_"
  source_file: "./input/example2.h"
  parameters {
    field_name: "arg1"
    field_type: "T1"
    default_arg: false
  }
  parameters {
    field_name: "arg2"
    field_type: "T2"
    default_arg: false
  }
  return_type: "bool"
  access: "public"
  template_kind: 1
  template_info {
    template_parameters {
      field_name: "T1"
    }
    template_parameters {
      field_name: "T2"
    }
  }
}
functions {
  function_name: "test3::Begin"
  mangled_function_name: "Begin_ZN5test35BeginIfiEEbT_T0_"
  source_file: "./input/example2.h"
  parameters {
    field_name: "arg1"
    field_type: "float"
    default_arg: false
  }
  parameters {
    field_name: "arg2"
    field_type: "int"
    default_arg: false
  }
  return_type: "bool"
  access: "public"
  template_kind: 3
  template_info {
    template_parameters {
      field_type: "float"
    }
    template_parameters {
      field_type: "int"
    }
  }
}
functions {
  function_name: "test3::Begin"
  mangled_function_name: "Begin_ZN5test35BeginIifEEbT_T0_"
  source_file: "./input/example2.h"
  parameters {
    field_name: "a"
    field_type: "int"
    default_arg: false
  }
  parameters {
    field_name: "b"
    field_type: "float"
    default_arg: false
  }
  return_type: "bool"
  access: "public"
  template_kind: 3
  template_info {
    template_parameters {
      field_type: "int"
    }
    template_parameters {
      field_type: "float"
    }
  }
}
functions {
  function_name: "test3::End"
  mangled_function_name: "End_ZN5test33EndEf"
  source_file: "./input/example2.h"
  parameters {
    field_name: "arg"
    field_type: "float"
    default_arg: true
  }
  return_type: "bool"
  access: "public"
  template_kind: 0
}
enums {
  enum_name: "Foo_s"
  enum_type: "unsigned int"
  enum_fields {
    enum_field_name: "Foo_s::foosball"
    enum_field_value: 10
  }
  enum_fields {
    enum_field_name: "Foo_s::foosbat"
    enum_field_value: 11
  }
}
enums {
  enum_name: "test3::Kind"
  enum_type: "unsigned int"
  enum_fields {
    enum_field_name: "test3::Kind::kind1"
    enum_field_value: 24
  }
  enum_fields {
    enum_field_name: "test3::Kind::kind2"
    enum_field_value: 2312
  }
}
