#include <jni.h>
#include <cstdint>
#include <cstdlib>
#include <cstring>
#include <iostream>
#include <list>
#include <utility>
#include "fenrir_native.h"

class Parcel {
public:
    explicit Parcel(int pFlags) {
        flags = pFlags;
    }

    static int8_t getBoolean(Parcel *&parcel) {
        if (parcel == nullptr) {
            return false;
        }

        ParcelEntity entity = parcel->read();
        int8_t ret = entity.readBool();

        if (parcel->parcels.empty()) {
            delete parcel;
            parcel = nullptr;
        }
        return ret;
    }

    static int8_t getByte(Parcel *&parcel) {
        if (parcel == nullptr) {
            return false;
        }

        ParcelEntity entity = parcel->read();
        int8_t ret = entity.readByte();

        if (parcel->parcels.empty()) {
            delete parcel;
            parcel = nullptr;
        }
        return ret;
    }

    static int32_t getInt(Parcel *&parcel) {
        if (parcel == nullptr) {
            return false;
        }

        ParcelEntity entity = parcel->read();
        int32_t ret = entity.readInt();

        if (parcel->parcels.empty()) {
            delete parcel;
            parcel = nullptr;
        }
        return ret;
    }

    static int64_t getLong(Parcel *&parcel) {
        if (parcel == nullptr) {
            return false;
        }

        ParcelEntity entity = parcel->read();
        int64_t ret = entity.readLong();

        if (parcel->parcels.empty()) {
            delete parcel;
            parcel = nullptr;
        }
        return ret;
    }

    static float getFloat(Parcel *&parcel) {
        if (parcel == nullptr) {
            return false;
        }

        ParcelEntity entity = parcel->read();
        float ret = entity.readFloat();

        if (parcel->parcels.empty()) {
            delete parcel;
            parcel = nullptr;
        }
        return ret;
    }

    static double getDouble(Parcel *&parcel) {
        if (parcel == nullptr) {
            return false;
        }

        ParcelEntity entity = parcel->read();
        double ret = entity.readDouble();

        if (parcel->parcels.empty()) {
            delete parcel;
            parcel = nullptr;
        }
        return ret;
    }

    static char *getString(Parcel *&parcel) {
        if (parcel == nullptr) {
            return (char *) "error";
        }

        ParcelEntity entity = parcel->read();
        char *ret = entity.readString();

        if (parcel->parcels.empty()) {
            delete parcel;
            parcel = nullptr;
        }
        return ret;
    }

    static int8_t *getBinary(Parcel *&parcel) {
        if (parcel == nullptr) {
            return nullptr;
        }

        ParcelEntity entity = parcel->read();
        auto *ret = entity.readBinary();

        if (parcel->parcels.empty()) {
            delete parcel;
            parcel = nullptr;
        }
        return ret;
    }

    static int getFlags(Parcel *parcel) {
        if (parcel == nullptr) {
            return 0;
        }
        return parcel->flags;
    }

    static void forceDestroy(Parcel *parcel) {
        if (parcel == nullptr) {
            return;
        }
        parcel->parcels.clear();
    }

    static void putBoolean(Parcel *parcel, int8_t value) {
        if (parcel == nullptr) {
            return;
        }
        ParcelEntity entity;
        entity.putBoolean(value);
        parcel->parcels.push_back(entity);
    }

    static void putByte(Parcel *parcel, int8_t value) {
        if (parcel == nullptr) {
            return;
        }
        ParcelEntity entity;
        entity.putByte(value);
        parcel->parcels.push_back(entity);
    }

    static void putInt(Parcel *parcel, int32_t value) {
        if (parcel == nullptr) {
            return;
        }
        ParcelEntity entity;
        entity.putInt(value);
        parcel->parcels.push_back(entity);
    }

    static void putFirstInt(Parcel *parcel, int32_t value) {
        if (parcel == nullptr) {
            return;
        }
        ParcelEntity entity;
        entity.putInt(value);
        parcel->parcels.push_front(entity);
    }

    static void putLong(Parcel *parcel, int64_t value) {
        if (parcel == nullptr) {
            return;
        }
        ParcelEntity entity;
        entity.putLong(value);
        parcel->parcels.push_back(entity);
    }

    static void putFloat(Parcel *parcel, float value) {
        if (parcel == nullptr) {
            return;
        }
        ParcelEntity entity;
        entity.putFloat(value);
        parcel->parcels.push_back(entity);
    }

