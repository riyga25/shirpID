package by.riyga.shirpid.data.database

import androidx.room.migration.Migration

object DatabaseMigrations {
    @JvmField val MIGRATION_1_2 = Migration(1, 2) { db ->
        db.execSQL("ALTER TABLE records ADD COLUMN chunkDuration INTEGER NOT NULL DEFAULT 800")
    }

    fun getAll() = arrayOf(
        MIGRATION_1_2
    )
}