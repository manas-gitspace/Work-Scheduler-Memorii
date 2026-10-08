package com.example.workscheduler;

import android.app.Application;
import androidx.lifecycle.LiveData;
import java.util.List;

public class ScheduleRepository {
    private ScheduleDao scheduleDao;

    public ScheduleRepository(Application application) {
        TaskDatabase db = TaskDatabase.getDatabase(application);
        scheduleDao = db.scheduleDao();
    }

    public LiveData<List<ScheduleEntity>> getSchedulesForDate(String date) {
        return scheduleDao.getSchedulesForDate(date);
    }

    public void insert(ScheduleEntity schedule) {
        TaskDatabase.databaseWriteExecutor.execute(() -> {
            scheduleDao.insert(schedule);
        });
    }

    public void update(ScheduleEntity schedule) {
        TaskDatabase.databaseWriteExecutor.execute(() -> {
            scheduleDao.update(schedule);
        });
    }

    public void delete(ScheduleEntity schedule) {
        TaskDatabase.databaseWriteExecutor.execute(() -> {
            scheduleDao.delete(schedule);
        });
    }
    
    public void copyFromLastSchedule(String currentDate) {
        TaskDatabase.databaseWriteExecutor.execute(() -> {
            String lastDate = scheduleDao.getLastScheduleDate();
            if (lastDate != null && !lastDate.equals(currentDate)) {
                copyFromDate(lastDate, currentDate);
            }
        });
    }

    public void copyFromDate(String sourceDate, String targetDate) {
        TaskDatabase.databaseWriteExecutor.execute(() -> {
            if (sourceDate != null && !sourceDate.equals(targetDate)) {
                List<ScheduleEntity> sourceSchedules = scheduleDao.getSchedulesForDateSync(sourceDate);
                for (ScheduleEntity old : sourceSchedules) {
                    ScheduleEntity newSchedule = new ScheduleEntity();
                    newSchedule.setTitle(old.getTitle());
                    newSchedule.setDate(targetDate);
                    newSchedule.setStartTimeMillis(old.getStartTimeMillis());
                    newSchedule.setEndTimeMillis(old.getEndTimeMillis());
                    newSchedule.setFullDay(old.isFullDay());
                    newSchedule.setAlarmEnabled(old.isAlarmEnabled());
                    newSchedule.setPriority(old.getPriority());
                    scheduleDao.insert(newSchedule);
                }
            }
        });
    }
}
