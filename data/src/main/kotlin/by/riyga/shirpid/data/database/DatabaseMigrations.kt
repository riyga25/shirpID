package by.riyga.shirpid.data.database

import androidx.room.migration.Migration

object DatabaseMigrations {
    @JvmField val MIGRATION_1_2 = Migration(1, 2) { db ->
        db.execSQL("ALTER TABLE records ADD COLUMN chunkDuration INTEGER NOT NULL DEFAULT 800")
    }

    @JvmField val MIGRATION_2_3 = Migration(2, 3) { db ->

        // 1. Создать новую таблицу с новой схемой
        db.execSQL("""
            CREATE TABLE records_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                timestamp INTEGER NOT NULL,
                birds TEXT NOT NULL, -- Room сериализует List автоматически
                latitude REAL,
                longitude REAL,
                locationName TEXT,
                audioFilePath TEXT NOT NULL,
                chunkDuration INTEGER NOT NULL
            )
        """.trimIndent())

        // 2. Копировать данные (timestamp -> id)
        db.execSQL("""
            INSERT INTO records_new (id, timestamp, birds, latitude, longitude, locationName, audioFilePath, chunkDuration)
            SELECT timestamp, timestamp, birds, latitude, longitude, locationName, audioFilePath, chunkDuration
            FROM records
        """.trimIndent())

        // 3. Удалить старую, переименовать новую
        db.execSQL("DROP TABLE records")
        db.execSQL("ALTER TABLE records_new RENAME TO records")
    }

    fun getAll() = arrayOf(
        MIGRATION_1_2,
        MIGRATION_2_3
    )
}