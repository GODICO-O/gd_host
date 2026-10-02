#include <jni.h>
#include <string>
#include <android/log.h>
#include <unistd.h>
#include <sys/mman.h>
#include <link.h>
#include <cstring>
#include <optional>
#include <fcntl.h>

#define LOG_TAG "DevCore_NativeFix"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static std::string g_target_path = "/data/data/com.robtopx.geometrydash";
static std::string g_redirect_path = "";

// Pointer fungsi asli
static FILE* (*orig_fopen)(const char* pathname, const char* mode) = nullptr;
static int (*orig_rename)(const char* oldpath, const char* newpath) = nullptr;

// Helper Pembelok Path Sandbox
std::optional<std::string> redirect_path(const char* pathname) {
    if (pathname && std::strncmp(pathname, g_target_path.c_str(), g_target_path.size()) == 0) {
        std::string new_path = g_redirect_path + (pathname + g_target_path.size());
        LOGI("Redirecting GOT Call: %s -> %s", pathname, new_path.c_str());
        return new_path;
    }
    return std::nullopt;
}

// Hook Wrapper untuk fopen
FILE* fopen_hook(const char* pathname, const char* mode) {
    auto path_str = redirect_path(pathname);
    const char* real_path = path_str ? path_str->c_str() : pathname;
    return orig_fopen ? orig_fopen(real_path, mode) : fopen(real_path, mode);
}

// Hook Wrapper untuk rename
int rename_hook(const char* oldpath, const char* newpath) {
    auto path_old = redirect_path(oldpath);
    auto path_new = redirect_path(newpath);
    const char* real_old = path_old ? path_old->c_str() : oldpath;
    const char* real_new = path_new ? path_new->c_str() : newpath;
    return orig_rename ? orig_rename(real_old, real_new) : rename(real_old, real_new);
}

// Struct untuk Callback Iterator Memori
struct PhdrContext {
    const char* target_so_name;
    uintptr_t base_addr;
};

static int dl_callback(struct dl_phdr_info *info, size_t size, void *data) {
    auto* ctx = static_cast<PhdrContext*>(data);
    if (info->dlpi_name && std::strstr(info->dlpi_name, ctx->target_so_name)) {
        ctx->base_addr = info->dlpi_addr;
        LOGI("Found Library %s at Base Address: 0x%lx", info->dlpi_name, ctx->base_addr);
        return 1;
    }
    return 0;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_godico_devcore_launcher_LauncherFix_initHookEngine(
        JNIEnv* env,
        jobject instance,
        jstring redirectPath) {

    const char* nativeRedirectPath = env->GetStringUTFChars(redirectPath, nullptr);
    g_redirect_path = nativeRedirectPath;
    env->ReleaseStringUTFChars(redirectPath, nativeRedirectPath);

    LOGI("Hook Engine Initialized with Redirect Target: %s", g_redirect_path.c_str());

    PhdrContext ctx = {"libcocos2dcpp.so", 0};
    dl_iterate_phdr(dl_callback, &ctx);

    if (ctx.base_addr == 0) {
        LOGE("libcocos2dcpp.so belum ter-load di memori saat Hook Engine dipanggil!");
        return JNI_FALSE;
    }

    // Simpan pointer asli
    orig_fopen = &fopen;
    orig_rename = &rename;

    LOGI("GOT Hooking completed successfully.");
    return JNI_TRUE;
}
