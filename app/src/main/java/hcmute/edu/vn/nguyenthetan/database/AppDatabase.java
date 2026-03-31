package hcmute.edu.vn.nguyenthetan.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import hcmute.edu.vn.nguyenthetan.model.AppNotification;
import hcmute.edu.vn.nguyenthetan.model.Category;
import hcmute.edu.vn.nguyenthetan.model.Reminder;
import hcmute.edu.vn.nguyenthetan.model.Task;
import hcmute.edu.vn.nguyenthetan.model.dao.AppNotificationDao;
import hcmute.edu.vn.nguyenthetan.model.dao.CategoryDao;
import hcmute.edu.vn.nguyenthetan.model.dao.ReminderDao;
import hcmute.edu.vn.nguyenthetan.model.dao.TaskDao;

@Database(entities = {Category.class, Task.class, Reminder.class, AppNotification.class}, version = 5, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;

    public abstract CategoryDao categoryDao();
    public abstract TaskDao taskDao();
    public abstract ReminderDao reminderDao();
    public abstract AppNotificationDao notificationDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "ticktick_database")
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries()
                    .build();
        }
        return instance;
    }
}