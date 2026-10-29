#include <jni.h>
#include <vector>
#include <string>
#include <android/log.h>
#include <chrono>
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
    // Low-latency, bounded inference for five-second microphone windows.
    // Do not run a full default-context decoding pass for each short window.
    p.n_threads = 2;
    p.no_context = true;
    p.single_segment = true;
    p.max_tokens = 48;
    p.audio_ctx = 384;
    p.temperature_inc = 0.0f;
    p.print_special = false;
    __android_log_print(ANDROID_LOG_INFO, "LiveInterpreterASR", "Native inference starting: samples=%d", (int)n);
    const auto start = std::chrono::steady_clock::now();
    int err = whisper_full(ctx, p, samples.data(), (int)n);
    const auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(
        std::chrono::steady_clock::now() - start).count();
    __android_log_print(err == 0 ? ANDROID_LOG_INFO : ANDROID_LOG_ERROR,
        "LiveInterpreterASR", "Native inference finished: code=%d elapsedMs=%lld",
        err, static_cast<long long>(duration));
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
