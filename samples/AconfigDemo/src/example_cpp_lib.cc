#include "example_cpp_lib.h"
#include <string>
#include <com_example_android_aconfig_demo_flags.h>

namespace demo_flags = com::example::android::aconfig::demo::flags;

// use static methods interface
static std::string get_flag_via_static_interface() {
  return std::string("flag value : ") +
      (demo_flags::append_static_content() ? "true" : "false");
}

// use flag provider for injection interface
static std::string get_flag_via_injection_interface(
  demo_flags::flag_provider_interface* provider) {
  return std::string("flag value : ") +
      ((provider->append_injected_content()) ? "true" : "false");
}

jstring Java_com_example_android_aconfig_demo_AconfigDemoActivity_printCFlag(
    JNIEnv* env,
    jobject thiz) {
  auto result = std::string("flag name : append_static_content\n");
  result += "use pattern : static method\n";
  result += get_flag_via_static_interface();

  result += "\n\n";

  result += "flag name : append_injected_content\n";
  result += "use pattern : injection\n";
  result += get_flag_via_injection_interface(demo_flags::provider_.get());

  return env->NewStringUTF(result.c_str());
}
