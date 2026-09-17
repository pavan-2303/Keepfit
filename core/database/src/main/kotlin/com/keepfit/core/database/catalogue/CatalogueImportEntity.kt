package com.keepfit.core.database.catalogue

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "catalogue_imports")
data class CatalogueImportEntity(
    @PrimaryKey val source: String,
    val revision: String,
    val recordCount: Int,
    val importedAt: Long,
)
