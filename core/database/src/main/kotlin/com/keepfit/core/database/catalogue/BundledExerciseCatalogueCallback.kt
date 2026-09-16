package com.keepfit.core.database.catalogue

import android.content.Context
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.gson.Gson
import java.io.InputStreamReader

class BundledExerciseCatalogueCallback(
    context: Context,
) : RoomDatabase.Callback() {
    private val applicationContext = context.applicationContext

    override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        if (db.hasCurrentCatalogueRevision()) {
            return
        }

        val records = applicationContext.assets.open(ASSET_PATH).use { input ->
            InputStreamReader(input, Charsets.UTF_8).use { reader ->
                Gson().fromJson(reader, Array<BundledExerciseRecord>::class.java).toList()
            }
        }
        require(records.size == RECORD_COUNT) {
            "Bundled exercise catalogue is incomplete."
        }
        require(records.all { it.source == SOURCE && it.sourceRevision == REVISION }) {
            "Bundled exercise catalogue provenance does not match the audited revision."
        }

        db.beginTransaction()
        try {
            db.insertRecords(records)
            db.execSQL(
                """
                INSERT OR REPLACE INTO catalogue_imports (source, revision, recordCount, importedAt)
                VALUES (?, ?, ?, ?)
                """.trimIndent(),
                arrayOf<Any>(SOURCE, REVISION, RECORD_COUNT, SOURCE_TIMESTAMP),
            )
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun SupportSQLiteDatabase.hasCurrentCatalogueRevision(): Boolean =
        query(
            "SELECT revision FROM catalogue_imports WHERE source = ? LIMIT 1",
            arrayOf(SOURCE),
        ).use { cursor ->
            cursor.moveToFirst() && cursor.getString(0) == REVISION
        }

    private fun SupportSQLiteDatabase.insertRecords(records: List<BundledExerciseRecord>) {
        records.chunked(INSERT_BATCH_SIZE).forEach { batch ->
            val values = List(batch.size) {
                "(?, ?, ?, ?, NULL, ?, ?, ?, NULL, ?, ?, ?, ?, ?)"
            }.joinToString(", ")
            val insert = compileStatement(
                """
                INSERT OR IGNORE INTO exercises (
                    id, name, muscleGroup, instructions, notes, isBodyweight,
                    createdAt, updatedAt, archivedAt, source, sourceId,
                    equipment, targetMuscle, secondaryMuscles
                ) VALUES $values
                """.trimIndent(),
            )
            var binding = 1
            batch.forEach { record ->
                insert.bindString(binding++, record.id)
                insert.bindString(binding++, record.name)
                insert.bindString(binding++, record.bodyPart)
                insert.bindString(binding++, record.instructions.joinToString("\n"))
                insert.bindLong(binding++, if (record.isBodyweight) 1 else 0)
                insert.bindLong(binding++, SOURCE_TIMESTAMP)
                insert.bindLong(binding++, SOURCE_TIMESTAMP)
                insert.bindString(binding++, record.source)
                insert.bindString(binding++, record.sourceId)
                insert.bindString(binding++, record.equipment)
                insert.bindString(binding++, record.targetMuscle)
                insert.bindString(binding++, record.secondaryMuscles.joinToString(", "))
            }
            insert.executeInsert()
        }
    }

    private data class BundledExerciseRecord(
        val id: String,
        val source: String,
        val sourceRevision: String,
        val sourceId: String,
        val name: String,
        val bodyPart: String,
        val targetMuscle: String,
        val secondaryMuscles: List<String>,
        val equipment: String,
        val instructions: List<String>,
        val isBodyweight: Boolean,
    )

    private companion object {
        const val ASSET_PATH = "catalogue/exercises-v1.json"
        const val SOURCE = "hasaneyldrm/exercises-dataset"
        const val REVISION = "7455efae41b330c265e7cd4b78dfa848e7ce5ebd"
        const val RECORD_COUNT = 1316
        const val INSERT_BATCH_SIZE = 1024
        const val SOURCE_TIMESTAMP = 1_784_184_640_000L
    }
}
