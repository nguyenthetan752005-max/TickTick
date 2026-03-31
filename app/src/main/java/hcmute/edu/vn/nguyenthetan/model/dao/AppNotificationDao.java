package hcmute.edu.vn.nguyenthetan.model.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.lifecycle.LiveData;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import hcmute.edu.vn.nguyenthetan.model.AppNotification;

@Dao
public interface AppNotificationDao {
    @Insert
    long insert(AppNotification notification);

    @Update
    void update(AppNotification notification);

    @Delete
    void delete(AppNotification notification);

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    LiveData<List<AppNotification>> getAllNotifications();

    @Query("SELECT * FROM notifications WHERE isRead = 0 ORDER BY timestamp DESC")
    LiveData<List<AppNotification>> getUnreadNotifications();
    
    @Query("DELETE FROM notifications WHERE id = :notificationId")
    void deleteById(int notificationId);

    @Query("DELETE FROM notifications")
    void deleteAll();
}