    static void putDouble(Parcel *parcel, double value) {
        if (parcel == nullptr) {
            return;
        }
        ParcelEntity entity;
        entity.putDouble(value);
        parcel->parcels.push_back(entity);
    }

    static void putString(Parcel *parcel, const char *value) {
        if (parcel == nullptr) {
            return;
        }
        ParcelEntity entity;
        entity.putString(value);
        parcel->parcels.push_back(entity);
    }

    static void putBinary(Parcel *parcel, int8_t *data, int32_t size) {
        if (parcel == nullptr) {
            return;
        }
        ParcelEntity entity;
        entity.putBinary(data, size);
        parcel->parcels.push_back(entity);
    }

    static void putNullString(Parcel *parcel) {
        if (parcel == nullptr) {
            return;
        }
        ParcelEntity entity;
        entity.putNullString();
        parcel->parcels.push_back(entity);
    }

    static void putNullBinary(Parcel *parcel) {
        if (parcel == nullptr) {
            return;
        }
        ParcelEntity entity;
        entity.putNullBinary();
        parcel->parcels.push_back(entity);
    }

private:
    class ParcelEntity {
    public:
        union Numeric {
            int8_t byteValue;
            int32_t intValue;
            float floatValue;
            double doubleValue;
            int64_t longValue;
        };

        ParcelEntity() {
            memset(&numeric, 0, sizeof(Numeric));
            type = Types::TYPE_NULL;
        }

        void putBoolean(int8_t value) {
            type = Types::TYPE_BOOL;
            numeric.byteValue = value;
        }

        void putByte(int8_t value) {
            type = Types::TYPE_BYTE;
            numeric.byteValue = value;
        }

        void putInt(int32_t value) {
            type = Types::TYPE_INT;
            numeric.intValue = value;
        }

        void putLong(int64_t value) {
            type = Types::TYPE_LONG;
            numeric.longValue = value;
        }

        void putFloat(float value) {
            type = Types::TYPE_FLOAT;
            numeric.floatValue = value;
        }

        void putDouble(double value) {
            type = Types::TYPE_DOUBLE;
            numeric.doubleValue = value;
        }

        void putString(const char *value) {
            type = Types::TYPE_STRING;
            auto sz = strlen(value);
            char *tmp = (char *) malloc(sz + 1);
            memcpy(tmp, value, sz);
            tmp[sz] = 0;
            numeric.longValue = (int64_t) (intptr_t) tmp;
        }

        void putNullString() {
            type = Types::TYPE_STRING;
            numeric.longValue = 0;
        }

        void putBinary(int8_t *value, int32_t size) {
            type = Types::TYPE_BINARY;
            auto *tmp = (int8_t *) malloc(size + sizeof(int32_t));
            memcpy(tmp, &size, sizeof(int32_t));
            if (size > 0) {
                memcpy(tmp + sizeof(int32_t), value, size);
            }
            numeric.longValue = (int64_t) (intptr_t) tmp;
        }

        void putNullBinary() {
            type = Types::TYPE_BINARY;
            numeric.longValue = 0;
        }

        int8_t readBool() {
            if (type != Types::TYPE_BOOL) {
                return false;
            }
            return numeric.byteValue;
        }

        int8_t readByte() {
            if (type != Types::TYPE_BYTE) {
                return 0;
            }
            return numeric.byteValue;
        }

        int32_t readInt() {
            if (type != Types::TYPE_INT) {
                return 0;
            }
            return numeric.intValue;
        }

        int64_t readLong() {
            if (type != Types::TYPE_LONG) {
                return 0;
            }
            return numeric.longValue;
        }

        float readFloat() {
            if (type != Types::TYPE_FLOAT) {
                return 0;
            }
            return numeric.floatValue;
        }

        double readDouble() {
            if (type != Types::TYPE_DOUBLE) {
                return 0;
            }
            return numeric.doubleValue;
        }

        char *readString() {
            if (type != Types::TYPE_STRING || !numeric.longValue) {
                return nullptr;
            }
            return (char *) (intptr_t) numeric.longValue;
        }

        int8_t *readBinary() {
            if (type != Types::TYPE_BINARY || !numeric.longValue) {
                return nullptr;
            }
            return (int8_t *) (intptr_t) numeric.longValue;
        }

        enum class Types {
            TYPE_NULL,
            TYPE_BOOL,
            TYPE_BYTE,
            TYPE_INT,
            TYPE_LONG,
            TYPE_FLOAT,
            TYPE_DOUBLE,
            TYPE_STRING,
            TYPE_BINARY
        };
    private:
        Types type;
        Numeric numeric;
    };

