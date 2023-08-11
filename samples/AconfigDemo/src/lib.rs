//! example rust crate to be used in AconfigDemoActivity app

use jni::objects::JClass;
use jni::sys::jstring;
use jni::JNIEnv;

/// get flag value via static interface
pub fn get_flag_via_static_interface() -> String {
    format!(
        "flag value: {}",
        if aconfig_demo_flags_rust::append_static_content() { "true" } else { "false" }
    )
}

/// get flag value via injection
pub fn get_flag_via_injection_interface(
    provider: &aconfig_demo_flags_rust::FlagProvider,
) -> String {
    format!("flag value: {}", if provider.append_injected_content() { "true" } else { "false" })
}

/// printRustFlag function
#[no_mangle]
#[allow(unused)]
pub extern "system" fn Java_com_example_android_aconfig_demo_AconfigDemoActivity_printRustFlag<
    'local,
>(
    mut env: JNIEnv<'local>,
    class: JClass<'local>,
) -> jstring {
    let mut result = String::new();

    result.push_str("flag name : append_static_content\n");
    result.push_str("use pattern : static method\n");
    result.push_str(&get_flag_via_static_interface());

    result.push_str("\n\n");

    result.push_str("flag name : append_injected_content\n");
    result.push_str("use pattern : injection\n");
    result.push_str(&get_flag_via_injection_interface(&aconfig_demo_flags_rust::PROVIDER));

    let output = env.new_string(result).expect("Couldn't create java string!");

    output.into_raw()
}
