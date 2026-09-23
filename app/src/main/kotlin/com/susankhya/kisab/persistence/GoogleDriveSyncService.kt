package com.susankhya.kisab.persistence

import com.susankhya.kisab.domain.FarmState
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class GoogleDriveSyncService {

    companion object {
        private const val BACKUP_FILE_NAME = "kisab_farm_backup.json"
        private const val DRIVE_FILES_URL = "https://www.googleapis.com/drive/v3/files"
    }

    fun syncToDrive(accessToken: String, farm: FarmState): String {
        val encodedContent = FarmBackupCodec.encode(farm)
        val fileId = findFileId(accessToken)
        if (fileId != null) {
            updateFile(accessToken, fileId, encodedContent)
        } else {
            createFile(accessToken, encodedContent)
        }
        return encodedContent
    }

    fun restoreFromDrive(accessToken: String): FarmState {
        val fileId = findFileId(accessToken)
            ?: throw FarmBackupException(BackupRejectionReason.UNREADABLE, "Backup file not found in Google Drive")
        val content = downloadFile(accessToken, fileId)
        val envelope = FarmBackupCodec.decode(content)
        return envelope.farm
    }

    private fun findFileId(accessToken: String): String? {
        val query = "name='$BACKUP_FILE_NAME' and trashed=false"
        val url = URL("$DRIVE_FILES_URL?q=${URLEncoder.encode(query, "UTF-8")}")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Authorization", "Bearer $accessToken")
        connection.connect()

        if (connection.responseCode != 200) {
            return null
        }
        val responseText = connection.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        val idIndex = responseText.indexOf("\"id\":")
        if (idIndex == -1) return null
        val startQuote = responseText.indexOf('"', idIndex + 5)
        if (startQuote == -1) return null
        val endQuote = responseText.indexOf('"', startQuote + 1)
        if (endQuote == -1) return null
        return responseText.substring(startQuote + 1, endQuote)
    }

    private fun updateFile(accessToken: String, fileId: String, content: String) {
        val url = URL("https://www.googleapis.com/upload/drive/v3/files/$fileId?uploadType=media")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "PATCH"
        connection.setRequestProperty("Authorization", "Bearer $accessToken")
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        connection.doOutput = true
        OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use { writer ->
            writer.write(content)
        }
        if (connection.responseCode !in 200..299) {
            throw FarmBackupException(BackupRejectionReason.UNREADABLE, "Failed to update backup on Google Drive: ${connection.responseCode}")
        }
    }

    private fun createFile(accessToken: String, content: String) {
        val metaUrl = URL(DRIVE_FILES_URL)
        val metaConn = metaUrl.openConnection() as HttpURLConnection
        metaConn.requestMethod = "POST"
        metaConn.setRequestProperty("Authorization", "Bearer $accessToken")
        metaConn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        metaConn.doOutput = true
        val metaJson = "{\"name\":\"$BACKUP_FILE_NAME\"}"
        OutputStreamWriter(metaConn.outputStream, StandardCharsets.UTF_8).use { it.write(metaJson) }
        if (metaConn.responseCode !in 200..299) {
            throw FarmBackupException(BackupRejectionReason.UNREADABLE, "Failed to create backup file in Google Drive")
        }
        val responseText = metaConn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        val idIndex = responseText.indexOf("\"id\":")
        if (idIndex != -1) {
            val startQuote = responseText.indexOf('"', idIndex + 5)
            val endQuote = responseText.indexOf('"', startQuote + 1)
            if (startQuote != -1 && endQuote != -1) {
                val fileId = responseText.substring(startQuote + 1, endQuote)
                updateFile(accessToken, fileId, content)
            }
        }
    }

    private fun downloadFile(accessToken: String, fileId: String): String {
        val url = URL("$DRIVE_FILES_URL/$fileId?alt=media")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Authorization", "Bearer $accessToken")
        connection.connect()
        if (connection.responseCode != 200) {
            throw FarmBackupException(BackupRejectionReason.UNREADABLE, "Failed to download backup from Google Drive")
        }
        return connection.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
    }
}
