package com.example.workscheduler;
import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {TaskEntity.class, ScheduleEntity.class}, version = 4, exportSchema = false)
public abstract class TaskDatabase extends RoomDatabase {
    private static volatile TaskDatabase INSTANCE;

    public abstract TaskDao taskDao();
    public abstract ScheduleDao scheduleDao();

    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE tasks ADD COLUMN alarmEnabled INTEGER NOT NULL DEFAULT 1");
            database.execSQL("ALTER TABLE tasks ADD COLUMN alarmRingtoneUri TEXT");
        }
    };

    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `schedules` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT, `date` TEXT, `startTimeMillis` INTEGER NOT NULL, `endTimeMillis` INTEGER NOT NULL, `isFullDay` INTEGER NOT NULL, `alarmEnabled` INTEGER NOT NULL, `priority` TEXT)");
        }
    };

    public static TaskDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (TaskDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    TaskDatabase.class, "task_database")
                            .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;

    }
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(4);
}

