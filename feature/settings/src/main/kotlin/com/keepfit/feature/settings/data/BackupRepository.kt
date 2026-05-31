package com.keepfit.feature.settings.data

import android.net.Uri

interface BackupRepository {
    suspend fun exportBackup(destinationUri: Uri, passphrase: String)

    suspend fun previewBackup(sourceUri: Uri, passphrase: String): BackupPreview

    suspend fun restoreBackup(sourceUri: Uri, passphrase: String)
}
