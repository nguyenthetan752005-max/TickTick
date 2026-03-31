package hcmute.edu.vn.nguyenthetan.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import java.util.List;

import hcmute.edu.vn.nguyenthetan.database.AppDatabase;
import hcmute.edu.vn.nguyenthetan.model.AppNotification;
import hcmute.edu.vn.nguyenthetan.model.dao.AppNotificationDao;

public class AppNotificationRepository {
    private AppNotificationDao notificationDao;

    public AppNotificationRepository(Context context) {
        notificationDao = AppDatabase.getInstance(context).notificationDao();
    }

    public long addNotification(AppNotification notification) {
        return notificationDao.insert(notification);
    }

    public void updateNotification(AppNotification notification) {
        notificationDao.update(notification);
    }

    public void deleteNotification(AppNotification notification) {
        notificationDao.delete(notification);
    }

    public void deleteAllNotifications() {
        notificationDao.deleteAll();
    }

    public LiveData<List<AppNotification>> getAllNotifications() {
        return notificationDao.getAllNotifications();
    }
}
