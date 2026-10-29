package app.liveinterpreter.translation

import android.content.Context
import com.google.mlkit.nl.translate.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions

/** ML Kit is free to use, but its first model download requires internet.
 * Explicit no-network policy after model installation is possible only after device testing.
 */
class OnDeviceTranslator(context: Context, language: String) : AutoCloseable {
    private val translator: Translator
    init {
        val source = TranslateLanguage.fromLanguageTag(language)
            ?: throw IllegalArgumentException("Unsupported translation language: $language")
        translator = Translation.getClient(
            TranslatorOptions.Builder().setSourceLanguage(source)
                .setTargetLanguage(TranslateLanguage.ENGLISH).build()
        )
    }
    fun prepare(onReady: () -> Unit, onError: (String) -> Unit) {
        val conditions = DownloadConditions.Builder().requireWifi().build()
        translator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener { onReady() }
            .addOnFailureListener { onError("Translation model unavailable: ${it.message}") }
    }
    fun translate(source: String, onResult: (String) -> Unit, onError: (String) -> Unit) {
        translator.translate(source).addOnSuccessListener(onResult)
            .addOnFailureListener { onError("Translation failed: ${it.message}") }
    }
    override fun close() = translator.close()
}