    ParcelEntity read() {
        ParcelEntity entity = *parcels.begin();
        parcels.pop_front();
        return entity;
    }

    std::list<ParcelEntity> parcels;
    int flags;
};

extern "C" JNIEXPORT jlong
Java_dev_ragnarok_fenrir_module_parcel_ParcelNative_init(JNIEnv *, jobject, jint flags) {
    return (jlong) (intptr_t) new Parcel(flags);
}

extern "C" JNIEXPORT void
Java_dev_ragnarok_fenrir_module_parcel_ParcelNative_forceDestroy(JNIEnv *, jobject,
                                                                 jlong parcel_native) {
    if (!parcel_native) {
        return;
    }
    auto *parcel = (Parcel *) (intptr_t) parcel_native;
    Parcel::forceDestroy(parcel);
    delete parcel;
}

extern "C" JNIEXPORT jint
Java_dev_ragnarok_fenrir_module_parcel_ParcelNative_getFlags(JNIEnv *, jobject,
                                                             jlong parcel_native) {
    if (!parcel_native) {
        return 0;
    }
    auto *parcel = (Parcel *) (intptr_t) parcel_native;
    return Parcel::getFlags(parcel);
}

extern "C" JNIEXPORT void
Java_dev_ragnarok_fenrir_module_parcel_ParcelNative_putBoolean(JNIEnv *, jobject,
                                                               jlong parcel_native,
                                                               jboolean value) {
    if (!parcel_native) {
        return;
    }
    auto *parcel = (Parcel *) (intptr_t) parcel_native;
    Parcel::putBoolean(parcel, (int8_t) value);
}

extern "C" JNIEXPORT void
Java_dev_ragnarok_fenrir_module_parcel_ParcelNative_putByte(JNIEnv *, jobject, jlong parcel_native,
                                                            jbyte value) {
    if (!parcel_native) {
        return;
    }
    auto *parcel = (Parcel *) (intptr_t) parcel_native;
    Parcel::putByte(parcel, value);
}

extern "C" JNIEXPORT void
Java_dev_ragnarok_fenrir_module_parcel_ParcelNative_putInt(JNIEnv *, jobject, jlong parcel_native,
                                                           jint value) {
    if (!parcel_native) {
        return;
    }
    auto *parcel = (Parcel *) (intptr_t) parcel_native;
    Parcel::putInt(parcel, value);
}

extern "C" JNIEXPORT void
Java_dev_ragnarok_fenrir_module_parcel_ParcelNative_putFirstInt(JNIEnv *, jobject,
                                                                jlong parcel_native,
                                                                jint value) {
    if (!parcel_native) {
        return;
    }
    auto *parcel = (Parcel *) (intptr_t) parcel_native;
    Parcel::putFirstInt(parcel, value);
}

extern "C" JNIEXPORT void
Java_dev_ragnarok_fenrir_module_parcel_ParcelNative_putLong(JNIEnv *, jobject, jlong parcel_native,
                                                            jlong value) {
    if (!parcel_native) {
        return;
    }
    auto *parcel = (Parcel *) (intptr_t) parcel_native;
    Parcel::putLong(parcel, value);
}

extern "C" JNIEXPORT void
Java_dev_ragnarok_fenrir_module_parcel_ParcelNative_putFloat(JNIEnv *, jobject, jlong parcel_native,
                                                             jfloat value) {
    if (!parcel_native) {
        return;
    }
    auto *parcel = (Parcel *) (intptr_t) parcel_native;
    Parcel::putFloat(parcel, value);
}

extern "C" JNIEXPORT void
Java_dev_ragnarok_fenrir_module_parcel_ParcelNative_putDouble(JNIEnv *, jobject,
                                                              jlong parcel_native,
                                                              jdouble value) {
    if (!parcel_native) {
        return;
    }
    auto *parcel = (Parcel *) (intptr_t) parcel_native;
    Parcel::putDouble(parcel, value);
}

extern "C" JNIEXPORT void
Java_dev_ragnarok_fenrir_module_parcel_ParcelNative_putNullString(JNIEnv *, jobject,
                                                                  jlong parcel_native) {
    if (!parcel_native) {
        return;
    }
    auto *parcel = (Parcel *) (intptr_t) parcel_native;
    Parcel::putNullString(parcel);
}

