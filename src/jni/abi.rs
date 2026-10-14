//! Minimal JNI ABI surface: raw types, the `JNINativeInterface_` dispatch
//! table, and the string marshalling helpers built on top of it.
//!
//! The function table is declared by hand rather than pulled in from a
//! third-party crate, so the library builds for Android with no extra
//! dependency. Field order in [`JniNativeInterface`] is the `jni.h` slot
//! order and is asserted at compile time by `VTABLE_SLOTS`; reordering it
//! would silently mis-dispatch every call.

use std::ffi::{c_char, c_void, CString};
use std::ptr;

pub type JInt = i32;
pub type JLong = i64;
pub type JByte = i8;
pub type JString = *mut c_void;
pub type JObject = *mut c_void;
pub type JClass = JObject;
pub type JByteArray = JObject;

type GetStringUtfChars = unsafe extern "system" fn(*mut JniEnv, JString) -> *const c_char;
type ReleaseStringUtfChars = unsafe extern "system" fn(*mut JniEnv, JString, *const c_char);
type NewStringUtf = unsafe extern "system" fn(*mut JniEnv, *const c_char) -> JString;
type GetArrayLength = unsafe extern "system" fn(*mut JniEnv, JByteArray) -> JInt;
type SetByteArrayRegion =
    unsafe extern "system" fn(*mut JniEnv, JByteArray, JInt, JInt, *const JByte);
type ThrowNew = unsafe extern "system" fn(*mut JniEnv, JClass, *const c_char) -> JInt;
type FindClass = unsafe extern "system" fn(*mut JniEnv, *const c_char) -> JClass;

/// The `JNINativeInterface_` dispatch table, in `jni.h` declaration order.
///
/// Unused slots are held as opaque pointers so the offsets of the entries we
/// actually call stay correct without transcribing all 139 signatures.
#[repr(C)]
pub struct JniNativeInterface {
    _reserved: [*const c_void; 4],
    _get_version: *const c_void,
    _unused_define_class: *const c_void,
    find_class: Option<FindClass>,
    _unused_02: [*const c_void; 7],
    throw_new: Option<ThrowNew>,
    _unused_03: [*const c_void; 106],
    new_string_utf: Option<NewStringUtf>,
    _unused_new_string_utf_len: *const c_void,
    get_string_utf_chars: Option<GetStringUtfChars>,
    release_string_utf_chars: Option<ReleaseStringUtfChars>,
    get_array_length: Option<GetArrayLength>,
    _unused_05: [*const c_void; 11],
    _get_byte_array_region: *const c_void,
    set_byte_array_region: Option<SetByteArrayRegion>,
}

/// Number of slots the table above must occupy.
pub const VTABLE_SLOTS: usize = 139;
const _: () = assert!(
    std::mem::size_of::<JniNativeInterface>()
        == VTABLE_SLOTS * std::mem::size_of::<*const c_void>()
);

#[repr(C)]
pub struct JniEnv {
    functions: *const JniNativeInterface,
}

/// Borrows the dispatch table, or `None` for a null `env`.
fn table(env: *mut JniEnv) -> Option<&'static JniNativeInterface> {
    if env.is_null() {
        return None;
    }
    let functions = unsafe { (*env).functions };
    if functions.is_null() {
        return None;
    }
    Some(unsafe { &*functions })
}

/// Owning copy of a Java string. `GetStringUTFChars` emits modified UTF-8,
/// which Rust cannot represent, so anything that is not valid UTF-8 is
/// rejected rather than silently mangled.
pub struct JavaString(Vec<u8>);

impl JavaString {
    /// # Safety
    ///
    /// `env` must be a live `JNIEnv*` supplied by the JVM, or null (which is
    /// handled by returning `None` rather than aborting).
    pub unsafe fn read(env: *mut JniEnv, string: JString) -> Option<JavaString> {
        if string.is_null() {
            return None;
        }
        let table = table(env)?;
        let (get_chars, release) = (table.get_string_utf_chars?, table.release_string_utf_chars?);
        unsafe {
            let chars = get_chars(env, string);
            if chars.is_null() {
                return None;
            }
            let mut len = 0usize;
            while *chars.add(len) != 0 {
                len += 1;
            }
            let bytes = std::slice::from_raw_parts(chars as *const u8, len).to_vec();
            release(env, string, chars);
            Some(JavaString(bytes))
        }
    }

    pub fn as_str(&self) -> Option<&str> {
        std::str::from_utf8(&self.0).ok()
    }
}

