package com.example.data.download

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.data.model.AudioQualityLevel
import com.example.data.model.BulkDownloadProgress
import com.example.data.model.BulkDownloadStatus
import com.example.data.model.DownloadState
import com.example.data.model.OfflineTextDownloadState
import com.example.data.model.Reciter
import com.example.data.model.ReciterStorageInfo
import com.example.data.model.Surah
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

data class StorageFileInfo(
    val file: File,
    val reciterId: String,
    val surahNumber: Int,
    val format: String,
    val sizeBytes: Long,
    val formattedSize: String
)

class AudioDownloadManager(context: Context) {

    private val appContext = context.applicationContext
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val _downloadStates = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val downloadStates: StateFlow<Map<String, DownloadState>> = _downloadStates.asStateFlow()

    private val _bulkDownloadProgress = MutableStateFlow(BulkDownloadProgress())
    val bulkDownloadProgress: StateFlow<BulkDownloadProgress> = _bulkDownloadProgress.asStateFlow()

    private val _offlineTextDownloadState = MutableStateFlow(OfflineTextDownloadState())
    val offlineTextDownloadState: StateFlow<OfflineTextDownloadState> = _offlineTextDownloadState.asStateFlow()

    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var bulkJob: Job? = null
    private var isBulkPaused: Boolean = false

    private val audioDir: File by lazy {
        File(appContext.filesDir, "quran_audio").apply {
            if (!exists()) mkdirs()
        }
    }

    init {
        scanExistingDownloads()
    }

    private fun getFileKey(reciterId: String, surahNumber: Int): String {
        return "${reciterId}_${surahNumber}"
    }

    fun getLocalFile(reciterId: String, surahNumber: Int, format: String = "mp3"): File {
        val fileName = "${reciterId}_${String.format("%03d", surahNumber)}.mp3"
        return File(audioDir, fileName)
    }

    fun getExistingDownloadedFile(reciterId: String, surahNumber: Int): File? {
        val mp3File = getLocalFile(reciterId, surahNumber, "mp3")
        if (mp3File.exists() && mp3File.length() > 1024) return mp3File
        return null
    }

    fun isSurahDownloaded(reciterId: String, surahNumber: Int): Boolean {
        return getExistingDownloadedFile(reciterId, surahNumber) != null
    }