extern "C" JNIEXPORT void
Java_dev_ragnarok_fenrir_module_parcel_ParcelNative_putString(JNIEnv *env, jobject,
                                                              jlong parcel_native,
                                                              jstring value) {
    if (!parcel_native) {
        return;
    }
    auto *parcel = (Parcel *) (intptr_t) parcel_native;
    char const *textString = SafeGetStringUTFChars(env, value, nullptr);
    Parcel::putString(parcel, textString);
    if (textString != nullptr) {
        env->ReleaseStringUTFChars(value, textString);
    }
}

extern "C" JNIEXPORT void
Java_dev_ragnarok_fenrir_module_parcel_ParcelNative_putBinary(JNIEnv *env, jobject,
                                                              jlong parcel_native,
                                                              jbyteArray value) {
    if (!parcel_native) {
        return;
    }
    auto *parcel = (Parcel *) (intptr_t) parcel_native;
    jboolean isCopy;
    jint length = env->GetArrayLength(value);
    jbyte *buf = env->GetByteArrayElements(value, &isCopy);
    if (!buf) {
        return;
    }
    Parcel::putBinary(parcel, buf, length);
    env->ReleaseByteArrayElements(value, buf, JNI_ABORT);
}

extern "C" JNIEXPORT void
Java_dev_ragnarok_fenrir_module_parcel_ParcelNative_putNullBinary(JNIEnv *, jobject,
                                                                  jlong parcel_native) {
    if (!parcel_native) {
        return;
    }
    auto *parcel = (Parcel *) (intptr_t) parcel_native;
    Parcel::putNullBinary(parcel);
}

extern "C" JNIEXPORT jboolean
Java_dev_ragnarok_fenrir_module_parcel_ParcelNative_readBoolean(JNIEnv *env, jobject,
                                                                jlong parcel_native,
                                                                jobject listener) {
    if (!parcel_native) {
        return 0;
    }
    auto *parcel = (Parcel *) (intptr_t) parcel_native;
    auto ret = Parcel::getBoolean(parcel);

    if (parcel == nullptr && listener != nullptr) {
        jweak store_Wlistener = env->NewWeakGlobalRef(listener);
        jclass ref = env->GetObjectClass(store_Wlistener);
        auto method = env->GetMethodID(ref, "doUpdateNative", "(J)V");
        if (ref != nullptr && method != nullptr) {
            env->CallVoidMethod(store_Wlistener, method, 0ll);
        }
    }

    return (uint8_t) ret;
}

extern "C" JNIEXPORT jbyte
Java_dev_ragnarok_fenrir_module_parcel_ParcelNative_readByte(JNIEnv *env, jobject,
                                                             jlong parcel_native,
                                                             jobject listener) {
    if (!parcel_native) {
        return 0;
    }
    auto *parcel = (Parcel *) (intptr_t) parcel_native;
    auto ret = Parcel::getByte(parcel);

    if (parcel == nullptr && listener != nullptr) {
        jweak store_Wlistener = env->NewWeakGlobalRef(listener);
        jclass ref = env->GetObjectClass(store_Wlistener);
        auto method = env->GetMethodID(ref, "doUpdateNative", "(J)V");
        if (ref != nullptr && method != nullptr) {
            env->CallVoidMethod(store_Wlistener, method, 0ll);
        }
    }

    return ret;
}

extern "C" JNIEXPORT jint
Java_dev_ragnarok_fenrir_module_parcel_ParcelNative_readInt(JNIEnv *env, jobject,
                                                            jlong parcel_native,
                                                            jobject listener) {
    if (!parcel_native) {
        return 0;
    }
    auto *parcel = (Parcel *) (intptr_t) parcel_native;
    auto ret = Parcel::getInt(parcel);

    if (parcel == nullptr && listener != nullptr) {
        jweak store_Wlistener = env->NewWeakGlobalRef(listener);
        jclass ref = env->GetObjectClass(store_Wlistener);
        auto method = env->GetMethodID(ref, "doUpdateNative", "(J)V");
        if (ref != nullptr && method != nullptr) {
            env->CallVoidMethod(store_Wlistener, method, 0ll);
        }
    }

    return ret;
}

