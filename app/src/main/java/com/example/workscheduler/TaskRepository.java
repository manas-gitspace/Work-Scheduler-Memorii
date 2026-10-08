package com.example.workscheduler;

import android.app.Application;
import android.content.Context;

import androidx.lifecycle.LiveData;

import java.util.List;

public class TaskRepository {
    private final TaskDao taskDao;
    private final LiveData<List<TaskEntity>> allTasks;

    // ✅ Constructor for Application (used in ViewModels, Activities, etc.)
    public TaskRepository(Application application) {
        TaskDatabase db = TaskDatabase.getDatabase(application);
        taskDao = db.taskDao();
        allTasks = taskDao.getAllTasks();
    }

    // ✅ Constructor for Context (used in BroadcastReceivers, Services, etc.)
    public TaskRepository(Context context) {
        TaskDatabase db = TaskDatabase.getDatabase(context.getApplicationContext());
        taskDao = db.taskDao();
        allTasks = taskDao.getAllTasks();
    }

    public LiveData<List<TaskEntity>> getAllTasks() {
        return allTasks;
    }

    public void insert(TaskEntity task) {
        TaskDatabase.databaseWriteExecutor.execute(() -> taskDao.insert(task));
    }

    public void insertAll(List<TaskEntity> tasks) {
        TaskDatabase.databaseWriteExecutor.execute(() -> taskDao.insertAll(tasks));
    }

    public void deleteAll() {
        TaskDatabase.databaseWriteExecutor.execute(taskDao::deleteAll);
    }

    public void delete(TaskEntity task) {
        TaskDatabase.databaseWriteExecutor.execute(() -> taskDao.delete(task));
    }

    public void update(TaskEntity task) {
        TaskDatabase.databaseWriteExecutor.execute(() -> taskDao.update(task));
    }

    // ✅ Sync fetch (used in NotificationReceiver)
    public TaskEntity getTaskByIdSync(int taskId) {
        return taskDao.getTaskById(taskId);
    }
}
