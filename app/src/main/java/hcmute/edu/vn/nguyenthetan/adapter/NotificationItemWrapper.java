package hcmute.edu.vn.nguyenthetan.adapter;

import hcmute.edu.vn.nguyenthetan.model.AppNotification;

public class NotificationItemWrapper implements InboxItem {
    private AppNotification notification;

    public NotificationItemWrapper(AppNotification notification) {
        this.notification = notification;
    }

    public AppNotification getNotification() {
        return notification;
    }

    @Override
    public int getViewType() {
        return TYPE_NOTIFICATION;
    }
}