extern "C" JNIEXPORT jlong
Java_dev_ragnarok_fenrir_module_parcel_ParcelNative_readLong(JNIEnv *env, jobject,
                                                             jlong parcel_native,
                                                             jobject listener) {
    if (!parcel_native) {
        return 0;
    }
    auto *parcel = (Parcel *) (intptr_t) parcel_native;
    auto ret = Parcel::getLong(parcel);

    if (parcel == nullptr && listener != nullptr) {
        jweak store_Wlistener = env->NewWeakGlobalRef(listener);
        jclass ref = env->GetObjectClass(store_Wlistener);
        auto method = env->GetMethodID(ref, "doUpdateNative", "(J)V");
        if (ref != nullptr && method != nullptr) {
            env->CallVoidMethod(store_Wlistener, method, 0ll);
        }
    }

    return ret;
}

extern "C" JNIEXPORT jfloat
Java_dev_ragnarok_fenrir_module_parcel_ParcelNative_readFloat(JNIEnv *env, jobject,
                                                              jlong parcel_native,
                                                              jobject listener) {
    if (!parcel_native) {
        return 0;
    }
    auto *parcel = (Parcel *) (intptr_t) parcel_native;
    auto ret = Parcel::getFloat(parcel);

    if (parcel == nullptr && listener != nullptr) {
        jweak store_Wlistener = env->NewWeakGlobalRef(listener);
        jclass ref = env->GetObjectClass(store_Wlistener);
        auto method = env->GetMethodID(ref, "doUpdateNative", "(J)V");
        if (ref != nullptr && method != nullptr) {
            env->CallVoidMethod(store_Wlistener, method, 0ll);
        }
    }

    return ret;
}

extern "C" JNIEXPORT jdouble
Java_dev_ragnarok_fenrir_module_parcel_ParcelNative_readDouble(JNIEnv *env, jobject,
                                                               jlong parcel_native,
                                                               jobject listener) {
    if (!parcel_native) {
        return 0;
    }
    auto *parcel = (Parcel *) (intptr_t) parcel_native;
    auto ret = Parcel::getDouble(parcel);


    if (parcel == nullptr && listener != nullptr) {
        jweak store_Wlistener = env->NewWeakGlobalRef(listener);
        jclass ref = env->GetObjectClass(store_Wlistener);
        auto method = env->GetMethodID(ref, "doUpdateNative", "(J)V");
        if (ref != nullptr && method != nullptr) {
            env->CallVoidMethod(store_Wlistener, method, 0ll);
        }
    }

    return ret;
}

extern "C" JNIEXPORT jstring
Java_dev_ragnarok_fenrir_module_parcel_ParcelNative_readString(JNIEnv *env, jobject,
                                                               jlong parcel_native,
                                                               jobject listener) {
    if (!parcel_native) {
        return env->NewStringUTF("error");
    }
    auto *parcel = (Parcel *) (intptr_t) parcel_native;
    auto ret = Parcel::getString(parcel);

    if (parcel == nullptr && listener != nullptr) {
        jweak store_Wlistener = env->NewWeakGlobalRef(listener);
        jclass ref = env->GetObjectClass(store_Wlistener);
        auto method = env->GetMethodID(ref, "doUpdateNative", "(J)V");
        if (ref != nullptr && method != nullptr) {
            env->CallVoidMethod(store_Wlistener, method, 0ll);
        }
    }
    jstring res;
    if (ret != nullptr) {
        res = env->NewStringUTF(ret);
        free(ret);
    } else {
        res = nullptr;
    }
    return res;
}

extern "C" JNIEXPORT jbyteArray
Java_dev_ragnarok_fenrir_module_parcel_ParcelNative_readBinary(JNIEnv *env, jobject,
                                                               jlong parcel_native,
                                                               jobject listener) {
    if (!parcel_native) {
        return nullptr;
    }
    auto *parcel = (Parcel *) (intptr_t) parcel_native;
    auto ret = Parcel::getBinary(parcel);

    if (parcel == nullptr && listener != nullptr) {
        jweak store_Wlistener = env->NewWeakGlobalRef(listener);
        jclass ref = env->GetObjectClass(store_Wlistener);
        auto method = env->GetMethodID(ref, "doUpdateNative", "(J)V");
        if (ref != nullptr && method != nullptr) {
            env->CallVoidMethod(store_Wlistener, method, 0ll);
        }
    }
    jbyteArray res;
    if (ret != nullptr) {
        int32_t sz = 0;
        memcpy(&sz, ret, sizeof(int32_t));
        res = env->NewByteArray(sz);
        if (sz > 0) {
            env->SetByteArrayRegion(res, 0, sz, ret + sizeof(int32_t));
        }
        free(ret);
    } else {
        res = nullptr;
    }
    return res;
}
