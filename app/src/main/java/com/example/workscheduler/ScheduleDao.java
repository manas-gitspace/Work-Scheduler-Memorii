package com.example.workscheduler;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ScheduleDao {

    @Insert
    void insert(ScheduleEntity schedule);

    @Update
    void update(ScheduleEntity schedule);

    @Delete
    void delete(ScheduleEntity schedule);

    @Query("SELECT * FROM schedules ORDER BY startTimeMillis ASC")
    LiveData<List<ScheduleEntity>> getAllSchedules();

    @Query("SELECT * FROM schedules WHERE date = :targetDate ORDER BY startTimeMillis ASC")
    LiveData<List<ScheduleEntity>> getSchedulesForDate(String targetDate);

    // Sync method for copying
    @Query("SELECT * FROM schedules WHERE date = :targetDate ORDER BY startTimeMillis ASC")
    List<ScheduleEntity> getSchedulesForDateSync(String targetDate);
    
    // To find the last scheduled date
    @Query("SELECT date FROM schedules ORDER BY date DESC LIMIT 1")
    String getLastScheduleDate();
}
