package com.luckyzero.tacotrainer.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room.databaseBuilder
import androidx.room.RoomDatabase
import com.luckyzero.tacotrainer.models.SegmentInterface
import kotlin.concurrent.Volatile

private const val FORCE_CLEAR = false

@Database(
    entities = [SegmentEntity::class, WorkoutEntity::class],
    version = 1,
    autoMigrations = []
)
abstract class TrainingDatabase : RoomDatabase() {
    abstract fun segmentDao(): SegmentDao
    abstract fun workoutDao(): WorkoutDao

    companion object {
        private const val DB_NAME = "training_db"
        fun createDatabase(context: Context): TrainingDatabase {
            if (FORCE_CLEAR) {
                context.deleteDatabase(DB_NAME)
            }
            return databaseBuilder(
                context.applicationContext,
                TrainingDatabase::class.java,
                DB_NAME
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}

class DbAccess(context: Context) {
    val db by lazy { getDatabase(context) }

    companion object {
        @Volatile
        private var INSTANCE: TrainingDatabase? = null

        fun getDatabase(context: Context): TrainingDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = TrainingDatabase.createDatabase(context)
                INSTANCE = instance
                instance
            }
        }
    }
}