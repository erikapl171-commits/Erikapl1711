#include <jni.h>
#include <vector>
#include <string>
#include "whisper.h"

extern "C" JNIEXPORT jlong JNICALL
Java_app_liveinterpreter_recognition_WhisperBridge_initModel(JNIEnv *env, jobject, jstring path) {
    const char *cpath = env->GetStringUTFChars(path, nullptr);
    whisper_context_params params = whisper_context_default_params();
    whisper_context *ctx = whisper_init_from_file_with_params(cpath, params);
    env->ReleaseStringUTFChars(path, cpath);
    return reinterpret_cast<jlong>(ctx);
}

extern "C" JNIEXPORT jstring JNICALL
Java_app_liveinterpreter_recognition_WhisperBridge_infer(JNIEnv *env, jobject, jlong handle, jshortArray pcm, jstring language) {
    auto *ctx = reinterpret_cast<whisper_context *>(handle);
    if (!ctx || !pcm) return env->NewStringUTF("");
    jsize n = env->GetArrayLength(pcm);
    if (n < 16000) return env->NewStringUTF("");
    std::vector<jshort> audio(n);
    env->GetShortArrayRegion(pcm, 0, n, audio.data());
    std::vector<float> samples(n);
    for (jsize i = 0; i < n; ++i) samples[i] = audio[i] / 32768.0f;
    const char *lang = env->GetStringUTFChars(language, nullptr);
    auto p = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    p.language = lang;
    p.translate = false;
    p.print_progress = false;
    p.print_realtime = false;
    p.print_timestamps = false;
    p.no_timestamps = true;
    p.n_threads = 4;
    int err = whisper_full(ctx, p, samples.data(), (int)n);
    env->ReleaseStringUTFChars(language, lang);
    if (err != 0) return env->NewStringUTF("");
    std::string result;
    const int count = whisper_full_n_segments(ctx);
    for (int i = 0; i < count; ++i) {
        const char *str = whisper_full_get_segment_text(ctx, i);
        if (str) result += str;
    }
    return env->NewStringUTF(result.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_app_liveinterpreter_recognition_WhisperBridge_release(JNIEnv *, jobject, jlong handle) {
    auto *ctx = reinterpret_cast<whisper_context *>(handle);
    if (ctx) whisper_free(ctx);
}
