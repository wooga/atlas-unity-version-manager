extern crate jni;
#[macro_use]
extern crate uvm_core;
#[macro_use]
extern crate error_chain;

use jni::JNIEnv;
use jni::objects::{JClass, JObject, JValue};
use jni::sys::{jstring};
use std::path::{Path,PathBuf};

mod error {
    use jni;
    use uvm_core;
    use std;

    error_chain! {
        types {
            UvmJniError, UvmJniErrorKind, ResultExt, UvmJniResult;
        }

        links {
            Another(jni::errors::Error, jni::errors::ErrorKind) #[cfg(unix)];
        }

        foreign_links {
            Uvm(uvm_core::error::UvmError);
            Io(std::io::Error) #[cfg(unix)];
        }
    }
}

mod jni_utils {
    use super::*;

    /// Converts a `java.io.File` `JObject` into a `PathBuf`
    pub fn get_path(env: &JNIEnv, path: JObject) -> error::UvmJniResult<PathBuf> {
        env.call_method(path, "getPath", "()Ljava/lang/String;", &[])
            .and_then(JValue::l)
            .and_then(|object| env.get_string(object.into()))
            .map(|p| Path::new(&String::from(p)).to_path_buf())
            .map_err(|e| e.into())
    }
}

#[no_mangle]
#[allow(non_snake_case)]
pub extern "system" fn Java_net_wooga_uvm_UnityVersionManager_uvmVersion(env: JNIEnv, _class: JClass) -> jstring {
    env.new_string(cargo_version!())
        .map(|s| s.into_inner() )
        .unwrap_or_else(|_| JObject::null().into_inner())
}

#[no_mangle]
#[allow(non_snake_case)]
pub extern "system" fn Java_net_wooga_uvm_UnityVersionManager_detectProjectVersion<'a>(env: JNIEnv, _class: JClass, path: JObject) -> jstring {
    jni_utils::get_path(&env,path)
        .and_then(|path| {
            uvm_core::dectect_project_version(&path,Some(true)).map_err(|e| e.into())
        })
        .and_then(|version| {
            env.new_string(version.to_string()).map_err(|e| e.into())
        })
        .map(|s| s.into_inner() )
        .unwrap_or_else(|_| {
            JObject::null().into_inner()
        })
}