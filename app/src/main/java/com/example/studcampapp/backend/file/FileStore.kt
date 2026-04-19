package com.example.studcampapp.backend.file

import com.example.studcampapp.model.FileInfo
import java.io.File
import java.util.UUID
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class StoredFile(
    val fileInfo: FileInfo,
    val file: File,
    val mimeType: String
)

class FileStore(
    private val rootDir: File
) {
    private val mutex = Mutex()
    private val filesById = LinkedHashMap<String, StoredFile>()

    init {
        rootDir.mkdirs()
    }

    suspend fun saveFile(
        originalName: String,
        mimeType: String,
        bytes: ByteArray
    ): FileInfo = mutex.withLock {
        val fileId = UUID.randomUUID().toString()
        val safeName = originalName.ifBlank { "upload.bin" }
        val diskFile = File(rootDir, "${fileId}_$safeName")
        diskFile.writeBytes(bytes)

        val fileInfo = FileInfo(
            id = fileId,
            fileName = safeName,
            size = bytes.size.toLong(),
            fileUrl = "/files/$fileId"
        )
        filesById[fileId] = StoredFile(
            fileInfo = fileInfo,
            file = diskFile,
            mimeType = mimeType.ifBlank { "application/octet-stream" }
        )
        fileInfo
    }

    suspend fun getStoredFile(fileId: String): StoredFile? = mutex.withLock {
        filesById[fileId]
    }

    suspend fun getFileInfo(fileId: String): FileInfo? = mutex.withLock {
        filesById[fileId]?.fileInfo
    }

    suspend fun clear() = mutex.withLock {
        filesById.values.forEach { stored ->
            runCatching { stored.file.delete() }
        }
        filesById.clear()
        runCatching { rootDir.deleteRecursively() }
    }
}

