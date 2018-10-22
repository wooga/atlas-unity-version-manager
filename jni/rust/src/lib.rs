extern crate jni;
#[macro_use]
extern crate uvm_core;

use jni::JNIEnv;
use jni::objects::{JClass, JObject, };
use jni::sys::{jstring};

#[no_mangle]
#[allow(non_snake_case)]
pub extern "system" fn Java_net_wooga_uvm_UnityVersionManager_uvmVersion(env: JNIEnv, _class: JClass) -> jstring {
    env.new_string(cargo_version!())
        .map(|s| s.into_inner() )
        .unwrap_or_else(|_| JObject::null().into_inner())
}
