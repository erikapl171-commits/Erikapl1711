package app.liveinterpreter.recognition

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/** Installs one pinned public multilingual Whisper model in private app storage.
 * The .part file is never used for inference; SHA-256 is checked before rename.
 */
object SpeechModelInstaller {
    const val FILE_NAME = "ggml-base.bin"
    private const val EXPECTED_BYTES = 147951465L
    private const val EXPECTED_SHA256 = "60ed5bc3dd14eea856493d334349b405782ddcaf0028d4b5df4088345fba2efe"
    private const val DOWNLOAD_URL = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin"

    fun target(context: Context): File = File(File(context.filesDir, "models"), FILE_NAME)

    fun isInstalled(context: Context): Boolean {
        val f = target(context)
        return f.isFile && f.length() == EXPECTED_BYTES && sha256(f) == EXPECTED_SHA256
    }

    suspend fun install(context: Context, onProgress: (Int) -> Unit) = withContext(Dispatchers.IO) {
        val destination = target(context)
        if (isInstalled(context)) { onProgress(100); return@withContext }
        require(destination.parentFile?.isDirectory == true || destination.parentFile?.mkdirs() == true) {
            "Cannot create private model directory"
        }
        val partial = File(destination.parentFile, "$FILE_NAME.part")
        var connection: HttpURLConnection? = null
        try {
            // Do not use arbitrary external URLs, credentials or insecure HTTP.
            connection = (URL(DOWNLOAD_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = 20_000
                readTimeout = 30_000
                instanceFollowRedirects = true
                setRequestProperty("Accept-Encoding", "identity")
            }
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                error("Model server returned HTTP ${connection.responseCode}")
            }
            if (connection.contentLengthLong > 0 && connection.contentLengthLong != EXPECTED_BYTES) {
                error("Unexpected model download size")
            }
            var total = 0L
            val digest = MessageDigest.getInstance("SHA-256")
            connection.inputStream.use { input ->
                partial.outputStream().buffered().use { output ->
                    val buffer = ByteArray(128 * 1024)
                    while (true) {
                        val size = input.read(buffer)
                        if (size < 0) break
                        total += size
                        if (total > EXPECTED_BYTES) error("Model is larger than expected")
                        output.write(buffer, 0, size)
                        digest.update(buffer, 0, size)
                        onProgress((total * 100L / EXPECTED_BYTES).toInt().coerceIn(0, 99))
                    }
                    output.flush()
                }
            }
            if (total != EXPECTED_BYTES || digest.digest().joinToString("") { "%02x".format(it) } != EXPECTED_SHA256) {
                error("Model failed size or SHA-256 verification")
            }
            if (destination.exists() && !destination.delete()) error("Cannot replace old model")
            if (!partial.renameTo(destination)) error("Cannot activate verified model")
            onProgress(100)
        } catch (t: Throwable) {
            partial.delete()
            throw t
        } finally {
            connection?.disconnect()
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(128 * 1024)
            while (true) {
                val n = input.read(buffer)
                if (n == -1) break
                digest.update(buffer, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
