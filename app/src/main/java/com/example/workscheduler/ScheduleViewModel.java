package com.example.workscheduler;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import java.util.List;

public class ScheduleViewModel extends AndroidViewModel {
    private ScheduleRepository repository;

    public ScheduleViewModel(Application application) {
        super(application);
        repository = new ScheduleRepository(application);
    }

    public LiveData<List<ScheduleEntity>> getSchedulesForDate(String date) {
        return repository.getSchedulesForDate(date);
    }

    public void insert(ScheduleEntity schedule) {
        repository.insert(schedule);
    }

    public void update(ScheduleEntity schedule) {
        repository.update(schedule);
    }

    public void delete(ScheduleEntity schedule) {
        repository.delete(schedule);
    }

    public void copyFromLastSchedule(String currentDate) {
        repository.copyFromLastSchedule(currentDate);
    }

    public void copyFromDate(String sourceDate, String targetDate) {
        repository.copyFromDate(sourceDate, targetDate);
    }
}
