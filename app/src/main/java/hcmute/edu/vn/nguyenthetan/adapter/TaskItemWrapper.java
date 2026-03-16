package hcmute.edu.vn.nguyenthetan.adapter;

import hcmute.edu.vn.nguyenthetan.model.Task;

public class TaskItemWrapper implements InboxItem {
    private Task task;

    public TaskItemWrapper(Task task) {
        this.task = task;
    }

    public Task getTask() {
        return task;
    }

    @Override
    public int getViewType() {
        return TYPE_TASK;
    }
}
