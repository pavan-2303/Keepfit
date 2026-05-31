package com.keepfit.feature.settings.data

data class BackupManifest(
    val formatVersion: Int,
    val exportedAtUtcEpochMillis: Long,
    val databaseSchemaVersion: Int,
    val recordCounts: Map<String, Int>,
    val mediaSizeBytes: Long,
    val files: List<BackupFileManifestEntry>,
)

data class BackupFileManifestEntry(
    val relativePath: String,
    val sha256: String,
    val sizeBytes: Long,
)

data class BackupSettingsSnapshot(
    val weightUnit: String,
    val measurementUnit: String,
    val restTimerSeconds: Int,
    val workoutReminderEnabled: Boolean,
    val workoutReminderHour: Int,
    val workoutReminderMinute: Int,
    val transformationReminderEnabled: Boolean,
    val transformationReminderDayOfWeek: Int,
    val transformationReminderHour: Int,
    val transformationReminderMinute: Int,
)

data class BackupPreview(
    val exportedAtUtcEpochMillis: Long,
    val databaseSchemaVersion: Int,
    val recordCounts: Map<String, Int>,
    val mediaSizeBytes: Long,
)

internal data class BackupArchiveSource(
    val databaseFile: java.io.File,
    val settingsSnapshot: BackupSettingsSnapshot,
    val mediaRootDirectory: java.io.File?,
    val databaseSchemaVersion: Int,
    val recordCounts: Map<String, Int>,
)

internal data class ExtractedBackup(
    val rootDirectory: java.io.File,
    val manifest: BackupManifest,
    val settingsSnapshot: BackupSettingsSnapshot,
) {
    val databaseFile: java.io.File
        get() = java.io.File(rootDirectory, BackupArchiveCodec.DATABASE_ENTRY)

    val mediaDirectory: java.io.File
        get() = java.io.File(rootDirectory, BackupArchiveCodec.MEDIA_DIRECTORY)
}