    fun isWifiConnected(): Boolean {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    suspend fun probeUrlFileSize(url: String): Long? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url(url).head().build()
            val resp = client.newCall(req).execute()
            if (resp.isSuccessful) {
                val len = resp.header("Content-Length")?.toLongOrNull()
                if (len != null && len > 0) return@withContext len
            }
        } catch (_: Exception) {}
        null
    }

    fun scanExistingDownloads() {
        val currentMap = mutableMapOf<String, DownloadState>()
        if (audioDir.exists()) {
            val files = audioDir.listFiles() ?: emptyArray()
            for (file in files) {
                if (file.isFile && file.extension == "mp3" && file.length() > 1024) {
                    val nameWithoutExt = file.nameWithoutExtension
                    val lastUnderscore = nameWithoutExt.lastIndexOf('_')
                    if (lastUnderscore > 0) {
                        val reciterId = nameWithoutExt.substring(0, lastUnderscore)
                        val surahNumStr = nameWithoutExt.substring(lastUnderscore + 1)
                        val surahNum = surahNumStr.toIntOrNull()
                        if (surahNum != null) {
                            val key = getFileKey(reciterId, surahNum)
                            currentMap[key] = DownloadState(
                                isDownloaded = true,
                                isDownloading = false,
                                progressPercent = 100,
                                localPath = file.absolutePath,
                                format = file.extension,
                                quality = AudioQualityLevel.KBPS_192,
                                fileSize = file.length()
                            )
                        }
                    }
                }
            }
        }
        _downloadStates.value = currentMap
    }

    fun getStorageSummary(): Triple<Int, Long, String> { // (fileCount, totalBytes, formattedString)
        val files = audioDir.listFiles()?.filter { it.isFile && it.length() > 1024 } ?: emptyList()
        val totalBytes = files.sumOf { it.length() }
        val formatted = formatBytes(totalBytes)
        return Triple(files.size, totalBytes, formatted)
    }

    fun getDownloadedFilesList(): List<StorageFileInfo> {
        val files = audioDir.listFiles()?.filter { it.isFile && it.length() > 1024 } ?: emptyList()
        return files.mapNotNull { file ->
            val nameWithoutExt = file.nameWithoutExtension
            val lastUnderscore = nameWithoutExt.lastIndexOf('_')
            if (lastUnderscore > 0) {
                val reciterId = nameWithoutExt.substring(0, lastUnderscore)
                val surahNum = nameWithoutExt.substring(lastUnderscore + 1).toIntOrNull() ?: 0
                StorageFileInfo(
                    file = file,
                    reciterId = reciterId,
                    surahNumber = surahNum,
                    format = file.extension.uppercase(),
                    sizeBytes = file.length(),
                    formattedSize = formatBytes(file.length())
                )
            } else null
        }
    }

    fun clearAllDownloads(): Boolean {
        val files = audioDir.listFiles() ?: emptyArray()
        var allDeleted = true
        for (f in files) {
            if (!f.delete()) allDeleted = false
        }
        _downloadStates.value = emptyMap()
        return allDeleted
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 MB"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(java.util.Locale.US, "%.2f GB", gb)
            mb >= 1.0 -> String.format(java.util.Locale.US, "%.1f MB", mb)
            else -> String.format(java.util.Locale.US, "%.0f KB", kb)
        }
    }

    suspend fun downloadSurah(
        reciterId: String,
        surahNumber: Int,
        audioSources: List<String>,
        preferredFormat: String = "mp3",
        wifiOnly: Boolean = true,
        onProgress: (Int) -> Unit = {},
        onComplete: (Boolean, String?) -> Unit = { _, _ -> }
    ): Boolean = withContext(Dispatchers.IO) {
        if (wifiOnly && !isWifiConnected()) {
            withContext(Dispatchers.Main) {
                onComplete(false, "تم إيقاف التحميل: إعدادات التحميل تتطلب الاتصال بشبكة Wi-Fi")
            }
            return@withContext false
        }

        val key = getFileKey(reciterId, surahNumber)
        val targetFormat = "mp3"
        val targetFile = getLocalFile(reciterId, surahNumber, targetFormat)

        _downloadStates.value = _downloadStates.value.toMutableMap().apply {
            put(key, DownloadState(isDownloaded = false, isDownloading = true, progressPercent = 0, format = targetFormat))
        }

        var downloadSuccess = false
        var savedPath: String? = null

        // Try downloading from the audioSources sequentially (Fallback mechanism)
        for (url in audioSources) {
            var inputStream: InputStream? = null
            var outputStream: FileOutputStream? = null
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36 QuranAudio/1.0")
                    .build()
                val response = client.newCall(request).execute()

                if (response.isSuccessful && response.body != null) {
                    val body = response.body!!
                    val contentLength = body.contentLength()
                    inputStream = body.byteStream()
                    val tempFile = File(audioDir, "${targetFile.name}.tmp")
                    outputStream = FileOutputStream(tempFile)

                    val buffer = ByteArray(32 * 1024)
                    var bytesRead: Int
                    var totalRead = 0L

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        if (contentLength > 0) {
                            val progress = ((totalRead * 100) / contentLength).toInt()
                            withContext(Dispatchers.Main) {
                                onProgress(progress)
                                _downloadStates.value = _downloadStates.value.toMutableMap().apply {
                                    put(key, DownloadState(
                                        isDownloaded = false,
                                        isDownloading = true,
                                        progressPercent = progress,
                                        format = targetFormat,
                                        fileSize = totalRead
                                    ))
                                }
                            }
                        }
                    }
                    outputStream.flush()
                    outputStream.close()
                    outputStream = null
                    inputStream.close()
                    inputStream = null

                    if (tempFile.renameTo(targetFile)) {
                        downloadSuccess = true
                        savedPath = targetFile.absolutePath
                        break
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try { outputStream?.close() } catch (_: Exception) {}
                try { inputStream?.close() } catch (_: Exception) {}
            }
        }

        withContext(Dispatchers.Main) {
            _downloadStates.value = _downloadStates.value.toMutableMap().apply {
                if (downloadSuccess) {
                    put(key, DownloadState(
                        isDownloaded = true,
                        isDownloading = false,
                        progressPercent = 100,
                        localPath = savedPath,
                        format = targetFormat,
                        quality = AudioQualityLevel.KBPS_192,
                        fileSize = targetFile.length()
                    ))
                } else {
                    remove(key)
                }
            }
            onComplete(downloadSuccess, savedPath)
        }

        downloadSuccess
    }

    fun deleteDownloadedSurah(reciterId: String, surahNumber: Int): Boolean {
        val key = getFileKey(reciterId, surahNumber)
        val mp3File = getLocalFile(reciterId, surahNumber, "mp3")
        var deleted = true
        if (mp3File.exists()) deleted = deleted && mp3File.delete()

        _downloadStates.value = _downloadStates.value.toMutableMap().apply {
            remove(key)
        }
        return deleted
    }

    fun deleteReciterDownloads(reciterId: String): Int {
        val files = audioDir.listFiles() ?: emptyArray()
        var deletedCount = 0
        val updatedMap = _downloadStates.value.toMutableMap()
        for (f in files) {
            if (f.isFile && f.name.startsWith("${reciterId}_")) {
                if (f.delete()) {
                    deletedCount++
                }
            }
        }
        // Clean download states for this reciter
        for (surahNum in 1..114) {
            val key = getFileKey(reciterId, surahNum)
            updatedMap.remove(key)
        }
        _downloadStates.value = updatedMap
        return deletedCount
    }

    fun getDownloadedSurahNumbersForReciter(reciterId: String): List<Int> {
        val list = mutableListOf<Int>()
        for (surahNum in 1..114) {
            if (isSurahDownloaded(reciterId, surahNum)) {
                list.add(surahNum)
            }
        }
        return list
    }

    fun getRecitersStorageBreakdown(allReciters: List<Reciter>): List<ReciterStorageInfo> {
        val files = audioDir.listFiles()?.filter { it.isFile && it.length() > 1024 } ?: emptyList()
        val reciterMap = mutableMapOf<String, MutableList<StorageFileInfo>>()

        for (file in files) {
            val nameWithoutExt = file.nameWithoutExtension
            val lastUnderscore = nameWithoutExt.lastIndexOf('_')
            if (lastUnderscore > 0) {
                val reciterId = nameWithoutExt.substring(0, lastUnderscore)
                val surahNum = nameWithoutExt.substring(lastUnderscore + 1).toIntOrNull() ?: 0
                val info = StorageFileInfo(
                    file = file,
                    reciterId = reciterId,
                    surahNumber = surahNum,
                    format = file.extension.uppercase(),
                    sizeBytes = file.length(),
                    formattedSize = formatBytes(file.length())
                )
                reciterMap.getOrPut(reciterId) { mutableListOf() }.add(info)
            }
        }

        return reciterMap.map { (reciterId, fileList) ->
            val matchingReciter = allReciters.find { it.id == reciterId }
            val reciterName = matchingReciter?.name ?: reciterId
            val riwayah = matchingReciter?.riwayah ?: "حفص عن عاصم"
            val totalBytes = fileList.sumOf { it.sizeBytes }
            val surahNumbers = fileList.map { it.surahNumber }.sorted()

            ReciterStorageInfo(
                reciterId = reciterId,
                reciterName = reciterName,
                riwayah = riwayah,
                downloadedSurahsCount = fileList.size,
                totalSurahsCount = 114,
                totalSizeBytes = totalBytes,
                formattedSize = formatBytes(totalBytes),
                downloadedSurahNumbers = surahNumbers
            )
        }.sortedByDescending { it.downloadedSurahsCount }
    }

    fun startBulkDownloadForReciter(
        reciter: Reciter,
        surahs: List<Surah>,
        wifiOnly: Boolean,
        getAudioSources: suspend (Surah, Reciter) -> List<String>,
        onFinished: (Boolean, Int, Int) -> Unit = { _, _, _ -> }
    ) {
        if (bulkJob?.isActive == true) {
            // Already running
            return
        }

        isBulkPaused = false
        val missingSurahs = surahs.filter { !isSurahDownloaded(reciter.id, it.number) }

        if (missingSurahs.isEmpty()) {
            _bulkDownloadProgress.value = BulkDownloadProgress(
                status = BulkDownloadStatus.COMPLETED,
                reciterId = reciter.id,
                reciterName = reciter.name,
                riwayah = reciter.riwayah,
                totalSurahs = surahs.size,
                completedSurahs = surahs.size,
                currentSurahProgressPercent = 100
            )
            onFinished(true, surahs.size, 0)
            return
        }

        val totalSurahsToProcess = surahs.size
        var alreadyDownloadedCount = totalSurahsToProcess - missingSurahs.size
        var failedCount = 0

        _bulkDownloadProgress.value = BulkDownloadProgress(
            status = BulkDownloadStatus.DOWNLOADING,
            reciterId = reciter.id,
            reciterName = reciter.name,
            riwayah = reciter.riwayah,
            totalSurahs = totalSurahsToProcess,
            completedSurahs = alreadyDownloadedCount,
            currentSurahNumber = missingSurahs.first().number,
            currentSurahName = missingSurahs.first().name,
            currentSurahProgressPercent = 0,
            failedSurahsCount = 0
        )

        bulkJob = managerScope.launch {
            try {
                for (surah in missingSurahs) {
                    if (!isActive || isBulkPaused) break

                    if (wifiOnly && !isWifiConnected()) {
                        _bulkDownloadProgress.value = _bulkDownloadProgress.value.copy(
                            status = BulkDownloadStatus.PAUSED,
                            errorMessage = "توقف مؤقت: في انتظار الاتصال بشبكة Wi-Fi"
                        )
                        break
                    }

                    _bulkDownloadProgress.value = _bulkDownloadProgress.value.copy(
                        currentSurahNumber = surah.number,
                        currentSurahName = surah.name,
                        currentSurahProgressPercent = 0
                    )

                    val sources = getAudioSources(surah, reciter)
                    val success = downloadSurah(
                        reciterId = reciter.id,
                        surahNumber = surah.number,
                        audioSources = sources,
                        wifiOnly = wifiOnly,
                        onProgress = { percent ->
                            if (_bulkDownloadProgress.value.status == BulkDownloadStatus.DOWNLOADING) {
                                _bulkDownloadProgress.value = _bulkDownloadProgress.value.copy(
                                    currentSurahProgressPercent = percent
                                )
                            }
                        }
                    )

                    if (success) {
                        alreadyDownloadedCount++
                        _bulkDownloadProgress.value = _bulkDownloadProgress.value.copy(
                            completedSurahs = alreadyDownloadedCount,
                            currentSurahProgressPercent = 100
                        )
                    } else {
                        failedCount++
                        _bulkDownloadProgress.value = _bulkDownloadProgress.value.copy(
                            failedSurahsCount = failedCount
                        )
                    }
                }

                if (!isBulkPaused && isActive) {
                    _bulkDownloadProgress.value = _bulkDownloadProgress.value.copy(
                        status = if (failedCount == 0 || alreadyDownloadedCount > 0) BulkDownloadStatus.COMPLETED else BulkDownloadStatus.ERROR,
                        currentSurahProgressPercent = 100
                    )
                    onFinished(failedCount == 0, alreadyDownloadedCount, failedCount)
                }
            } catch (e: CancellationException) {
                _bulkDownloadProgress.value = _bulkDownloadProgress.value.copy(
                    status = BulkDownloadStatus.CANCELLED
                )
            } catch (e: Exception) {
                _bulkDownloadProgress.value = _bulkDownloadProgress.value.copy(
                    status = BulkDownloadStatus.ERROR,
                    errorMessage = e.message
                )
            }
        }
    }

    fun pauseBulkDownload() {
        isBulkPaused = true
        bulkJob?.cancel()
        bulkJob = null
        _bulkDownloadProgress.value = _bulkDownloadProgress.value.copy(
            status = BulkDownloadStatus.PAUSED
        )
    }

    fun cancelBulkDownload() {
        isBulkPaused = false
        bulkJob?.cancel()
        bulkJob = null
        _bulkDownloadProgress.value = BulkDownloadProgress(
            status = BulkDownloadStatus.CANCELLED
        )
    }

    fun downloadAllQuranTexts(
        surahs: List<Surah>,
        fetcher: suspend (Int) -> Boolean,
        onFinished: (Boolean, Int) -> Unit = { _, _ -> }
    ) {
        if (_offlineTextDownloadState.value.isDownloading) return

        val validSurahs = if (surahs.isNotEmpty()) {
            surahs.distinctBy { it.number }.sortedBy { it.number }
        } else {
            (1..com.example.data.provider.QuranManifest.TOTAL_SURAHS).map {
                Surah(
                    number = it,
                    name = com.example.data.provider.QuranManifest.getSurahNameArabic(it),
                    englishName = "",
                    ayahs = com.example.data.provider.QuranManifest.getCanonicalAyahCount(it),
                    type = ""
                )
            }
        }

        _offlineTextDownloadState.value = OfflineTextDownloadState(
            isDownloading = true,
            totalSurahs = validSurahs.size,
            downloadedSurahs = 0,
            currentSurahName = validSurahs.firstOrNull()?.name ?: "",
            isCompleted = false
        )

        managerScope.launch {
            var count = 0
            for (s in validSurahs) {
                _offlineTextDownloadState.value = _offlineTextDownloadState.value.copy(
                    currentSurahName = s.name
                )
                try {
                    val success = fetcher(s.number)
                    if (success) count++
                } catch (_: Exception) {}
                _offlineTextDownloadState.value = _offlineTextDownloadState.value.copy(
                    downloadedSurahs = count
                )
            }
            _offlineTextDownloadState.value = _offlineTextDownloadState.value.copy(
                isDownloading = false,
                isCompleted = count == validSurahs.size
            )
            onFinished(count == validSurahs.size, count)
        }
    }
}
