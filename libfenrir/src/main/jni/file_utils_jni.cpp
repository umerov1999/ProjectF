#include <jni.h>
#include <cstdint>
#include <cstdlib>
#include <cstring>
#include <iostream>
#include <list>
#include <filesystem>
#include "tools.h"
#include "log.h"

using namespace std;
namespace fs = std::filesystem;

static void listDirRecursive(JNIEnv *env, const string &name, jobject listener, int64_t pointer) {
    for (const auto &entry: fs::directory_iterator(name)) {
        if (entry.path().filename().string() == "." || entry.path().filename().string() == "..")
            continue;
        if (entry.is_directory()) {
            listDirRecursive(env, entry.path().string(), listener, pointer);
        } else if (entry.is_regular_file()) {
            if (listener != nullptr) {
                jweak dir_Wlistener = env->NewWeakGlobalRef(listener);
                jclass ref = env->GetObjectClass(dir_Wlistener);
                auto method = env->GetMethodID(ref, "onEntry", "(Ljava/lang/String;)V");
                if (ref != nullptr && method != nullptr) {
                    env->CallVoidMethod(dir_Wlistener, method,
                                        env->NewStringUTF(entry.path().string().c_str()));
                }
            } else if (pointer != 0) {
                auto *dt = (list<string> *) (intptr_t) pointer;
                dt->push_back(entry.path().string());
            }
        }
    }
}

extern "C" JNIEXPORT void
Java_dev_ragnarok_fenrir_module_FileUtils_listDirRecursiveNative(JNIEnv *env, jobject, jstring dir,
                                                                 jobject listener) {
    char const *dirString = SafeGetStringUTFChars(env, dir, nullptr);
    if (!dirString) {
        return;
    }
    string v = dirString;
    env->ReleaseStringUTFChars(dir, dirString);
    listDirRecursive(env, v, listener, 0);
}

extern "C" JNIEXPORT void
Java_dev_ragnarok_fenrir_module_FileUtils_listDirRecursiveNativePointer(JNIEnv *env, jobject,
                                                                        jstring dir,
                                                                        jlong pointer) {
    char const *dirString = SafeGetStringUTFChars(env, dir, nullptr);
    if (!dirString) {
        return;
    }
    string v = dirString;
    env->ReleaseStringUTFChars(dir, dirString);
    listDirRecursive(env, v, nullptr, pointer);
}
