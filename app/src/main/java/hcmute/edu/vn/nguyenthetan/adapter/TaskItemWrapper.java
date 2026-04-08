/**
 * TaskItemWrapper: Wrapper class đóng gói Task cho InboxAdapter.
 * Cho phép hiển thị task trong RecyclerView hỗn hợp (hiện chưa sử dụng).
 */
package hcmute.edu.vn.nguyenthetan.adapter;

import hcmute.edu.vn.nguyenthetan.model.Task;

public class TaskItemWrapper implements InboxItem {
    private Task task;

    /**
     * Constructor tạo wrapper từ Task.
     * @param task Đối tượng task cần đóng gói
     */
    public TaskItemWrapper(Task task) {
        this.task = task;
    }

    /**
     * Trả về đối tượng Task gốc.
     * @return Task được đóng gói
     */
    public Task getTask() {
        return task;
    }

    /**
     * Trả về loại view TYPE_TASK.
     * @return InboxItem.TYPE_TASK
     */
    @Override
    public int getViewType() {
        return TYPE_TASK;
    }
}
