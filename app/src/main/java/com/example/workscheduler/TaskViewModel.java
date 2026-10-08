package com.example.workscheduler;
import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

public class TaskViewModel extends AndroidViewModel {
    private TaskRepository repository;
    private LiveData<List<TaskEntity>> allTasks;

    public TaskViewModel(Application application) {
        super(application);
        repository = new TaskRepository(application);
        allTasks = repository.getAllTasks();
    }

    public LiveData<List<TaskEntity>> getAllTasks() { return allTasks; }

    public void insert(TaskEntity task) { repository.insert(task); }

    public void insertAll(List<TaskEntity> tasks) { repository.insertAll(tasks); }


    public void deleteAll() { repository.deleteAll(); }

    public void delete(TaskEntity task) { repository.delete(task); }

    public void update(TaskEntity task) { repository.update(task); }
}

