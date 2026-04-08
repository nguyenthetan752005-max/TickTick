/**
 * NotificationItemWrapper: Wrapper class đóng gói AppNotification cho InboxAdapter.
 * Cho phép hiển thị thông báo trong RecyclerView hỗn hợp cùng với Header.
 */
package hcmute.edu.vn.nguyenthetan.adapter;

import hcmute.edu.vn.nguyenthetan.model.AppNotification;

public class NotificationItemWrapper implements InboxItem {
    private AppNotification notification;

    /**
     * Constructor tạo wrapper từ AppNotification.
     * @param notification Đối tượng thông báo cần đóng gói
     */
    public NotificationItemWrapper(AppNotification notification) {
        this.notification = notification;
    }

    /**
     * Trả về đối tượng AppNotification gốc.
     * @return AppNotification được đóng gói
     */
    public AppNotification getNotification() {
        return notification;
    }

    /**
     * Trả về loại view TYPE_NOTIFICATION.
     * @return InboxItem.TYPE_NOTIFICATION
     */
    @Override
    public int getViewType() {
        return TYPE_NOTIFICATION;
    }
}
