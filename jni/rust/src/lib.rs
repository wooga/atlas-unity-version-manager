extern crate jni;
#[macro_use]
extern crate uvm_core;
#[macro_use]
extern crate error_chain;
#[macro_use]
extern crate log;


mod install;

use uvm_core::Version;
use jni::JNIEnv;
use jni::objects::{JClass, JObject, JValue, JString};
use jni::sys::{jstring, jobject, jobjectArray, jsize, jboolean, jint};
use std::path::{Path, PathBuf};
use std::str::FromStr;
use std::collections::HashSet;
use uvm_core::install::InstallVariant;
use std::error::Error;
use uvm_core::unity::Component;

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
    use uvm_core::unity;

    /// Converts a `java.io.File` `JObject` into a `PathBuf`
    pub fn get_path(env: &JNIEnv, path: JObject) -> error::UvmJniResult<PathBuf> {
        env.call_method(path, "getPath", "()Ljava/lang/String;", &[])
            .and_then(JValue::l)
            .and_then(|object| env.get_string(object.into()))
            .map(|p| Path::new(&String::from(p)).to_path_buf())
            .map_err(|e| e.into())
    }

    pub fn get_file<'a, 'b>(env: &'a JNIEnv<'b>, path: &'b Path) -> error::UvmJniResult<JObject<'b>> {
        let class = env.find_class("java/io/File")?;
        let path_string = env.new_string(path.to_string_lossy())?;
        let object = env.new_object(class, "(Ljava/lang/String;)V", &[JValue::Object(path_string.into())])?;
        Ok(object)
    }

    pub fn get_installation<'a, 'b>(env: &'a JNIEnv<'b>, installation: &'b unity::Installation) -> error::UvmJniResult<JObject<'b>> {
        let installation_class = env.find_class("net/wooga/uvm/Installation")?;
        let install_path = jni_utils::get_file(&env, installation.path())?;
        let install_version = env.new_string(installation.version().to_string())?;
        let native_installation = env.new_object(installation_class, "(Ljava/io/File;Ljava/lang/String;)V", &[JValue::Object(install_path.into()), JValue::Object(install_version.into())])?;
        Ok(native_installation)
    }

    pub fn get_component<'a, 'b>(env: &'a JNIEnv<'b>, component: &'b Component) -> error::UvmJniResult<JObject<'a>> {
        let component_class = env.find_class("net/wooga/uvm/Component")?;
        let component_method = match component {
            &Component::Android => "android",
            &Component::Ios => "ios",
            &Component::TvOs => "tvOs",
            &Component::WebGl => "webGl",
            &Component::Linux => "linux",
            &Component::Windows => "windows",
            &Component::WindowsMono => "windowsMono",
            &Component::Editor => "editor",
            &Component::Mono => "mono",
            &Component::VisualStudio => "visualStudio",
            &Component::MonoDevelop => "monoDevelop",
            &Component::StandardAssets => "standardAssets",
            &Component::Documentation => "documentation",
        };
        let native_component = env.get_static_field(component_class, component_method, "Lnet/wooga/uvm/Component;")?;
        let native_component = native_component.l()?;
        Ok(native_component)
    }

    pub fn print_error_and_return_null<E: Error>(err: E) -> jobject {
        eprintln!("{}", err.description());
        if let Some(source) = err.source() {
            eprintln!("{}", source);
        }

        JObject::null().into_inner()
    }
}

#[no_mangle]
#[allow(non_snake_case)]
pub extern "system" fn Java_net_wooga_uvm_UnityVersionManager_uvmVersion(env: JNIEnv, _class: JClass) -> jstring {
    env.new_string(cargo_version!())
        .map(|s| s.into_inner())
        .unwrap_or_else(jni_utils::print_error_and_return_null)
}

fn list_installations(env: &JNIEnv) -> error::UvmJniResult<jobjectArray> {
    let installations = uvm_core::list_all_installations()?;
    let installations: Vec<uvm_core::Installation> = installations.collect();
    let installation_class = env.find_class("net/wooga/uvm/Installation")?;

    let output = env.new_object_array(installations.len() as jsize, installation_class, JObject::null())?;
    for (i, installation) in installations.iter().enumerate() {
        let native_installation = jni_utils::get_installation(&env, &installation)?;
        env.set_object_array_element(output, i as jsize, native_installation)?;
    }

    Ok(output)
}

#[no_mangle]
#[allow(non_snake_case)]
pub extern "system" fn Java_net_wooga_uvm_UnityVersionManager_listInstallations(env: JNIEnv, _class: JClass) -> jobjectArray {
    list_installations(&env)
        .unwrap_or_else(jni_utils::print_error_and_return_null)
}

#[no_mangle]
#[allow(non_snake_case)]
pub extern "system" fn Java_net_wooga_uvm_UnityVersionManager_detectProjectVersion(env: JNIEnv, _class: JClass, path: JObject) -> jstring {
    jni_utils::get_path(&env, path)
        .and_then(|path| {
            uvm_core::dectect_project_version(&path, Some(true)).map_err(|e| e.into())
        })
        .and_then(|version| {
            env.new_string(version.to_string()).map_err(|e| e.into())
        })
        .map(|s| s.into_inner())
        .unwrap_or_else(jni_utils::print_error_and_return_null)
}

fn locate_installation(env: &JNIEnv, version: JString) -> error::UvmJniResult<jobject> {
    let version_string = env.get_string(version)?;
    let version_string: String = version_string.into();
    let version = Version::from_str(&version_string)?;
    let installation = uvm_core::find_installation(&version)?;

    let native_installation = jni_utils::get_installation(&env, &installation)?;
    Ok(native_installation.into_inner())
}

#[no_mangle]
#[allow(non_snake_case)]
pub extern "system" fn Java_net_wooga_uvm_UnityVersionManager_locateUnityInstallation(env: JNIEnv, _class: JClass, version: JString) -> jobject {
    locate_installation(&env, version)
        .unwrap_or_else(jni_utils::print_error_and_return_null)
}

struct Variant(InstallVariant);

impl Variant {
    pub fn value(self) -> InstallVariant {
        self.0
    }
}

impl From<jint> for Variant {
    fn from(component: jint) -> Self {
        match component {
            0 => Variant(InstallVariant::Android),
            1 => Variant(InstallVariant::Ios),
            3 => Variant(InstallVariant::WebGl),
            4 => Variant(InstallVariant::Linux),
            5 => Variant(InstallVariant::Windows),
            6 => Variant(InstallVariant::WindowsMono),
            _ => Variant(InstallVariant::Android),
        }
    }
}

fn install_unity_editor(env: &JNIEnv, version: JString, destination: JObject, components: Option<jobjectArray>) -> error::UvmJniResult<jobject> {
    let version = env.get_string(version)?;
    let version: String = version.into();
    let version = Version::from_str(&version)?;
    let destination = jni_utils::get_path(env, destination)?;

    let variants = if let Some(components) = components {
        let length = env.get_array_length(components)?;
        let mut variants: HashSet<InstallVariant> = HashSet::with_capacity(length as usize);
        for i in 0..length {
            let item = env.get_object_array_element(components, i)?;
            let item_value = env.call_method(item, "value", "()I", &[])?;
            let item_value: jint = item_value.i()?;
            let variant: Variant = item_value.into();
            variants.insert(variant.value());
        }
        Some(variants)
    } else {
        None
    };

    let installation = install::install(version, Some(destination), variants)?;
    let native_installation = jni_utils::get_installation(&env, &installation)?;
    Ok(native_installation.into_inner())
}

#[no_mangle]
#[allow(non_snake_case)]
pub extern "system" fn Java_net_wooga_uvm_UnityVersionManager_installUnityEditor__Ljava_lang_String_2Ljava_io_File_2(env: JNIEnv, _class: JClass, version: JString, destination: JObject) -> jobject {
    install_unity_editor(&env, version, destination, None)
        .unwrap_or_else(jni_utils::print_error_and_return_null)
}

#[no_mangle]
#[allow(non_snake_case)]
pub extern "system" fn Java_net_wooga_uvm_UnityVersionManager_installUnityEditor__Ljava_lang_String_2Ljava_io_File_2_3Lnet_wooga_uvm_Component_2(env: JNIEnv, _class: JClass, version: JString, destination: JObject, components: jobjectArray) -> jobject {
    install_unity_editor(&env, version, destination, Some(components))
        .unwrap_or_else(jni_utils::print_error_and_return_null)
}

fn get_installation_components(env: &JNIEnv, object: JObject) -> error::UvmJniResult<jobjectArray> {
    let location = env.call_method(object, "getLocation", "()Ljava/io/File;", &[])?;
    let location = location.l()?;
    let path = jni_utils::get_path(&env, location)?;

    let installation = uvm_core::unity::Installation::new(path)?;
    let components = uvm_core::unity::InstalledComponents::new(installation);
    let components: Vec<Component> = components.collect();
    let component_class = env.find_class("net/wooga/uvm/Component")?;

    let output = env.new_object_array(components.len() as jsize, component_class, JObject::null())?;
    for (i, component) in components.iter().enumerate() {
        let native_component = jni_utils::get_component(&env, &component)?;
        env.set_object_array_element(output, i as jsize, native_component)?;
    }

    Ok(output)
}


#[no_mangle]
#[allow(non_snake_case)]
pub extern "system" fn Java_net_wooga_uvm_Installation_getComponents(env: JNIEnv, object: JObject) -> jobjectArray {
    get_installation_components(&env, object)
        .unwrap_or_else(jni_utils::print_error_and_return_null)
}
