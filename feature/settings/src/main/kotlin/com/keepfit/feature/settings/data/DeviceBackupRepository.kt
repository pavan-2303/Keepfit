package com.keepfit.feature.settings.data

import android.content.Context
import android.net.Uri
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SimpleSQLiteQuery
import com.keepfit.core.database.KeepfitDatabase
import com.keepfit.core.database.KeepfitDatabaseFactory
import com.keepfit.core.preferences.AppSettingsRepository
import com.keepfit.core.preferences.MeasurementUnit
import com.keepfit.core.preferences.NutritionTrackingDepth
import com.keepfit.core.preferences.WeightUnit
import java.io.File
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class DeviceBackupRepository @Inject constructor(
    private val context: Context,
    private val database: KeepfitDatabase,
    private val settingsRepository: AppSettingsRepository,
    private val codec: BackupArchiveCodec = BackupArchiveCodec(),
) : BackupRepository {
    override suspend fun exportBackup(destinationUri: Uri, passphrase: String) = withContext(Dispatchers.IO) {
        val workingDirectory = createWorkingDirectory("export")
        try {
            val databaseFile = File(workingDirectory, BackupArchiveCodec.DATABASE_ENTRY)
            exportDatabaseSnapshot(databaseFile)
            val settingsSnapshot = settingsRepository.observeSettings().first().toBackupSnapshot()
            val source = BackupArchiveSource(
                databaseFile = databaseFile,
                settingsSnapshot = settingsSnapshot,
                mediaRootDirectory = File(context.filesDir, BackupArchiveCodec.MEDIA_DIRECTORY).takeIf(File::exists),
                databaseSchemaVersion = KeepfitDatabase.VERSION,
                recordCounts = collectRecordCounts(),
            )
            context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                codec.writeEncryptedArchive(passphrase, source, workingDirectory, outputStream)
            } ?: error("The backup destination could not be opened.")
        } finally {
            workingDirectory.deleteRecursively()
        }
    }

    override suspend fun previewBackup(sourceUri: Uri, passphrase: String): BackupPreview = withContext(Dispatchers.IO) {
        val workingDirectory = createWorkingDirectory("preview")
        try {
            val extracted = context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                codec.extractValidatedArchive(passphrase, inputStream, workingDirectory)
            } ?: error("The selected backup file could not be opened.")
            extracted.manifest.toPreview()
        } finally {
            workingDirectory.deleteRecursively()
        }
    }

    override suspend fun restoreBackup(sourceUri: Uri, passphrase: String) = withContext(Dispatchers.IO) {
        val workingDirectory = createWorkingDirectory("restore")
        val previousSettings = settingsRepository.observeSettings().first().toBackupSnapshot()
        try {
            val extracted = context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                codec.extractValidatedArchive(passphrase, inputStream, workingDirectory)
            } ?: error("The selected backup file could not be opened.")
            val rollbackDirectory = File(workingDirectory, "rollback").apply { mkdirs() }
            val databasePath = context.getDatabasePath(KeepfitDatabaseFactory.DATABASE_NAME)
            val walPath = File(databasePath.parentFile, "${databasePath.name}-wal")
            val shmPath = File(databasePath.parentFile, "${databasePath.name}-shm")
            val currentMediaDirectory = File(context.filesDir, BackupArchiveCodec.MEDIA_DIRECTORY)
            val rollbackDatabase = File(rollbackDirectory, databasePath.name)
            val rollbackWal = File(rollbackDirectory, walPath.name)
            val rollbackShm = File(rollbackDirectory, shmPath.name)
            val rollbackMediaDirectory = File(rollbackDirectory, BackupArchiveCodec.MEDIA_DIRECTORY)

            database.close()
            backupFile(databasePath, rollbackDatabase)
            backupFile(walPath, rollbackWal)
            backupFile(shmPath, rollbackShm)
            backupDirectory(currentMediaDirectory, rollbackMediaDirectory)

            try {
                replaceDatabase(extracted.databaseFile, databasePath, walPath, shmPath)
                replaceMedia(extracted.mediaDirectory, currentMediaDirectory)
                applySettingsSnapshot(extracted.settingsSnapshot)
            } catch (error: Exception) {
                restoreFile(rollbackDatabase, databasePath)
                restoreFile(rollbackWal, walPath)
                restoreFile(rollbackShm, shmPath)
                replaceMedia(rollbackMediaDirectory, currentMediaDirectory)
                applySettingsSnapshot(previousSettings)
                throw error
            }
        } finally {
            workingDirectory.deleteRecursively()
        }
    }

    private fun createWorkingDirectory(prefix: String): File =
        File(context.cacheDir, "keepfit-backup-$prefix-${UUID.randomUUID()}").apply { mkdirs() }

    private fun exportDatabaseSnapshot(destination: File) {
        destination.parentFile?.mkdirs()
        if (destination.exists()) {
            destination.delete()
        }
        val liveDatabasePath = context.getDatabasePath(KeepfitDatabaseFactory.DATABASE_NAME)
        val liveWalPath = File(liveDatabasePath.parentFile, "${liveDatabasePath.name}-wal")
        val liveShmPath = File(liveDatabasePath.parentFile, "${liveDatabasePath.name}-shm")
        val stagedDatabasePath = File(destination.parentFile, "snapshot-source.db")
        val stagedWalPath = File(destination.parentFile, "${stagedDatabasePath.name}-wal")
        val stagedShmPath = File(destination.parentFile, "${stagedDatabasePath.name}-shm")

        liveDatabasePath.copyTo(stagedDatabasePath, overwrite = true)
        if (liveWalPath.exists()) {
            liveWalPath.copyTo(stagedWalPath, overwrite = true)
        }
        if (liveShmPath.exists()) {
            liveShmPath.copyTo(stagedShmPath, overwrite = true)
        }

        val stagedDatabase = SQLiteDatabase.openDatabase(
            stagedDatabasePath.absolutePath,
            null,
            SQLiteDatabase.OPEN_READWRITE,
        )
        try {
            stagedDatabase.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", emptyArray()).close()
        } finally {
            stagedDatabase.close()
        }

        stagedDatabasePath.copyTo(destination, overwrite = true)
    }

    private fun collectRecordCounts(): Map<String, Int> {
        val supportDatabase = database.openHelper.writableDatabase
        return linkedMapOf(
            "profiles" to supportDatabase.countRows("body_profiles"),
            "journeyProfiles" to supportDatabase.countRows("journey_profiles"),
            "foods" to supportDatabase.countRows("foods"),
            "savedMeals" to supportDatabase.countRows("saved_meals"),
            "diaryEntries" to supportDatabase.countRows("food_diary_entries"),
            "mealQualityCheckIns" to supportDatabase.countRows("meal_quality_check_ins"),
            "exercises" to supportDatabase.countRows("exercises"),
            "workoutTemplates" to supportDatabase.countRows("workout_templates"),
            "plannedWorkouts" to supportDatabase.countRows("planned_workouts"),
            "workoutOccurrences" to supportDatabase.countRows("workout_occurrences"),
            "workoutOccurrenceExercises" to supportDatabase.countRows("workout_occurrence_exercises"),
            "workoutSessions" to supportDatabase.countRows("workout_sessions"),
            "weeklyReviewOutcomes" to supportDatabase.countRows("weekly_review_outcomes"),
            "measurements" to supportDatabase.countRows("body_measurements"),
            "transformationCycles" to supportDatabase.countRows("transformation_cycles"),
            "transformationPhotos" to supportDatabase.countRows("transformation_photos"),
        )
    }

    private suspend fun applySettingsSnapshot(snapshot: BackupSettingsSnapshot) {
        settingsRepository.updateUnits(
            weightUnit = WeightUnit.valueOf(snapshot.weightUnit),
            measurementUnit = MeasurementUnit.valueOf(snapshot.measurementUnit),
        )
        settingsRepository.updateRestTimerSeconds(snapshot.restTimerSeconds)
        settingsRepository.updateWeeklyReviewPaused(snapshot.weeklyReviewPaused)
        settingsRepository.updateNutritionTracking(
            depth = runCatching { NutritionTrackingDepth.valueOf(snapshot.nutritionTrackingDepth) }
                .getOrDefault(NutritionTrackingDepth.DETAILED_MACROS),
            targetRangePercent = snapshot.nutritionTargetRangePercent,
        )
        settingsRepository.updateWorkoutReminder(
            enabled = snapshot.workoutReminderEnabled,
            hour = snapshot.workoutReminderHour,
            minute = snapshot.workoutReminderMinute,
        )
        settingsRepository.updateTransformationReminder(
            enabled = snapshot.transformationReminderEnabled,
            dayOfWeekOrdinal = snapshot.transformationReminderDayOfWeek,
            hour = snapshot.transformationReminderHour,
            minute = snapshot.transformationReminderMinute,
        )
    }

    private fun replaceDatabase(source: File, target: File, walPath: File, shmPath: File) {
        target.parentFile?.mkdirs()
        source.copyTo(target, overwrite = true)
        walPath.delete()
        shmPath.delete()
    }

    private fun replaceMedia(sourceDirectory: File, targetDirectory: File) {
        if (targetDirectory.exists()) {
            targetDirectory.deleteRecursively()
        }
        if (!sourceDirectory.exists()) {
            return
        }
        sourceDirectory.walkTopDown().forEach { source ->
            val target = File(targetDirectory, source.relativeTo(sourceDirectory).path)
            if (source.isDirectory) {
                target.mkdirs()
            } else {
                target.parentFile?.mkdirs()
                source.copyTo(target, overwrite = true)
            }
        }
    }

    private fun backupFile(source: File, destination: File) {
        if (!source.exists()) return
        destination.parentFile?.mkdirs()
        source.copyTo(destination, overwrite = true)
    }

    private fun restoreFile(source: File, destination: File) {
        if (!source.exists()) {
            destination.delete()
            return
        }
        destination.parentFile?.mkdirs()
        source.copyTo(destination, overwrite = true)
    }

    private fun backupDirectory(source: File, destination: File) {
        if (!source.exists()) return
        source.walkTopDown().forEach { file ->
            val target = File(destination, file.relativeTo(source).path)
            if (file.isDirectory) {
                target.mkdirs()
            } else {
                target.parentFile?.mkdirs()
                file.copyTo(target, overwrite = true)
            }
        }
    }

    private fun BackupManifest.toPreview(): BackupPreview =
        BackupPreview(
            exportedAtUtcEpochMillis = exportedAtUtcEpochMillis,
            databaseSchemaVersion = databaseSchemaVersion,
            recordCounts = recordCounts,
            mediaSizeBytes = mediaSizeBytes,
        )

    private fun androidx.sqlite.db.SupportSQLiteDatabase.countRows(tableName: String): Int =
        query(SimpleSQLiteQuery("SELECT COUNT(*) FROM $tableName")).use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }

    private fun com.keepfit.core.preferences.AppSettings.toBackupSnapshot(): BackupSettingsSnapshot =
        BackupSettingsSnapshot(
            weightUnit = weightUnit.name,
            measurementUnit = measurementUnit.name,
            restTimerSeconds = restTimerSeconds,
            weeklyReviewPaused = weeklyReviewPaused,
            nutritionTrackingDepth = nutritionTrackingDepth.name,
            nutritionTargetRangePercent = nutritionTargetRangePercent,
            workoutReminderEnabled = workoutReminder.enabled,
            workoutReminderHour = workoutReminder.hour,
            workoutReminderMinute = workoutReminder.minute,
            transformationReminderEnabled = transformationReminder.enabled,
            transformationReminderDayOfWeek = transformationReminder.dayOfWeek.value,
            transformationReminderHour = transformationReminder.hour,
            transformationReminderMinute = transformationReminder.minute,
        )
}
