extern crate jni;
#[macro_use]
extern crate uvm_core;
#[macro_use]
extern crate error_chain;

use uvm_core::Version;
use jni::JNIEnv;
use jni::objects::{JClass, JObject, JValue, JString};
use jni::sys::{jstring, jobject, jobjectArray, jsize};
use std::path::{Path,PathBuf};
use std::str::FromStr;

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
            ParseVersionError(uvm_core::unity::ParseVersionError);
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

    pub fn get_file<'a,'b>(env: &'a JNIEnv<'b>, path: &'b Path) -> error::UvmJniResult<JObject<'b>> {
        let class = env.find_class("java/io/File")?;
        let path_string = env.new_string(path.to_string_lossy())?;
        let object = env.new_object(class, "(Ljava/lang/String;)V", &[JValue::Object(path_string.into())])?;
        Ok(object)
    }
}

#[no_mangle]
#[allow(non_snake_case)]
pub extern "system" fn Java_net_wooga_uvm_UnityVersionManager_uvmVersion(env: JNIEnv, _class: JClass) -> jstring {
    env.new_string(cargo_version!())
        .map(|s| s.into_inner() )
        .unwrap_or_else(|_| JObject::null().into_inner())
}

fn list_installations(env: &JNIEnv) -> error::UvmJniResult<jobjectArray> {
    let installations = uvm_core::list_all_installations()?;
    let installations:Vec<uvm_core::Installation> = installations.collect();
    let installation_class = env.find_class("net/wooga/uvm/Installation")?;

    let output = env.new_object_array(installations.len() as jsize,installation_class,JObject::null())?;
    for (i, installation) in installations.iter().enumerate() {
        let install_path = jni_utils::get_file(&env, installation.path())?;
        let install_version = env.new_string(installation.version().to_string())?;
        let native_installation = env.new_object(installation_class, "(Ljava/io/File;Ljava/lang/String;)V", &[JValue::Object(install_path.into()), JValue::Object(install_version.into())])?;
        env.set_object_array_element(output, i as jsize, native_installation)?;
    }

    Ok(output)
}

#[no_mangle]
#[allow(non_snake_case)]
pub extern "system" fn Java_net_wooga_uvm_UnityVersionManager_listInstallations(env: JNIEnv, _class: JClass) -> jobjectArray {
    list_installations(&env)
        .unwrap_or_else(|_| {
            JObject::null().into_inner()
        })
}

#[no_mangle]
#[allow(non_snake_case)]
pub extern "system" fn Java_net_wooga_uvm_UnityVersionManager_detectProjectVersion(env: JNIEnv, _class: JClass, path: JObject) -> jstring {
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

fn locate_installation(env: &JNIEnv, version: JString) -> error::UvmJniResult<jobject> {
    let version_string = env.get_string(version)?;
    let version_string:String = version_string.into();
    let version = Version::from_str(&version_string)?;
    let installation = uvm_core::find_installation(&version)?;
    let object = jni_utils::get_file(&env, &installation.path())?;
    Ok(object.into_inner())
}

#[no_mangle]
#[allow(non_snake_case)]
pub extern "system" fn Java_net_wooga_uvm_UnityVersionManager_locateUnityInstallation(env: JNIEnv, _class: JClass, version: JString) -> jobject {
    locate_installation(&env, version)
        .unwrap_or_else(|_| {
            JObject::null().into_inner()
        })
}