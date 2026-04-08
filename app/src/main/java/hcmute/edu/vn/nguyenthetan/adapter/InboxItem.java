/**
 * InboxItem: Interface định nghĩa các loại item có thể hiển thị trong Inbox.
 * Hỗ trợ multi-type RecyclerView với Header, Task và Notification.
 */
package hcmute.edu.vn.nguyenthetan.adapter;

public interface InboxItem {
    int TYPE_HEADER = 0;
    int TYPE_TASK = 1;
    int TYPE_NOTIFICATION = 2;

    /**
     * Trả về loại view để RecyclerView biết cần tạo ViewHolder nào.
     * @return TYPE_HEADER, TYPE_TASK hoặc TYPE_NOTIFICATION
     */
    int getViewType();
}
