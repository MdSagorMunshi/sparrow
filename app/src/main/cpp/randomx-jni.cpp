#include <jni.h>
#include <string>
#include <mutex>
#include <cstring>
#include "randomx/randomx.h"

static randomx_cache *g_cache = nullptr;
static randomx_vm *g_vm = nullptr;
static std::string g_current_key = "";
static std::mutex g_vm_mutex;

extern "C" JNIEXPORT void JNICALL
Java_com_ryanshelby_spw_wallet_mining_RandomXNative_initKey(
    JNIEnv *env,
    jclass clazz,
    jbyteArray key_bytes) {
    
    std::lock_guard<std::mutex> lock(g_vm_mutex);
    if (!key_bytes) return;

    jsize len = env->GetArrayLength(key_bytes);
    jbyte *buf = env->GetByteArrayElements(key_bytes, nullptr);
    std::string key((char*)buf, len);
    env->ReleaseByteArrayElements(key_bytes, buf, JNI_ABORT);

    if (key == g_current_key && g_vm != nullptr) {
        return;
    }

    if (g_vm != nullptr) {
        randomx_destroy_vm(g_vm);
        g_vm = nullptr;
    }
    if (g_cache != nullptr) {
        randomx_release_cache(g_cache);
        g_cache = nullptr;
    }

    randomx_flags flags = randomx_get_flags();
    g_cache = randomx_alloc_cache(flags);
    if (!g_cache) {
        flags = RANDOMX_FLAG_DEFAULT;
        g_cache = randomx_alloc_cache(flags);
    }
    if (g_cache) {
        randomx_init_cache(g_cache, key.data(), key.size());
        g_vm = randomx_create_vm(flags, g_cache, nullptr);
    }
    g_current_key = key;
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_ryanshelby_spw_wallet_mining_RandomXNative_calculateHash(
    JNIEnv *env,
    jclass clazz,
    jbyteArray input_bytes) {
    
    std::lock_guard<std::mutex> lock(g_vm_mutex);
    if (g_vm == nullptr || !input_bytes) {
        return nullptr;
    }

    jsize len = env->GetArrayLength(input_bytes);
    jbyte *buf = env->GetByteArrayElements(input_bytes, nullptr);

    uint8_t hash_out[RANDOMX_HASH_SIZE];
    randomx_calculate_hash(g_vm, buf, len, hash_out);
    env->ReleaseByteArrayElements(input_bytes, buf, JNI_ABORT);

    jbyteArray result = env->NewByteArray(RANDOMX_HASH_SIZE);
    env->SetByteArrayRegion(result, 0, RANDOMX_HASH_SIZE, (jbyte*)hash_out);
    return result;
}

extern "C" JNIEXPORT void JNICALL
Java_com_ryanshelby_spw_wallet_mining_RandomXNative_close(
    JNIEnv *env,
    jclass clazz) {
    
    std::lock_guard<std::mutex> lock(g_vm_mutex);
    if (g_vm != nullptr) {
        randomx_destroy_vm(g_vm);
        g_vm = nullptr;
    }
    if (g_cache != nullptr) {
        randomx_release_cache(g_cache);
        g_cache = nullptr;
    }
    g_current_key = "";
}
