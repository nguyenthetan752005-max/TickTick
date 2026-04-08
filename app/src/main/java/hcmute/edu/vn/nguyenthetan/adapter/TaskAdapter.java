/**
 * TaskAdapter: Adapter hiển thị danh sách nhiệm vụ trong RecyclerView.
 * Chức năng: Hiển thị thông tin task, hỗ trợ chế độ chọn nhiều (multi-select),
 * đánh dấu hoàn thành, và tương tác click/long-click.
 */
package hcmute.edu.vn.nguyenthetan.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import hcmute.edu.vn.nguyenthetan.R;
import hcmute.edu.vn.nguyenthetan.model.Task;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<Task> taskList = new ArrayList<>();
    private OnTaskClickListener listener;
    private List<Task> selectedTasks = new ArrayList<>();
    private boolean isMultiSelectMode = false;
    private boolean canToggleComplete = true;

    /**
     * Interface callback xử lý các sự kiện tương tác với task.
     */
    public interface OnTaskClickListener {
        void onTaskClick(Task task);
        void onTaskLongClick(Task task);
        void onCompleteClick(Task task);
        void onSelectionChanged(int count);
    }

    /**
     * Constructor khởi tạo adapter.
     * @param taskList Danh sách task ban đầu
     * @param listener Callback khi người dùng tương tác với task
     */
    public TaskAdapter(List<Task> taskList, OnTaskClickListener listener) {
        if (taskList != null) {
            this.taskList = new ArrayList<>(taskList);
        }
        this.listener = listener;
    }

    /**
     * Cập nhật danh sách task và làm mới giao diện.
     * @param tasks Danh sách Task mới
     */
    public void setData(List<Task> tasks) {
        if (tasks == null) {
            this.taskList = new ArrayList<>();
        } else {
            this.taskList = new ArrayList<>(tasks);
        }
        notifyDataSetChanged();
    }

    /**
     * Cho phép hoặc không cho phép đánh dấu hoàn thành task.
     * @param canToggle true để cho phép, false để vô hiệu hóa
     */
    public void setCanToggleComplete(boolean canToggle) {
        this.canToggleComplete = canToggle;
    }

    /**
     * Trả về danh sách các task đang được chọn trong chế độ multi-select.
     * @return List<Task> đã chọn
     */
    public List<Task> getSelectedTasks() { return new ArrayList<>(selectedTasks); }
    
    /**
     * Xóa toàn bộ selection và thoát chế độ multi-select.
     */
    public void clearSelection() {
        selectedTasks.clear();
        isMultiSelectMode = false;
        notifyDataSetChanged();
        if (listener != null) listener.onSelectionChanged(0);
    }

    /**
     * Bắt đầu chế độ chọn nhiều task (multi-select mode).
     */
    public void startMultiSelect() {
        isMultiSelectMode = true;
        notifyDataSetChanged();
    }

    /**
     * Tạo ViewHolder mới bằng cách inflate layout item_task.
     * @param parent ViewGroup cha
     * @param viewType Loại view (không sử dụng)
     * @return TaskViewHolder mới
     */
    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    /**
     * Gắn dữ liệu Task vào ViewHolder.
     * Hiển thị tên, mô tả, hạn chót, checkbox hoàn thành,
     * và xử lý chế độ multi-select.
     * @param holder ViewHolder cần gắn dữ liệu
     * @param position Vị trí trong danh sách
     */
    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);
        holder.tvName.setText(task.getName());
        holder.tvDesc.setText(task.getDescription());
        
        if (task.getDueDate() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            holder.tvDate.setText(sdf.format(new Date(task.getDueDate())));
        } else {
            holder.tvDate.setText("Không có hạn chót");
        }

        if (task.isCompleted()) {
            holder.ivCheckbox.setVisibility(View.GONE);
        } else {
            holder.ivCheckbox.setVisibility(View.VISIBLE);
            holder.ivCheckbox.setImageResource(android.R.drawable.checkbox_off_background);
        }

        holder.ivSelected.setVisibility(selectedTasks.contains(task) ? View.VISIBLE : View.GONE);

        holder.ivCheckbox.setOnClickListener(v -> {
            if (!isMultiSelectMode && canToggleComplete) {
                listener.onCompleteClick(task);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (isMultiSelectMode) {
                toggleSelection(task);
            } else {
                listener.onTaskClick(task);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (!isMultiSelectMode) {
                isMultiSelectMode = true;
                toggleSelection(task);
                listener.onTaskLongClick(task);
            }
            return true;
        });
    }

    /**
     * Chuyển đổi trạng thái chọn/bỏ chọn của một task.
     * Cập nhật UI và thông báo cho listener về số lượng đã chọn.
     * @param task Task cần toggle selection
     */
    private void toggleSelection(Task task) {
        if (selectedTasks.contains(task)) {
            selectedTasks.remove(task);
        } else {
            selectedTasks.add(task);
        }
        notifyDataSetChanged();
        if (listener != null) listener.onSelectionChanged(selectedTasks.size());
        if (selectedTasks.isEmpty()) isMultiSelectMode = false;
    }

    /**
     * Trả về tổng số task trong danh sách.
     * @return Số lượng Task
     */
    @Override
    public int getItemCount() {
        return taskList.size();
    }

    /**
     * ViewHolder chứa các thành phần UI của một item task.
     * Bao gồm: tên, mô tả, ngày hết hạn, checkbox hoàn thành, indicator chọn.
     */
    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDesc, tvDate;
        ImageView ivCheckbox, ivSelected;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvTaskName);
            tvDesc = itemView.findViewById(R.id.tvTaskDesc);
            tvDate = itemView.findViewById(R.id.tvTaskDate);
            ivCheckbox = itemView.findViewById(R.id.ivCheckbox);
            ivSelected = itemView.findViewById(R.id.ivSelected);
        }
    }
}