/// Throws `java.lang.IllegalArgumentException` so the Kotlin `catch` branch
/// produces a real `BridgeResult.Failed` instead of an opaque crash.
///
/// # Safety
///
/// `env` must be a live `JNIEnv*` supplied by the JVM, or null (a no-op).
pub unsafe fn throw(env: *mut JniEnv, message: &str) {
    let Some(table) = table(env) else {
        return;
    };
    let (Some(find_class), Some(throw_new)) = (table.find_class, table.throw_new) else {
        return;
    };
    let Ok(class_name) = CString::new("java/lang/IllegalArgumentException") else {
        return;
    };
    let Ok(text) = CString::new(message) else {
        return;
    };
    unsafe {
        let clazz = find_class(env, class_name.as_ptr());
        if !clazz.is_null() {
            throw_new(env, clazz, text.as_ptr());
        }
    }
}

/// Allocates a Java `String`, or a null reference if the value contains an
/// interior NUL that modified UTF-8 cannot round-trip.
///
/// # Safety
///
/// `env` must be a live `JNIEnv*` supplied by the JVM, or null.
pub unsafe fn new_java_string(env: *mut JniEnv, value: &str) -> JString {
    let Some(table) = table(env) else {
        return ptr::null_mut();
    };
    let Some(new_string_utf) = table.new_string_utf else {
        return ptr::null_mut();
    };
    let Ok(text) = CString::new(value) else {
        return ptr::null_mut();
    };
    unsafe { new_string_utf(env, text.as_ptr()) }
}

/// Reads a Java `String` argument into an owned Rust `String`.
///
/// # Safety
///
/// `env` must be a live `JNIEnv*` supplied by the JVM, or null.
pub unsafe fn arg_string(env: *mut JniEnv, value: JString) -> Option<String> {
    JavaString::read(env, value).and_then(|s| s.as_str().map(|v| v.to_owned()))
}

/// Length of a Java byte array, or `None` if `env` is unusable.
///
/// # Safety
///
/// `env` and `array` must both be live references supplied by the JVM, or null.
pub unsafe fn array_len(env: *mut JniEnv, array: JByteArray) -> Option<JInt> {
    table(env)?
        .get_array_length
        .map(|f| unsafe { f(env, array) })
}

/// Copies `len` bytes into `array` starting at `start`.
/// Copies `buf` into `array` at `start`, reporting whether the table call
/// could be dispatched.
///
/// # Safety
///
/// `env` and `array` must be live references supplied by the JVM. `buf` must
/// remain readable for the length passed to the runtime.
pub unsafe fn set_array_region(
    env: *mut JniEnv,
    array: JByteArray,
    start: JInt,
    buf: &[u8],
) -> bool {
    let Some(f) = table(env).and_then(|t| t.set_byte_array_region) else {
        return false;
    };
    let capped = buf.len().min(JInt::MAX as usize) as JInt;
    unsafe { f(env, array, start, capped, buf.as_ptr() as *const JByte) };
    true
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_vtable_layout_matches_jni_header_slots() {
        assert_eq!(
            std::mem::size_of::<JniNativeInterface>(),
            VTABLE_SLOTS * std::mem::size_of::<*const c_void>()
        );
        assert_eq!(std::mem::size_of::<JniNativeInterface>(), 1112);
    }

    #[test]
    fn test_null_env_and_null_references_are_rejected() {
        // A null `env` must be rejected before any dereference happens.
        assert!(unsafe { JavaString::read(ptr::null_mut(), 1usize as JString) }.is_none());
        assert!(unsafe { arg_string(ptr::null_mut(), 1usize as JString) }.is_none());
        assert!(unsafe { new_java_string(ptr::null_mut(), "x") }.is_null());
        assert_eq!(
            unsafe { array_len(ptr::null_mut(), 1usize as JByteArray) },
            None
        );
        assert!(!unsafe { set_array_region(ptr::null_mut(), 1usize as JByteArray, 0, &[1]) });
        // A null `string` is rejected before `env` is ever dereferenced.
        assert!(
            unsafe { JavaString::read(ptr::dangling_mut::<JniEnv>(), ptr::null_mut()) }.is_none()
        );
    }

    #[test]
    fn test_java_string_rejects_invalid_utf8() {
        let invalid = JavaString(vec![0x66, 0x80, 0x6f]);
        assert!(invalid.as_str().is_none());
        assert_eq!(JavaString(b"ok".to_vec()).as_str(), Some("ok"));
    }
}
