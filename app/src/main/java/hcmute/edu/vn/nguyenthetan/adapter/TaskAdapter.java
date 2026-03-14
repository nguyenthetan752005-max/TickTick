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

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
        void onTaskLongClick(Task task);
        void onCompleteClick(Task task);
        void onSelectionChanged(int count);
    }

    public TaskAdapter(List<Task> taskList, OnTaskClickListener listener) {
        if (taskList != null) {
            this.taskList = new ArrayList<>(taskList);
        }
        this.listener = listener;
    }

    public void setData(List<Task> tasks) {
        if (tasks == null) {
            this.taskList = new ArrayList<>();
        } else {
            this.taskList = new ArrayList<>(tasks);
        }
        notifyDataSetChanged();
    }

    public void setCanToggleComplete(boolean canToggle) {
        this.canToggleComplete = canToggle;
    }

    public List<Task> getSelectedTasks() { return new ArrayList<>(selectedTasks); }
    
    public void clearSelection() {
        selectedTasks.clear();
        isMultiSelectMode = false;
        notifyDataSetChanged();
        if (listener != null) listener.onSelectionChanged(0);
    }

    public void startMultiSelect() {
        isMultiSelectMode = true;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);
        holder.tvName.setText(task.getName());
        holder.tvDesc.setText(task.getDescription());
        
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        holder.tvDate.setText(sdf.format(new Date(task.getDueDate())));

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

    @Override
    public int getItemCount() {
        return taskList.size();
    }

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
